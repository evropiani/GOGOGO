package com.gogogo.game.game;

import com.gogogo.game.engine.MeshBuilder;

/** The car catalog. Index in ALL == CarDef.id. The last one is the secret car. */
public final class Cars {
    private Cars() {}

    private static final float PI = (float) Math.PI;

    public static final CarDef[] ALL = {
            new CarDef(0, "BUMPER BEAN", "Round, reliable, slightly dented.", 0, 2.5f, 3f, 2.5f, 2f, 0, 1) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 0.78f, 0, 1.6f, 0.78f, 2.3f, 0.36f);
                    m.paint(1.04f).box(0, 1.34f, -0.15f, 1.26f, 0.62f, 1.25f, 0.3f);
                    m.fixed(GLASS).box(0, 1.34f, 0.44f, 1.02f, 0.4f, 0.1f, 0.05f);
                    m.fixed(GLASS).box(0, 1.34f, -0.75f, 1.02f, 0.4f, 0.1f, 0.05f);
                    m.fixed(GLASS).box(0, 1.34f, -0.15f, 1.3f, 0.36f, 0.9f, 0.05f);
                    m.accent(1f).box(0, 0.56f, 1.14f, 1.5f, 0.26f, 0.26f, 0.12f);
                    m.accent(1f).box(0, 0.56f, -1.14f, 1.5f, 0.26f, 0.26f, 0.12f);
                    m.fixed(LIGHT).sphere(-0.5f, 0.86f, 1.1f, 0.14f, 10);
                    m.fixed(LIGHT).sphere(0.5f, 0.86f, 1.1f, 0.14f, 10);
                    eyes(m, 0.25f, 1.4f, 0.5f, 0.17f);
                }
            }.wheels(0.42f, 0.34f, 0.78f, 0.74f, -0.74f).topper(1.64f, -0.15f),

            new CarDef(1, "ZOOMBA", "Low, light and allergic to brakes.", 600, 4f, 3f, 2.5f, 1f, 2, 6) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 0.62f, -0.1f, 1.76f, 0.5f, 2.6f, 0.22f);
                    m.paint(1f).box(0, 0.54f, 1.25f, 1.3f, 0.34f, 0.8f, 0.16f);
                    m.fixed(GLASS).ellipsoid(0, 0.92f, -0.15f, 0.5f, 0.34f, 0.78f, 16);
                    m.accent(1f).box(0, 0.88f, 0.55f, 0.36f, 0.04f, 1.2f, 0.02f);
                    m.accent(1f).box(0, 1.26f, -1.28f, 1.8f, 0.1f, 0.42f, 0.05f);
                    m.fixed(DARK).box(-0.55f, 1.02f, -1.24f, 0.1f, 0.4f, 0.12f, 0.03f);
                    m.fixed(DARK).box(0.55f, 1.02f, -1.24f, 0.1f, 0.4f, 0.12f, 0.03f);
                    m.fixed(0x555566).cylinder(-0.45f, 0.5f, -1.4f, 0.1f, -0.1f, 0.1f, 0.03f, 8);
                    eyes(m, 0.3f, 0.78f, 1.48f, 0.14f);
                }
            }.wheels(0.4f, 0.42f, 0.86f, 0.92f, -0.86f).topper(1.24f, -0.15f),

            new CarDef(2, "TACO TRUCK", "Heavy. Spicy. Smells amazing.", 900, 2f, 2f, 2.5f, 5f, 4, 1) {
                public void build(MeshBuilder m) {
                    m.fixed(DARK).box(0, 0.52f, 0.05f, 1.6f, 0.3f, 2.7f, 0.1f);
                    m.paint(1f).box(0, 1.02f, 0.86f, 1.7f, 1.1f, 0.95f, 0.25f);
                    m.fixed(GLASS).box(0, 1.22f, 1.32f, 1.4f, 0.45f, 0.1f, 0.05f);
                    m.accent(1f).box(0, 1.22f, -0.45f, 1.84f, 1.5f, 1.75f, 0.2f);
                    m.fixed(GLASS).box(0.92f, 1.36f, -0.45f, 0.06f, 0.6f, 1.1f, 0.03f);
                    m.paint(1f).box(0.95f, 1.0f, -0.45f, 0.12f, 0.1f, 1.2f, 0.04f);
                    // the taco
                    m.push().translate(0, 2.25f, -0.45f);
                    m.fixed(0xFFC94D).ellipsoid(0, 0, 0, 0.28f, 0.55f, 0.82f, 16);
                    for (int i = 0; i < 6; i++) {
                        float z = -0.62f + i * 0.25f;
                        m.fixed(0x5ED64A).sphere(i % 2 == 0 ? 0.12f : -0.12f, 0.42f, z, 0.17f, 8);
                        if (i % 2 == 1) m.fixed(0xFF4040).sphere(0.05f, 0.5f, z + 0.1f, 0.11f, 8);
                        else m.fixed(0x8A4B2A).sphere(-0.06f, 0.46f, z + 0.12f, 0.12f, 8);
                    }
                    m.pop();
                    eyes(m, 0.36f, 1.64f, 1.18f, 0.17f);
                }
            }.wheels(0.48f, 0.4f, 0.86f, 0.95f, -0.85f).topper(1.58f, 0.86f),

            new CarDef(3, "HOT DOG", "Grips like mustard on a shirt.", 1300, 3f, 4.5f, 3f, 2f, 6, 1) {
                public void build(MeshBuilder m) {
                    m.fixed(0xF2B66D).box(0, 0.62f, 0, 1.5f, 0.5f, 2.9f, 0.25f);
                    m.twoTone(0xF7C88A, 0xE89A4F).box(-0.56f, 0.98f, 0, 0.42f, 0.55f, 2.8f, 0.2f);
                    m.twoTone(0xF7C88A, 0xE89A4F).box(0.56f, 0.98f, 0, 0.42f, 0.55f, 2.8f, 0.2f);
                    m.paint(1f).ellipsoid(0, 1.06f, 0, 0.42f, 0.4f, 1.72f, 18);
                    for (int i = 0; i < 7; i++) {
                        float z = -1.05f + i * 0.35f;
                        m.push().translate(0, 1.44f, z).rotateY(i % 2 == 0 ? 0.7f : -0.7f);
                        m.accent(1f).box(0, 0, 0, 0.55f, 0.09f, 0.13f, 0.045f);
                        m.pop();
                    }
                    eyes(m, 0.17f, 1.2f, 1.58f, 0.14f);
                }
            }.wheels(0.38f, 0.34f, 0.78f, 0.98f, -0.98f).topper(1.48f, 0.25f),

            new CarDef(4, "ICE SCREAM", "Brain freeze on wheels.", 1800, 3f, 3f, 3.5f, 3f, 8, 0) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 1.1f, -0.05f, 1.7f, 1.36f, 2.6f, 0.36f);
                    m.accent(1f).box(0, 0.8f, -0.05f, 1.74f, 0.26f, 2.64f, 0.12f);
                    m.fixed(GLASS).box(0, 1.36f, 1.21f, 1.34f, 0.5f, 0.1f, 0.05f);
                    m.fixed(GLASS).box(0.85f, 1.36f, -0.25f, 0.06f, 0.55f, 1.0f, 0.05f);
                    m.fixed(0xE8A65A).cone(0, 1.72f, -0.35f, 0.06f, 0.46f, 0.85f, 14);
                    m.accent(1f).sphere(0, 2.72f, -0.35f, 0.52f, 16);
                    m.accent(1f).sphere(0.35f, 2.48f, -0.1f, 0.18f, 10);
                    m.accent(1f).sphere(-0.3f, 2.45f, -0.55f, 0.16f, 10);
                    m.fixed(0xFF2244).sphere(0, 3.3f, -0.35f, 0.15f, 10);
                    eyes(m, 0.34f, 1.68f, 1.2f, 0.16f);
                }
            }.wheels(0.42f, 0.36f, 0.82f, 0.95f, -0.9f).topper(1.78f, 0.78f),

            new CarDef(5, "MONSTER CUBE", "Big wheels. Bigger attitude.", 2500, 3f, 2.5f, 3f, 4.5f, 3, 9) {
                public void build(MeshBuilder m) {
                    m.fixed(DARK).box(0, 0.86f, 0, 1.2f, 0.3f, 2.0f, 0.1f);
                    m.fixed(0xC0C0D0).cylinder(-0.6f, 0.9f, 0.7f, 0.09f, 0, 0.4f, 0.03f, 8);
                    m.fixed(0xC0C0D0).cylinder(0.6f, 0.9f, 0.7f, 0.09f, 0, 0.4f, 0.03f, 8);
                    m.fixed(0xC0C0D0).cylinder(-0.6f, 0.9f, -0.7f, 0.09f, 0, 0.4f, 0.03f, 8);
                    m.fixed(0xC0C0D0).cylinder(0.6f, 0.9f, -0.7f, 0.09f, 0, 0.4f, 0.03f, 8);
                    m.paint(1f).box(0, 1.58f, 0, 1.62f, 1.1f, 2.0f, 0.3f);
                    m.fixed(0x2A1A30).box(0, 1.34f, 1.0f, 0.95f, 0.22f, 0.06f, 0.1f);
                    for (int i = 0; i < 4; i++) m.fixed(0xFFFFFF).box(-0.3f + i * 0.2f, 1.4f, 1.02f, 0.14f, 0.1f, 0.06f, 0.03f);
                    m.accent(1f).box(0, 2.18f, 0.2f, 1.3f, 0.1f, 0.12f, 0.05f);
                    for (int i = -1; i <= 1; i++) m.fixed(0xFFF080).box(i * 0.42f, 2.26f, 0.2f, 0.24f, 0.18f, 0.14f, 0.05f);
                    eyes(m, 0.38f, 1.86f, 1.0f, 0.22f);
                }
            }.wheels(0.72f, 0.56f, 0.98f, 0.86f, -0.86f).topper(2.13f, -0.35f),

            new CarDef(6, "ROCKET TOASTER", "Pops up. Blasts off. Burns toast.", 3200, 4f, 3f, 5f, 2f, 8, 6) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 1.15f, 0, 1.6f, 1.3f, 2.1f, 0.4f);
                    m.fixed(DARK).box(-0.35f, 1.8f, 0, 0.24f, 0.04f, 1.4f, 0.02f);
                    m.fixed(DARK).box(0.35f, 1.8f, 0, 0.24f, 0.04f, 1.4f, 0.02f);
                    m.twoTone(0xF6D38C, 0xC9813A).box(-0.35f, 2.02f, 0, 0.16f, 0.62f, 1.26f, 0.12f);
                    m.twoTone(0xF6D38C, 0xC9813A).box(0.35f, 2.1f, 0.05f, 0.16f, 0.62f, 1.26f, 0.12f);
                    m.fixed(DARK).box(0.84f, 1.45f, 0.35f, 0.12f, 0.14f, 0.34f, 0.05f);
                    m.push().translate(0, 1.1f, -1.0f).rotateX(-PI / 2);
                    m.fixed(0x555566).cone(0, 0, 0, 0.42f, 0.3f, 0.45f, 14);
                    m.fixed(0xFF8A2B).cone(0, 0.44f, 0, 0.26f, 0.1f, 0.1f, 12);
                    m.pop();
                    m.accent(1f).box(-0.86f, 0.95f, -0.75f, 0.12f, 0.62f, 0.62f, 0.05f);
                    m.accent(1f).box(0.86f, 0.95f, -0.75f, 0.12f, 0.62f, 0.62f, 0.05f);
                    m.accent(1f).box(0, 1.9f, -0.8f, 0.12f, 0.5f, 0.55f, 0.05f);
                    eyes(m, 0.36f, 1.28f, 1.06f, 0.19f);
                }
            }.wheels(0.4f, 0.34f, 0.8f, 0.82f, -0.82f).topper(1.8f, 0.82f),

            new CarDef(7, "BATHTUB BOMBER", "Squeaky clean. Dirty driver.", 4000, 3.5f, 4f, 4f, 3f, 8, 1) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 1.0f, 0, 1.7f, 0.96f, 2.6f, 0.42f);
                    m.fixed(0x6FD3FF).box(0, 1.44f, 0, 1.3f, 0.1f, 2.16f, 0.05f);
                    m.fixed(0xFFFFFF).sphere(-0.4f, 1.55f, 0.5f, 0.26f, 10);
                    m.fixed(0xFFFFFF).sphere(0.3f, 1.6f, -0.3f, 0.3f, 10);
                    m.fixed(0xFFFFFF).sphere(0.1f, 1.53f, 0.75f, 0.18f, 8);
                    m.fixed(0xFFFFFF).sphere(-0.3f, 1.58f, -0.7f, 0.22f, 8);
                    m.fixed(0xC8CCD8).cylinder(0, 1.3f, -1.18f, 0.08f, 0, 1.2f, 0.03f, 8);
                    m.fixed(0xC8CCD8).box(0, 2.5f, -1.0f, 0.12f, 0.12f, 0.5f, 0.05f);
                    m.fixed(0xC8CCD8).cylinder(0, 2.3f, -0.75f, 0.2f, 0, 0.16f, 0.05f, 12);
                    for (int s = -1; s <= 1; s += 2) {
                        m.accent(1f).sphere(s * 0.7f, 0.5f, 0.9f, 0.14f, 8);
                        m.accent(1f).sphere(s * 0.7f, 0.5f, -0.9f, 0.14f, 8);
                    }
                    eyes(m, 0.36f, 1.16f, 1.3f, 0.17f);
                }
            }.wheels(0.38f, 0.3f, 0.8f, 0.9f, -0.9f).topper(1.85f, 0.45f),

            new CarDef(8, "SOFA SO GOOD", "Maximum comfort. Maximum velocity.", 5200, 4.5f, 4f, 4f, 4f, 5, 1) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 0.76f, 0, 1.9f, 0.5f, 2.4f, 0.2f);
                    m.accent(1f).box(-0.42f, 1.1f, 0.22f, 0.8f, 0.26f, 1.5f, 0.12f);
                    m.accent(1f).box(0.42f, 1.1f, 0.22f, 0.8f, 0.26f, 1.5f, 0.12f);
                    m.paint(1.03f).box(0, 1.56f, -0.86f, 1.9f, 1.15f, 0.5f, 0.22f);
                    m.paint(0.95f).box(-0.86f, 1.26f, 0.12f, 0.34f, 0.56f, 1.95f, 0.15f);
                    m.paint(0.95f).box(0.86f, 1.26f, 0.12f, 0.34f, 0.56f, 1.95f, 0.15f);
                    m.push().translate(0.42f, 1.5f, -0.48f).rotateZ(0.3f);
                    m.fixed(0xFFE07A).box(0, 0, 0, 0.52f, 0.46f, 0.2f, 0.1f);
                    m.pop();
                    m.fixed(0x2A2A3A).box(-0.3f, 1.27f, 0.5f, 0.14f, 0.06f, 0.36f, 0.03f);
                    eyes(m, 0.42f, 0.8f, 1.2f, 0.15f);
                }
            }.wheels(0.36f, 0.3f, 0.86f, 0.92f, -0.92f).topper(2.13f, -0.86f),

            new CarDef(9, "GOLDEN QUACK", "???", 0, 5f, 5f, 5f, 5f, 16, 4) {
                public void build(MeshBuilder m) {
                    m.paint(1f).ellipsoid(0, 1.1f, -0.15f, 0.95f, 0.66f, 1.3f, 20);
                    m.push().translate(0, 1.45f, -1.2f).rotateX(-0.6f);
                    m.paint(1f).ellipsoid(0, 0, 0, 0.42f, 0.32f, 0.4f, 12);
                    m.pop();
                    m.paint(1.05f).sphere(0, 2.05f, 0.62f, 0.6f, 18);
                    m.accent(1f).ellipsoid(0, 1.93f, 1.2f, 0.34f, 0.12f, 0.32f, 12);
                    m.paint(0.92f).ellipsoid(-0.9f, 1.18f, -0.1f, 0.18f, 0.42f, 0.78f, 12);
                    m.paint(0.92f).ellipsoid(0.9f, 1.18f, -0.1f, 0.18f, 0.42f, 0.78f, 12);
                    eyes(m, 0.26f, 2.2f, 1.06f, 0.15f);
                }
            }.wheels(0.4f, 0.34f, 0.8f, 0.85f, -0.85f).topper(2.62f, 0.6f),
    };

    public static final int SECRET = 9;

    static {
        ALL[SECRET].secret = true;
    }
}
