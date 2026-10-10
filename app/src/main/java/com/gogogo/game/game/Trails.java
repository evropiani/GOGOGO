package com.gogogo.game.game;

/** Boost trails: what comes out of the back of a car while it boosts. */
public final class Trails {
    private Trails() {}

    public static final String[] NAME = {
            "PUFF", "FIRE", "BUBBLES", "STARS", "HEARTS", "RAINBOW", "LIGHTNING", "CONFETTI", "TRAFFIC LIGHTS", "GOLD RUSH"};
    public static final String[] BLURB = {
            "A classic cloud of zoom.",
            "Spicy exhaust.",
            "Squeaky clean speed.",
            "Twinkle twinkle, out of my way.",
            "Drive with love.",
            "Taste the speed.",
            "Bzzzt!",
            "Every bump is a party.",
            "Red, yellow, GO!",
            "Leave a trail of riches."};
    public static final int[] RULE = {
            Items.FREE, Items.LEVEL, Items.COINS, Items.LEVEL, Items.COINS, Items.LEVEL, Items.RIMS, Items.LEVEL, Items.LEVEL, Items.LEVEL};
    public static final int[] VALUE = {0, 4, 500, 16, 1200, 30, 100, 58, 75, 95};
    public static final int COUNT = NAME.length;
    public static final int PUFF = 0, FIRE = 1, BUBBLES = 2, STARS = 3, HEARTS = 4, RAINBOW = 5, LIGHTNING = 6, CONFETTI = 7,
            TRAFFIC = 8, GOLD = 9;

    private static final int[] RAINBOW_COLORS = {0xFF3B4F, 0xFF9A2B, 0xFFE03B, 0x4FE65A, 0x3BA8FF, 0xA46BFF};
    private static final int[] PARTY_COLORS = {0xFF4FA3, 0xFFD23B, 0x3BD8FF, 0x7CFF5A, 0xFF8A2B, 0xB98BFF, 0xFFFFFF};
    private static final int[] HEART_COLORS = {0xFF3B6E, 0xFF6FA8, 0xFF2E50};
    private static final int[] STAR_COLORS = {0xFFE14D, 0xFFFFFF, 0xFFC23B};
    private static final int[] LIGHT_COLORS = {0xFF3B3B, 0xFFD21F, 0x34D058};

    private static float rnd(float a, float b) {
        return a + (float) Math.random() * (b - a);
    }

    private static int pick(int[] colors) {
        return colors[(int) (Math.random() * colors.length)];
    }

    /**
     * Emits one frame's worth of trail particles behind a boosting car (called on about 60% of its boost frames).
     * (x, y, z) is the spot behind the car, (bx, bz) the backwards direction.
     */
    public static void emit(Particles fx, int trail, float x, float y, float z, float bx, float bz, float time) {
        // a little sideways jitter
        float sx = -bz, sz = bx;
        float j = rnd(-0.35f, 0.35f);
        x += sx * j;
        z += sz * j;
        int i;
        switch (trail) {
            case FIRE:
                i = fx.spawn(Particles.BALL, x, y - 0.1f, z, bx * 3f + rnd(-1f, 1f), rnd(0.8f, 2.4f), bz * 3f + rnd(-1f, 1f),
                        rnd(0.4f, 0.62f), rnd(0.3f, 0.42f), 0xFFE84A, -2.5f);
                fx.fade(i, 0xFF2A0A);
                fx.glow(i, 0.12f);
                fx.drag(i, 3f);
                if (Math.random() < 0.4) {
                    i = fx.spawn(Particles.CUBE, x, y, z, bx * 4f + rnd(-2f, 2f), rnd(3f, 5f), bz * 4f + rnd(-2f, 2f),
                            rnd(0.1f, 0.16f), rnd(0.4f, 0.6f), 0xFFA020, 9f);
                    fx.glow(i, 0.45f);
                }
                break;
            case BUBBLES:
                i = fx.spawn(Particles.BUBBLE, x, y + rnd(-0.2f, 0.3f), z, bx * 2f + rnd(-0.8f, 0.8f), rnd(1f, 2.2f), bz * 2f + rnd(-0.8f, 0.8f),
                        rnd(0.18f, 0.36f), rnd(0.6f, 0.9f), Math.random() < 0.5 ? 0xBFF4FF : 0xD8E4FF, -0.6f);
                fx.drag(i, 2.5f);
                fx.glow(i, 0.15f);
                break;
            case STARS:
                i = fx.spawn(Particles.STAR, x, y + rnd(-0.1f, 0.4f), z, bx * 2.5f + rnd(-1.5f, 1.5f), rnd(2f, 4f), bz * 2.5f + rnd(-1.5f, 1.5f),
                        rnd(0.24f, 0.36f), rnd(0.45f, 0.65f), pick(STAR_COLORS), 7f);
                fx.glow(i, 0.3f);
                fx.drag(i, 2f);
                break;
            case HEARTS:
                i = fx.spawn(Particles.HEART, x, y + rnd(0f, 0.4f), z, bx * 1.5f + rnd(-0.6f, 0.6f), rnd(1.6f, 2.6f), bz * 1.5f + rnd(-0.6f, 0.6f),
                        rnd(0.28f, 0.42f), rnd(0.6f, 0.85f), pick(HEART_COLORS), -0.4f);
                fx.drag(i, 2.2f);
                fx.glow(i, 0.12f);
                fx.spin(i, rnd(4f, 7f), 0);
                break;
            case RAINBOW: {
                // a short column of colored blocks; as the car moves on they line up into a rainbow ribbon
                float yaw = (float) Math.atan2(bx, bz);
                x -= sx * j;
                z -= sz * j;
                for (int k = 0; k < RAINBOW_COLORS.length; k++) {
                    i = fx.spawn(Particles.CUBE, x, y + 0.42f - k * 0.17f, z, 0, 0, 0, 0.24f, 0.45f, RAINBOW_COLORS[k], 0f);
                    fx.orient(i, 0, yaw);
                    fx.spin(i, 0, 0);
                    fx.glow(i, 0.1f);
                }
                break;
            }
            case LIGHTNING:
                if (Math.random() < 0.6) {
                    i = fx.spawn(Particles.BOLT, x, y + rnd(0.1f, 0.6f), z, 0, 0, 0, rnd(0.65f, 0.95f), rnd(0.14f, 0.22f),
                            Math.random() < 0.7 ? 0xFFEE3A : 0x7FE8FF, 0f);
                    fx.orient(i, rnd(-0.6f, 0.6f), 0);
                    fx.spin(i, 0, 0);
                    fx.glow(i, 0.3f);
                }
                for (int k = 0; k < 2; k++) {
                    i = fx.spawn(Particles.CUBE, x, y, z, bx * 3f + rnd(-3f, 3f), rnd(1f, 4f), bz * 3f + rnd(-3f, 3f),
                            rnd(0.08f, 0.13f), rnd(0.2f, 0.32f), Math.random() < 0.5 ? 0x6FE8FF : 0xFFF27A, 6f);
                    fx.glow(i, 0.5f);
                }
                break;
            case CONFETTI:
                for (int k = 0; k < 2; k++) {
                    i = fx.spawn(Particles.CUBE, x, y + 0.2f, z, bx * 3f + rnd(-2.5f, 2.5f), rnd(3f, 6f), bz * 3f + rnd(-2.5f, 2.5f),
                            rnd(0.14f, 0.2f), rnd(0.6f, 0.8f), pick(PARTY_COLORS), 12f);
                    fx.drag(i, 1.5f);
                }
                break;
            case TRAFFIC: {
                // red, yellow, green lights popping out in turn
                int c = LIGHT_COLORS[((int) (time * 9f)) % 3];
                i = fx.spawn(Particles.BALL, x, y + 0.15f, z, bx * 1.5f, rnd(0.3f, 0.8f), bz * 1.5f, rnd(0.3f, 0.36f), 0.5f, c, 0f);
                fx.drag(i, 3f);
                fx.glow(i, 0.3f);
                break;
            }
            case GOLD:
                if (Math.random() < 0.55) {
                    i = fx.spawn(Particles.COIN, x, y + 0.2f, z, bx * 3f + rnd(-1.5f, 1.5f), rnd(4f, 6.5f), bz * 3f + rnd(-1.5f, 1.5f),
                            rnd(0.32f, 0.42f), rnd(0.55f, 0.7f), 0xFFFFFF, 16f);
                    fx.spin(i, 0, rnd(10f, 16f));
                    fx.drag(i, 1.2f);
                } else {
                    fx.sparkle(x + rnd(-0.4f, 0.4f), y + rnd(0f, 0.6f), z + rnd(-0.4f, 0.4f), rnd(0.3f, 0.45f), Math.random() < 0.6 ? 0xFFE27A : 0xFFFFFF);
                }
                break;
            default:
                fx.puff(x, y, z, 1, Math.random() < 0.5 ? 0xFFB23B : 0xFFE14D, 1.5f);
                break;
        }
    }

    /** The burst at the moment a boost starts, in the trail's own style. */
    public static void burst(Particles fx, int trail, float x, float y, float z) {
        int i;
        switch (trail) {
            case FIRE:
                for (int k = 0; k < 6; k++) {
                    float a = rnd(0f, 6.283f);
                    i = fx.spawn(Particles.BALL, x, y, z, (float) Math.cos(a) * 3f, rnd(1f, 3f), (float) Math.sin(a) * 3f,
                            rnd(0.4f, 0.6f), rnd(0.35f, 0.5f), 0xFFE84A, -2f);
                    fx.fade(i, 0xFF2A0A);
                    fx.glow(i, 0.12f);
                    fx.drag(i, 4f);
                }
                break;
            case BUBBLES:
                for (int k = 0; k < 6; k++) {
                    float a = rnd(0f, 6.283f);
                    i = fx.spawn(Particles.BUBBLE, x, y, z, (float) Math.cos(a) * 2.5f, rnd(1f, 2.5f), (float) Math.sin(a) * 2.5f,
                            rnd(0.2f, 0.4f), rnd(0.6f, 0.9f), 0xBFF4FF, -0.6f);
                    fx.drag(i, 3f);
                    fx.glow(i, 0.15f);
                }
                break;
            case STARS:
                fx.stars(x, y + 0.6f, z, 5);
                break;
            case HEARTS:
                for (int k = 0; k < 4; k++) {
                    float a = rnd(0f, 6.283f);
                    i = fx.spawn(Particles.HEART, x, y + 0.4f, z, (float) Math.cos(a) * 2f, rnd(2f, 3f), (float) Math.sin(a) * 2f,
                            rnd(0.32f, 0.45f), rnd(0.6f, 0.8f), pick(HEART_COLORS), -0.4f);
                    fx.drag(i, 2.5f);
                    fx.glow(i, 0.12f);
                }
                break;
            case RAINBOW:
                for (int k = 0; k < RAINBOW_COLORS.length; k++) fx.puff(x, y, z, 1, RAINBOW_COLORS[k], 3f);
                break;
            case LIGHTNING:
                for (int k = 0; k < 2; k++) {
                    i = fx.spawn(Particles.BOLT, x + rnd(-0.6f, 0.6f), y + 0.6f, z + rnd(-0.6f, 0.6f), 0, 0, 0, rnd(0.7f, 0.9f), 0.2f, 0xFFEE3A, 0f);
                    fx.orient(i, rnd(-0.5f, 0.5f), 0);
                    fx.spin(i, 0, 0);
                    fx.glow(i, 0.3f);
                }
                break;
            case CONFETTI:
                fx.confetti(x, y + 0.5f, z, 14, 0.7f);
                break;
            case TRAFFIC:
                for (int k = 0; k < 3; k++) {
                    float a = k * 2.094f + rnd(0f, 1f);
                    i = fx.spawn(Particles.BALL, x, y + 0.3f, z, (float) Math.cos(a) * 2.5f, 1.5f, (float) Math.sin(a) * 2.5f, 0.38f, 0.5f, LIGHT_COLORS[k], 0f);
                    fx.drag(i, 3f);
                    fx.glow(i, 0.3f);
                }
                break;
            case GOLD:
                for (int k = 0; k < 4; k++) {
                    float a = rnd(0f, 6.283f);
                    i = fx.spawn(Particles.COIN, x, y + 0.3f, z, (float) Math.cos(a) * 2f, rnd(5f, 7f), (float) Math.sin(a) * 2f,
                            rnd(0.34f, 0.42f), rnd(0.6f, 0.75f), 0xFFFFFF, 16f);
                    fx.spin(i, 0, rnd(10f, 16f));
                }
                break;
            default:
                fx.puff(x, y, z, 6, 0xFFFFFF, 3f);
                break;
        }
    }
}
