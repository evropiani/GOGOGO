package com.gogogo.game.game;

import java.util.ArrayList;

/** Player levels 1..100: the XP curve and what every level up gives you. */
public final class Levels {
    private Levels() {}

    public static final int MAX = 100;

    /** XP needed to go from this level to the next. */
    public static int xpToNext(int level) {
        return 80 + 10 * level;
    }

    /** Total XP needed to reach a level (level 1 = 0 XP). */
    public static int xpAt(int level) {
        int n = Math.max(0, Math.min(MAX, level) - 1);
        return 80 * n + 5 * n * (n + 1);
    }

    public static int levelFor(int xp) {
        int l = 1;
        while (l < MAX && xp >= xpAt(l + 1)) l++;
        return l;
    }

    /** How far into the current level (0..1); 1 at max level. */
    public static float progress(int xp) {
        int l = levelFor(xp);
        if (l >= MAX) return 1f;
        return (xp - xpAt(l)) / (float) xpToNext(l);
    }

    // ------------------------------------------------------------------ rewards

    public static final int R_COINS = 0, R_RIMS = 1, R_ITEM = 2;

    public static final class Reward {
        public final int kind, cat, idx, amount;

        public Reward(int kind, int cat, int idx, int amount) {
            this.kind = kind;
            this.cat = cat;
            this.idx = idx;
            this.amount = amount;
        }

        public String label() {
            switch (kind) {
                case R_COINS: return amount + " COINS";
                case R_RIMS: return amount + " RIMS";
                default: return Items.name(cat, idx);
            }
        }
    }

    public static int rimsAt(int level) {
        if (level % 25 == 0) return 25;
        if (level % 10 == 0) return 10;
        if (level % 5 == 0) return 5;
        return 0;
    }

    /** Everything you get for reaching a level: items first, then coins (doubled on levels without items), then Rims. */
    public static void rewardsAt(int level, ArrayList<Reward> out) {
        if (level <= 1 || level > MAX) return;
        boolean item = false;
        for (int cat = 0; cat < Items.CATEGORIES; cat++) {
            int n = Items.count(cat);
            for (int i = 0; i < n; i++) {
                if (Items.rule(cat, i) == Items.LEVEL && Items.value(cat, i) == level) {
                    out.add(new Reward(R_ITEM, cat, i, 1));
                    item = true;
                }
            }
        }
        out.add(new Reward(R_COINS, 0, 0, (40 + 4 * level) * (item ? 1 : 2)));
        int rims = rimsAt(level);
        if (rims > 0) out.add(new Reward(R_RIMS, 0, 0, rims));
    }

    public static ArrayList<Reward> rewardsAt(int level) {
        ArrayList<Reward> out = new ArrayList<Reward>();
        rewardsAt(level, out);
        return out;
    }

    public static void grant(Save s, Reward r) {
        switch (r.kind) {
            case R_COINS:
                s.addCoins(r.amount);
                break;
            case R_RIMS:
                s.addRims(r.amount);
                break;
            default:
                Items.grant(s, r.cat, r.idx);
                break;
        }
    }

    /** Grants the rewards of every level reached but not yet rewarded. Returns what was granted. */
    public static ArrayList<Reward> catchUp(Save s) {
        ArrayList<Reward> got = new ArrayList<Reward>();
        int level = levelFor(s.xp);
        while (s.rewardedLevel < level) {
            s.rewardedLevel++;
            int before = got.size();
            rewardsAt(s.rewardedLevel, got);
            for (int i = before; i < got.size(); i++) grant(s, got.get(i));
            s.markDirty();
        }
        return got;
    }
}
