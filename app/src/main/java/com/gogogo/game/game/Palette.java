package com.gogogo.game.game;

/** Tile colors, cosmetic catalogs and shared UI colors. */
public final class Palette {
    private Palette() {}

    // ---- tile colors (neon + pastel, each paired with a symbol for color-blind players)
    public static final int[] TILE = {0xFF4FA3, 0xFFD23B, 0x3BB8FF, 0x5EE65A, 0xFF8A2B, 0xA46BFF};
    public static final String[] TILE_NAME = {"PINK", "YELLOW", "BLUE", "GREEN", "ORANGE", "PURPLE"};

    // ---- UI
    public static final int INK = 0xFF2A1840;       // outlines / dark text
    public static final int CREAM = 0xFFFFF6E8;
    public static final int PANEL = 0xFFFFFFFF;
    public static final int BTN_GO = 0xFF34D058;
    public static final int BTN_PINK = 0xFFFF4FA3;
    public static final int BTN_BLUE = 0xFF3BA8FF;
    public static final int BTN_PURPLE = 0xFF8E62FF;
    public static final int BTN_ORANGE = 0xFFFF9A2B;
    public static final int BTN_YELLOW = 0xFFFFC21F;
    public static final int GOLD = 0xFFFFD23F;

    // ---- paints (negative values are animated paints)
    public static final int RAINBOW = -1, TRAFFIC = -2, GALAXY = -3, LAVA = -4;
    public static final int[] PAINT = {
            0xFF5FA8, 0xFFD23F, 0x4FC3FF, 0x7CE84F, 0xFF8A3D, 0x9B6BFF, 0xFF3B5C, 0x5CF2C2,
            0xF4F4FF, 0x3A3F7A, 0xFFB59E, 0xC9A8FF, 0xB8FF2E, 0xFF2EC4, 0x2E7BFF, 0x45455A,
            0xFFC83D, RAINBOW, 0xFFF0F5, 0xC8803A, 0xD8E0EA, GALAXY, LAVA, TRAFFIC};
    public static final String[] PAINT_NAME = {
            "Bubblegum", "Sunny", "Sky", "Lime", "Tangerine", "Grape", "Cherry", "Mint",
            "Snow", "Midnight", "Peach", "Lavender", "Toxic", "Magenta", "Ocean", "Coal",
            "Gold Rush", "Rainbow", "Pearl", "Bronze", "Chrome", "Galaxy", "Lava", "Traffic"};
    /** How each paint is unlocked (Items.FREE / LEVEL / COINS / RIMS) and the matching level or price. */
    public static final int[] PAINT_RULE = {
            0, 0, 0, 0, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 1, 2, 1, 3, 1};
    public static final int[] PAINT_PRICE = {
            0, 0, 0, 0, 100, 100, 150, 150,
            200, 200, 250, 250, 300, 300, 350, 350,
            900, 1600, 400, 9, 1200, 48, 60, 66};

    public static final String[] TOPPER_NAME = {
            "None", "Antenna Ball", "Traffic Cone", "Party Hat", "Propeller Cap", "Chef Hat",
            "Banana", "Shark Fin", "Flower", "Top Hat", "Viking Horns", "Crown", "Halo",
            "Bunny Ears", "Wizard Hat", "Pirate Hat", "Unicorn Horn", "Headphones", "Disco Ball",
            "Traffic Light", "Laurel Wreath"};
    public static final int[] TOPPER_RULE = {0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 3, 1, 1, 1, 1};
    public static final int[] TOPPER_PRICE = {0, 100, 150, 200, 250, 300, 300, 350, 250, 400, 450, 650, 800, 7, 500, 26, 50, 34, 44, 85, 99};

    public static final String[] WHEEL_NAME = {"Classic", "Candy", "Neon", "Donut", "Gold", "Pizza", "Rainbow", "Spiky", "Flames", "Diamond"};
    public static final int[] WHEEL_RULE = {0, 2, 2, 2, 2, 1, 2, 1, 3, 1};
    public static final int[] WHEEL_PRICE = {0, 200, 300, 450, 800, 11, 900, 40, 70, 90};

    /** Animated rainbow color. */
    public static int rainbow(float t) {
        float h = (t * 0.25f) % 1f;
        return hsv(h, 0.65f, 1f);
    }

    /** True for paints a random bot may wear (plain colors you can buy with coins). */
    public static boolean botPaint(int i) {
        return PAINT[i] >= 0 && PAINT_RULE[i] <= 2 && PAINT_PRICE[i] < 900;
    }

    static final int[] TRAFFIC_COLORS = {0xFF3B3B, 0xFFD21F, 0x34D058};

    /** Animated paints: cycles through red, yellow and green like a traffic light. */
    public static int traffic(float t) {
        float k = (t * 0.6f) % 3f;
        int[] c = TRAFFIC_COLORS;
        int i = (int) k;
        float f = k - i;
        f = f < 0.75f ? 0f : (f - 0.75f) / 0.25f;
        return mix(c[i], c[(i + 1) % 3], f);
    }

    public static int hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f * 6f;
        int i = (int) h;
        float f = h - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int paintColor(int paint, float time) {
        int c = PAINT[Math.max(0, Math.min(PAINT.length - 1, paint))];
        switch (c) {
            case RAINBOW: return rainbow(time);
            case TRAFFIC: return traffic(time);
            case GALAXY: return hsv(0.62f + (float) Math.sin(time * 0.7f) * 0.14f, 0.7f, 0.85f + (float) Math.sin(time * 2.1f) * 0.1f);
            case LAVA: return mix(0xFF2A00, 0xFFA21F, 0.5f + (float) Math.sin(time * 2.6f) * 0.5f);
            default: return c;
        }
    }

    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
