package com.gogogo.game.game;

/**
 * One API over every unlockable thing: cars, paints, toppers, wheels, maps, arena skins, skies, themes and trails.
 * Each item is unlocked one way: free, by reaching a player level, with coins, or with Rims (premium).
 */
public final class Items {
    private Items() {}

    // categories
    public static final int CAR = 0, PAINT = 1, TOPPER = 2, WHEEL = 3, MAP = 4, SKIN = 5, SKY = 6, THEME = 7, TRAIL = 8;
    public static final int CATEGORIES = 9;
    public static final String[] CAT_NAME = {"CAR", "PAINT", "TOPPER", "WHEELS", "MAP", "ARENA SKIN", "SKY", "THEME", "TRAIL"};

    // unlock rules
    public static final int FREE = 0, LEVEL = 1, COINS = 2, RIMS = 3, HIDDEN = 4;

    public static int count(int cat) {
        switch (cat) {
            case CAR: return Cars.ALL.length;
            case PAINT: return Palette.PAINT.length;
            case TOPPER: return Palette.TOPPER_NAME.length;
            case WHEEL: return Palette.WHEEL_NAME.length;
            case MAP: return Maps.COUNT;
            case SKIN: return Skins.COUNT;
            case SKY: return Skies.COUNT;
            case THEME: return Themes.COUNT;
            default: return Trails.COUNT;
        }
    }

    public static String name(int cat, int i) {
        switch (cat) {
            case CAR: return Cars.ALL[i].name;
            case PAINT: return Palette.PAINT_NAME[i];
            case TOPPER: return Palette.TOPPER_NAME[i];
            case WHEEL: return Palette.WHEEL_NAME[i];
            case MAP: return Maps.NAME[i];
            case SKIN: return Skins.NAME[i];
            case SKY: return Skies.NAME[i];
            case THEME: return Themes.NAME[i];
            default: return Trails.NAME[i];
        }
    }

    /** One of FREE, LEVEL, COINS, RIMS, HIDDEN. */
    public static int rule(int cat, int i) {
        switch (cat) {
            case CAR: {
                CarDef d = Cars.ALL[i];
                if (d.secret) return HIDDEN;
                if (d.levelReq > 0) return LEVEL;
                if (d.rimsPrice > 0) return RIMS;
                return d.price > 0 ? COINS : FREE;
            }
            case PAINT: return Palette.PAINT_RULE[i];
            case TOPPER: return Palette.TOPPER_RULE[i];
            case WHEEL: return Palette.WHEEL_RULE[i];
            case MAP: return Maps.RULE[i];
            case SKIN: return Skins.RULE[i];
            case SKY: return Skies.RULE[i];
            case THEME: return Themes.RULE[i];
            default: return Trails.RULE[i];
        }
    }

    /** The level (LEVEL) or the price (COINS / RIMS) that goes with rule(). */
    public static int value(int cat, int i) {
        switch (cat) {
            case CAR: {
                CarDef d = Cars.ALL[i];
                if (d.levelReq > 0) return d.levelReq;
                if (d.rimsPrice > 0) return d.rimsPrice;
                return d.price;
            }
            case PAINT: return Palette.PAINT_PRICE[i];
            case TOPPER: return Palette.TOPPER_PRICE[i];
            case WHEEL: return Palette.WHEEL_PRICE[i];
            case MAP: return Maps.VALUE[i];
            case SKIN: return Skins.VALUE[i];
            case SKY: return Skies.VALUE[i];
            case THEME: return Themes.VALUE[i];
            default: return Trails.VALUE[i];
        }
    }

    public static boolean[] ownedArray(Save s, int cat) {
        switch (cat) {
            case CAR: return s.carOwned;
            case PAINT: return s.paintOwned;
            case TOPPER: return s.topperOwned;
            case WHEEL: return s.wheelOwned;
            case MAP: return s.mapOwned;
            case SKIN: return s.skinOwned;
            case SKY: return s.skyOwned;
            case THEME: return s.themeOwned;
            default: return s.trailOwned;
        }
    }

    public static boolean owned(Save s, int cat, int i) {
        return ownedArray(s, cat)[i];
    }

    /** Unlocks an item without charging for it (level rewards). */
    public static void grant(Save s, int cat, int i) {
        ownedArray(s, cat)[i] = true;
        if (cat == CAR) {
            CarDef d = Cars.ALL[i];
            s.paintOwned[d.defPaint] = true;
            s.paintOwned[d.defAccent] = true;
        }
        s.markDirty();
    }

    /** True if the player could buy this right now (not owned, sold for coins or Rims, enough of it). */
    public static boolean affordable(Save s, int cat, int i) {
        if (owned(s, cat, i)) return false;
        int r = rule(cat, i), v = value(cat, i);
        if (r == COINS) return s.coins >= v;
        if (r == RIMS) return s.rims >= v;
        return false;
    }

    /** Buys an item for coins or Rims. Returns false (and charges nothing) if it can't be bought. */
    public static boolean buy(Save s, int cat, int i) {
        if (owned(s, cat, i)) return true;
        int r = rule(cat, i), v = value(cat, i);
        boolean paid;
        if (r == COINS) paid = s.spend(v);
        else if (r == RIMS) paid = s.spendRims(v);
        else paid = false;
        if (!paid) return false;
        grant(s, cat, i);
        s.stats[Achievements.ST_PURCHASES]++;
        s.markDirty();
        Progress.refresh(s);
        s.flush();
        return true;
    }

    /** Equipped item of a match-cosmetic category (MAP .. TRAIL). For MAP, -1 means shuffle. */
    public static int selected(Save s, int cat) {
        switch (cat) {
            case MAP: return s.map;
            case SKIN: return s.skin;
            case SKY: return s.sky;
            case THEME: return s.theme;
            case TRAIL: return s.trail;
            default: return 0;
        }
    }

    public static void select(Save s, int cat, int i) {
        switch (cat) {
            case MAP: s.map = i; break;
            case SKIN: s.skin = i; break;
            case SKY: s.sky = i; break;
            case THEME: s.theme = i; break;
            case TRAIL: s.trail = i; break;
            default: return;
        }
        s.markDirty();
        s.flush();
    }

    /** Short label for how to get an item: "LEVEL 12", "500" (coins), "80 RIMS", "FREE". */
    public static String unlockLabel(int cat, int i) {
        switch (rule(cat, i)) {
            case LEVEL: return "LEVEL " + value(cat, i);
            case COINS: return String.valueOf(value(cat, i));
            case RIMS: return value(cat, i) + " RIMS";
            case HIDDEN: return "???";
            default: return "FREE";
        }
    }
}
