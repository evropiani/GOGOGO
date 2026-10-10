package com.gogogo.game.game;

import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** Developer testing menu. */
public final class CheatScreen extends Screen {
    private String msg;
    private float msgT;

    public CheatScreen(Game game) {
        super(game);
    }

    public void update(float dt) {
        if (msgT > 0) msgT -= dt;
    }

    private void done(String m) {
        msg = m;
        msgT = 1.4f;
        game.sfx.play(Sfx.BUY, 0.8f, 1.3f);
        game.save.markDirty();
        game.save.flush();
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        Save s = game.save;
        float W = b.width, H = b.height, top = game.safeTop;
        b.rect(0, 0, W, H, 0x40200040);
        float left = game.safeLeft + 20, right = game.safeRight + 20;
        if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) game.setScreen(new SettingsScreen(game));
        ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        b.textShadow(b.title, "CHEATS", left + 100, top + 56, 56f, 0xFFFFE14D, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);
        float cw = TitleScreen.coinPill(game, W - right, top + 52);
        TitleScreen.rimsPill(game, W - right - cw - 14, top + 52);

        // toggles on the left, grouped actions on the right; both scroll together if the screen is short
        float px = left + 20, pw = Math.min(640f, W * 0.46f);
        float y0 = top + 100;
        float viewH = H - game.safeBottom - 16 - y0;
        int nf = Cheats.FLAG_NAME.length;
        float rowH = 64, bh = 80, step = 94, head = 46;
        float content = Math.max(nf * rowH + 110, 4 * head + 9 * step) + 20;
        float off = ui.beginScroll("cheats", 0, y0, W, viewH, content);
        float y = y0 + 6 - off;
        ui.panel(px, y, pw, nf * rowH + 24, 0xFFFFFFFF);
        for (int i = 0; i < nf; i++) {
            float ry = y + 12 + i * rowH + rowH / 2;
            b.text(b.title, Cheats.FLAG_NAME[i], px + 28, ry, 32f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            boolean v = ui.toggle("cf" + i, px + pw - 140, ry - 25, 104, 50, s.devFlags[i]);
            if (v != s.devFlags[i]) {
                s.devFlags[i] = v;
                s.markDirty();
                s.flush();
            }
        }
        // profile status under the toggles
        float sy = y + nf * rowH + 64;
        String status = "LEVEL " + s.level() + "   " + ShopScreen.num(s.xp) + " XP   " + Achievements.doneCount(s) + "/" + Achievements.COUNT + " AWARDS";
        b.textFit(b.title, status, px + pw / 2, sy, 30f, pw - 20, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);

        float gx = px + pw + 30, gw = W - right - gx;
        float bw = (gw - 20) / 2, bx2 = gx + bw + 20;
        y = y0 + 6 - off;

        y = section(b, "MONEY", gx, y);
        if (ui.button("c1", gx, y, bw, bh, 0xFF34D058, "+10,000 COINS", 34f)) {
            s.addCoins(10000);
            done("KA-CHING!");
        }
        if (ui.button("c8", bx2, y, bw, bh, 0xFFB8B0C8, "ZERO COINS", 32f)) {
            s.coins = 0;
            done("BROKE");
        }
        y += step;
        if (ui.iconButton("c10", gx, y, bw, bh, 0xFF3BB8FF, "+1,000", UI.ICON_RIM, "RIMS", 34f, true)) {
            s.addRims(1000);
            done("BLING!");
        }
        if (ui.button("c15", bx2, y, bw, bh, 0xFFB8B0C8, "ZERO RIMS", 32f)) {
            s.rims = 0;
            done("NO BLING");
        }
        y += step;

        y = section(b, "LEVELS", gx, y);
        if (ui.button("c11", gx, y, bw, bh, 0xFF34D058, "+5 LEVELS", 34f)) {
            int lv = Math.min(Levels.MAX, s.level() + 5);
            s.xp = Math.max(s.xp, Levels.xpAt(lv));
            Progress.refresh(s);
            done("LEVEL " + s.level() + "!");
        }
        if (ui.button("c12", bx2, y, bw, bh, 0xFFFFB321, "MAX LEVEL", 34f)) {
            s.xp = Math.max(s.xp, Levels.xpAt(Levels.MAX));
            Progress.refresh(s);
            done("LEVEL 100!");
        }
        y += step;
        if (ui.button("c14", gx, y, bw, bh, 0xFFFF4FA3, "RESET LEVEL", 32f)) {
            s.xp = 0;
            s.rewardedLevel = 1;
            for (int i = 0; i < s.achDone.length; i++) s.achDone[i] = false;
            // the counters behind the achievements too, or the next refresh pays them all out again
            s.matches = s.wins = s.ties = s.bonks = s.roundsSurvived = s.bestPlace = 0;
            for (int i = 0; i < s.stats.length; i++) s.stats[i] = 0;
            Progress.refresh(s);
            done("BACK TO 1");
        }
        y += step;

        y = section(b, "UNLOCKS", gx, y);
        if (ui.button("c2", gx, y, bw, bh, 0xFF3BA8FF, "ALL CARS", 34f)) {
            for (int i = 0; i < Cars.ALL.length; i++) if (!Cars.ALL[i].secret) s.carOwned[i] = true;
            done("VROOM!");
        }
        if (ui.button("c3", bx2, y, bw, bh, 0xFFFF9A2B, "MAX UPGRADES", 34f)) {
            for (int i = 0; i < 4; i++) s.levels[s.selectedCar][i] = CarDef.MAX_LEVEL;
            done("MAXED!");
        }
        y += step;
        if (ui.button("c4", gx, y, bw, bh, 0xFFFF4FA3, "ALL STYLES", 34f)) {
            for (int i = 0; i < s.paintOwned.length; i++) s.paintOwned[i] = true;
            for (int i = 0; i < s.topperOwned.length; i++) s.topperOwned[i] = true;
            for (int i = 0; i < s.wheelOwned.length; i++) s.wheelOwned[i] = true;
            done("FANCY!");
        }
        if (ui.button("c13", bx2, y, bw, bh, 0xFF8E62FF, "UNLOCK ITEMS", 32f)) {
            for (int cat = Items.PAINT; cat < Items.CATEGORIES; cat++) {
                for (int i = 0; i < Items.count(cat); i++) Items.grant(s, cat, i);
            }
            done("ALL YOURS!");
        }
        y += step;

        y = section(b, "SECRET CAR", gx, y);
        if (ui.button("c5", gx, y, bw, bh, 0xFF8E62FF, "UNLOCK SECRET", 32f)) {
            s.unlockSecret();
            done("QUACK!");
        }
        if (ui.button("c6", bx2, y, bw, bh, 0xFF8E62FF, "RELOCK SECRET", 32f)) {
            s.carOwned[Cars.SECRET] = false;
            if (s.selectedCar == Cars.SECRET) s.selectedCar = 0;
            s.secretSeen = false;
            done("LOCKED");
        }
        y += step;
        if (ui.button("c7", gx, y, bw, bh, 0xFFFFC21F, "REVEAL ANIM", 32f)) {
            game.setScreen(new RevealScreen(game, new CheatScreen(game)));
        }
        if (ui.button("c9", bx2, y, bw, bh, 0xFFB8B0C8, "HIDE CHEATS", 32f)) {
            s.dev = false;
            for (int i = 0; i < s.devFlags.length; i++) s.devFlags[i] = false;
            s.markDirty();
            s.flush();
            game.setScreen(new SettingsScreen(game));
        }
        ui.endScroll();

        if (msgT > 0) {
            b.alpha(Math.min(1f, msgT * 2f));
            b.textShadow(b.title, msg, W / 2, H / 2, 90f, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 8f, 8f, 0x60200040);
            b.alpha(1f);
        }
    }

    /** Section title above a group of buttons; returns the y where the buttons start. */
    private float section(UIBatch b, String title, float x, float y) {
        b.textShadow(b.title, title, x + 6, y + 22, 30f, 0xFFFFE14D, UIBatch.LEFT, 0xFF2A1840, 4f, 4f, 0x50200040);
        return y + 46;
    }

    public boolean back() {
        game.setScreen(new SettingsScreen(game));
        return true;
    }
}
