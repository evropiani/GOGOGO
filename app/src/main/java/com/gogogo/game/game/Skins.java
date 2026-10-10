package com.gogogo.game.game;

import com.gogogo.game.engine.MeshBuilder;

/**
 * Arena skins: the look of the tiles. The top face always shows the tile's gameplay color
 * (PRIMARY slot); skins add frames, studs, sprinkles and other decoration around it.
 * Up to 361 tiles are drawn every frame, so each skin stays within roughly 1000 triangles.
 */
public final class Skins {
    private Skins() {}

    public static final String[] NAME = {
            "CLASSIC", "CANDY", "COOKIE", "BRICKS", "JELLY", "NEON", "FROSTY", "PIXEL", "CRATES", "GOLDEN GLORY"};
    public static final String[] BLURB = {
            "Shiny plastic, maximum bounce.",
            "Sweet stripes, sticky tires.",
            "Fresh from the oven.",
            "Step on them barefoot. We dare you.",
            "Wobbly and proud of it.",
            "Glows in the dark.",
            "Brrrrr-illiant.",
            "Retro blocks, modern crashes.",
            "Handle with care.",
            "Only for true legends."};
    public static final int[] RULE = {
            Items.FREE, Items.COINS, Items.LEVEL, Items.LEVEL, Items.COINS, Items.LEVEL, Items.LEVEL, Items.RIMS, Items.LEVEL, Items.LEVEL};
    public static final int[] VALUE = {0, 800, 10, 18, 1500, 32, 42, 120, 60, 100};
    public static final int COUNT = NAME.length;
    public static final int CLASSIC = 0, CANDY = 1, COOKIE = 2, BRICKS = 3, JELLY = 4, NEON = 5, FROSTY = 6, PIXEL = 7,
            CRATES = 8, GOLDEN = 9;

    private static final float H = Art.TILE / 2f;      // half footprint
    private static final float PI = (float) Math.PI;

    /**
     * Builds one tile for a skin: a TILE x TILE_H x TILE block whose top face sits at y = 0.
     * Use the PRIMARY slot for the gameplay color. The middle (about 2.2 x 2.2) stays flat for the symbols.
     */
    public static void build(MeshBuilder m, int id) {
        switch (id) {
            case CANDY: candy(m); break;
            case COOKIE: cookie(m); break;
            case BRICKS: bricks(m); break;
            case JELLY: jelly(m); break;
            case NEON: neon(m); break;
            case FROSTY: frosty(m); break;
            case PIXEL: pixel(m); break;
            case CRATES: crates(m); break;
            case GOLDEN: golden(m); break;
            default:
                m.twoTone(0xFFFFFF, 0xC8C4D4).slot(MeshBuilder.PRIMARY);
                m.box(0, -Art.TILE_H / 2, 0, Art.TILE, Art.TILE_H, Art.TILE, 0.32f, 2);
                break;
        }
    }

    /**
     * Optional second mesh drawn over the tile with a pulsing flash (see glow()): the NEON tubes,
     * the GOLDEN GLORY frame and gems. Leaves the builder empty for skins without one.
     */
    public static void buildGlow(MeshBuilder m, int id) {
        if (id == NEON) {
            // light tubes along the top rim, the bottom edge and up the corners
            m.paint(1f);
            for (int s = 0; s < 4; s++) {
                m.push().rotateY(s * PI / 2);
                m.box(0, 0.03f, H - 0.16f, 3.56f, 0.1f, 0.12f, 0f);
                m.box(0, -0.88f, H + 0.01f, 3.5f, 0.1f, 0.1f, 0f);
                m.box(H - 0.05f, -0.45f, H - 0.05f, 0.14f, 0.8f, 0.14f, 0f);
                m.pop();
            }
        } else if (id == GOLDEN) {
            // raised, beveled gold frame around the colored panel
            m.twoTone(0xF4B832, 0xB87410).slot(MeshBuilder.FIXED);
            ring(m, 1.58f, 1.99f, -0.15f, 0.09f, 0.09f);
            // gold corner brackets on the panel (outside the symbol area)
            m.twoTone(0xFFD050, 0xB87410);
            for (int s = 0; s < 4; s++) {
                m.push().rotateY(s * PI / 2);
                m.box(1.3f, 0.02f, 1.13f, 0.12f, 0.05f, 0.42f, 0f);
                m.box(1.13f, 0.02f, 1.3f, 0.42f, 0.05f, 0.12f, 0f);
                m.pop();
            }
            // big diamonds on the corners, tile-colored gems in the middle of each side
            m.fixed(0xF4FDFF);
            for (int s = 0; s < 4; s++) {
                m.push().rotateY(s * PI / 2 + PI / 4).translate(0, 0.22f, H * 1.414f - 0.34f);
                m.ellipsoid(0, 0, 0, 0.3f, 0.26f, 0.3f, 4);
                m.pop();
            }
            m.paint(1f);
            for (int s = 0; s < 4; s++) {
                m.push().rotateY(s * PI / 2).translate(0, 0.13f, H - 0.21f).rotateY(PI / 4);
                m.ellipsoid(0, 0, 0, 0.26f, 0.17f, 0.26f, 4);
                m.pop();
            }
        }
    }

    /** Flash amount for the glow mesh of a tile at (x, z). Cheap: called for every tile every frame. */
    public static float glow(int id, float x, float z, float time) {
        if (id == NEON) return 0.3f + 0.12f * (float) Math.sin(time * 3f + (x - z) * 0.12f);
        // GOLDEN: a shine sweeping diagonally across the arena every few seconds
        float p = (x + z) * 0.012f - time * 0.32f;
        p -= (float) Math.floor(p);
        float d = Math.abs(p - 0.5f) * 12f;
        return Math.max(0f, 1f - d) * 0.55f;
    }

    /** Flash for the color-blind symbols (NEON symbols glow a little too). */
    public static float symbolFlash(int id) {
        return id == NEON ? 0.18f : 0f;
    }

    // ------------------------------------------------------------------ skins

    private static void candy(MeshBuilder m) {
        // hard candy block: diagonal stripes on top, candy-cane bands around the sides
        m.twoTone(0xFFFFFF, 0xE6DCEC).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.5f, 0, Art.TILE, 1f, Art.TILE, 0.24f, 2);
        m.paint(0.78f);
        float f = H - 0.22f;
        for (int k = -5; k <= 4; k++) {
            float c0 = k * 0.9f + 0.1f, c1 = c0 + 0.38f;
            float[] p = band(rect(-f, -f, f, f), 1f, 1f, c0, c1);
            if (p != null) m.extrude(p, -0.01f, 0.018f);
        }
        m.fixed(0xFFFFFF);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2).translate(H, 0, 0).rotateZ(-PI / 2);
            for (int k = -4; k <= 4; k++) {
                float c0 = k * 0.8f, c1 = c0 + 0.34f;
                float[] p = band(rect(0.24f, -f, 0.78f, f), 1.6f, 1f, c0, c1);
                if (p != null) m.extrude(p, -0.01f, 0.022f);
            }
            m.pop();
        }
    }

    private static void cookie(MeshBuilder m) {
        // baked cookie with a thick frosting top
        m.twoTone(0xEDB46E, 0xC98A44).slot(MeshBuilder.FIXED);
        m.box(0, -0.56f, 0, Art.TILE, 0.88f, Art.TILE, 0.4f, 1);
        m.twoTone(0xFFFFFF, 0xEDE6F2).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.13f, 0, 3.5f, 0.26f, 3.5f, 0.13f, 1);
        // frosting drips over the edge
        m.paint(0.97f);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            m.ellipsoid(-0.85f + s * 0.25f, -0.26f, 1.7f, 0.3f, 0.24f, 0.16f, 6);
            m.ellipsoid(0.8f - s * 0.18f, -0.22f, 1.7f, 0.22f, 0.19f, 0.15f, 6);
            m.pop();
        }
        // chocolate chunks pressed into the dough
        m.fixed(0x5A3020);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            chunk(m, -1.35f + s * 0.12f, -0.62f, H - 0.04f, 0.3f, s * 0.7f);
            chunk(m, 0.15f + s * 0.1f, -0.72f, H - 0.04f, 0.24f, 1.1f + s);
            chunk(m, 1.45f - s * 0.1f, -0.5f, H - 0.04f, 0.26f, 2.3f - s);
            chunk(m, H - 0.12f, -0.14f, H - 0.12f, 0.24f, s * 1.3f);
            m.pop();
        }
    }

    /** A rough chocolate chunk: a tilted cube. */
    private static void chunk(MeshBuilder m, float x, float y, float z, float s, float rot) {
        m.push().translate(x, y, z).rotateY(rot).rotateX(0.5f + rot * 0.3f).rotateZ(0.6f);
        m.box(0, 0, 0, s, s * 0.8f, s, 0f);
        m.pop();
    }

    private static void bricks(MeshBuilder m) {
        // toy brick: studs around the edge (the middle stays free for the symbol)
        m.twoTone(0xFFFFFF, 0xD2D0DA).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.5f, 0, Art.TILE, 1f, Art.TILE, 0.08f, 1);
        float[] stud = {0.36f, -0.02f, 0.36f, 0.22f, 0.36f, 0.22f, 0f, 0.22f};
        for (int gz = 0; gz < 4; gz++) {
            for (int gx = 0; gx < 4; gx++) {
                if ((gx == 1 || gx == 2) && (gz == 1 || gz == 2)) continue;
                m.push().translate(-1.5f + gx, 0, -1.5f + gz);
                m.lathe(stud, 10);
                m.pop();
            }
        }
    }

    private static void jelly(MeshBuilder m) {
        // extra round, with a darker jelly base and glossy streaks on top
        m.twoTone(0xFFFFFF, 0xF2E8FA).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.42f, 0, Art.TILE, 0.84f, Art.TILE, 0.42f, 3);
        m.paint(0.7f).box(0, -0.86f, 0, 3.7f, 0.24f, 3.7f, 0.12f, 1);
        m.fixed(0xFFFFFF);
        m.ellipsoid(-1.48f, 0.0f, -0.2f, 0.13f, 0.05f, 0.9f, 6);
        m.ellipsoid(-1.48f, 0.0f, 1.02f, 0.13f, 0.05f, 0.16f, 6);
        m.ellipsoid(-0.4f, 0.0f, -1.48f, 0.62f, 0.05f, 0.12f, 6);
    }

    private static void neon(MeshBuilder m) {
        // dark frame around a colored panel (the light tubes are the glow mesh)
        m.twoTone(0x2C2348, 0x1A1430).slot(MeshBuilder.FIXED);
        m.box(0, -0.5f, 0, Art.TILE, 1f, Art.TILE, 0.16f, 1);
        m.twoTone(0xFFFFFF, 0x9A9A9A).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.06f, 0, 3.24f, 0.14f, 3.24f, 0.06f, 1);
    }

    private static void frosty(MeshBuilder m) {
        // icy block with a snowy rim and icicles hanging below
        m.twoTone(0xFFFFFF, 0xB8D4F4).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.5f, 0, Art.TILE, 1f, Art.TILE, 0.3f, 1);
        m.twoTone(0xF2F8FF, 0xD4E6FA).slot(MeshBuilder.FIXED);
        ring(m, 1.74f, 2.0f, -0.1f, 0.08f, 0.08f);
        m.fixed(0xF6FBFF);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            m.ellipsoid(H - 0.24f, 0.04f, H - 0.24f, 0.36f, 0.2f, 0.36f, 6);
            m.ellipsoid(0.55f - s * 0.35f, 0.06f, H - 0.11f, 0.42f, 0.11f, 0.19f, 6);
            m.pop();
        }
        m.fixed(0xCFE9FF);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            for (int k = 0; k < 2; k++) {
                float len = 0.45f + ((k * 7 + s * 3) % 5) * 0.12f;
                float x = -0.95f + k * 1.6f + (s % 2) * 0.3f;
                m.cone(x, -0.97f - len, H - 0.22f, 0.01f, 0.18f, len + 0.04f, 4);
            }
            m.pop();
        }
    }

    private static void pixel(MeshBuilder m) {
        // stepped 8-bit block: dark base, notched top, light and dark bevels, a white shine pixel
        m.twoTone(0xB4B4C0, 0x8C8C9C).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.58f, 0, Art.TILE, 0.84f, Art.TILE, 0f);
        m.twoTone(0xEEEEEE, 0xC8C8D0).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.08f, 0, 3.7f, 0.16f, 3.3f, 0f);
        m.box(0, -0.08f, 0, 3.3f, 0.16f, 3.7f, 0f);
        m.paint(1f);
        m.box(-1.55f, 0.015f, -0.1f, 0.2f, 0.03f, 3.1f, 0f);
        m.box(0.1f, 0.015f, -1.55f, 3.1f, 0.03f, 0.2f, 0f);
        m.paint(0.62f);
        m.box(1.55f, 0.015f, 0.1f, 0.2f, 0.03f, 3.1f, 0f);
        m.box(-0.1f, 0.015f, 1.55f, 3.1f, 0.03f, 0.2f, 0f);
        m.fixed(0xFFFFFF);
        m.box(-1.15f, 0.02f, -1.15f, 0.2f, 0.04f, 0.2f, 0f);
        m.box(-0.85f, 0.02f, -1.15f, 0.2f, 0.04f, 0.2f, 0f);
        m.box(-1.15f, 0.02f, -0.85f, 0.2f, 0.04f, 0.2f, 0f);
        // side pixels: a darker step band around the base
        m.paint(0.5f);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            m.box(0, -0.86f, H + 0.005f, 3.6f, 0.2f, 0.02f, 0f);
            m.pop();
        }
    }

    private static void crates(MeshBuilder m) {
        // wooden crate body (dark inside, so the gaps between the planks show)
        m.twoTone(0x4A2E18, 0x8A5E38).slot(MeshBuilder.FIXED);
        m.box(0, -0.58f, 0, 3.9f, 0.84f, 3.9f, 0f);
        // painted planks
        m.twoTone(0xFFFFFF, 0xC4C4C4).slot(MeshBuilder.PRIMARY);
        for (int k = -1; k <= 1; k++) m.box(0, -0.12f, k * 1.08f, 3.3f, 0.24f, 1.05f, 0f);
        // frame boards on top, corner posts, side boards and braces
        m.twoTone(0xC0905E, 0x8E6038).slot(MeshBuilder.FIXED);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            m.box(0.16f, -0.06f, H - 0.16f, 3.68f, 0.2f, 0.32f, 0f);
            m.box(H - 0.16f, -0.55f, H - 0.16f, 0.32f, 0.9f, 0.32f, 0f);
            m.box(0, -0.86f, H - 0.05f, 3.4f, 0.22f, 0.12f, 0f);
            m.push().translate(0, -0.5f, H - 0.05f).rotateZ(0.22f);
            m.box(0, 0, 0, 3.2f, 0.18f, 0.1f, 0f);
            m.pop();
            m.pop();
        }
        // nails
        m.fixed(0x4A4A58);
        for (int s = 0; s < 4; s++) {
            m.push().rotateY(s * PI / 2);
            m.box(H - 0.19f, 0.045f, H - 0.19f, 0.1f, 0.03f, 0.1f, 0f);
            m.box(-0.6f, 0.045f, H - 0.19f, 0.08f, 0.03f, 0.08f, 0f);
            m.pop();
        }
    }

    private static void golden(MeshBuilder m) {
        // solid gold block (the frame and gems are the glow mesh)
        m.twoTone(0xF0B838, 0xC88A18).slot(MeshBuilder.FIXED);
        m.box(0, -0.55f, 0, Art.TILE, 0.9f, Art.TILE, 0.3f, 1);
        m.fixed(0xA86A08).box(0, -0.5f, 0, Art.TILE + 0.05f, 0.12f, Art.TILE + 0.05f, 0f);
        m.fixed(0xFFF0A0).box(0, -0.98f, 0, 3.5f, 0.06f, 3.5f, 0f);
        m.twoTone(0xFFFFFF, 0xD8D8D8).slot(MeshBuilder.PRIMARY);
        m.box(0, -0.1f, 0, 3.2f, 0.2f, 3.2f, 0f);
        // a jewel point underneath, seen when the tile falls
        m.twoTone(0xFFD24A, 0xE8A82A).slot(MeshBuilder.FIXED);
        m.push().rotateY(PI / 4);
        m.cone(0, -2.0f, 0, 0.05f, 1.45f, 1.0f, 4);
        m.pop();
        m.paint(1f).ellipsoid(0, -2.1f, 0, 0.24f, 0.3f, 0.24f, 4);
    }

    /**
     * A square frame around the middle of the tile: half sizes inner..outer, from y0 up to y1 with rounded top
     * edges. Lathed with four segments, so it costs about 40 triangles instead of hundreds for rounded boxes.
     */
    private static void ring(MeshBuilder m, float inner, float outer, float y0, float y1, float bevel) {
        float k = 1.4142f; // half side -> distance to the corner
        float ri = inner * k, ro = outer * k, b = bevel * k;
        m.push().rotateY(PI / 4);
        m.lathe(new float[]{ro, y0, ro, y1 - bevel, ro - b, y1, ri + b, y1, ri, y1 - bevel, ri, y0}, 4);
        m.pop();
    }

    // ------------------------------------------------------------------ polygons

    /** Rectangle as a polygon (counter-clockwise like Art.ngon). */
    private static float[] rect(float x0, float z0, float x1, float z1) {
        return new float[]{x1, z0, x1, z1, x0, z1, x0, z0};
    }

    /** The part of a convex polygon where c0 <= a*x + b*z <= c1, or null if (almost) empty. */
    private static float[] band(float[] p, float a, float b, float c0, float c1) {
        p = clip(p, a, b, c0);
        if (p == null) return null;
        p = clip(p, -a, -b, -c1);
        if (p == null) return null;
        float area = 0;
        int n = p.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
        }
        return Math.abs(area) < 0.02f ? null : p;
    }

    /** Keeps the part of a convex polygon where a*x + b*z >= c. */
    private static float[] clip(float[] p, float a, float b, float c) {
        int n = p.length / 2;
        float[] out = new float[n * 4];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float x0 = p[i * 2], z0 = p[i * 2 + 1], x1 = p[j * 2], z1 = p[j * 2 + 1];
            float d0 = a * x0 + b * z0 - c, d1 = a * x1 + b * z1 - c;
            if (d0 >= 0) {
                out[k++] = x0;
                out[k++] = z0;
            }
            if ((d0 >= 0) != (d1 >= 0)) {
                float t = d0 / (d0 - d1);
                out[k++] = x0 + (x1 - x0) * t;
                out[k++] = z0 + (z1 - z0) * t;
            }
        }
        if (k < 6) return null;
        float[] r = new float[k];
        System.arraycopy(out, 0, r, 0, k);
        return r;
    }
}
