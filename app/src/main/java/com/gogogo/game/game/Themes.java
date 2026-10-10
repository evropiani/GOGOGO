package com.gogogo.game.game;

/** Tile color themes. Every theme keeps the same six hues so the color names stay true. */
public final class Themes {
    private Themes() {}

    public static final String[] NAME = {"CLASSIC", "PASTEL", "NEON", "RETRO", "JEWEL", "SORBET", "MIDNIGHT"};
    public static final String[] BLURB = {
            "Bright and bubbly.",
            "Soft as a marshmallow.",
            "Turn it up to eleven.",
            "Groovy, baby.",
            "Precious and shiny.",
            "Cool, sweet, slightly melty.",
            "Colors for night owls."};
    public static final int[] RULE = {Items.FREE, Items.LEVEL, Items.COINS, Items.LEVEL, Items.LEVEL, Items.RIMS, Items.LEVEL};
    public static final int[] VALUE = {0, 6, 1000, 22, 46, 80, 70};
    public static final int COUNT = NAME.length;

    /** PINK, YELLOW, BLUE, GREEN, ORANGE, PURPLE for each theme. */
    public static final int[][] COLORS = {
            {0xFF4FA3, 0xFFD23B, 0x3BB8FF, 0x5EE65A, 0xFF8A2B, 0xA46BFF},
            {0xFF9CC2, 0xFFEC70, 0x8ACFFF, 0x84E68A, 0xFFA060, 0xB89CFF},
            {0xFF1F8F, 0xFFF01F, 0x1FD5FF, 0x39FF3A, 0xFF7A00, 0xB026FF},
            {0xE86A92, 0xF2C14E, 0x4D9DE0, 0x7FB069, 0xE07A3F, 0x8E6CB8},
            {0xD81B7A, 0xF5B700, 0x1565C0, 0x00A86B, 0xE65100, 0x6A1B9A},
            {0xFF7EB6, 0xFFE066, 0x6EC6FF, 0x7EE8A2, 0xFFA45E, 0xB18CFF},
            {0xC81E5E, 0xCCA42A, 0x2270C4, 0x2E9058, 0xCC5E1C, 0x7440B4}};

    private static int current = -1;

    /** Makes Palette.TILE use this theme's colors. */
    public static void apply(int id) {
        if (id < 0 || id >= COUNT) id = 0;
        if (id == current) return;
        current = id;
        System.arraycopy(COLORS[id], 0, Palette.TILE, 0, Palette.TILE.length);
    }
}
