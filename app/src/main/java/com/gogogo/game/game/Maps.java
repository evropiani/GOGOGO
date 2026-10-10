package com.gogogo.game.game;

/** Arena layouts: which cells of an n x n grid have a tile. */
public final class Maps {
    private Maps() {}

    public static final String[] NAME = {
            "CLASSIC", "DONUT", "PLUS", "DIAMOND", "SWISS CHEESE", "ISLANDS", "HEART", "STAR", "BULLSEYE", "MEGA GRID"};
    public static final String[] BLURB = {
            "The original checkered chaos.",
            "Mind the hole in the middle.",
            "Four arms, zero hugs.",
            "Pointy on every side.",
            "Holey moley!",
            "Four islands, four tiny bridges.",
            "Love hurts. So does falling.",
            "Shine bright, fall hard.",
            "Rings, spokes and nowhere to hide.",
            "Huge. Roomy. Still not safe."};
    public static final int[] RULE = {
            Items.FREE, Items.LEVEL, Items.LEVEL, Items.LEVEL, Items.LEVEL, Items.LEVEL, Items.LEVEL, Items.LEVEL, Items.LEVEL, Items.LEVEL};
    public static final int[] VALUE = {0, 3, 8, 14, 20, 28, 36, 45, 55, 65};
    public static final int COUNT = NAME.length;

    private static final int[] SIZE = {15, 17, 17, 19, 15, 17, 19, 21, 19, 19};

    // swiss cheese holes as x, z, kind (0 = one cell, 1 = 2x2, 2 = plus): two tiles or more between holes and to the rim
    private static final int[] CHEESE = {2, 2, 1, 7, 2, 0, 11, 3, 2, 5, 6, 0, 9, 7, 1, 3, 10, 2, 12, 11, 0, 7, 11, 1};

    /** Grid side length. */
    public static int size(int id) {
        return SIZE[id];
    }

    /** Cells that have a tile, indexed gz * n + gx. */
    public static boolean[] mask(int id) {
        int n = SIZE[id];
        boolean[] m = new boolean[n * n];
        float c = (n - 1) / 2f;
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                float dx = gx - c, dz = gz - c;
                float r = (float) Math.sqrt(dx * dx + dz * dz);
                boolean on;
                switch (id) {
                    case 1: // donut
                        on = r <= 8.4f && r >= 2.3f;
                        break;
                    case 2: // plus
                        on = Math.abs(dx) <= 3 || Math.abs(dz) <= 3;
                        break;
                    case 3: // diamond
                        on = Math.abs(dx) + Math.abs(dz) <= 9;
                        break;
                    case 5: { // four 7x7 islands joined by 3-wide bridges
                        boolean ix = gx <= 6 || gx >= 10, iz = gz <= 6 || gz >= 10;
                        boolean bridgeX = gx >= 7 && gx <= 9 && ((gz >= 2 && gz <= 4) || (gz >= 12 && gz <= 14));
                        boolean bridgeZ = gz >= 7 && gz <= 9 && ((gx >= 2 && gx <= 4) || (gx >= 12 && gx <= 14));
                        on = (ix && iz) || bridgeX || bridgeZ;
                        break;
                    }
                    case 6: { // heart: two round lobes with a dip between them, and a v down to the tip
                        float lx = Math.abs(dx) - 4f, lz = dz + 3.3f;
                        on = lx * lx + lz * lz <= 4.9f * 4.9f || (lz >= 0f && Math.abs(dx) <= (8.6f - dz) * (8.9f / 11.9f));
                        break;
                    }
                    case 7: // five-pointed star
                        on = inStar(dx, dz, 11f, 5.8f);
                        break;
                    case 8: // bullseye: middle and two rings (all 2+ tiles wide), diagonal links inside, 3-wide spokes outside
                        on = r <= 2.0f || (r >= 3.3f && r <= 5.6f) || (r >= 6.9f && r <= 9.2f)
                                || (r <= 6.1f && Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1)
                                || (r > 3.3f && r <= 9.2f && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1));
                        break;
                    default:
                        on = true;
                        break;
                }
                m[gz * n + gx] = on;
            }
        }
        if (id == 4) {
            // swiss cheese: holes of a few sizes, the middle stays solid
            for (int i = 0; i < CHEESE.length; i += 3) {
                int x = CHEESE[i], z = CHEESE[i + 1], kind = CHEESE[i + 2];
                m[z * n + x] = false;
                if (kind == 1) {
                    m[z * n + x + 1] = false;
                    m[(z + 1) * n + x] = false;
                    m[(z + 1) * n + x + 1] = false;
                } else if (kind == 2) {
                    m[z * n + x - 1] = false;
                    m[z * n + x + 1] = false;
                    m[(z - 1) * n + x] = false;
                    m[(z + 1) * n + x] = false;
                }
            }
        }
        cleanUp(m, n);
        return m;
    }

    /** Shaves off one-cell spikes and keeps only the biggest connected piece. */
    private static void cleanUp(boolean[] m, int n) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < m.length; i++) {
                if (m[i] && neighbors(m, n, i) < 2) {
                    m[i] = false;
                    changed = true;
                }
            }
        }
        int[] comp = new int[m.length];
        int[] queue = new int[m.length];
        int best = 0, bestSize = 0, id = 0;
        for (int s = 0; s < m.length; s++) {
            if (!m[s] || comp[s] != 0) continue;
            id++;
            int head = 0, tail = 0, size = 0;
            queue[tail++] = s;
            comp[s] = id;
            while (head < tail) {
                int i = queue[head++];
                size++;
                int x = i % n, z = i / n;
                for (int k = 0; k < 4; k++) {
                    int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), nz = z + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (nx < 0 || nz < 0 || nx >= n || nz >= n) continue;
                    int j = nz * n + nx;
                    if (m[j] && comp[j] == 0) {
                        comp[j] = id;
                        queue[tail++] = j;
                    }
                }
            }
            if (size > bestSize) {
                bestSize = size;
                best = id;
            }
        }
        for (int i = 0; i < m.length; i++) if (m[i] && comp[i] != best) m[i] = false;
    }

    private static int neighbors(boolean[] m, int n, int i) {
        int x = i % n, z = i / n, k = 0;
        if (x > 0 && m[i - 1]) k++;
        if (x < n - 1 && m[i + 1]) k++;
        if (z > 0 && m[i - n]) k++;
        if (z < n - 1 && m[i + n]) k++;
        return k;
    }

    private static boolean inStar(float x, float z, float ro, float ri) {
        // point-in-polygon against a 10-vertex star pointing "up" (-z)
        float[] px = new float[10], pz = new float[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float r = i % 2 == 0 ? ro : ri;
            px[i] = (float) Math.cos(a) * r;
            pz[i] = (float) Math.sin(a) * r;
        }
        boolean in = false;
        for (int i = 0, j = 9; i < 10; j = i++) {
            if ((pz[i] > z) != (pz[j] > z) && x < (px[j] - px[i]) * (z - pz[i]) / (pz[j] - pz[i]) + px[i]) in = !in;
        }
        return in;
    }

    private static boolean[][] masks;

    /** Draws the map's outline as a little grid of squares centered at (cx, cy), fitting in size x size. */
    public static void drawPreview(com.gogogo.game.engine.UIBatch b, int id, float cx, float cy, float size, int color) {
        if (masks == null) {
            masks = new boolean[COUNT][];
            for (int i = 0; i < COUNT; i++) masks[i] = mask(i);
        }
        int n = SIZE[id];
        boolean[] m = masks[id];
        float cell = size / n;
        float x0 = cx - size / 2, y0 = cy - size / 2;
        float r = cell * 0.22f;
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                if (!m[gz * n + gx]) continue;
                b.roundRect(x0 + gx * cell + cell * 0.08f, y0 + gz * cell + cell * 0.08f, cell * 0.84f, cell * 0.84f, r, color);
            }
        }
    }

    /** Picks the map for the next match: the chosen one, or a random owned one when shuffling. */
    public static int pick(Save s, com.gogogo.game.engine.Rng rng) {
        if (s.map >= 0 && s.map < COUNT && s.mapOwned[s.map]) return s.map;
        int owned = 0;
        for (int i = 0; i < COUNT; i++) if (s.mapOwned[i]) owned++;
        if (owned == 0) return 0;
        int k = rng.i(owned);
        for (int i = 0; i < COUNT; i++) {
            if (s.mapOwned[i] && k-- == 0) return i;
        }
        return 0;
    }
}
