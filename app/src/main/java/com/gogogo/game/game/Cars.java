package com.gogogo.game.game;

import com.gogogo.game.engine.MeshBuilder;

/** The car catalog. Index in ALL == CarDef.id (never reorder: saves use the index). */
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
            }.wheels(0.48f, 0.4f, 0.86f, 0.95f, -0.85f).topper(1.58f, 0.86f).hood(2.1f, 0.55f),

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
            }.wheels(0.42f, 0.36f, 0.82f, 0.95f, -0.9f).topper(1.78f, 0.78f).hood(2.1f, 0.6f),

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

            // ---- premium and level-reward cars (moving parts are drawn by CarRenderer, see Art)
            new CarDef(10, "DISCO DASHER", "Groovy moves. Shiny ball. Zero chill.", 0, 4.5f, 4f, 4f, 3.5f, 13, 8) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 0.74f, 0.02f, 1.72f, 0.58f, 2.6f, 0.28f);
                    m.paint(1.06f).box(0, 0.98f, 0.66f, 1.44f, 0.2f, 1.16f, 0.1f);
                    for (int s = -1; s <= 1; s += 2) {
                        m.paint(0.94f).box(s * 0.8f, 0.88f, 0.85f, 0.34f, 0.36f, 0.88f, 0.16f);
                        m.paint(0.94f).box(s * 0.8f, 0.88f, -0.85f, 0.34f, 0.36f, 0.88f, 0.16f);
                    }
                    // bubble cabin with a wrap-around window band
                    m.paint(1.04f).box(0, 1.26f, -0.14f, 1.34f, 0.52f, 1.18f, 0.25f);
                    m.fixed(GLASS).box(0, 1.27f, -0.14f, 1.38f, 0.26f, 0.96f, 0.06f);
                    m.fixed(GLASS).box(0, 1.26f, 0.42f, 1.08f, 0.28f, 0.12f, 0.05f);
                    // chrome bumpers and tail fins
                    m.fixed(0xE4EAF4).box(0, 0.56f, 1.3f, 1.56f, 0.2f, 0.18f, 0.09f);
                    m.fixed(0xE4EAF4).box(0, 0.56f, -1.28f, 1.56f, 0.2f, 0.18f, 0.09f);
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.7f, 1.1f, -1.02f).rotateX(-0.45f);
                        m.accent(1f).box(0, 0, 0, 0.1f, 0.4f, 0.5f, 0.05f);
                        m.pop();
                        m.fixed(0xFF3B5C).box(s * 0.62f, 0.8f, -1.3f, 0.3f, 0.12f, 0.06f, 0.03f, 1);
                    }
                    // light-up dance floor along the sides
                    for (int s = -1; s <= 1; s += 2) {
                        for (int i = 0; i < 4; i++) {
                            m.fixed(DANCE[(i + (s > 0 ? 0 : 2)) % 4]).box(s * 0.875f, 0.72f, -0.54f + i * 0.36f, 0.04f, 0.3f, 0.3f, 0.03f, 1);
                        }
                    }
                    // the stand for the mirror ball
                    float stand = Art.DISCO_Y - Art.DISCO_R - 0.95f;
                    m.fixed(0xE4EAF4).cylinder(0, 0.98f, Art.DISCO_Z, 0.2f, 0, 0.08f, 0.03f, 14);
                    m.fixed(0xE4EAF4).cylinder(0, 0.98f, Art.DISCO_Z, 0.05f, 0, stand + 0.06f, 0.02f, 8);
                    m.fixed(0xFFD23B).sphere(0, 0.98f + stand, Art.DISCO_Z, 0.08f, 8);
                    // round headlights, googly eyes behind gold star shades
                    m.fixed(LIGHT).sphere(-0.56f, 0.84f, 1.24f, 0.13f, 10);
                    m.fixed(LIGHT).sphere(0.56f, 0.84f, 1.24f, 0.13f, 10);
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.3f, 1.21f, 0.86f).rotateX(-PI / 2).rotateY(s * 0.12f);
                        m.fixed(0xFFD23B).extrude(Art.starPoly(0.27f, 0.15f), -0.025f, 0.025f);
                        m.pop();
                    }
                    m.fixed(0xFFD23B).box(0, 1.24f, 0.86f, 0.2f, 0.045f, 0.045f, 0.02f, 1);
                    eyes(m, 0.3f, 1.2f, 0.9f, 0.16f);
                }
            }.wheels(0.42f, 0.34f, 0.82f, 0.85f, -0.85f).topper(1.5f, -0.1f).rims(250),

            new CarDef(11, "BIG CHEESE", "Say cheese! Then say goodbye.", 0, 4f, 4f, 4.5f, 4.5f, 1, 4) {
                public void build(MeshBuilder m) {
                    // a wedge of cheese: side profile as (y, z), extruded across the car
                    m.push().rotateZ(PI / 2);
                    m.paint(1f).extrude(Art.roundPoly(new float[]{0.46f, 1.3f, 1.02f, 1.3f, 1.84f, -1.28f, 0.46f, -1.28f}, 0.14f, 3), -0.84f, 0.84f);
                    m.pop();
                    // waxy rind round the bottom
                    m.accent(1f).box(0, 0.52f, 0.01f, 1.76f, 0.16f, 2.64f, 0.07f);
                    // holes: each bar pokes out on both sides
                    for (int i = 0; i < HOLES.length; i += 3) {
                        m.push().translate(0, HOLES[i], HOLES[i + 1]).rotateZ(PI / 2);
                        m.paint(0.74f).cylinder(0, 0, 0, HOLES[i + 2], -0.86f, 0.86f, 0.02f, 14);
                        m.pop();
                    }
                    for (int i = 0; i < TOP_HOLES.length; i += 3) {
                        float z = TOP_HOLES[i + 1];
                        m.push().translate(TOP_HOLES[i], 1.03f + (1.3f - z) * 0.318f, z).rotateX(0.31f);
                        m.paint(0.74f).cylinder(0, 0, 0, TOP_HOLES[i + 2], -0.05f, 0.012f, 0.01f, 14);
                        m.pop();
                    }
                    // cheese-board flag
                    m.fixed(0xD8A060).cylinder(0.42f, 1.5f, -1.02f, 0.025f, 0, 0.62f, 0.01f, 6);
                    m.accent(1f).box(0.42f, 2.0f, -1.2f, 0.04f, 0.2f, 0.34f, 0.02f);
                    // big cheesy grin
                    m.fixed(0x5A2A1A).box(0, 0.76f, 1.31f, 0.86f, 0.22f, 0.06f, 0.1f);
                    for (int i = 0; i < 4; i++) m.fixed(0xFFFFFF).box(-0.24f + i * 0.16f, 0.83f, 1.335f, 0.13f, 0.08f, 0.04f, 0.02f);
                    eyes(m, 0.34f, 1.12f, 1.16f, 0.18f);
                }
            }.wheels(0.42f, 0.36f, 0.84f, 0.85f, -0.85f).topper(1.44f, -0.1f).hood(1.82f, 0.1f).level(50),

            new CarDef(12, "UNICORN DREAM", "Powered by glitter and pure confidence.", 0, 4.5f, 4.5f, 4.5f, 3.5f, 18, 11) {
                public void build(MeshBuilder m) {
                    m.paint(1f).box(0, 0.8f, -0.05f, 1.66f, 0.64f, 2.5f, 0.32f);
                    m.paint(1.02f).box(0, 0.94f, 0.82f, 1.32f, 0.42f, 0.9f, 0.2f);
                    m.paint(1.04f).ellipsoid(0, 1.16f, -0.28f, 0.68f, 0.44f, 0.9f, 18);
                    m.fixed(GLASS).ellipsoid(0, 1.22f, -0.22f, 0.7f, 0.22f, 0.72f, 16);
                    // rainbow mane swept back over the cabin, and a curly rainbow tail
                    for (int i = 0; i < 6; i++) {
                        float z = 0.16f - i * 0.24f;
                        float k = (z + 0.28f) / 0.9f;
                        float y = 1.16f + 0.44f * (float) Math.sqrt(Math.max(0f, 1f - k * k));
                        m.push().translate(0, y, z).rotateX(-0.55f);
                        m.fixed(RAINBOW[i]).ellipsoid(0, 0.06f, 0, 0.12f, 0.22f - i * 0.012f, 0.15f, 10);
                        m.pop();
                    }
                    for (int i = 0; i < 4; i++) {
                        m.push().translate(0, TAIL[i * 2], TAIL[i * 2 + 1]).rotateX(0.9f - i * 0.6f);
                        m.fixed(RAINBOW[(i + 1) % 6]).ellipsoid(0, 0, 0, 0.12f - i * 0.015f, 0.17f - i * 0.025f, 0.12f - i * 0.015f, 10);
                        m.pop();
                    }
                    // ears and horn
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.32f, 1.46f, 0.2f).rotateZ(-s * 0.35f).rotateX(-0.2f);
                        m.paint(1f).cone(0, 0, 0, 0.12f, 0.02f, 0.3f, 10);
                        m.fixed(0xFFB0D8).cone(0, 0.02f, 0.05f, 0.07f, 0.01f, 0.22f, 8);
                        m.pop();
                    }
                    m.push().translate(0, 1.2f, 0.92f).rotateX(0.95f);
                    m.fixed(0xFFD86B).cone(0, 0, 0, 0.16f, 0.015f, 0.76f, 16);
                    for (int i = 0; i < 12; i++) {
                        float t = i / 12f;
                        double a = t * Math.PI * 5;
                        float rr = 0.16f * (1 - t) + 0.005f;
                        m.fixed(0xFFF4F8).sphere((float) Math.cos(a) * rr, t * 0.7f + 0.02f, (float) Math.sin(a) * rr, 0.045f * (1 - t * 0.6f), 6);
                    }
                    m.pop();
                    // little feathered wings
                    for (int s = -1; s <= 1; s += 2) {
                        for (int f = 0; f < 3; f++) {
                            m.push().translate(s * 0.86f, 1.0f + f * 0.04f, -0.2f).rotateZ(-s * (0.25f + f * 0.2f)).rotateX(0.35f + f * 0.25f);
                            m.accent(1f - f * 0.05f).ellipsoid(0, 0, -0.26f, 0.05f, 0.1f, 0.34f - f * 0.05f, 10);
                            m.pop();
                        }
                    }
                    // blush, lashes, eyes
                    for (int s = -1; s <= 1; s += 2) {
                        m.fixed(0xFF9EC8).ellipsoid(s * 0.55f, 0.95f, 1.24f, 0.13f, 0.07f, 0.04f, 8);
                        for (int l = 0; l < 3; l++) {
                            m.push().translate(s * (0.24f + l * 0.07f), 1.33f - Math.abs(l - 1) * 0.0f + (l == 2 ? -0.03f : 0f), 1.17f).rotateZ(-s * (0.2f + l * 0.35f));
                            m.fixed(0x1A1028).box(0, 0.07f, 0, 0.03f, 0.13f, 0.03f, 0.012f);
                            m.pop();
                        }
                    }
                    m.fixed(0x1A1028).sphere(-0.12f, 0.84f, 1.28f, 0.035f, 6);
                    m.fixed(0x1A1028).sphere(0.12f, 0.84f, 1.28f, 0.035f, 6);
                    eyes(m, 0.3f, 1.16f, 1.12f, 0.17f);
                }
            }.wheels(0.4f, 0.32f, 0.8f, 0.85f, -0.85f).topper(1.46f, 0.38f).hood(2.25f, -0.1f).rims(400),

            new CarDef(13, "TRAFFIC TITAN", "Red means go. Yellow means go. Green means GO GO GO!", 0, 5.5f, 5.5f, 5.5f, 5.5f, 15, 16) {
                public void build(MeshBuilder m) {
                    // wide, low body with muscular fenders and gold pinstripes
                    m.paint(1f).box(0, 0.74f, 0.02f, 1.8f, 0.56f, 2.72f, 0.26f);
                    m.paint(1.1f).box(0, 0.98f, 0.8f, 1.44f, 0.2f, 1.06f, 0.1f);
                    for (int s = -1; s <= 1; s += 2) {
                        m.paint(1.06f).box(s * 0.8f, 0.9f, 0.9f, 0.4f, 0.44f, 1.0f, 0.18f);
                        m.paint(1.06f).box(s * 0.8f, 0.94f, -0.88f, 0.42f, 0.5f, 1.04f, 0.18f);
                        m.accent(1f).box(s * 0.905f, 0.74f, 0.0f, 0.04f, 0.09f, 0.8f, 0.02f, 1);
                        m.accent(1f).box(s * 0.66f, 1.12f, 0.9f, 0.08f, 0.05f, 0.9f, 0.025f, 1);
                        m.accent(1f).box(s * 0.66f, 1.19f, -0.88f, 0.08f, 0.05f, 0.92f, 0.025f, 1);
                        // side intakes behind the front wheels
                        m.fixed(0x1A1A24).box(s * 0.9f, 0.82f, 0.28f, 0.04f, 0.2f, 0.3f, 0.02f, 1);
                    }
                    // cabin with tinted glass and a gold roof stripe
                    m.paint(0.94f).box(0, 1.3f, -0.02f, 1.3f, 0.5f, 1.1f, 0.24f);
                    m.fixed(0x1E2238).box(0, 1.31f, -0.02f, 1.34f, 0.26f, 0.9f, 0.06f);
                    m.fixed(0x1E2238).box(0, 1.3f, 0.51f, 1.08f, 0.28f, 0.1f, 0.05f);
                    m.accent(1f).box(0, 1.56f, -0.28f, 0.2f, 0.04f, 0.42f, 0.02f, 1);
                    // gold bumper with hazard stripes, grille, headlights
                    m.accent(1f).box(0, 0.6f, 1.4f, 1.56f, 0.22f, 0.16f, 0.05f);
                    for (int i = -3; i <= 3; i++) {
                        m.push().translate(i * 0.19f, 0.6f, 1.485f).rotateZ(0.7f);
                        m.fixed(0x22222E).box(0, 0, 0, 0.07f, 0.17f, 0.02f, 0.01f, 1);
                        m.pop();
                    }
                    m.fixed(0x22222E).box(0, 0.86f, 1.38f, 0.78f, 0.18f, 0.06f, 0.04f);
                    for (int i = 0; i < 3; i++) m.accent(1f).box(0, 0.81f + i * 0.05f, 1.41f, 0.7f, 0.018f, 0.02f, 0f);
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.6f, 0.86f, 1.3f).rotateX(PI / 2);
                        m.accent(1f).cylinder(0, 0, 0, 0.16f, 0f, 0.08f, 0.02f, 12);
                        m.pop();
                        m.fixed(LIGHT).sphere(s * 0.6f, 0.86f, 1.38f, 0.12f, 10);
                    }
                    // crown ornament on the nose
                    m.push().translate(0, 1.06f, 1.24f).scale(0.42f);
                    crown(m);
                    m.pop();
                    // rear: dark diffuser and twin chrome exhausts
                    m.fixed(0x22222E).box(0, 0.56f, -1.32f, 1.1f, 0.16f, 0.12f, 0.05f);
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.42f, 0.58f, -1.3f).rotateX(-PI / 2);
                        m.fixed(0xE4EAF4).cylinder(0, 0, 0, 0.1f, 0, 0.16f, 0.03f, 10);
                        m.fixed(0x22222E).cylinder(0, 0, 0, 0.06f, 0.1f, 0.17f, 0f, 8);
                        m.pop();
                    }
                    // the rear wing is a traffic light: gold struts, dark housing, lens rims and visors on both faces
                    float ly = Art.TITAN_LAMP_Y, lz = Art.TITAN_LAMP_Z, lr = Art.TITAN_LAMP_R;
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.52f, 1.32f, lz + 0.04f).rotateX(-0.25f);
                        m.accent(1f).box(0, 0, 0, 0.1f, 0.42f, 0.2f, 0.04f);
                        m.pop();
                    }
                    m.fixed(0x262633).box(0, ly, lz, 1.5f, 0.5f, 0.36f, 0.2f);
                    m.accent(1f).box(0, ly, lz, 1.58f, 0.56f, 0.2f, 0.1f);
                    for (int i = 0; i < 3; i++) {
                        float x = Art.TITAN_LAMP_X[i];
                        for (int s = -1; s <= 1; s += 2) {
                            m.push().translate(x, ly, lz + s * 0.19f).rotateX(PI / 2);
                            m.accent(1f).torus(0, 0, 0, lr + 0.02f, 0.035f, 18, 6);
                            m.pop();
                            m.push().translate(x, ly, lz + s * 0.17f).rotateY(s > 0 ? 0f : PI).rotateX(-0.12f);
                            m.accent(0.92f);
                            Art.visor(m, lr + 0.03f, 0.035f, 0.2f, -0.2f, PI + 0.2f, 12);
                            m.pop();
                        }
                    }
                    // golden feather wings on the ends of the light
                    for (int s = -1; s <= 1; s += 2) {
                        for (int f = 0; f < 3; f++) {
                            m.push().translate(s * 0.74f, ly + 0.06f, lz - 0.02f).rotateX(-0.35f).rotateZ(-s * (0.35f + f * 0.42f));
                            m.fixed(f == 1 ? 0xFFE07A : 0xFFC83D).ellipsoid(0, 0.3f - f * 0.04f, 0, 0.09f, 0.32f - f * 0.05f, 0.05f, 10);
                            m.pop();
                        }
                    }
                    // determined brows over the googly eyes
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(s * 0.34f, 1.42f, 1.08f).rotateZ(s * 0.28f);
                        m.fixed(0x22222E).box(0, 0, 0, 0.34f, 0.07f, 0.1f, 0.03f);
                        m.pop();
                    }
                    eyes(m, 0.34f, 1.2f, 1.02f, 0.18f);
                }
            }.wheels(0.47f, 0.4f, 0.86f, 0.92f, -0.9f).topper(1.56f, -0.02f).level(100),
    };

    /** Gold crown with traffic light jewels: base radius 0.42, about 0.6 tall. */
    private static void crown(MeshBuilder m) {
        m.fixed(0xFFC83D).lathe(new float[]{0, 0, 0.4f, 0, 0.4f, 0, 0.42f, 0.3f, 0.42f, 0.3f, 0.34f, 0.3f, 0.34f, 0.3f, 0.32f, 0.04f, 0.32f, 0.04f, 0, 0.04f}, 18);
        for (int k = 0; k < 5; k++) {
            double a = PI / 2 + k * Math.PI * 2 / 5;
            float cx = (float) Math.cos(a) * 0.37f, cz = (float) Math.sin(a) * 0.37f;
            m.fixed(0xFFC83D).cone(cx, 0.28f, cz, 0.08f, 0.01f, 0.26f, 8);
            m.fixed(0xFFF0A0).sphere(cx, 0.56f, cz, 0.05f, 6);
        }
        m.fixed(0xFF3B3B).sphere(0, 0.16f, 0.42f, 0.08f, 8);
        m.fixed(0xFFD21F).sphere(0.3f, 0.16f, 0.3f, 0.07f, 8);
        m.fixed(0x34D058).sphere(-0.3f, 0.16f, 0.3f, 0.07f, 8);
    }

    private static final int[] DANCE = {0xFF4FA3, 0xFFD23B, 0x3BE0FF, 0xA46BFF};
    private static final int[] RAINBOW = {0xFF6FB5, 0xFFA45C, 0xFFE15C, 0x7EE87A, 0x5CC8FF, 0xA88BFF};
    /** UNICORN DREAM tail pieces (y, z), curling up behind the car. */
    private static final float[] TAIL = {1.08f, -1.3f, 1.2f, -1.44f, 1.26f, -1.58f, 1.18f, -1.68f};
    /** BIG CHEESE holes on the sides (y, z, radius) and on the slope (x, z, radius). */
    private static final float[] HOLES = {0.98f, 0.05f, 0.17f, 1.34f, -0.62f, 0.2f, 1.0f, 0.86f, 0.09f, 1.62f, -1.06f, 0.12f, 0.68f, -0.16f, 0.09f};
    private static final float[] TOP_HOLES = {-0.36f, 0.42f, 0.13f, 0.3f, -0.5f, 0.16f, -0.18f, -1.0f, 0.1f, 0.5f, 0.75f, 0.08f};

    public static final int SECRET = 9;
    /** The level 100 car. */
    public static final int TITAN = 13;
    /** The car with the spinning mirror ball (drawn by CarRenderer). */
    public static final int DISCO = 10;

    static {
        ALL[SECRET].secret = true;
        ALL[SECRET].exclusive = true;
    }

    /** True for cars random bots may drive. */
    public static boolean forBots(int id) {
        CarDef d = ALL[id];
        return !d.secret && !d.exclusive && d.levelReq == 0;
    }
}
