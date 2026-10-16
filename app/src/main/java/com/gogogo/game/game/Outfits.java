package com.gogogo.game.game;

import com.gogogo.game.engine.Rng;

/**
 * The drivers' wardrobe: hats, hair, faces, skin tones, shirts, pants, shoes and things to hold. A look is one item
 * per part (an int[PARTS]); item 0 of every part is a free basic. Items sells and unlocks them like everything else
 * (categories Items.HAT .. Items.HELD, in part order); Art builds their meshes (see the drivers block there).
 */
public final class Outfits {
    private Outfits() {}

    public static final int HAT = 0, HAIR = 1, FACE = 2, SKIN = 3, SHIRT = 4, PANTS = 5, SHOES = 6, HELD = 7;
    public static final int PARTS = 8;
    /** Tab names of the DRIVER screen. */
    public static final String[] PART_NAME = {"HAT", "HAIR", "FACE", "SKIN", "SHIRT", "PANTS", "SHOES", "HANDS"};

    private static final int F = Items.FREE, L = Items.LEVEL, C = Items.COINS, R = Items.RIMS;

    // hats
    public static final int H_NONE = 0, H_CAP = 1, H_BEANIE = 2, H_BUCKET = 3, H_HARD = 4, H_COWBOY = 5, H_PROPELLER = 6,
            H_HELMET = 7, H_CONE = 8, H_VIKING = 9, H_SHARK = 10, H_TOPHAT = 11, H_CROWN = 12, H_SPACE = 13;
    // hair
    public static final int HR_BUZZ = 0, HR_BALD = 1, HR_MOP = 2, HR_SPIKY = 3, HR_PONY = 4, HR_CURLS = 5, HR_AFRO = 6,
            HR_PIGTAILS = 7, HR_MOHAWK = 8, HR_LONG = 9, HR_POMP = 10, HR_RAINBOW = 11, HR_FLAME = 12;
    // faces
    public static final int FC_SMILE = 0, FC_GRIN = 1, FC_SHADES = 2, FC_SILLY = 3, FC_GRUMPY = 4, FC_MUSTACHE = 5,
            FC_SHOCKED = 6, FC_WINK = 7, FC_NERD = 8, FC_PATCH = 9, FC_CLOWN = 10, FC_STARS = 11, FC_LASER = 12;
    // shirts
    public static final int SH_TEE = 0, SH_STRIPES = 1, SH_TANK = 2, SH_HOODIE = 3, SH_SWEATER = 4, SH_HAWAII = 5,
            SH_OVERALLS = 6, SH_JERSEY = 7, SH_RACING = 8, SH_HERO = 9, SH_TUX = 10, SH_DISCO = 11, SH_SPACE = 12;
    // pants
    public static final int P_JEANS = 0, P_KHAKI = 1, P_BLACK = 2, P_SHORTS = 3, P_RED_SHORTS = 4, P_TRACK = 5, P_PINK = 6,
            P_CHECKER = 7, P_TUX = 8, P_GOLD = 9, P_SPACE = 10;
    // shoes
    public static final int S_SNEAKERS = 0, S_BOOTS = 1, S_FLIPFLOPS = 2, S_HIGHTOPS = 3, S_RAIN = 4, S_DRESS = 5,
            S_CLOWN = 6, S_BUNNY = 7, S_SKATES = 8, S_GOLD = 9, S_ROCKET = 10;
    // things to hold
    public static final int HD_NONE = 0, HD_FINGER = 1, HD_ICECREAM = 2, HD_BANANA = 3, HD_CHICKEN = 4, HD_BALLOON = 5,
            HD_PHONE = 6, HD_TACO = 7, HD_LOLLIPOP = 8, HD_FLAG = 9, HD_TROPHY = 10, HD_MEGAPHONE = 11, HD_WAND = 12,
            HD_DIAMOND = 13;

    public static final String[][] NAME = {
            {"NO HAT", "BALL CAP", "BEANIE", "BUCKET HAT", "HARD HAT", "COWBOY HAT", "PROPELLER BEANIE", "RACING HELMET",
                    "CONE HEAD", "VIKING HELMET", "SHARK HAT", "TINY TOP HAT", "ROYAL CROWN", "SPACE HELMET"},
            {"BUZZ CUT", "BALD", "MOP TOP", "SPIKY", "PONYTAIL", "CURLS", "AFRO", "PIGTAILS", "MOHAWK", "LONG HAIR",
                    "POMPADOUR", "RAINBOW FRO", "FLAME HAIR"},
            {"SMILEY", "GRIN", "COOL SHADES", "SILLY", "GRUMPY", "MUSTACHE", "SHOCKED", "WINK", "NERD", "EYE PATCH",
                    "CLOWN NOSE", "STAR EYES", "LASER EYES"},
            {"PEACH", "TAN", "OLIVE", "BROWN", "COCOA", "PORCELAIN", "ALIEN", "BLUEBERRY", "ZOMBIE", "PUMPKIN",
                    "BUBBLEGUM", "SOLID GOLD", "CHROME"},
            {"RED TEE", "STRIPES", "TANK TOP", "HOODIE", "ZIGZAG SWEATER", "HAWAIIAN", "OVERALLS", "JERSEY",
                    "RACING SUIT", "SUPERHERO", "TUXEDO", "DISCO JACKET", "SPACE SUIT"},
            {"JEANS", "KHAKIS", "BLACK PANTS", "SHORTS", "RED SHORTS", "TRACK PANTS", "PINK PANTS", "CHECKERED",
                    "TUX PANTS", "GOLDEN PANTS", "SPACE PANTS"},
            {"SNEAKERS", "BOOTS", "FLIP-FLOPS", "HIGH-TOPS", "RAIN BOOTS", "DRESS SHOES", "CLOWN SHOES",
                    "BUNNY SLIPPERS", "ROLLER SKATES", "GOLDEN KICKS", "ROCKET BOOTS"},
            {"EMPTY HANDS", "FOAM FINGER", "ICE CREAM", "BANANA", "RUBBER CHICKEN", "BALLOON", "PHONE", "TACO",
                    "LOLLIPOP", "RACING FLAG", "TROPHY", "MEGAPHONE", "MAGIC WAND", "BIG DIAMOND"},
    };

    public static final int[][] RULE = {
            {F, F, C, C, C, C, L, C, L, C, L, C, R, R},
            {F, F, F, C, C, C, C, C, L, C, L, R, R},
            {F, F, C, C, C, C, L, C, C, L, C, L, R},
            {F, F, F, F, F, F, C, C, L, L, C, R, R},
            {F, F, C, C, C, C, C, L, C, L, R, R, R},
            {F, F, F, C, C, C, C, L, R, R, L},
            {F, C, C, C, C, C, C, L, L, R, R},
            {F, C, C, C, C, C, C, C, C, L, L, L, R, R},
    };

    /** The level (LEVEL) or the price (COINS / RIMS) that goes with RULE. */
    public static final int[][] VALUE = {
            {0, 0, 150, 200, 250, 350, 5, 600, 17, 800, 31, 500, 60, 80},
            {0, 0, 0, 150, 200, 200, 300, 250, 13, 250, 27, 40, 70},
            {0, 0, 200, 150, 250, 300, 15, 200, 350, 23, 400, 39, 60},
            {0, 0, 0, 0, 0, 0, 500, 500, 29, 19, 800, 50, 70},
            {0, 0, 150, 300, 250, 450, 350, 21, 600, 37, 60, 80, 70},
            {0, 0, 0, 100, 150, 200, 250, 33, 30, 50, 41},
            {0, 150, 100, 250, 200, 300, 500, 25, 35, 50, 80},
            {0, 200, 150, 100, 300, 250, 150, 200, 150, 43, 47, 49, 40, 70},
    };

    /**
     * Main color of each item: the hat, the hair, the skin tone, the shirt body, the pants, the shoe; meshes tinted
     * PRIMARY use it (faces and things to hold use fixed colors).
     */
    public static final int[][] COLOR = {
            {0, 0xFF3B4F, 0x3BB8FF, 0xF2C84B, 0xFFC21F, 0xB0703A, 0xFF4FA3, 0xF4F4FF, 0xFF7A1A, 0x9AA0B4, 0x6D8AB8, 0x2A2430,
                    0xFFC83D, 0xF4F6FF},
            {0x5A3A24, 0, 0xF2C84B, 0xFFD23B, 0xE0502A, 0x6B3A1E, 0x2A1E1A, 0xF6D25A, 0xFF4FA3, 0x2A2030, 0x1E1A24, 0xFF6FB5,
                    0xFF8A1F},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0xF6C7A1, 0xE2A877, 0xC89A68, 0x9A643E, 0x6B4228, 0xFBE2D2, 0x8BE36A, 0x6FA8FF, 0x9DB59A, 0xFF9A3D, 0xFF9AD0,
                    0xFFC83D, 0xD8E0EA},
            {0xFF3B4F, 0x2E4A9A, 0x5EE65A, 0x8E62FF, 0xFF8A2B, 0x2EC4B6, 0x3A6FD8, 0xFFD23B, 0xF4F4FF, 0x2E7BFF, 0x22202C,
                    0xFFC83D, 0xF0F2F8},
            {0x3A6FD8, 0xD2B07A, 0x2A2834, 0xD2B07A, 0xFF3B4F, 0x2A3A7A, 0xFF7EC0, 0xF4F4FF, 0x22202C, 0xFFC83D, 0xE8ECF4},
            {0xF4F4FF, 0x8A5A30, 0x3BB8FF, 0xFF3B4F, 0xFFD23B, 0x22202C, 0xFF3B4F, 0xFFC0DA, 0xFF4FA3, 0xFFC83D, 0xC8CCD8},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    };

    /** Shirts: sleeve color (0 = bare arms: the skin tone shows). */
    public static final int[] SLEEVE = {
            0xFF3B4F, 0xF4F4FF, 0, 0x8E62FF, 0xFF8A2B, 0x2EC4B6, 0xF4F4FF, 0xFFD23B, 0xF4F4FF, 0x2E7BFF, 0x22202C, 0xFFC83D,
            0xF0F2F8};
    /** Shirts: sleeve length, 0 = long, 1 = short (forearms bare), 2 = no sleeves. */
    public static final int[] SLEEVE_LEN = {1, 1, 2, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0};
    /** Pants: true for shorts (bare shins). */
    public static final boolean[] SHORTS = {false, false, false, true, true, false, false, false, false, false, false};
    /** Pants: legs mesh, 0 plain, 1 side stripes, 2 checkered (see Art.drvThigh). */
    public static final int[] PANTS_STYLE = {0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0};

    /** How much hair a hat hides: 0 none, 1 the top (hair shows below the brim), 2 all of it. */
    public static final int[] HAT_COVER = {0, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 2};
    /** Hats that cover the face too (the face mesh is not drawn). */
    public static final boolean[] HAT_FACE = {false, false, false, false, false, false, false, false, false, false, false,
            false, false, true};

    public static int count(int part) {
        return NAME[part].length;
    }

    /** The default look of a new profile. */
    public static void defaults(int[] look) {
        look[HAT] = H_CAP;
        look[HAIR] = HR_BUZZ;
        look[FACE] = FC_SMILE;
        look[SKIN] = 0;
        look[SHIRT] = SH_TEE;
        look[PANTS] = P_JEANS;
        look[SHOES] = S_SNEAKERS;
        look[HELD] = HD_NONE;
    }

    /** A random look for a bot: mostly plain stuff, now and then something fancy. */
    public static void random(Rng g, int[] look) {
        for (int p = 0; p < PARTS; p++) {
            int n = count(p), i;
            do {
                i = g.i(n);
            } while (RULE[p][i] == R && g.chance(0.6f));
            look[p] = i;
        }
        if (g.chance(0.3f)) look[HAT] = H_NONE;
        // mostly everyday skin tones
        if (g.chance(0.8f)) look[SKIN] = g.i(6);
        if (g.chance(0.45f)) look[HELD] = HD_NONE;
    }

    /** The first free item of a part (what a look falls back to). */
    public static int first(int part) {
        for (int i = 0; i < count(part); i++) if (RULE[part][i] == Items.FREE) return i;
        return 0;
    }

    /** Clamps a loaded look into range. */
    public static void sanitize(int[] look) {
        for (int p = 0; p < PARTS; p++) {
            if (look[p] < 0 || look[p] >= count(p)) look[p] = 0;
        }
    }

    /** Skin color of a look. */
    public static int skin(int[] look) {
        return COLOR[SKIN][look[SKIN]];
    }

    /** Color of what is on top of the head (the hat, else the hair), -1 if nothing (bald, no hat). */
    public static int topColor(int[] look) {
        if (look[HAT] != H_NONE) return COLOR[HAT][look[HAT]];
        return look[HAIR] == HR_BALD ? -1 : COLOR[HAIR][look[HAIR]];
    }
}
