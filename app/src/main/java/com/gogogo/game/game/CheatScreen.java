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
        if (ui.roundButton("back", 62, top + 66, 44, 0xFFFFFFFF)) game.setScreen(new SettingsScreen(game));
        ui.iconBack(62, top + 66 + ui.lastRoundPress, 44, 0xFF2A1840);
        b.textShadow(b.title, "CHEATS", W / 2, top + 68, 64f, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 6f, 6f, 0x50200040);
        TitleScreen.coinPill(game, W - 24, top + 66);

        float px = 30, pw = W - 60;
        float y0 = top + 140;
        float viewH = H - game.safeBottom - 30 - y0;
        int nf = Cheats.FLAG_NAME.length;
        float content = 40 + nf * 84 + 30 + 6 * 110 + 40;
        float off = ui.beginScroll("cheats", 0, y0, W, viewH, content);
        float y = y0 - off;
        ui.panel(px, y, pw, nf * 84 + 40, 0xFFFFFFFF);
        for (int i = 0; i < nf; i++) {
            float ry = y + 20 + i * 84 + 42;
            b.text(b.title, Cheats.FLAG_NAME[i], px + 30, ry, 36f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            boolean v = ui.toggle("cf" + i, px + pw - 160, ry - 30, 120, 60, s.devFlags[i]);
            if (v != s.devFlags[i]) {
                s.devFlags[i] = v;
                s.markDirty();
                s.flush();
            }
        }
        y += nf * 84 + 70;
        float bw = (pw - 20) / 2;
        if (ui.button("c1", px, y, bw, 96, 0xFF34D058, "+10,000 COINS", 34f)) {
            s.addCoins(10000);
            done("KA-CHING!");
        }
        if (ui.button("c2", px + bw + 20, y, bw, 96, 0xFF3BA8FF, "ALL CARS", 34f)) {
            for (int i = 0; i < Cars.ALL.length; i++) if (!Cars.ALL[i].secret) s.carOwned[i] = true;
            done("VROOM!");
        }
        y += 110;
        if (ui.button("c3", px, y, bw, 96, 0xFFFF9A2B, "MAX UPGRADES", 34f)) {
            for (int i = 0; i < 4; i++) s.levels[s.selectedCar][i] = CarDef.MAX_LEVEL;
            done("MAXED!");
        }
        if (ui.button("c4", px + bw + 20, y, bw, 96, 0xFFFF4FA3, "ALL STYLES", 34f)) {
            for (int i = 0; i < s.paintOwned.length; i++) s.paintOwned[i] = true;
            for (int i = 0; i < s.topperOwned.length; i++) s.topperOwned[i] = true;
            for (int i = 0; i < s.wheelOwned.length; i++) s.wheelOwned[i] = true;
            done("FANCY!");
        }
        y += 110;
        if (ui.button("c5", px, y, bw, 96, 0xFF8E62FF, "UNLOCK SECRET", 32f)) {
            s.unlockSecret();
            done("QUACK!");
        }
        if (ui.button("c6", px + bw + 20, y, bw, 96, 0xFF8E62FF, "RELOCK SECRET", 32f)) {
            s.carOwned[Cars.SECRET] = false;
            if (s.selectedCar == Cars.SECRET) s.selectedCar = 0;
            s.secretSeen = false;
            done("LOCKED");
        }
        y += 110;
        if (ui.button("c7", px, y, bw, 96, 0xFFFFC21F, "REVEAL ANIM", 32f)) {
            game.setScreen(new RevealScreen(game, new CheatScreen(game)));
        }
        if (ui.button("c8", px + bw + 20, y, bw, 96, 0xFFFFC21F, "ZERO COINS", 32f)) {
            s.coins = 0;
            done("BROKE");
        }
        y += 110;
        if (ui.button("c9", px, y, pw, 96, 0xFFB8B0C8, "HIDE CHEATS", 36f)) {
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
