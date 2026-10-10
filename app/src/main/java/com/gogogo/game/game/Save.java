package com.gogogo.game.game;

import com.gogogo.game.engine.Platform;

import java.util.Properties;

/** Persistent player profile: coins, Rims, XP, garage, cosmetics, settings, stats, achievements. */
public final class Save {
    private static final String KEY = "profile";

    private final Platform platform;
    private boolean dirty;

    /** Version the player last saw the what's-new screen for (empty for profiles from before 1.3.0). */
    public String lastVersion = "";
    /** True if a saved profile was found at load (false on a fresh install). */
    public boolean existed;

    public int coins = 150;
    /** Premium currency. */
    public int rims;
    /** Total XP (the level is derived from it). */
    public int xp;
    /** Highest level whose rewards have been paid out. */
    public int rewardedLevel = 1;
    public int selectedCar = 0;
    public final boolean[] carOwned = new boolean[Cars.ALL.length];
    public final int[][] levels = new int[Cars.ALL.length][4];
    public final int[] carPaint = new int[Cars.ALL.length];
    public final int[] carAccent = new int[Cars.ALL.length];
    public final int[] carTopper = new int[Cars.ALL.length];
    public final int[] carWheel = new int[Cars.ALL.length];
    public final boolean[] paintOwned = new boolean[Palette.PAINT.length];
    public final boolean[] topperOwned = new boolean[Palette.TOPPER_NAME.length];
    public final boolean[] wheelOwned = new boolean[Palette.WHEEL_NAME.length];
    public final boolean[] mapOwned = new boolean[Maps.COUNT];
    public final boolean[] skinOwned = new boolean[Skins.COUNT];
    public final boolean[] skyOwned = new boolean[Skies.COUNT];
    public final boolean[] themeOwned = new boolean[Themes.COUNT];
    public final boolean[] trailOwned = new boolean[Trails.COUNT];
    /** Equipped match look. map = -1 picks a random owned map every match. */
    public int map = 0, skin = 0, sky = 0, theme = 0, trail = 0;

    // settings
    public boolean sound = true, music = true, vibration = true, shake = true, symbols = true, leftHanded = false;
    /** Match camera: 0 near, 1 far, 2 hood. */
    public int camMode = 1;

    // stats
    public int matches, wins, bestPlace = 0, bonks, roundsSurvived, ties;
    public final int[] stats = new int[Achievements.STATS];
    public final boolean[] achDone = new boolean[Achievements.COUNT];

    // hidden stuff
    public boolean dev;          // developer menu revealed
    public final boolean[] devFlags = new boolean[16];
    public boolean secretSeen;   // the special car's reveal has been shown

    public Save(Platform platform) {
        this.platform = platform;
        reset();
    }

    public void reset() {
        coins = 150;
        rims = 0;
        xp = 0;
        rewardedLevel = 1;
        selectedCar = 0;
        for (int i = 0; i < Cars.ALL.length; i++) {
            carOwned[i] = i == 0;
            levels[i] = new int[4];
            carPaint[i] = Cars.ALL[i].defPaint;
            carAccent[i] = Cars.ALL[i].defAccent;
            carTopper[i] = 0;
            carWheel[i] = 0;
        }
        for (int cat = Items.PAINT; cat < Items.CATEGORIES; cat++) {
            boolean[] owned = Items.ownedArray(this, cat);
            for (int i = 0; i < owned.length; i++) owned[i] = Items.rule(cat, i) == Items.FREE;
        }
        // the default colors of the coin cars are always available
        for (int i = 0; i < Cars.ALL.length; i++) {
            int r = Items.rule(Items.CAR, i);
            if (r == Items.FREE || r == Items.COINS) {
                paintOwned[Cars.ALL[i].defPaint] = true;
                paintOwned[Cars.ALL[i].defAccent] = true;
            }
        }
        map = skin = sky = theme = trail = 0;
        matches = wins = bestPlace = bonks = roundsSurvived = ties = 0;
        for (int i = 0; i < stats.length; i++) stats[i] = 0;
        for (int i = 0; i < achDone.length; i++) achDone[i] = false;
        secretSeen = false;
        dirty = true;
    }

    public boolean secretUnlocked() {
        return carOwned[Cars.SECRET];
    }

    public void unlockSecret() {
        carOwned[Cars.SECRET] = true;
        CarDef d = Cars.ALL[Cars.SECRET];
        paintOwned[d.defPaint] = true;
        paintOwned[d.defAccent] = true;
        markDirty();
        flush();
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean spend(int amount) {
        if (coins < amount) return false;
        coins -= amount;
        markDirty();
        flush();
        return true;
    }

    public void addCoins(int amount) {
        coins = Math.max(0, Math.min(9999999, coins + amount));
        markDirty();
    }

    public boolean spendRims(int amount) {
        if (rims < amount) return false;
        rims -= amount;
        markDirty();
        flush();
        return true;
    }

    public void addRims(int amount) {
        rims = Math.max(0, Math.min(999999, rims + amount));
        markDirty();
    }

    public int level() {
        return Levels.levelFor(xp);
    }

    // ------------------------------------------------------------------ io

    public void load() {
        String s = platform.load(KEY);
        if (s == null) return;
        existed = true;
        Properties p = new Properties();
        try {
            p.load(new java.io.StringReader(s));
        } catch (Exception e) {
            return;
        }
        coins = geti(p, "coins", coins);
        String ver = p.getProperty("ver");
        lastVersion = ver == null ? "" : ver.trim();
        rims = Math.max(0, geti(p, "rims", 0));
        selectedCar = geti(p, "car", 0);
        for (int i = 0; i < Cars.ALL.length; i++) {
            carOwned[i] = i == 0 || geti(p, "c" + i + ".own", 0) == 1;
            String lv = p.getProperty("c" + i + ".lv");
            if (lv != null) {
                String[] t = lv.split(",");
                for (int k = 0; k < 4 && k < t.length; k++) levels[i][k] = clamp(parse(t[k], 0), 0, CarDef.MAX_LEVEL);
            }
            carPaint[i] = clamp(geti(p, "c" + i + ".paint", carPaint[i]), 0, Palette.PAINT.length - 1);
            carAccent[i] = clamp(geti(p, "c" + i + ".accent", carAccent[i]), 0, Palette.PAINT.length - 1);
            carTopper[i] = clamp(geti(p, "c" + i + ".top", 0), 0, Palette.TOPPER_NAME.length - 1);
            carWheel[i] = clamp(geti(p, "c" + i + ".wheel", 0), 0, Palette.WHEEL_NAME.length - 1);
        }
        bits(p.getProperty("paints"), paintOwned);
        bits(p.getProperty("toppers"), topperOwned);
        bits(p.getProperty("wheels"), wheelOwned);
        bits(p.getProperty("maps"), mapOwned);
        bits(p.getProperty("skins"), skinOwned);
        bits(p.getProperty("skies"), skyOwned);
        bits(p.getProperty("themes"), themeOwned);
        bits(p.getProperty("trails"), trailOwned);
        map = clamp(geti(p, "selmap", 0), -1, Maps.COUNT - 1);
        skin = clamp(geti(p, "selskin", 0), 0, Skins.COUNT - 1);
        sky = clamp(geti(p, "selsky", 0), 0, Skies.COUNT - 1);
        theme = clamp(geti(p, "seltheme", 0), 0, Themes.COUNT - 1);
        trail = clamp(geti(p, "seltrail", 0), 0, Trails.COUNT - 1);
        if (map >= 0 && !mapOwned[map]) map = 0;
        if (!skinOwned[skin]) skin = 0;
        if (!skyOwned[sky]) sky = 0;
        if (!themeOwned[theme]) theme = 0;
        if (!trailOwned[trail]) trail = 0;
        sound = geti(p, "sound", 1) == 1;
        music = geti(p, "music", 1) == 1;
        vibration = geti(p, "vib", 1) == 1;
        shake = geti(p, "shake", 1) == 1;
        symbols = geti(p, "symbols", 1) == 1;
        leftHanded = geti(p, "lefty", 0) == 1;
        camMode = clamp(geti(p, "cam", 1), 0, 2);
        matches = geti(p, "matches", 0);
        wins = geti(p, "wins", 0);
        ties = geti(p, "ties", 0);
        bestPlace = geti(p, "best", 0);
        bonks = geti(p, "bonks", 0);
        roundsSurvived = geti(p, "rounds", 0);
        String st = p.getProperty("st");
        if (st != null) {
            String[] t = st.split(",");
            for (int i = 0; i < stats.length && i < t.length; i++) stats[i] = parse(t[i], 0);
        }
        bits(p.getProperty("ach"), achDone);
        if (p.getProperty("xp") != null) {
            xp = Math.max(0, geti(p, "xp", 0));
            rewardedLevel = clamp(geti(p, "lvr", 1), 1, Levels.MAX);
        } else {
            // profile from before levels existed: credit the matches already played
            xp = Math.min(Levels.xpAt(50), matches * 40 + wins * 80 + bonks * 8 + roundsSurvived * 4);
            rewardedLevel = 1;
        }
        dev = geti(p, "x1", 0) == 1;
        bits(p.getProperty("x2"), devFlags);
        secretSeen = geti(p, "x3", 0) == 1;
        if (selectedCar < 0 || selectedCar >= Cars.ALL.length || !carOwned[selectedCar]) selectedCar = 0;
        dirty = false;
    }

    public void flush() {
        if (!dirty) return;
        StringBuilder sb = new StringBuilder();
        put(sb, "coins", coins);
        sb.append("ver=").append(lastVersion).append('\n');
        put(sb, "rims", rims);
        put(sb, "xp", xp);
        put(sb, "lvr", rewardedLevel);
        put(sb, "car", selectedCar);
        for (int i = 0; i < Cars.ALL.length; i++) {
            put(sb, "c" + i + ".own", carOwned[i] ? 1 : 0);
            sb.append("c").append(i).append(".lv=").append(levels[i][0]).append(',').append(levels[i][1]).append(',')
                    .append(levels[i][2]).append(',').append(levels[i][3]).append('\n');
            put(sb, "c" + i + ".paint", carPaint[i]);
            put(sb, "c" + i + ".accent", carAccent[i]);
            put(sb, "c" + i + ".top", carTopper[i]);
            put(sb, "c" + i + ".wheel", carWheel[i]);
        }
        sb.append("paints=").append(bits(paintOwned)).append('\n');
        sb.append("toppers=").append(bits(topperOwned)).append('\n');
        sb.append("wheels=").append(bits(wheelOwned)).append('\n');
        sb.append("maps=").append(bits(mapOwned)).append('\n');
        sb.append("skins=").append(bits(skinOwned)).append('\n');
        sb.append("skies=").append(bits(skyOwned)).append('\n');
        sb.append("themes=").append(bits(themeOwned)).append('\n');
        sb.append("trails=").append(bits(trailOwned)).append('\n');
        put(sb, "selmap", map);
        put(sb, "selskin", skin);
        put(sb, "selsky", sky);
        put(sb, "seltheme", theme);
        put(sb, "seltrail", trail);
        put(sb, "sound", sound ? 1 : 0);
        put(sb, "music", music ? 1 : 0);
        put(sb, "vib", vibration ? 1 : 0);
        put(sb, "shake", shake ? 1 : 0);
        put(sb, "symbols", symbols ? 1 : 0);
        put(sb, "lefty", leftHanded ? 1 : 0);
        put(sb, "cam", camMode);
        put(sb, "matches", matches);
        put(sb, "wins", wins);
        put(sb, "ties", ties);
        put(sb, "best", bestPlace);
        put(sb, "bonks", bonks);
        put(sb, "rounds", roundsSurvived);
        sb.append("st=");
        for (int i = 0; i < stats.length; i++) sb.append(i > 0 ? "," : "").append(stats[i]);
        sb.append('\n');
        sb.append("ach=").append(bits(achDone)).append('\n');
        put(sb, "x1", dev ? 1 : 0);
        sb.append("x2=").append(bits(devFlags)).append('\n');
        put(sb, "x3", secretSeen ? 1 : 0);
        platform.save(KEY, sb.toString());
        dirty = false;
    }

    private static void put(StringBuilder sb, String k, int v) {
        sb.append(k).append('=').append(v).append('\n');
    }

    private static int geti(Properties p, String k, int def) {
        return parse(p.getProperty(k), def);
    }

    private static int parse(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String bits(boolean[] b) {
        StringBuilder sb = new StringBuilder();
        for (boolean x : b) sb.append(x ? '1' : '0');
        return sb.toString();
    }

    private static void bits(String s, boolean[] out) {
        if (s == null) return;
        for (int i = 0; i < out.length && i < s.length(); i++) out[i] = out[i] || s.charAt(i) == '1';
    }

    // ------------------------------------------------------------------ economy

    public static int upgradeCost(int level) {
        int[] c = {120, 250, 450, 700, 1000};
        return level < c.length ? c[level] : 0;
    }
}
