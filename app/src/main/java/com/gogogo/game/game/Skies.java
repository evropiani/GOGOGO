package com.gogogo.game.game;

import com.gogogo.game.engine.Renderer;

/** Backgrounds: sky gradient, fog, lighting tint and the decorations floating around the arena. */
public final class Skies {
    private Skies() {}

    public static final String[] NAME = {
            "DREAMY", "BLUE SKY", "SUNSET", "STARRY NIGHT", "DEEP SEA", "CANDYLAND", "OUTER SPACE", "VOLCANO", "SYNTHWAVE", "AURORA"};
    public static final String[] BLURB = {
            "Cotton candy clouds forever.",
            "A perfect day to fall off things.",
            "Golden hour, golden crashes.",
            "Make a wish. Then drive.",
            "Blub blub blub.",
            "Sugar rush included.",
            "In space nobody hears you honk.",
            "Hot hot hot!",
            "Totally radical, dude.",
            "The sky is dancing."};
    public static final int[] RULE = {
            Items.FREE, Items.LEVEL, Items.COINS, Items.LEVEL, Items.LEVEL, Items.COINS, Items.LEVEL, Items.LEVEL, Items.RIMS, Items.LEVEL};
    public static final int[] VALUE = {0, 2, 600, 12, 24, 1800, 38, 52, 150, 80};
    public static final int COUNT = NAME.length;
    public static final int DREAMY = 0, BLUE_SKY = 1, SUNSET = 2, STARRY_NIGHT = 3, DEEP_SEA = 4, CANDYLAND = 5, OUTER_SPACE = 6,
            VOLCANO = 7, SYNTHWAVE = 8, AURORA = 9;

    /** What floats around the arena (drawn by MatchView). */
    public static final int DECOR_CLOUDS = 0, DECOR_STARS = 1, DECOR_BUBBLES = 2, DECOR_CANDY = 3, DECOR_PLANETS = 4,
            DECOR_ROCKS = 5, DECOR_GRID = 6;
    public static final int[] DECOR = {
            DECOR_CLOUDS, DECOR_CLOUDS, DECOR_CLOUDS, DECOR_STARS, DECOR_BUBBLES, DECOR_CANDY, DECOR_PLANETS, DECOR_ROCKS, DECOR_GRID, DECOR_STARS};
    /** Tint for the decorations (clouds, bubbles, ...). */
    public static final int[] DECOR_COLOR = {
            0xFFFFFF, 0xFFFFFF, 0xFFD8C8, 0xFFF6C0, 0xBFF4FF, 0xFFC8E8, 0xC8B8FF, 0x8A6A6E, 0xFF4FD8, 0xC8FFE8};

    // top, mid, bottom gradient; fog; abyss; sky ambient; ground ambient (RGB 0..1); horizon
    // Ambient tints stay mild so the six tile colors read the same under every sky; the abyss (what falling
    // things fade into) matches the lower part of the gradient they are seen against.
    private static final float[][] TOP = {
            {1.0f, 0.80f, 0.86f}, {0.30f, 0.60f, 1.0f}, {1.0f, 0.62f, 0.40f}, {0.05f, 0.06f, 0.20f}, {0.20f, 0.75f, 0.85f},
            {1.0f, 0.78f, 0.92f}, {0.02f, 0.01f, 0.06f}, {0.22f, 0.05f, 0.08f}, {0.10f, 0.02f, 0.26f}, {0.04f, 0.10f, 0.22f}};
    private static final float[][] MID = {
            {0.78f, 0.62f, 0.96f}, {0.62f, 0.84f, 1.0f}, {1.0f, 0.42f, 0.50f}, {0.12f, 0.12f, 0.36f}, {0.06f, 0.42f, 0.62f},
            {0.72f, 0.88f, 1.0f}, {0.10f, 0.05f, 0.25f}, {0.62f, 0.16f, 0.08f}, {0.85f, 0.22f, 0.62f}, {0.15f, 0.55f, 0.50f}};
    private static final float[][] BOTTOM = {
            {0.36f, 0.30f, 0.70f}, {0.85f, 0.94f, 1.0f}, {0.40f, 0.20f, 0.52f}, {0.04f, 0.03f, 0.12f}, {0.02f, 0.12f, 0.28f},
            {0.80f, 0.62f, 1.0f}, {0.04f, 0.02f, 0.12f}, {0.95f, 0.38f, 0.08f}, {0.10f, 0.02f, 0.20f}, {0.06f, 0.04f, 0.16f}};
    private static final float[][] FOG = {
            {1.0f, 0.78f, 0.88f}, {0.80f, 0.90f, 1.0f}, {1.0f, 0.60f, 0.55f}, {0.10f, 0.10f, 0.28f}, {0.08f, 0.40f, 0.58f},
            {0.95f, 0.80f, 1.0f}, {0.08f, 0.04f, 0.20f}, {0.50f, 0.14f, 0.08f}, {0.50f, 0.10f, 0.50f}, {0.10f, 0.30f, 0.35f}};
    private static final float[][] ABYSS = {
            {0.36f, 0.27f, 0.62f}, {0.70f, 0.84f, 1.0f}, {0.38f, 0.18f, 0.45f}, {0.03f, 0.03f, 0.10f}, {0.02f, 0.10f, 0.22f},
            {0.75f, 0.55f, 0.95f}, {0.03f, 0.01f, 0.08f}, {0.95f, 0.36f, 0.06f}, {0.12f, 0.02f, 0.22f}, {0.05f, 0.04f, 0.14f}};
    private static final float[][] SKY_AMB = {
            {0.80f, 0.85f, 1.0f}, {0.85f, 0.92f, 1.0f}, {1.0f, 0.84f, 0.78f}, {0.66f, 0.70f, 1.0f}, {0.70f, 0.92f, 1.0f},
            {0.95f, 0.88f, 1.0f}, {0.70f, 0.66f, 1.0f}, {1.0f, 0.84f, 0.74f}, {0.95f, 0.74f, 1.0f}, {0.70f, 0.95f, 0.92f}};
    private static final float[][] GROUND_AMB = {
            {0.70f, 0.55f, 0.75f}, {0.70f, 0.72f, 0.70f}, {0.70f, 0.45f, 0.50f}, {0.30f, 0.28f, 0.50f}, {0.25f, 0.45f, 0.55f},
            {0.85f, 0.65f, 0.80f}, {0.35f, 0.25f, 0.55f}, {0.75f, 0.36f, 0.20f}, {0.50f, 0.22f, 0.60f}, {0.30f, 0.45f, 0.55f}};
    private static final float[] HORIZON = {0.62f, 0.60f, 0.62f, 0.62f, 0.55f, 0.62f, 0.60f, 0.58f, 0.62f, 0.60f};
    // distance fog starts / is complete
    private static final float[] FOG_NEAR = {60f, 60f, 70f, 70f, 50f, 60f, 80f, 60f, 90f, 90f};
    private static final float[] FOG_FAR = {170f, 180f, 210f, 200f, 150f, 170f, 240f, 180f, 260f, 240f};

    private static int current = -1;

    /** Sets the renderer's sky and lighting colors. Cheap; call every frame. */
    public static void apply(Renderer r, int id) {
        if (id < 0 || id >= COUNT) id = 0;
        if (id == current) return;
        current = id;
        System.arraycopy(TOP[id], 0, r.bgTop, 0, 3);
        System.arraycopy(MID[id], 0, r.bgMid, 0, 3);
        System.arraycopy(BOTTOM[id], 0, r.bgBottom, 0, 3);
        System.arraycopy(FOG[id], 0, r.fog, 0, 3);
        System.arraycopy(ABYSS[id], 0, r.abyss, 0, 3);
        System.arraycopy(SKY_AMB[id], 0, r.skyAmb, 0, 3);
        System.arraycopy(GROUND_AMB[id], 0, r.groundAmb, 0, 3);
        r.horizon = HORIZON[id];
        r.fogNear = FOG_NEAR[id];
        r.fogFar = FOG_FAR[id];
    }

    /** Forces the next apply() to rewrite the renderer (after a GL context loss, for instance). */
    public static void invalidate() {
        current = -1;
    }
}
