package com.gogogo.game.game;

import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

import java.util.ArrayList;

/** Player profile: level, stats, the road of level rewards up to 100, and all achievements. */
public final class ProfileScreen extends Screen {
    public static final int LEVELS = 0, AWARDS = 1;
    private static final String[] TABS = {"LEVELS", "AWARDS"};
    private static final int[] TAB_COLORS = {0xFF34D058, 0xFFFFB321};
    private static final float ROW_H = 96f, LAST_H = 168f;

    private int tab;
    private float t;
    private boolean jumped;
    /** Rewards of every level, index = level. */
    private final ArrayList<ArrayList<Levels.Reward>> rewards = new ArrayList<ArrayList<Levels.Reward>>();

    public ProfileScreen(Game game, int tab) {
        super(game);
        this.tab = tab;
        for (int l = 0; l <= Levels.MAX; l++) rewards.add(Levels.rewardsAt(l));
        game.ui.resetScroll("awards");
    }

    public void update(float dt) {
        t += dt;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        Save s = game.save;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;
        b.rect(0, 0, W, H, 0x30200040);

        if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) game.setScreen(new TitleScreen(game));
        ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        b.textShadow(b.title, "PROFILE", left + 100, top + 56, 56f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);
        float pr = W - right;
        float cw = TitleScreen.coinPill(game, pr, top + 52);
        TitleScreen.rimsPill(game, pr - cw - 14, top + 52);

        float px = left + 10, lw = Math.min(410f, W * 0.28f);
        summary(b, ui, s, px, top + 100, lw, bottom - 20 - (top + 100));

        // tabs + content
        float rx = px + lw + 24, rw = W - right - rx;
        float ty = top + 100;
        float tw = rw / 2f;
        for (int i = 0; i < 2; i++) {
            float x = rx + i * tw;
            boolean on = tab == i;
            if (ui.hit("ptab" + i, x, ty - 6, tw - 8, 82) && tab != i) {
                tab = i;
                game.sfx.play(Sfx.POP, 0.6f, 1.2f);
            }
            int col = on ? TAB_COLORS[i] : 0xFFFFFFFF;
            b.shape(x + (tw - 8) / 2, ty + 35 + (on ? 0 : 5), tw - 8, 70 - (on ? 0 : 5), 22, col, 0, 0, 0.3f, 0, 0);
            String label = i == AWARDS ? "AWARDS  " + Achievements.doneCount(s) + "/" + Achievements.COUNT : TABS[i];
            b.textFit(b.title, label, x + (tw - 8) / 2, ty + 37 + (on ? 0 : 3), 34f, tw - 30, on ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, on ? 0xFF2A1840 : 0, on ? 4f : 0);
        }
        float cy = ty + 84, ch = bottom - 20 - cy;
        b.shadow(rx, cy + 8, rw, ch, 30, 0x40200040, 14);
        b.shape(rx + rw / 2, cy + ch / 2, rw, ch, 30, 0xFFFFFFFF, TAB_COLORS[tab], 6f, 0f, 0, 0);
        if (tab == LEVELS) road(b, ui, s, rx, cy, rw, ch);
        else awards(b, ui, s, rx, cy, rw, ch);
    }

    /** Left column: big level seal, XP and a few stats. */
    private void summary(UIBatch b, UI ui, Save s, float x, float y, float w, float h) {
        int lv = s.level();
        boolean max = lv >= Levels.MAX;
        ui.panel(x, y, w, h, 0xFF2A1840);
        float cx = x + w / 2;
        float pop = 1f + (float) Math.sin(t * 2.5f) * 0.03f;
        TitleScreen.levelSeal(game, cx, y + 86, 58 * pop, lv, max ? 0xFFFF9A2B : 0xFFFFC21F);
        b.textFit(b.title, max ? "MAX LEVEL!" : "LEVEL " + lv, cx, y + 178, 46f, w - 40, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        TitleScreen.xpBar(b, x + 30, y + 222, w - 60, 26, Levels.progress(s.xp), max ? 0xFFFFC21F : 0xFF5EE65A);
        String xpText;
        if (max) xpText = ShopScreen.num(s.xp) + " XP";
        else xpText = ShopScreen.num(s.xp - Levels.xpAt(lv)) + " / " + ShopScreen.num(Levels.xpToNext(lv)) + " XP";
        b.textFit(b.body, xpText, cx, y + 258, 24f, w - 40, 0xFFD8CCEA, UIBatch.CENTER, 0, 0);

        float sy = y + 290;
        if (sy + 180 > y + h) return;
        b.roundRect(x + 24, sy, w - 48, 3, 1.5f, 0x30FFFFFF);
        String[] labels = {"WINS", "MATCHES", "BEST", "BONKS"};
        String[] vals = {String.valueOf(s.wins), String.valueOf(s.matches), s.bestPlace > 0 ? "#" + s.bestPlace : "-", String.valueOf(s.bonks)};
        float colW = (w - 40) / 2;
        for (int i = 0; i < 4; i++) {
            float sx = x + 20 + (i % 2) * colW + colW / 2, ry = sy + 44 + (i / 2) * 78;
            b.textFit(b.title, vals[i], sx, ry, 38f, colW - 16, 0xFFFFE14D, UIBatch.CENTER, 0, 0);
            b.text(b.body, labels[i], sx, ry + 32, 20f, 0xFFB8A8D8, UIBatch.CENTER, 0, 0);
        }
        float ay = sy + 220;
        if (ay + 30 > y + h) return;
        ui.medal(x + 52, ay - 4, 20, 0xFFFFB321);
        b.textFit(b.title, Achievements.doneCount(s) + " / " + Achievements.COUNT + " AWARDS", x + 86, ay, 30f, w - 110, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
    }

    // ------------------------------------------------------------------ level road

    private static float rowY(int level) {
        return 14 + (level - 2) * ROW_H;
    }

    private void road(UIBatch b, UI ui, Save s, float px, float py, float pw, float ph) {
        int cur = s.level();
        float gy = py + 10, gh = ph - 20;
        float content = rowY(Levels.MAX) + LAST_H + 20;
        if (!jumped) {
            jumped = true;
            ui.setScroll("road", Math.max(0f, rowY(Math.max(2, cur)) - gh * 0.3f));
        }
        float off = ui.beginScroll("road", px + 8, gy, pw - 16, gh, content);
        float nx = px + 70;
        float progress = Levels.progress(s.xp);
        for (int l = 2; l <= Levels.MAX; l++) {
            float y = gy + rowY(l) - off;
            float rh = l == Levels.MAX ? LAST_H : ROW_H;
            if (y > gy + gh || y + rh < gy) continue;
            float mid = y + rh / 2;
            boolean past = l <= cur, now = l == cur, next = l == cur + 1;
            // the road between this node and the next one
            if (l < Levels.MAX) {
                float ny = gy + rowY(l + 1) - off + (l + 1 == Levels.MAX ? LAST_H : ROW_H) / 2;
                b.roundRect(nx - 7, mid, 14, ny - mid, 7, 0xFFE6E0EE);
                if (l < cur) b.roundRect(nx - 7, mid, 14, ny - mid, 7, 0xFF5EE65A);
                else if (now) b.roundRect(nx - 7, mid, 14, (ny - mid) * progress, 7, 0xFF5EE65A);
            }
            if (now) b.shape(px + pw / 2, mid, pw - 36, rh - 8, 24, 0xFFFFF3C4, 0xFFFFC21F, 4f, 0, 0, 0);
            else if (next) b.shape(px + pw / 2, mid, pw - 36, rh - 8, 24, 0xFFF3EEFA, 0, 0, 0, 0, 0);
            if (l == Levels.MAX) {
                b.shape(px + pw / 2, mid, pw - 36, rh - 8, 28, now ? 0xFFFFE9A0 : 0xFFFFF6D8, 0xFFFFB321, 5f, 0.2f, 0, 0);
            }
            if (!past && !next) b.alpha(0.55f);
            // node
            if (now) {
                TitleScreen.levelSeal(game, nx, mid, 34 * (1f + (float) Math.sin(t * 4f) * 0.05f), l, 0xFFFFC21F);
            } else {
                int fill = past ? 0xFF34D058 : (l == Levels.MAX ? 0xFFFFB321 : 0xFFD8D0E4);
                b.circle(nx, mid + 3, 30, 0x40200040);
                b.shape(nx, mid, 60, 60, 30, fill, 0xFFFFFFFF, 4f, 0.35f, 0, 0);
                b.textFit(b.title, String.valueOf(l), nx, mid + 1, 28f, 44, past || l == Levels.MAX ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, past ? 0xFF2A1840 : 0, past ? 2.5f : 0);
                if (past) {
                    b.circle(nx + 24, mid - 22, 13, 0xFFFFFFFF);
                    ui.iconCheck(nx + 24, mid - 22, 16, 0xFF34D058);
                }
            }
            // rewards
            float x = nx + 60;
            if (l == Levels.MAX) {
                b.textShadow(b.title, "LEGENDARY", x, mid - 46, 40f, 0xFFFFB321, UIBatch.LEFT, 0xFF2A1840, 4f, 4f, 0x40200040);
                if (now) b.text(b.title, "YOU MADE IT!", x + b.title.width("LEGENDARY", 40f) + 24, mid - 46, 30f, 0xFF34D058, UIBatch.LEFT, 0, 0);
            } else if (now) {
                b.text(b.title, "YOU", px + pw - 40, mid - 12, 24f, 0xFFE89A00, UIBatch.RIGHT, 0, 0);
                b.text(b.title, "ARE HERE", px + pw - 40, mid + 14, 24f, 0xFFE89A00, UIBatch.RIGHT, 0, 0);
            } else if (next) {
                b.text(b.title, "NEXT!", px + pw - 40, mid, 28f, 0xFF8E62FF, UIBatch.RIGHT, 0, 0);
            }
            float chipY = l == Levels.MAX ? mid + 22 : mid;
            float chipH = l == Levels.MAX ? 78 : 66;
            float maxX = px + pw - ((now || next) && l < Levels.MAX ? 160 : 30);
            ArrayList<Levels.Reward> rs = rewards.get(l);
            // shrink the chips a little if they don't fit in one row
            float need = 0;
            for (int k = 0; k < rs.size(); k++) need += rewardChipWidth(game, rs.get(k), chipH) + 12;
            if (need > maxX - x) chipH *= Math.max(0.7f, (maxX - x) / need);
            for (int k = 0; k < rs.size() && x < maxX - 60; k++) {
                x += rewardChip(game, rs.get(k), x, chipY, chipH, maxX - x, t) + 12;
            }
            b.alpha(1f);
        }
        ui.endScroll();
    }

    /** Width rewardChip() would use. */
    static float rewardChipWidth(Game game, Levels.Reward r, float h) {
        float tw = Math.max(game.b.title.width(chipBig(r), h * 0.38f), game.b.body.width(chipSmall(r), h * 0.27f));
        return Math.max(h * 2.2f, h * 0.95f + tw + 22);
    }

    private static String chipSmall(Levels.Reward r) {
        if (r.kind == Levels.R_COINS) return "COINS";
        if (r.kind == Levels.R_RIMS) return "RIMS";
        return Items.CAT_NAME[r.cat];
    }

    private static String chipBig(Levels.Reward r) {
        if (r.kind == Levels.R_COINS) return "+" + ShopScreen.num(r.amount);
        if (r.kind == Levels.R_RIMS) return "+" + r.amount;
        return Items.name(r.cat, r.idx);
    }

    /** One level reward as a chip: picture, small label, big name / amount. Returns the width used. */
    static float rewardChip(Game game, Levels.Reward r, float x, float cy, float h, float maxW, float t) {
        UIBatch b = game.b;
        UI ui = game.ui;
        String small = chipSmall(r), big = chipBig(r);
        int fill, border, bigColor;
        if (r.kind == Levels.R_COINS) {
            fill = 0xFFFFF4D6;
            border = 0xFFFFC21F;
            bigColor = 0xFFE89A00;
        } else if (r.kind == Levels.R_RIMS) {
            fill = 0xFFE2F6FF;
            border = 0xFF3BB8FF;
            bigColor = 0xFF2A9FD8;
        } else {
            boolean legend = (r.cat == Items.CAR && r.idx == Cars.TITAN) || (r.cat == Items.SKIN && r.idx == Skins.GOLDEN);
            fill = legend ? 0xFFFFF0B8 : 0xFFF1EAFF;
            border = legend ? 0xFFFFB321 : 0xFF8E62FF;
            bigColor = 0xFF2A1840;
        }
        float bigSize = h * 0.38f, smallSize = h * 0.27f;
        float tw = Math.max(b.title.width(big, bigSize), b.body.width(small, smallSize));
        float w = Math.min(maxW, Math.max(h * 2.2f, h * 0.95f + tw + 22));
        b.shape(x + w / 2, cy + 3, w, h, h * 0.3f, 0x30200040, 0, 0, 0, 3, 0);
        b.shape(x + w / 2, cy, w, h, h * 0.3f, fill, border, 3f, 0.15f, 0, 0);
        float ix = x + h * 0.5f;
        if (r.kind == Levels.R_COINS) ui.coin(ix, cy, h * 0.27f);
        else if (r.kind == Levels.R_RIMS) ui.rim(ix, cy, h * 0.29f);
        else {
            b.circle(ix, cy, h * 0.38f, 0xFFFFFFFF);
            LockerScreen.itemIcon(game, r.cat, r.idx, ix, cy, h * 0.66f, t);
        }
        float tx = x + h * 0.95f, aw = w - h * 0.95f - 12;
        b.textFit(b.body, small, tx, cy - h * 0.2f, smallSize, aw, 0xFF9A8AB0, UIBatch.LEFT, 0, 0);
        b.textFit(b.title, big, tx, cy + h * 0.14f, bigSize, aw, bigColor, UIBatch.LEFT, 0, 0);
        return w;
    }

    // ------------------------------------------------------------------ achievements

    private void awards(UIBatch b, UI ui, Save s, float px, float py, float pw, float ph) {
        float gy = py + 10, gh = ph - 20;
        int cols = pw - 40 >= 960 ? 2 : 1;
        float gap = 14;
        float cardW = (pw - 40 - gap * (cols - 1)) / cols, cardH = 132;
        int rows = (Achievements.COUNT + cols - 1) / cols;
        float head = 70;
        float content = head + rows * (cardH + gap) + 10;
        float off = ui.beginScroll("awards", px + 8, gy, pw - 16, gh, content);

        // summary strip
        int done = Achievements.doneCount(s);
        float sy = gy + 34 - off;
        b.textFit(b.title, done + " / " + Achievements.COUNT + " COMPLETE", px + 30, sy, 32f, pw * 0.45f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
        TitleScreen.xpBar(b, px + pw * 0.5f, sy, pw * 0.5f - 30, 22, done / (float) Achievements.COUNT, 0xFFFFB321);

        for (int a = 0; a < Achievements.COUNT; a++) {
            float x = px + 20 + (a % cols) * (cardW + gap);
            float y = gy + head + (a / cols) * (cardH + gap) - off;
            if (y > gy + gh || y + cardH < gy) continue;
            awardCard(b, ui, s, a, x, y, cardW, cardH);
        }
        ui.endScroll();
    }

    private void awardCard(UIBatch b, UI ui, Save s, int a, float x, float y, float w, float h) {
        boolean done = s.achDone[a];
        int goal = Achievements.goal(a);
        int prog = Math.min(goal, Achievements.progress(s, a));
        b.shape(x + w / 2, y + h / 2 + 4, w, h, 24, 0x30200040, 0, 0, 0, 4, 0);
        b.shape(x + w / 2, y + h / 2, w, h, 24, done ? 0xFFF0FFF0 : 0xFFF6F2FB, done ? 0xFF34D058 : 0, done ? 4f : 0f, 0.1f, 0, 0);
        if (done) {
            ui.medal(x + 56, y + h / 2 - 8, 34, 0xFFFFB321);
        } else {
            b.circle(x + 56, y + h / 2 - 4, 36, 0xFFE0D8EC);
            ui.star(x + 56, y + h / 2 - 4, 22, 0xFFC8BEDA);
        }
        float tx = x + 108, tw = w - 108 - 20;
        b.textFit(b.title, Achievements.name(a), tx, y + 30, 30f, tw, done ? 0xFF1F8E3C : 0xFF2A1840, UIBatch.LEFT, 0, 0);
        b.textFit(b.body, Achievements.desc(a), tx, y + 62, 22f, tw, 0xFF7A6A90, UIBatch.LEFT, 0, 0);

        // progress bar + rewards on the bottom line
        float ly = y + 100;
        float rewW = rewards(b, ui, a, x + w - 20, ly, done);
        float bw = Math.max(80f, tw - rewW - 20);
        if (done) {
            b.roundRect(tx, ly - 13, bw, 26, 13, 0xFF34D058);
            b.text(b.title, "DONE!", tx + bw / 2, ly + 1, 22f, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        } else {
            b.roundRect(tx, ly - 13, bw, 26, 13, 0xFFE0D8EC);
            if (prog > 0) b.roundRect(tx, ly - 13, Math.max(26f, bw * prog / goal), 26, 13, 0xFFFFC21F);
            b.textFit(b.title, ShopScreen.num(prog) + " / " + ShopScreen.num(goal), tx + bw / 2, ly + 1, 21f, bw - 16, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        }
    }

    /** XP / coin / Rims payout of an achievement, right-aligned at rx. Returns the width used. */
    private float rewards(UIBatch b, UI ui, int a, float rx, float cy, boolean done) {
        float x = rx;
        int col = done ? 0xFF9A8AB0 : 0xFF2A1840;
        int rims = Achievements.rims(a), coins = Achievements.coins(a), xp = Achievements.xp(a);
        float size = 22f;
        if (rims > 0) {
            String v = String.valueOf(rims);
            float w = b.title.width(v, size) + 34;
            ui.iconLabel(x - w / 2, cy, w, null, UI.ICON_RIM, v, size, done ? col : 0xFF2A9FD8, 0, 0);
            x -= w + 14;
        }
        if (coins > 0) {
            String v = ShopScreen.num(coins);
            float w = b.title.width(v, size) + 34;
            ui.iconLabel(x - w / 2, cy, w, null, UI.ICON_COIN, v, size, done ? col : 0xFFE89A00, 0, 0);
            x -= w + 14;
        }
        if (xp > 0) {
            String v = "+" + xp + " XP";
            float w = b.title.width(v, size);
            b.text(b.title, v, x, cy, size, done ? col : 0xFF2EAE4C, UIBatch.RIGHT, 0, 0);
            x -= w + 14;
        }
        return rx - x;
    }

    public boolean back() {
        game.setScreen(new TitleScreen(game));
        return true;
    }
}
