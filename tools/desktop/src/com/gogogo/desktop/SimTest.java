package com.gogogo.desktop;

import com.gogogo.game.game.Car;
import com.gogogo.game.game.Match;

/** Headless balance check: runs bot-only matches and prints round/elimination stats. */
public final class SimTest {
    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        double totalRounds = 0, totalTime = 0;
        int ties = 0;
        for (int r = 0; r < runs; r++) {
            Match.Options o = new Match.Options();
            o.attract = true;
            o.bots = 100;
            final Match m = new Match(1234 + r * 77, o, null);
            final int[] cause = new int[6];
            m.listener = new Match.Listener() {
                public void event(int type, Car a, Car b, float v) {
                    if (type == Match.EV_DROP && m.round <= 2) {
                        for (Car c : m.cars) {
                            if (!c.alive) continue;
                            com.gogogo.game.game.Arena.Tile t = m.arena.cellAt(c.x, c.z);
                            if (t != null && t.color == m.target) continue;
                            com.gogogo.game.game.Arena.Tile g = c.bot.goal;
                            float gd = g == null ? -1 : (float) Math.hypot(g.x - c.x, g.z - c.z);
                            System.out.printf("  r%d miss: confused=%b goal=%s gd=%.1f sp=%.1f react=%.2f think=%.2f goalcolor=%d tgt=%d crowd=%d%n", m.round, c.bot.confused, g == null ? "null" : g.index, gd, c.speed(), c.bot.reaction, c.bot.thinkT, g == null ? -1 : g.color, m.target, g == null ? 0 : g.crowd);
                        }
                    }
                    if (type != Match.EV_ELIM) return;
                    com.gogogo.game.game.Arena.Tile t = m.arena.cellAt(a.x, a.z);
                    if (t == null) cause[0]++;                         // off the edge
                    else if (m.phase == Match.SHOW) cause[1]++;        // mid-round fall
                    else if (t.color != m.target) cause[2]++;          // not on target at drop
                    else cause[3]++;                                   // on target tile but fell
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
            totalRounds += m.round;
            totalTime += m.time;
            if (m.tie) ties++;
            Car w = m.winner;
            System.out.printf("run %d: rounds=%d time=%.0fs %s alive/round: %s%n", r, m.round, m.time,
                    m.tie ? "TIE(" + m.tieGroup.size() + ")" : "winner=" + (w == null ? "-" : w.name + " " + w.def.name), sb);
            System.out.printf("   causes: edge=%d midround=%d wrongtile=%d ontarget=%d%n", cause[0], cause[1], cause[2], cause[3]);
        }
        System.out.printf("avg rounds %.1f, avg time %.0fs, ties %d/%d%n", totalRounds / runs, totalTime / runs, ties, runs);
    }
}
