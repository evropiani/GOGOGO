package com.gogogo.game.game;

import com.gogogo.game.engine.Platform;

import java.util.Properties;

/** Persistent player profile: coins, garage, cosmetics, settings, stats. */
public final class Save {
    private static final String KEY = "profile";

    private final Platform platform;
    private boolean dirty;

    public int coins = 150;
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

    // settings
    public boolean sound = true, music = true, vibration = true, shake = true, symbols = true, leftHanded = false;
    /** Match camera: 0 near, 1 far, 2 hood. */
    public int camMode = 1;

    // stats
    public int matches, wins, bestPlace = 0, bonks, roundsSurvived, ties;

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
        selectedCar = 0;
        for (int i = 0; i < Cars.ALL.length; i++) {
            carOwned[i] = i == 0;
            levels[i] = new int[4];
            carPaint[i] = Cars.ALL[i].defPaint;
            carAccent[i] = Cars.ALL[i].defAccent;
            carTopper[i] = 0;
            carWheel[i] = 0;
        }
        for (int i = 0; i < paintOwned.length; i++) paintOwned[i] = Palette.PAINT_PRICE[i] == 0;
        for (int i = 0; i < topperOwned.length; i++) topperOwned[i] = Palette.TOPPER_PRICE[i] == 0;
        for (int i = 0; i < wheelOwned.length; i++) wheelOwned[i] = Palette.WHEEL_PRICE[i] == 0;
        // default car colors are always available
        for (CarDef d : Cars.ALL) {
            if (!d.secret) {
                paintOwned[d.defPaint] = true;
                paintOwned[d.defAccent] = true;
            }
        }
        matches = wins = bestPlace = bonks = roundsSurvived = ties = 0;
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

    // ------------------------------------------------------------------ io

    public void load() {
        String s = platform.load(KEY);
        if (s == null) return;
        Properties p = new Properties();
        try {
            p.load(new java.io.StringReader(s));
        } catch (Exception e) {
            return;
        }
        coins = geti(p, "coins", coins);
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
