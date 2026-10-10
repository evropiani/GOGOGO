package com.gogogo.game.game;

import java.util.ArrayList;

/** Achievements: a stat reaching a goal pays out XP, coins and sometimes Rims. */
public final class Achievements {
    private Achievements() {}

    // ---- stored counters (Save.stats)
    public static final int ST_TOP10 = 0, ST_TOP3 = 1, ST_BEST_ROUNDS = 2, ST_BEST_BONKS = 3, ST_BOOSTS = 4, ST_CLUTCH = 5,
            ST_HOOD_WINS = 6, ST_COINS_EARNED = 7, ST_BEAN_WINS = 8, ST_MAPS_MASK = 9, ST_PURCHASES = 10;
    public static final int STATS = 16;

    // ---- values computed from the save
    private static final int V_MATCHES = 100, V_WINS = 101, V_TIES = 102, V_ROUNDS = 103, V_BONKS = 104, V_LEVEL = 105,
            V_CARS = 106, V_COIN_CARS = 107, V_MAXED = 108, V_COSMETICS = 109, V_MAPS = 110;

    private static final Object[][] DEFS = {
            // name, description, stat, goal, xp, coins, rims
            {"FIRST LAP", "Play your first match", V_MATCHES, 1, 20, 50, 0},
            {"REGULAR", "Play 25 matches", V_MATCHES, 25, 100, 200, 0},
            {"ADDICTED", "Play 100 matches", V_MATCHES, 100, 300, 600, 10},
            {"NO BRAKES", "Play 500 matches", V_MATCHES, 500, 800, 2000, 50},
            {"WINNER WINNER", "Win a match", V_WINS, 1, 100, 300, 0},
            {"CHAMP", "Win 10 matches", V_WINS, 10, 300, 800, 10},
            {"LEGEND", "Win 50 matches", V_WINS, 50, 800, 2500, 50},
            {"EVERYBODY WINS", "Finish a match in a tie", V_TIES, 1, 100, 200, 0},
            {"TOP TEN", "Finish in the top 10 ten times", ST_TOP10, 10, 150, 400, 0},
            {"PODIUM", "Finish in the top 3 five times", ST_TOP3, 5, 150, 400, 0},
            {"SURVIVOR", "Survive 10 rounds in one match", ST_BEST_ROUNDS, 10, 80, 150, 0},
            {"IRON WILL", "Survive 20 rounds in one match", ST_BEST_ROUNDS, 20, 200, 500, 0},
            {"UNSTOPPABLE", "Survive 25 rounds in one match", ST_BEST_ROUNDS, 25, 400, 1000, 20},
            {"MARATHON", "Survive 500 rounds in total", V_ROUNDS, 500, 300, 600, 0},
            {"FIRST BONK", "Knock a car off the arena", V_BONKS, 1, 30, 50, 0},
            {"BONK SQUAD", "Knock off 50 cars", V_BONKS, 50, 200, 500, 0},
            {"BONK MASTER", "Knock off 250 cars", V_BONKS, 250, 600, 1500, 20},
            {"TRIPLE BONK", "Knock off 3 cars in one match", ST_BEST_BONKS, 3, 150, 300, 0},
            {"BONKANZA", "Knock off 6 cars in one match", ST_BEST_BONKS, 6, 300, 800, 10},
            {"BUMPER CARS", "Bump 100 times", ST_BOOSTS, 100, 100, 200, 0},
            {"BUMP ADDICT", "Bump 1,000 times", ST_BOOSTS, 1000, 300, 600, 0},
            {"CLUTCH!", "Reach the right color with less than half a second left", ST_CLUTCH, 1, 100, 200, 0},
            {"ICE IN YOUR VEINS", "Pull off 25 clutch saves", ST_CLUTCH, 25, 300, 600, 10},
            {"LEVEL 10", "Reach level 10", V_LEVEL, 10, 0, 300, 0},
            {"LEVEL 25", "Reach level 25", V_LEVEL, 25, 0, 800, 10},
            {"LEVEL 50", "Reach level 50", V_LEVEL, 50, 0, 2000, 25},
            {"LEVEL 100", "Reach the max level", V_LEVEL, 100, 0, 5000, 100},
            {"COLLECTOR", "Own 5 cars", V_CARS, 5, 150, 300, 0},
            {"CAR CRAZY", "Own every car you can buy with coins", V_COIN_CARS, 1, 500, 1000, 20},
            {"TUNED", "Max out one stat of a car", V_MAXED, 1, 100, 200, 0},
            {"FULLY LOADED", "Max out every stat of a car", V_MAXED, 4, 300, 600, 0},
            {"FASHIONISTA", "Own 15 paints, toppers or wheels", V_COSMETICS, 15, 150, 300, 0},
            {"GLOBETROTTER", "Play on 5 different maps", V_MAPS, 5, 200, 400, 0},
            {"WORLD TOUR", "Play on every map", V_MAPS, Maps.COUNT, 600, 1200, 20},
            {"BEHIND THE WHEEL", "Win a match in HOOD view", ST_HOOD_WINS, 1, 200, 500, 0},
            {"PIGGY BANK", "Earn 10,000 coins in matches", ST_COINS_EARNED, 10000, 200, 0, 10},
            {"UNDERDOG", "Win a match with the BUMPER BEAN", ST_BEAN_WINS, 1, 200, 500, 0},
            {"SHOPAHOLIC", "Buy 20 things", ST_PURCHASES, 20, 150, 300, 0},
    };

    public static final int COUNT = DEFS.length;

    public static String name(int a) {
        return (String) DEFS[a][0];
    }

    public static String desc(int a) {
        return (String) DEFS[a][1];
    }

    public static int goal(int a) {
        return (Integer) DEFS[a][3];
    }

    public static int xp(int a) {
        return (Integer) DEFS[a][4];
    }

    public static int coins(int a) {
        return (Integer) DEFS[a][5];
    }

    public static int rims(int a) {
        return (Integer) DEFS[a][6];
    }

    /** Current progress toward an achievement (may exceed the goal). */
    public static int progress(Save s, int a) {
        return value(s, (Integer) DEFS[a][2]);
    }

    private static int value(Save s, int stat) {
        switch (stat) {
            case V_MATCHES: return s.matches;
            case V_WINS: return s.wins;
            case V_TIES: return s.ties;
            case V_ROUNDS: return s.roundsSurvived;
            case V_BONKS: return s.bonks;
            case V_LEVEL: return Levels.levelFor(s.xp);
            case V_CARS: {
                int n = 0;
                for (int i = 0; i < Cars.ALL.length; i++) if (s.carOwned[i] && !Cars.ALL[i].secret) n++;
                return n;
            }
            case V_COIN_CARS: {
                for (int i = 0; i < Cars.ALL.length; i++) {
                    if (Items.rule(Items.CAR, i) == Items.COINS && !s.carOwned[i]) return 0;
                }
                return 1;
            }
            case V_MAXED: {
                int best = 0;
                for (int i = 0; i < Cars.ALL.length; i++) {
                    if (!s.carOwned[i]) continue;
                    int n = 0;
                    for (int k = 0; k < 4; k++) if (s.levels[i][k] >= CarDef.MAX_LEVEL) n++;
                    best = Math.max(best, n);
                }
                return best;
            }
            case V_COSMETICS: {
                int n = 0;
                for (int i = 0; i < s.paintOwned.length; i++) {
                    if (s.paintOwned[i] && Palette.PAINT_RULE[i] != Items.FREE && !starterPaint(i)) n++;
                }
                for (int i = 1; i < s.topperOwned.length; i++) if (s.topperOwned[i]) n++;
                for (int i = 1; i < s.wheelOwned.length; i++) if (s.wheelOwned[i]) n++;
                return n;
            }
            case V_MAPS:
                return Integer.bitCount(s.stats[ST_MAPS_MASK]);
            default:
                return stat >= 0 && stat < STATS ? s.stats[stat] : 0;
        }
    }

    /** Paints every profile gets for free: the default colors of the coin cars. */
    private static boolean starterPaint(int p) {
        for (int i = 0; i < Cars.ALL.length; i++) {
            int r = Items.rule(Items.CAR, i);
            if ((r == Items.FREE || r == Items.COINS) && (Cars.ALL[i].defPaint == p || Cars.ALL[i].defAccent == p)) return true;
        }
        return false;
    }

    /** Marks newly reached achievements done and pays them out. Returns their ids. */
    public static ArrayList<Integer> check(Save s) {
        ArrayList<Integer> got = new ArrayList<Integer>();
        for (int a = 0; a < COUNT; a++) {
            if (s.achDone[a] || progress(s, a) < goal(a)) continue;
            s.achDone[a] = true;
            s.xp += xp(a);
            s.addCoins(coins(a));
            s.addRims(rims(a));
            s.markDirty();
            got.add(a);
        }
        return got;
    }

    public static int doneCount(Save s) {
        int n = 0;
        for (int a = 0; a < COUNT; a++) if (s.achDone[a]) n++;
        return n;
    }
}
