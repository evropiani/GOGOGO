package com.gogogo.game.game;

import java.util.ArrayList;

/** Ties matches, XP, levels and achievements together. */
public final class Progress {
    private Progress() {}

    /** A pop-up for the player ("LEVEL UP!", "ACHIEVEMENT!"), shown by Game on top of any screen. */
    public static final class Note {
        public static final int LEVEL = 0, ACHIEVEMENT = 1, ITEM = 2;
        public final int kind;
        public final String title, sub;

        public Note(int kind, String title, String sub) {
            this.kind = kind;
            this.title = title;
            this.sub = sub;
        }
    }

    /** Pending pop-ups; Game shows and removes them one by one. */
    public static final ArrayList<Note> notes = new ArrayList<Note>();

    /** What the player did in one match. */
    public static final class MatchResult {
        public boolean won, tied, hoodCam;
        public int place, total, rounds, bonks, boosts, clutches, map, car, coins;
    }

    /** What a match (or anything else) changed: XP, levels, rewards, achievements. */
    public static final class Outcome {
        public int xpGained;
        public int oldXp, newXp, oldLevel, newLevel;
        public final ArrayList<Levels.Reward> rewards = new ArrayList<Levels.Reward>();
        public final ArrayList<Integer> achievements = new ArrayList<Integer>();
    }

    /** XP earned for a match. */
    public static int xpFor(MatchResult r) {
        // the base XP is for surviving at least the first drop (no farming by driving off the edge right away)
        int xp = (r.rounds > 0 || r.won || r.tied ? 20 : 5) + r.rounds * 6 + r.bonks * 12 + r.clutches * 10;
        if (r.won) xp += 100;
        else if (r.tied) xp += 70;
        else if (r.place <= 3) xp += 50;
        else if (r.place <= 10) xp += 30;
        else if (r.place <= 25) xp += 15;
        else if (r.place <= 50) xp += 8;
        return xp;
    }

    /** Records a finished match: stats, XP, level ups and achievements. Coins are added by the caller. */
    public static Outcome commitMatch(Save s, MatchResult r) {
        Outcome o = new Outcome();
        o.oldXp = s.xp;
        o.oldLevel = Levels.levelFor(s.xp);
        s.matches++;
        if (r.won) s.wins++;
        if (r.tied) s.ties++;
        if (s.bestPlace == 0 || r.place < s.bestPlace) s.bestPlace = r.place;
        s.bonks += r.bonks;
        s.roundsSurvived += r.rounds;
        int[] st = s.stats;
        if (r.place <= 10 || r.won || r.tied) st[Achievements.ST_TOP10]++;
        if (r.place <= 3 || r.won || r.tied) st[Achievements.ST_TOP3]++;
        st[Achievements.ST_BEST_ROUNDS] = Math.max(st[Achievements.ST_BEST_ROUNDS], r.rounds);
        st[Achievements.ST_BEST_BONKS] = Math.max(st[Achievements.ST_BEST_BONKS], r.bonks);
        st[Achievements.ST_BOOSTS] += r.boosts;
        st[Achievements.ST_CLUTCH] += r.clutches;
        if (r.won && r.hoodCam) st[Achievements.ST_HOOD_WINS]++;
        if (r.won && r.car == 0) st[Achievements.ST_BEAN_WINS]++;
        st[Achievements.ST_COINS_EARNED] += r.coins;
        if (r.map >= 0 && r.map < 31) st[Achievements.ST_MAPS_MASK] |= 1 << r.map;
        o.xpGained = xpFor(r);
        s.xp += o.xpGained;
        settle(s, o);
        o.newXp = s.xp;
        o.newLevel = Levels.levelFor(s.xp);
        s.markDirty();
        return o;
    }

    /** Pays out anything newly earned (after purchases and upgrades) and queues pop-ups for it. */
    public static Outcome refresh(Save s) {
        Outcome o = new Outcome();
        o.oldXp = s.xp;
        o.oldLevel = Levels.levelFor(s.xp);
        settle(s, o);
        o.newXp = s.xp;
        o.newLevel = Levels.levelFor(s.xp);
        announce(o);
        return o;
    }

    /** Queues pop-ups for an outcome's level ups, unlocks and achievements (a few at most, then a summary). */
    public static void announce(Outcome o) {
        if (o.newLevel > o.oldLevel) notes.add(new Note(Note.LEVEL, "LEVEL UP!", "LEVEL " + o.newLevel));
        int items = 0;
        for (Levels.Reward r : o.rewards) if (r.kind == Levels.R_ITEM) items++;
        int shown = 0;
        for (Levels.Reward r : o.rewards) {
            if (r.kind != Levels.R_ITEM) continue;
            if (shown == 2 && items > 3) {
                notes.add(new Note(Note.ITEM, "UNLOCKED!", "+" + (items - 2) + " MORE GOODIES"));
                break;
            }
            notes.add(new Note(Note.ITEM, "UNLOCKED!", Items.name(r.cat, r.idx)));
            shown++;
        }
        int n = o.achievements.size();
        for (int i = 0; i < n; i++) {
            if (i == 2 && n > 3) {
                notes.add(new Note(Note.ACHIEVEMENT, "ACHIEVEMENTS!", "+" + (n - 2) + " MORE"));
                break;
            }
            notes.add(new Note(Note.ACHIEVEMENT, "ACHIEVEMENT!", Achievements.name(o.achievements.get(i))));
        }
    }

    // achievements give XP, XP gives levels, levels give coins and items, which can complete achievements...
    private static void settle(Save s, Outcome o) {
        for (int guard = 0; guard < 50; guard++) {
            ArrayList<Integer> a = Achievements.check(s);
            ArrayList<Levels.Reward> r = Levels.catchUp(s);
            o.achievements.addAll(a);
            o.rewards.addAll(r);
            if (a.isEmpty() && r.isEmpty()) break;
        }
    }
}
