package com.gogogo.game.game;

import com.gogogo.game.engine.Rng;

/** The checkered floating grid of colored tiles. */
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
    }

    public final int n;
    public final float half;
    public final Tile[] tiles;

    public Arena(int n) {
        this.n = n;
        this.half = n * PITCH / 2f;
        tiles = new Tile[n * n];
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                Tile t = new Tile();
                t.gx = gx;
                t.gz = gz;
                t.index = gz * n + gx;
                t.x = -half + (gx + 0.5f) * PITCH;
                t.z = -half + (gz + 0.5f) * PITCH;
                tiles[t.index] = t;
            }
        }
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
            if (t.color != target) {
                t.color = target;
                count++;
            }
        }
    }

    public int countColor(int c) {
        int k = 0;
        for (Tile t : tiles) if (t.color == c) k++;
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
