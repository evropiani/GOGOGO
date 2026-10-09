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
        TitleScreen.coinPill(game, W - right, top + 52);

        // toggles on the left, actions on the right; both scroll together if the screen is short
        float px = left + 20, pw = Math.min(640f, W * 0.46f);
        float y0 = top + 100;
        float viewH = H - game.safeBottom - 16 - y0;
        int nf = Cheats.FLAG_NAME.length;
        float rowH = 64;
        float content = Math.max(nf * rowH + 40, 5 * 98) + 20;
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
        float gx = px + pw + 30, gw = W - right - gx;
        float bw = (gw - 20) / 2;
        y = y0 + 6 - off;
        px = gx;
        if (ui.button("c1", px, y, bw, 84, 0xFF34D058, "+10,000 COINS", 34f)) {
            s.addCoins(10000);
            done("KA-CHING!");
        }
        if (ui.button("c2", px + bw + 20, y, bw, 84, 0xFF3BA8FF, "ALL CARS", 34f)) {
            for (int i = 0; i < Cars.ALL.length; i++) if (!Cars.ALL[i].secret) s.carOwned[i] = true;
            done("VROOM!");
        }
        y += 98;
        if (ui.button("c3", px, y, bw, 84, 0xFFFF9A2B, "MAX UPGRADES", 34f)) {
            for (int i = 0; i < 4; i++) s.levels[s.selectedCar][i] = CarDef.MAX_LEVEL;
            done("MAXED!");
        }
        if (ui.button("c4", px + bw + 20, y, bw, 84, 0xFFFF4FA3, "ALL STYLES", 34f)) {
            for (int i = 0; i < s.paintOwned.length; i++) s.paintOwned[i] = true;
            for (int i = 0; i < s.topperOwned.length; i++) s.topperOwned[i] = true;
            for (int i = 0; i < s.wheelOwned.length; i++) s.wheelOwned[i] = true;
            done("FANCY!");
        }
        y += 98;
        if (ui.button("c5", px, y, bw, 84, 0xFF8E62FF, "UNLOCK SECRET", 32f)) {
            s.unlockSecret();
            done("QUACK!");
        }
        if (ui.button("c6", px + bw + 20, y, bw, 84, 0xFF8E62FF, "RELOCK SECRET", 32f)) {
            s.carOwned[Cars.SECRET] = false;
            if (s.selectedCar == Cars.SECRET) s.selectedCar = 0;
            s.secretSeen = false;
            done("LOCKED");
        }
        y += 98;
        if (ui.button("c7", px, y, bw, 84, 0xFFFFC21F, "REVEAL ANIM", 32f)) {
            game.setScreen(new RevealScreen(game, new CheatScreen(game)));
        }
        if (ui.button("c8", px + bw + 20, y, bw, 84, 0xFFFFC21F, "ZERO COINS", 32f)) {
            s.coins = 0;
            done("BROKE");
        }
        y += 98;
        if (ui.button("c9", px, y, gw, 84, 0xFFB8B0C8, "HIDE CHEATS", 36f)) {
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

    public boolean back() {
        game.setScreen(new SettingsScreen(game));
        return true;
    }
}
