package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** Main menu over a live bot match. */
public final class TitleScreen extends Screen {
    private Match attract;
    private MatchView view;
    private float t;
    private float camAngle;

    public TitleScreen(Game game) {
        super(game);
        newAttract();
    }

    private void newAttract() {
        Match.Options o = new Match.Options();
        o.attract = true;
        o.bots = 60;
        attract = new Match(System.nanoTime(), o, null);
        if (view == null) view = new MatchView(game, attract);
        else view.setMatch(attract);
        view.quiet = true;
    }

    public void enter() {
        t = 0;
        game.sfx.music(Sfx.MUSIC_MENU);
    }

    public void update(float dt) {
        t += dt;
        attract.update(dt);
        view.update(dt);
        if (attract.phase == Match.OVER && attract.phaseT > 4f) newAttract();
        camAngle += dt * 0.07f;
    }

    public boolean render3d() {
        float r = 66f;
        float cx = (float) Math.sin(camAngle) * r, cz = (float) Math.cos(camAngle) * r;
        game.cam.fov = 48f;
        game.cam.set(cx, 40f, cz, 0, -4f, 0);
        game.beginWorld();
        view.draw();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height;
        float top = game.safeTop;
        float lx = W * 0.27f; // logo column
        float rx = W * 0.72f; // button column

        // soft glow behind the logo for readability
        b.shadow(lx - 330, top + 60, 660, 520, 240, 0x40200040, 120);

        // logo: three bouncing GO!s marching down the left side
        int[] cols = {0xFFFF4FA3, 0xFFFFE14D, 0xFF3BE0FF};
        for (int i = 0; i < 3; i++) {
            float appear = Ease.outBack(Math.min(1f, Math.max(0f, (t - i * 0.18f) * 2.2f)));
            float bounce = (float) Math.abs(Math.sin(t * 3.2f + i * 0.9f)) * 12f;
            float x = lx + (i - 1) * 120f;
            float y = top + 140 + i * 125 - bounce;
            float size = 140f * appear;
            if (size > 1f) b.textShadow(b.title, "GO!", x, y, size, cols[i], UIBatch.CENTER, 0xFF2A1840, 13f, 11f, 0x70200040);
        }
        String tag = "100 cars. 1 color. 0 chill.";
        float tw = b.body.width(tag, 34f) + 60;
        b.shape(lx, top + 545, tw, 62, 31, 0xC02A1840, 0, 0, 0, 0, 0);
        b.text(b.body, tag, lx, top + 545, 34f, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);

        // coins pill
        coinPill(game, W - game.safeRight - 24, top + 52);

        // buttons
        float by = H * 0.43f;
        float pulse = 1f + (float) Math.sin(t * 5f) * 0.03f;
        float bw = 460 * pulse, bh = 150 * pulse;
        if (ui.button("play", rx - bw / 2, by - bh / 2, bw, bh, 0xFF34D058, "PLAY!", 84f)) {
            game.setScreen(new MatchScreen(game));
        }
        float sy = by + 110;
        if (ui.button("garage", rx - 280, sy, 270, 116, 0xFF8E62FF, "GARAGE", 48f)) {
            game.setScreen(new GarageScreen(game));
        }
        if (canAffordSomething(game.save)) {
            float bx = rx - 24, byy = sy + 8;
            float s2 = 1f + (float) Math.abs(Math.sin(t * 4f)) * 0.15f;
            b.circle(bx, byy + 3, 24 * s2, 0x50200040);
            b.shape(bx, byy, 48 * s2, 48 * s2, 24 * s2, 0xFFFF3B5C, 0xFFFFFFFF, 4f, 0.4f, 0, 0);
            b.text(b.title, "!", bx, byy + 1, 34f * s2, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        }
        if (ui.button("settings", rx + 10, sy, 270, 116, 0xFF3BA8FF, "SETTINGS", 48f)) {
            game.setScreen(new SettingsScreen(game));
        }
        Save s = game.save;
        String stats = "WINS " + s.wins + "   MATCHES " + s.matches + (s.bestPlace > 0 ? "   BEST #" + s.bestPlace : "");
        b.text(b.body, stats, rx, H - game.safeBottom - 60, 30f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
    }

    /** True if any car, upgrade or cosmetic is affordable right now. */
    static boolean canAffordSomething(Save s) {
        for (CarDef d : Cars.ALL) {
            if (!d.secret && !s.carOwned[d.id] && s.coins >= d.price) return true;
        }
        for (int i = 0; i < 4; i++) {
            int lv = s.levels[s.selectedCar][i];
            if (lv < CarDef.MAX_LEVEL && s.coins >= Save.upgradeCost(lv)) return true;
        }
        return false;
    }

    /** Coin counter pill anchored at its right edge. */
    static void coinPill(Game game, float right, float cy) {
        UIBatch b = game.b;
        String s = String.valueOf(game.save.coins);
        float w = b.title.width(s, 42f) + 110;
        b.shadow(right - w, cy - 34 + 6, w, 68, 34, 0x50200040, 8);
        b.shape(right - w / 2, cy, w, 68, 34, 0xFF2A1840, 0xFFFFFFFF, 0, 0, 0, 0);
        game.ui.coin(right - w + 40, cy, 26);
        b.text(b.title, s, right - 28, cy + 2, 42f, 0xFFFFE14D, UIBatch.RIGHT, 0, 0);
    }

    public boolean back() {
        return false;
    }
}
