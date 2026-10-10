package com.gogogo.game.game;

import com.gogogo.game.engine.Rng;

/** The floating grid of colored tiles. Maps leave some cells empty (no tile, ever). */
public final class Arena {
    public static final int PRESENT = 0, FALLING = 1, GONE = 2, SPAWNING = 3;
    public static final float PITCH = 4.4f;
    public static final float GRAVITY = 34f;

    public static final class Tile {
        public int gx, gz, index;
        public float x, z;
        public int color;
        public int state = PRESENT;
        public float y, vy;
        public float rotX, rotZ, spinX, spinZ;
        public float delay;      // seconds until falling / spawning starts
        public float squash;     // bounce animation 0..1
        public float flip;       // color-change flip animation 0..1
        public int prevColor;
        public int crowd;        // number of cars heading here (bot planning)
        /** False for empty cells of the map: never present, never colored, never respawned. */
        public boolean exists = true;
        /** Next to the map's outline or a hole (bots avoid these a bit). */
        public boolean edge;
    }

    public final int n;
    public final float half;
    public final Tile[] tiles;
    /** Number of cells that have a tile. */
    public final int count;
    /** True when every cell has a tile (a plain square: every straight line stays on the floor). */
    public final boolean full;
    /** Distance from the middle to the farthest tile corner (for framing cameras). */
    public final float reach;

    // bot navigation around empty cells (maps with holes only): dist[slot[to] * count + slot[from]] is the walk
    // from one tile to another in cells, filled in by one breadth-first search per goal tile the first time it's needed
    private final int[] slot;
    private final short[] dist;
    private final boolean[] searched;
    private final int[] queue;

    public Arena(int n) {
        this(n, null);
    }

    /** mask[gz * n + gx] tells which cells have a tile (null = all). */
    public Arena(int n, boolean[] mask) {
        this.n = n;
        this.half = n * PITCH / 2f;
        tiles = new Tile[n * n];
        int c = 0;
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                Tile t = new Tile();
                t.gx = gx;
                t.gz = gz;
                t.index = gz * n + gx;
                t.x = -half + (gx + 0.5f) * PITCH;
                t.z = -half + (gz + 0.5f) * PITCH;
                t.exists = mask == null || mask[t.index];
                if (!t.exists) t.state = GONE;
                else c++;
                tiles[t.index] = t;
            }
        }
        count = c;
        full = c == n * n;
        float far = 0f;
        for (Tile t : tiles) {
            if (!t.exists) continue;
            t.edge = !has(t.gx - 1, t.gz) || !has(t.gx + 1, t.gz) || !has(t.gx, t.gz - 1) || !has(t.gx, t.gz + 1);
            float cx = Math.abs(t.x) + PITCH / 2, cz = Math.abs(t.z) + PITCH / 2;
            far = Math.max(far, cx * cx + cz * cz);
        }
        reach = (float) Math.sqrt(far);
        if (full) {
            slot = null;
            dist = null;
            searched = null;
            queue = null;
        } else {
            slot = new int[tiles.length];
            int k = 0;
            for (int i = 0; i < tiles.length; i++) slot[i] = tiles[i].exists ? k++ : -1;
            dist = new short[count * count];
            searched = new boolean[count];
            queue = new int[count];
        }
    }

    /** Offset of the walks to tile to in dist[] (searched once, then kept: the holes of a map never change). */
    private int searchTo(int to) {
        int base = slot[to] * count;
        if (searched[slot[to]]) return base;
        searched[slot[to]] = true;
        for (int i = 0; i < count; i++) dist[base + i] = -1;
        dist[base + slot[to]] = 0;
        queue[0] = to;
        int head = 0, tail = 1;
        while (head < tail) {
            int i = queue[head++];
            int x = i % n, z = i / n;
            short d = (short) (dist[base + slot[i]] + 1);
            if (x > 0) tail = visit(base, i - 1, d, tail);
            if (x < n - 1) tail = visit(base, i + 1, d, tail);
            if (z > 0) tail = visit(base, i - n, d, tail);
            if (z < n - 1) tail = visit(base, i + n, d, tail);
        }
        return base;
    }

    private int visit(int base, int i, short d, int tail) {
        int s = slot[i];
        if (s < 0 || dist[base + s] >= 0) return tail;
        dist[base + s] = d;
        queue[tail] = i;
        return tail + 1;
    }

    /**
     * Cells to walk from one tile to another over tiles (4-neighbors), -1 if there is no way.
     * Asking many tiles about the same tile costs a single search.
     */
    public int steps(int from, int to) {
        Tile a = tiles[from], b = tiles[to];
        if (!a.exists || !b.exists) return -1;
        if (full) return Math.abs(a.gx - b.gx) + Math.abs(a.gz - b.gz);
        // walks are the same both ways: use a search that's already done
        if (searched[slot[from]] && !searched[slot[to]]) return dist[slot[from] * count + slot[to]];
        return dist[searchTo(to) + slot[from]];
    }

    /** The neighbor of tile from that is one step closer to tile to (the one most in line with to on ties), or -1. */
    public int nextStep(int from, int to) {
        if (from == to) return to;
        if (!full && tiles[to].exists) searchTo(to);
        int d = steps(from, to);
        if (d <= 0) return -1;
        Tile a = tiles[from], b = tiles[to];
        float dx = b.gx - a.gx, dz = b.gz - a.gz;
        int best = -1;
        float bestAlign = -1e9f;
        for (int k = 0; k < 4; k++) {
            int ox = k == 0 ? 1 : k == 1 ? -1 : 0, oz = k == 2 ? 1 : k == 3 ? -1 : 0;
            if (!has(a.gx + ox, a.gz + oz)) continue;
            int j = from + oz * n + ox;
            if (steps(j, to) != d - 1) continue;
            float align = ox * dx + oz * dz;
            if (align > bestAlign) {
                bestAlign = align;
                best = j;
            }
        }
        return best;
    }

    /**
     * True if a strip along the segment, margin wide on each side, stays over cells that have tiles in this map
     * (only the map's empty cells count: tiles that fall or drop back in don't).
     */
    public boolean clearLine(float x0, float z0, float x1, float z1, float margin) {
        if (full) {
            // a plain square: inside if both ends are
            float lim = half - margin;
            return Math.abs(x0) <= lim && Math.abs(z0) <= lim && Math.abs(x1) <= lim && Math.abs(z1) <= lim;
        }
        float dx = x1 - x0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3f) {
            return hasAt(x0, z0) && hasAt(x0 + margin, z0) && hasAt(x0 - margin, z0) && hasAt(x0, z0 + margin) && hasAt(x0, z0 - margin);
        }
        float px = -dz / len * margin, pz = dx / len * margin;
        int samples = (int) (len / 0.7f) + 1; // well under a cell apart: no hole slips between two samples
        for (int i = 0; i <= samples; i++) {
            float k = i / (float) samples;
            float x = x0 + dx * k, z = z0 + dz * k;
            if (!hasAt(x, z) || !hasAt(x + px, z + pz) || !hasAt(x - px, z - pz)) return false;
        }
        return true;
    }

    /** True if the point is over a cell that has a tile in this map (whatever the tile is doing right now). */
    public boolean hasAt(float x, float z) {
        int gx = (int) Math.floor((x + half) / PITCH);
        int gz = (int) Math.floor((z + half) / PITCH);
        return has(gx, gz);
    }

    /** True if the cell is inside the grid and has a tile in this map. */
    public boolean has(int gx, int gz) {
        return gx >= 0 && gz >= 0 && gx < n && gz < n && tiles[gz * n + gx].exists;
    }

    /** The existing tile closest to a point (for spawning near the middle of maps with a hole there). */
    public Tile nearest(float x, float z) {
        return nearest(x, z, false);
    }

    /** Like nearest(x, z); inner skips tiles at the outline or next to a hole (unless there are no others). */
    public Tile nearest(float x, float z, boolean inner) {
        Tile best = null;
        float bd = Float.MAX_VALUE;
        for (Tile t : tiles) {
            if (!t.exists || (inner && t.edge)) continue;
            float d = (t.x - x) * (t.x - x) + (t.z - z) * (t.z - z);
            if (d < bd) {
                bd = d;
                best = t;
            }
        }
        return best == null && inner ? nearest(x, z, false) : best;
    }

    /** Tile whose cell contains the point, or null outside the grid. */
    public Tile cellAt(float x, float z) {
        int gx = (int) Math.floor((x + half) / PITCH);
        int gz = (int) Math.floor((z + half) / PITCH);
        if (gx < 0 || gz < 0 || gx >= n || gz >= n) return null;
        return tiles[gz * n + gx];
    }

    /** True if a car centered here is held up by a tile. */
    public boolean supported(float x, float z) {
        Tile t = cellAt(x, z);
        // tiles dropping back in already count as floor, so nobody falls into a hole that is about to close
        return t != null && (t.state == PRESENT || t.state == SPAWNING);
    }

    /** Randomly recolors all tiles; target gets roughly pTarget share (at least minTarget tiles). */
    public void assignColors(Rng rng, int numColors, int target, float pTarget, int minTarget) {
        int count = 0;
        for (Tile t : tiles) {
            if (!t.exists) continue;
            t.prevColor = t.color;
            if (rng.chance(pTarget)) {
                t.color = target;
                count++;
            } else {
                int c = rng.i(numColors - 1);
                t.color = c >= target ? c + 1 : c;
                if (t.color >= numColors) t.color = (target + 1) % numColors;
            }
        }
        int guard = 0;
        while (count < minTarget && guard++ < 1000) {
            Tile t = tiles[rng.i(tiles.length)];
            if (t.exists && t.color != target) {
                t.color = target;
                count++;
            }
        }
    }

    public int countColor(int c) {
        int k = 0;
        for (Tile t : tiles) if (t.exists && t.color == c) k++;
        return k;
    }

    public void update(float dt) {
        for (Tile t : tiles) {
            if (t.squash > 0) t.squash = Math.max(0f, t.squash - dt * 2.8f);
            if (t.flip > 0) t.flip = Math.max(0f, t.flip - dt * 3.5f);
            switch (t.state) {
                case FALLING:
                    if (t.delay > 0) {
                        t.delay -= dt;
                        break;
                    }
                    t.vy -= GRAVITY * dt;
                    t.y += t.vy * dt;
                    t.rotX += t.spinX * dt;
                    t.rotZ += t.spinZ * dt;
                    if (t.y < -90f) t.state = GONE;
                    break;
                case SPAWNING:
                    if (t.delay > 0) {
                        t.delay -= dt;
                        break;
                    }
                    t.vy -= GRAVITY * 1.6f * dt;
                    t.y += t.vy * dt;
                    if (t.y <= 0f) {
                        t.y = 0f;
                        t.vy = 0f;
                        t.state = PRESENT;
                        t.squash = 1f;
                        landed++;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** Count of tiles that landed since last reset (for sound throttling). */
    public int landed;

    public void dropAllBut(Rng rng, int keep) {
        for (Tile t : tiles) {
            if (t.state == PRESENT && t.color != keep) {
                t.state = FALLING;
                t.delay = rng.range(0f, 0.18f);
                t.vy = rng.range(0f, 2f);
                t.spinX = rng.range(-1.2f, 1.2f);
                t.spinZ = rng.range(-1.2f, 1.2f);
            }
        }
    }

    /** Brings every missing tile back, dropping in from the sky in a ripple. */
    public void respawn(Rng rng, float centerX, float centerZ) {
        for (Tile t : tiles) {
            if (!t.exists) continue;
            if (t.state == PRESENT) {
                t.flip = 1f;
                continue;
            }
            t.state = SPAWNING;
            float d = (float) Math.sqrt((t.x - centerX) * (t.x - centerX) + (t.z - centerZ) * (t.z - centerZ));
            t.delay = d * 0.006f + rng.range(0f, 0.08f);
            t.y = 14f + rng.range(0f, 5f);
            t.vy = -8f;
            t.rotX = t.rotZ = 0f;
        }
    }

    /** Instantly restores all tiles (new match). */
    public void reset() {
        for (Tile t : tiles) {
            if (!t.exists) continue;
            t.state = PRESENT;
            t.y = 0;
            t.vy = 0;
            t.rotX = t.rotZ = 0;
            t.delay = 0;
            t.squash = 0;
            t.flip = 0;
        }
    }
}
