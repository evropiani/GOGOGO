package com.gogogo.game.game;

import com.gogogo.game.engine.Mesh;
import com.gogogo.game.engine.MeshBuilder;
import com.gogogo.game.engine.Renderer;

/** All procedural meshes, built once at startup. */
public final class Art {
    public static final float TILE = 4.0f;       // tile size
    public static final float TILE_H = 1.0f;     // tile thickness

    private static final float PI = (float) Math.PI;

    public final Mesh tile;
    public final Mesh[] symbols = new Mesh[6];
    public final Mesh[] cars = new Mesh[Cars.ALL.length];
    public final Mesh[] wheels = new Mesh[Palette.WHEEL_NAME.length];
    public final Mesh[] toppers = new Mesh[Palette.TOPPER_NAME.length];
    public final Mesh propeller;
    public final Mesh cube, ball, star, cloud, duck, marker, pedestal, ring, bonk;

    public Art(Renderer r) {
        MeshBuilder m;

        m = new MeshBuilder();
        m.slot(MeshBuilder.PRIMARY).twoTone(0xFFFFFF, 0xC8C4D4).slot(MeshBuilder.PRIMARY);
        m.box(0, -TILE_H / 2, 0, TILE, TILE_H, TILE, 0.32f, 2);
        tile = r.register(m.build());

        for (int i = 0; i < 6; i++) {
            m = new MeshBuilder();
            m.paint(1f);
            m.extrude(symbolPoly(i), -0.05f, 0.07f);
            symbols[i] = r.register(m.build());
        }

        for (int i = 0; i < cars.length; i++) {
            m = new MeshBuilder();
            Cars.ALL[i].build(m);
            cars[i] = r.register(m.build());
            placeHoodCamera(Cars.ALL[i], cars[i]);
        }

        for (int i = 0; i < wheels.length; i++) {
            m = new MeshBuilder();
            m.rotateZ(PI / 2); // lathe axis Y -> X
            buildWheel(m, i);
            wheels[i] = r.register(m.build());
        }

        for (int i = 0; i < toppers.length; i++) {
            m = new MeshBuilder();
            buildTopper(m, i);
            if (m.isEmpty()) m.fixed(0xFFFFFF).box(0, -10, 0, 0.01f, 0.01f, 0.01f, 0);
            toppers[i] = r.register(m.build());
        }

        m = new MeshBuilder();
        m.fixed(0xFF3B5C).box(0, 0, 0, 1.3f, 0.05f, 0.16f, 0.04f);
        m.fixed(0x3BB8FF).box(0, 0, 0, 0.16f, 0.05f, 1.3f, 0.04f);
        m.fixed(0xFFD23B).sphere(0, 0.02f, 0, 0.1f, 8);
        propeller = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).box(0, 0, 0, 1, 1, 1, 0.18f, 1);
        cube = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).sphere(0, 0, 0, 1, 12);
        ball = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2).extrude(starPoly(1f, 0.45f), -0.2f, 0.2f).pop();
        star = r.register(m.build());

        m = new MeshBuilder();
        m.fixed(0xFFFFFF);
        m.sphere(0, 0, 0, 3.2f, 14);
        m.sphere(3.4f, -0.6f, 0.5f, 2.4f, 12);
        m.sphere(-3.2f, -0.8f, -0.3f, 2.2f, 12);
        m.sphere(1.2f, 1.6f, -0.8f, 2.2f, 12);
        m.sphere(-1.5f, 1.0f, 1.2f, 1.8f, 10);
        cloud = r.register(m.build());

        m = new MeshBuilder();
        m.fixed(0xFFE03B).ellipsoid(0, 0.45f, 0, 0.5f, 0.38f, 0.62f, 14);
        m.fixed(0xFFE03B).sphere(0, 0.98f, 0.28f, 0.33f, 14);
        m.push().translate(0, 0.62f, -0.55f).rotateX(-0.6f);
        m.fixed(0xFFE03B).ellipsoid(0, 0, 0, 0.2f, 0.16f, 0.2f, 8);
        m.pop();
        m.fixed(0xFF8A1F).ellipsoid(0, 0.92f, 0.62f, 0.18f, 0.07f, 0.16f, 10);
        m.fixed(0x1A1028).sphere(-0.15f, 1.06f, 0.55f, 0.06f, 6);
        m.fixed(0x1A1028).sphere(0.15f, 1.06f, 0.55f, 0.06f, 6);
        duck = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f);
        m.lathe(new float[]{0, -0.75f, 0.62f, 0.05f, 0.62f, 0.05f, 0.5f, 0.2f, 0.5f, 0.2f, 0, 0.2f}, 4);
        m.paint(1.1f).box(0, 0.55f, 0, 0.36f, 0.62f, 0.36f, 0.12f);
        marker = r.register(m.build());

        m = new MeshBuilder();
        m.twoTone(0xFFFFFF, 0xD8D0F0).slot(MeshBuilder.PRIMARY);
        m.cylinder(0, -1.2f, 0, 3.4f, 0, 1.2f, 0.25f, 40);
        m.accent(1f).cylinder(0, -0.75f, 0, 3.46f, 0, 0.3f, 0.12f, 40);
        pedestal = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).torus(0, 0, 0, 1f, 0.12f, 32, 8);
        ring = r.register(m.build());

        // comic "BONK" burst: a spiky star disc
        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2).extrude(starPoly2(10, 1f, 0.6f), -0.1f, 0.1f).pop();
        bonk = r.register(m.build());
    }

    /** Puts the hood camera over the middle of the car, just above its roof, so the hood shows below. */
    private static void placeHoodCamera(CarDef d, Mesh body) {
        float top = 0f;
        float[] v = body.vertices;
        for (int k = 0; k < v.length; k += Mesh.VERTEX_FLOATS) {
            float z = v[k + 2];
            if (z > -0.3f && z < 0.6f && Math.abs(v[k]) < 0.6f) top = Math.max(top, v[k + 1]);
        }
        d.hoodY = Math.max(1.3f, top) + 0.3f;
        d.hoodZ = 0f;
    }

    // ------------------------------------------------------------------ shapes

    static float[] symbolPoly(int i) {
        switch (i) {
            case 0: return ngon(18, 0.8f, 0f);                       // circle
            case 1: return starPoly(0.95f, 0.42f);                   // star
            case 2: return ngon(4, 0.95f, 0f);                        // diamond
            case 3: return ngon(3, 0.95f, PI / 2);                    // triangle (points to +z)
            case 4: return plusPoly(0.9f, 0.3f);                      // plus
            default: return ngon(6, 0.85f, 0f);                       // hexagon
        }
    }

    static float[] ngon(int n, float r, float rot) {
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = rot + i * Math.PI * 2 / n;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    static float[] starPoly(float ro, float ri) {
        float[] p = new float[20];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            float r = (i % 2 == 0) ? ro : ri;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    static float[] starPoly2(int points, float ro, float ri) {
        float[] p = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = i * Math.PI / points;
            float r = (i % 2 == 0) ? ro : ri;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    static float[] plusPoly(float a, float b) {
        return new float[]{
                b, -b, a, -b, a, b, b, b, b, a, -b, a,
                -b, b, -a, b, -a, -b, -b, -b, -b, -a, b, -a};
    }

    // ------------------------------------------------------------------ wheels (built along Y, rotated to X)

    private static void buildWheel(MeshBuilder m, int style) {
        switch (style) {
            case 1: // candy
                m.fixed(0xFF6FB5).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.55f, -0.54f, 0.54f, 0.08f, 14);
                m.fixed(0xFF3B8A).box(0.25f, 0.55f, 0, 0.2f, 0.06f, 0.2f, 0.04f);
                m.fixed(0xFF3B8A).box(0.25f, -0.55f, 0, 0.2f, 0.06f, 0.2f, 0.04f);
                break;
            case 2: // neon
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0x3BF0FF).cylinder(0, 0, 0, 0.62f, -0.54f, 0.54f, 0.08f, 14);
                m.fixed(0xFF3BF0).cylinder(0, 0, 0, 0.3f, -0.58f, 0.58f, 0.06f, 10);
                m.fixed(0x2B2B38).box(0.38f, 0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                m.fixed(0x2B2B38).box(0.38f, -0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                break;
            case 3: // donut
                m.push().rotateX(0);
                m.fixed(0xE0A060).torus(0, 0, 0, 0.62f, 0.4f, 20, 10);
                m.fixed(0xFF7AC8).torus(0, 0.12f, 0, 0.62f, 0.33f, 20, 8);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    m.fixed(i % 2 == 0 ? 0xFFFFFF : 0x3BB8FF).box((float) Math.cos(a) * 0.62f, 0.45f, (float) Math.sin(a) * 0.62f, 0.18f, 0.06f, 0.06f, 0.02f);
                }
                m.pop();
                break;
            case 4: // gold
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0xFFC83D).cylinder(0, 0, 0, 0.72f, -0.54f, 0.54f, 0.1f, 14);
                m.fixed(0xFFF0A0).cylinder(0, 0, 0, 0.3f, -0.58f, 0.58f, 0.06f, 10);
                m.fixed(0xC08A10).box(0.45f, 0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                m.fixed(0xC08A10).box(0.45f, -0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                break;
            default: // classic
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.3f, 14);
                m.fixed(0xF0F0F8).cylinder(0, 0, 0, 0.52f, -0.54f, 0.54f, 0.08f, 14);
                m.fixed(0xFF4FA3).box(0.24f, 0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                m.fixed(0xFF4FA3).box(0.24f, -0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                break;
        }
    }

    // ------------------------------------------------------------------ toppers (base at y = 0)

    private static void buildTopper(MeshBuilder m, int i) {
        switch (i) {
            case 1: // antenna ball
                m.fixed(0xB0B0C0).cylinder(0, 0, 0, 0.04f, 0, 1.0f, 0.01f, 6);
                m.fixed(0xFFE14D).sphere(0, 1.05f, 0, 0.2f, 12);
                m.fixed(0x1A1028).sphere(-0.07f, 1.09f, 0.15f, 0.05f, 6);
                m.fixed(0x1A1028).sphere(0.07f, 1.09f, 0.15f, 0.05f, 6);
                break;
            case 2: // traffic cone
                m.fixed(0xFF7A1A).box(0, 0.05f, 0, 0.8f, 0.1f, 0.8f, 0.05f);
                m.fixed(0xFF7A1A).cone(0, 0.08f, 0, 0.32f, 0.22f, 0.3f, 14);
                m.fixed(0xFFFFFF).cone(0, 0.38f, 0, 0.22f, 0.15f, 0.2f, 14);
                m.fixed(0xFF7A1A).cone(0, 0.58f, 0, 0.15f, 0.04f, 0.32f, 14);
                break;
            case 3: // party hat
                m.fixed(0x9B6BFF).cone(0, 0, 0, 0.34f, 0.02f, 0.9f, 16);
                m.fixed(0xFFE14D).cone(0, 0.25f, 0, 0.27f, 0.25f, 0.08f, 16);
                m.fixed(0x5EE65A).cone(0, 0.55f, 0, 0.17f, 0.15f, 0.08f, 16);
                m.fixed(0xFF4FA3).sphere(0, 0.95f, 0, 0.13f, 10);
                break;
            case 4: // propeller cap (blades drawn separately)
                m.fixed(0xFF3B5C).ellipsoid(0, 0, 0, 0.45f, 0.3f, 0.45f, 16);
                m.fixed(0x3BB8FF).box(0, 0.03f, 0.42f, 0.5f, 0.06f, 0.3f, 0.03f);
                m.fixed(0xFFD23B).cylinder(0, 0.25f, 0, 0.05f, 0, 0.18f, 0.01f, 6);
                break;
            case 5: // chef hat
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.3f, 0, 0.38f, 0.05f, 14);
                m.fixed(0xFFFFFF).sphere(0, 0.5f, 0, 0.3f, 12);
                m.fixed(0xFFFFFF).sphere(0.2f, 0.45f, 0.1f, 0.22f, 10);
                m.fixed(0xFFFFFF).sphere(-0.2f, 0.45f, 0.08f, 0.22f, 10);
                m.fixed(0xFFFFFF).sphere(0, 0.45f, -0.2f, 0.22f, 10);
                break;
            case 6: // banana
                for (int k = 0; k < 7; k++) {
                    float t = k / 6f;
                    float a = -1.1f + t * 2.2f;
                    float x = (float) Math.sin(a) * 0.6f, y = 0.15f + (float) Math.cos(a) * 0.1f + (1 - Math.abs(t - 0.5f) * 2) * 0.05f;
                    float rr = 0.2f * (1f - Math.abs(t - 0.5f) * 1.1f);
                    m.fixed(0xFFE14D).sphere(x, y + 0.4f - (float) Math.cos(a) * 0.4f, 0, rr + 0.04f, 10);
                }
                m.fixed(0x6B4A2A).sphere(-0.55f, 0.32f, 0, 0.06f, 6);
                m.fixed(0x6B4A2A).sphere(0.55f, 0.32f, 0, 0.06f, 6);
                break;
            case 7: // shark fin
                m.push().rotateX(-0.35f).scale(0.28f, 1f, 1f);
                m.fixed(0x6D7FA8).cone(0, 0, 0, 0.5f, 0.02f, 1.0f, 16);
                m.pop();
                break;
            case 8: // flower
                m.fixed(0x3FAF4A).cylinder(0, 0, 0, 0.05f, 0, 0.7f, 0.01f, 6);
                m.fixed(0x5EE65A).ellipsoid(0.15f, 0.3f, 0, 0.16f, 0.05f, 0.08f, 8);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    m.fixed(0xFF6FB5).ellipsoid((float) Math.cos(a) * 0.2f, 0.78f, (float) Math.sin(a) * 0.2f, 0.15f, 0.08f, 0.15f, 8);
                }
                m.fixed(0xFFD23B).sphere(0, 0.8f, 0, 0.12f, 8);
                break;
            case 9: // top hat
                m.fixed(0x252030).cylinder(0, 0, 0, 0.5f, 0, 0.07f, 0.03f, 18);
                m.fixed(0x252030).cylinder(0, 0.05f, 0, 0.32f, 0, 0.7f, 0.04f, 18);
                m.fixed(0xFF3B5C).cylinder(0, 0.1f, 0, 0.335f, 0, 0.12f, 0.02f, 18);
                break;
            case 10: // viking horns
                m.fixed(0x9AA0B0).ellipsoid(0, 0, 0, 0.45f, 0.38f, 0.45f, 16);
                m.fixed(0xC8A050).cylinder(0, 0.02f, 0, 0.47f, 0, 0.1f, 0.03f, 16);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.38f, 0.15f, 0).rotateZ(-s * 1.0f);
                    m.fixed(0xFFF6E0).cone(0, 0, 0, 0.13f, 0.02f, 0.55f, 10);
                    m.pop();
                }
                break;
            case 11: // crown
                m.fixed(0xFFC83D).lathe(new float[]{0, 0, 0.4f, 0, 0.4f, 0, 0.42f, 0.3f, 0.42f, 0.3f, 0.34f, 0.3f, 0.34f, 0.3f, 0.32f, 0.04f, 0.32f, 0.04f, 0, 0.04f}, 18);
                for (int k = 0; k < 5; k++) {
                    double a = k * Math.PI * 2 / 5;
                    m.fixed(0xFFC83D).cone((float) Math.cos(a) * 0.37f, 0.28f, (float) Math.sin(a) * 0.37f, 0.08f, 0.01f, 0.22f, 8);
                    m.fixed(k % 2 == 0 ? 0xFF3B5C : 0x3BB8FF).sphere((float) Math.cos(a) * 0.42f, 0.15f, (float) Math.sin(a) * 0.42f, 0.06f, 6);
                }
                break;
            case 12: // halo
                m.fixed(0xFFE58A).torus(0, 0.55f, 0, 0.42f, 0.07f, 24, 8);
                break;
            default:
                break;
        }
    }
}
