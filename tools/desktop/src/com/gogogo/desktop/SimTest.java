package com.gogogo.desktop;

import com.gogogo.game.game.Arena;
import com.gogogo.game.game.Car;
import com.gogogo.game.game.Maps;
import com.gogogo.game.game.Match;
import com.gogogo.game.game.PowerUps;

/**
 * Headless balance check: runs bot-only matches (100 bots) and prints round/elimination stats per map.
 * Usage: SimTest [RUNS] [MAP|all] [v] [nopow]   (MAP is a map id or name, default CLASSIC; "v" prints every match,
 * "nopow" plays without power-ups)
 */
public final class SimTest {
    // elimination causes
    private static final int MISS = 0, SHOVED = 1, EDGE = 2, HOLE = 3, OTHER = 4, CAUSES = 5;

    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        String which = args.length > 1 ? args[1] : "0";
        boolean verbose = false, noPow = false;
        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith("v")) verbose = true;
            if (args[i].equalsIgnoreCase("nopow")) noPow = true;
        }
        int from = 0, to = Maps.COUNT - 1;
        if (!which.equalsIgnoreCase("all")) {
            from = to = mapId(which);
        }
        float classic = -1f;
        StringBuilder table = new StringBuilder();
        table.append(String.format("%-13s %5s %11s %6s %5s  %6s %6s %6s %6s %6s  %9s  %s%n",
                "map", "tiles", "rounds", "time", "ties", "miss", "shoved", "edge", "hole", "other", "falls/m", "rounds vs CLASSIC"));
        StringBuilder pow = new StringBuilder();
        pow.append(String.format("%-13s %9s %9s %9s %9s %9s %9s %9s%n", "power-ups", "spawned/m", "picked/m", "used/m", "elims/m", "rescues/m", "hits/m", "used: snow ice jump sticky bump"));
        for (int map = from; map <= to; map++) {
            double totalRounds = 0, totalRounds2 = 0, totalTime = 0;
            int ties = 0;
            int[] cause = new int[CAUSES];
            int showVoid = 0, elims = 0;
            int tiles = 0;
            int spawned = 0, picked = 0, used = 0, rescues = 0;
            final int[] powElims = new int[1], hits = new int[1];
            final int[] byType = new int[PowerUps.COUNT];
            for (int r = 0; r < runs; r++) {
                Match.Options o = new Match.Options();
                o.attract = true;
                o.bots = 100;
                o.map = map;
                o.noPowerUps = noPow;
                final Match m = new Match(1234 + r * 77, o, null);
                tiles = m.arena.count;
                final boolean[] outside = outsideCells(m.arena);
                final boolean[] safeAtDrop = new boolean[m.cars.length];
                final int[] mc = new int[CAUSES];
                final int[] sv = new int[1];
                m.listener = new Match.Listener() {
                    public void event(int type, Car a, Car b, float v) {
                        if (type == Match.EV_POWER) byType[(int) v]++;
                        if ((type == Match.EV_SPLAT && b != null) || type == Match.EV_FREEZE || type == Match.EV_SHOCK) hits[0]++;
                        if (type == Match.EV_DROP) {
                            for (Car c : m.cars) {
                                Arena.Tile t = m.arena.cellAt(c.x, c.z);
                                safeAtDrop[c.index] = c.alive && t != null && t.exists && t.color == m.target;
                            }
                            return;
                        }
                        if (type != Match.EV_ELIM) return;
                        if (a.lastHitPower >= 0 && a.lastHitBy >= 0 && m.time - a.lastHitTime < 2.2f) powElims[0]++;
                        Arena.Tile t = m.arena.cellAt(a.x, a.z);
                        int k;
                        if (t == null || (!t.exists && outside[t.index])) k = EDGE;
                        else if (!t.exists) k = HOLE;
                        else if (m.phase == Match.DROP && safeAtDrop[a.index]) k = SHOVED;
                        else if (m.phase == Match.DROP) k = MISS;
                        else k = OTHER;
                        mc[k]++;
                        if ((k == EDGE || k == HOLE) && m.phase == Match.SHOW) sv[0]++;
                    }
                };
                StringBuilder sb = new StringBuilder();
                int lastRound = 0;
                int steps = 0;
                while (m.phase != Match.OVER && steps < 60 * 60 * 15) {
                    m.step(Match.STEP);
                    steps++;
                    if (m.round != lastRound) {
                        sb.append(m.alive).append(' ');
                        lastRound = m.round;
                    }
                }
                spawned += m.power.spawned;
                used += m.power.used;
                for (Car c : m.cars) {
                    rescues += c.rescues;
                    picked += c.pickups;
                }
                totalRounds += m.round;
                totalRounds2 += m.round * m.round;
                totalTime += m.time;
                if (m.tie) ties++;
                for (int i = 0; i < CAUSES; i++) {
                    cause[i] += mc[i];
                    elims += mc[i];
                }
                showVoid += sv[0];
                if (verbose) {
                    Car w = m.winner;
                    System.out.printf("%s run %d: rounds=%d time=%.0fs %s alive/round: %s%n", Maps.NAME[map], r, m.round, m.time,
                            m.tie ? "TIE(" + m.tieGroup.size() + ")" : "winner=" + (w == null ? "-" : w.name + " " + w.def.name), sb);
                    System.out.printf("   causes: miss=%d shoved=%d edge=%d hole=%d other=%d (void falls mid-round %d)%n",
                            mc[MISS], mc[SHOVED], mc[EDGE], mc[HOLE], mc[OTHER], sv[0]);
                }
            }
            float avgRounds = (float) (totalRounds / runs);
            // standard error of the average: differences much smaller than this are noise
            double var = Math.max(0.0, totalRounds2 / runs - avgRounds * (double) avgRounds);
            float se = (float) Math.sqrt(var / Math.max(1, runs - 1));
            if (map == 0) classic = avgRounds;
            table.append(String.format("%-13s %5d %5.1f +-%3.1f %5.0fs %2d/%-2d  %5.1f%% %5.1f%% %5.1f%% %5.1f%% %5.1f%%  %9.1f  %s%n",
                    Maps.NAME[map], tiles, avgRounds, se, totalTime / runs, ties, runs,
                    pct(cause[MISS], elims), pct(cause[SHOVED], elims), pct(cause[EDGE], elims), pct(cause[HOLE], elims),
                    pct(cause[OTHER], elims), showVoid / (float) runs,
                    classic > 0 && map > 0 ? String.format("%+.0f%%", (avgRounds / classic - 1f) * 100f) : ""));
            pow.append(String.format("%-13s %9.1f %9.1f %9.1f %9.1f %9.2f %9.1f    %d %d %d %d %d%n", Maps.NAME[map], spawned / (float) runs,
                    picked / (float) runs,
                    used / (float) runs, powElims[0] / (float) runs, rescues / (float) runs, hits[0] / (float) runs,
                    byType[0], byType[1], byType[2], byType[3], byType[4]));
        }
        System.out.print(table);
        if (!noPow) System.out.print(pow);
        System.out.println("causes (share of all eliminations): miss = not on the color at the drop, shoved = was safe at the drop");
        System.out.println("  but fell after it, edge = off the outside of the map, hole = into a hole inside the map, other = anything else");
        System.out.println("falls/m = cars per match that drove or got pushed off the floor while the color was showing");
    }

    private static float pct(int k, int total) {
        return total == 0 ? 0f : k * 100f / total;
    }

    private static int mapId(String s) {
        for (int i = 0; i < Maps.COUNT; i++) {
            if (Maps.NAME[i].replace(" ", "").equalsIgnoreCase(s.replace("_", "").replace(" ", ""))) return i;
        }
        return Integer.parseInt(s);
    }

    /** Empty cells that connect to the outside of the grid (open void, not a hole inside the map). */
    private static boolean[] outsideCells(Arena a) {
        int n = a.n;
        boolean[] out = new boolean[n * n];
        int[] queue = new int[n * n];
        int head = 0, tail = 0;
        for (int i = 0; i < n * n; i++) {
            int x = i % n, z = i / n;
            if ((x == 0 || z == 0 || x == n - 1 || z == n - 1) && !a.tiles[i].exists) {
                out[i] = true;
                queue[tail++] = i;
            }
        }
        while (head < tail) {
            int i = queue[head++];
            int x = i % n, z = i / n;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), nz = z + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || nz < 0 || nx >= n || nz >= n) continue;
                int j = nz * n + nx;
                if (!out[j] && !a.tiles[j].exists) {
                    out[j] = true;
                    queue[tail++] = j;
                }
            }
        }
        return out;
    }
}
