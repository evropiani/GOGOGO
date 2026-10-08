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
        float r = 62f;
        float cx = (float) Math.sin(camAngle) * r, cz = (float) Math.cos(camAngle) * r;
        game.cam.fov = 55f;
        game.cam.set(cx, 52f, cz, 0, -6f, 0);
        view.draw();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height;
        float top = game.safeTop;

        // soft vignette band behind the logo for readability
        b.shadow(-40, top + 120, W + 80, 520, 200, 0x40200040, 120);

        // logo: three bouncing GO!s
        int[] cols = {0xFFFF4FA3, 0xFFFFE14D, 0xFF3BE0FF};
        for (int i = 0; i < 3; i++) {
            float appear = Ease.outBack(Math.min(1f, Math.max(0f, (t - i * 0.18f) * 2.2f)));
            float bounce = (float) Math.abs(Math.sin(t * 3.2f + i * 0.9f)) * 14f;
            float x = W / 2 + (i - 1) * 150f;
            float y = top + 230 + i * 120 - bounce;
            float size = 150f * appear;
            if (size > 1f) b.textShadow(b.title, "GO!", x + (i - 1) * 40f, y, size, cols[i], UIBatch.CENTER, 0xFF2A1840, 14f, 12f, 0x70200040);
        }
        b.textShadow(b.body, "100 cars. 1 color. 0 chill.", W / 2, top + 640, 34f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 5f, 4f, 0x50200040);

        // coins pill
        coinPill(game, W - 24, top + 52);

        // buttons
        float by = H - game.safeBottom - 470;
        float pulse = 1f + (float) Math.sin(t * 5f) * 0.03f;
        float bw = 470 * pulse, bh = 150 * pulse;
        if (ui.button("play", W / 2 - bw / 2, by - bh / 2, bw, bh, 0xFF34D058, "PLAY!", 84f)) {
            game.setScreen(new MatchScreen(game));
        }
        if (ui.button("garage", W / 2 - 300, by + 120, 290, 120, 0xFF8E62FF, "GARAGE", 50f)) {
            game.setScreen(new GarageScreen(game));
        }
        if (ui.button("settings", W / 2 + 10, by + 120, 290, 120, 0xFF3BA8FF, "SETTINGS", 50f)) {
            game.setScreen(new SettingsScreen(game));
        }
        Save s = game.save;
        String stats = "WINS " + s.wins + "   MATCHES " + s.matches + (s.bestPlace > 0 ? "   BEST #" + s.bestPlace : "");
        b.text(b.body, stats, W / 2, H - game.safeBottom - 120, 30f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
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
