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
        long seed = System.nanoTime();
        Save s = game.save;
        o.map = Maps.pick(s, new com.gogogo.game.engine.Rng(seed ^ 0x5EEDL));
        Themes.apply(s.theme);
        attract = new Match(seed, o, null);
        if (view == null) view = new MatchView(game, attract);
        else view.setMatch(attract);
        view.quiet = true;
        view.skin = s.skin;
        view.sky = s.sky;
    }

    public void enter() {
        t = 0;
        game.sfx.music(Sfx.MUSIC_MENU);
    }

    public int skyId() {
        return view.sky;
    }

    public void update(float dt) {
        t += dt;
        attract.update(dt);
        view.update(dt);
        if (attract.phase == Match.OVER && attract.phaseT > 4f) newAttract();
        camAngle += dt * 0.07f;
    }

    public boolean render3d() {
        // framed for the 15x15 classic grid: bigger maps pull the camera back and widen the view a little
        // (only partly backing off keeps the far side of the map out of the fog)
        float k = Math.max(0.9f, Math.min(1.5f, attract.arena.reach / (15 * Arena.PITCH * 0.5f * 1.4142f)));
        float back = 1f + (k - 1f) * 0.6f;
        float r = 66f * back;
        float cx = (float) Math.sin(camAngle) * r, cz = (float) Math.cos(camAngle) * r;
        game.cam.fov = (float) Math.toDegrees(2.0 * Math.atan(Math.tan(Math.toRadians(24.0)) * k / back));
        game.cam.set(cx, 40f * back, cz, 0, -4f, 0);
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

        // logo: three bouncing GO!s marching down the left side, in start-light colors
        int[] cols = MatchScreen.LIGHT_COLORS;
        float cycle = t % 3.6f; // every few seconds the lights run red, yellow, green
        for (int i = 0; i < 3; i++) {
            float appear = Ease.outBack(Math.min(1f, Math.max(0f, (t - i * 0.18f) * 2.2f)));
            float bounce = (float) Math.abs(Math.sin(t * 3.2f + i * 0.9f)) * 12f;
            float x = lx + (i - 1) * 120f;
            float y = top + 140 + i * 125 - bounce;
            float size = 140f * appear;
            float on = t < 1.5f ? 0f : Math.max(0f, 1f - Math.abs(cycle - 0.4f - i * 0.38f) / 0.3f);
            if (on > 0) b.shape(x, y, 240f + 30f * on, 120f + 20f * on, 60f, UIBatch.withAlpha(cols[i], 0.45f * on), 0, 0, 0, 40f, 0);
            size *= 1f + 0.05f * on;
            if (size > 1f) b.textShadow(b.title, "GO!", x, y, size, UI.shade(cols[i], 1f + 0.22f * on), UIBatch.CENTER, 0xFF2A1840, 13f, 11f, 0x70200040);
        }
        String tag = "100 cars. 1 color. 0 chill.";
        float tw = b.body.width(tag, 34f) + 60;
        b.shape(lx, top + 545, tw, 62, 31, 0xC02A1840, 0, 0, 0, 0, 0);
        b.text(b.body, tag, lx, top + 545, 34f, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);

        // level badge (opens the profile) and currency pills
        levelBadge(b, ui, game.safeLeft + 18, top + 8);
        float pr = W - game.safeRight - 24;
        float cw = coinPill(game, pr, top + 52);
        rimsPill(game, pr - cw - 14, top + 52);

        // buttons
        float by = H * 0.43f;
        float pulse = 1f + (float) Math.sin(t * 5f) * 0.03f;
        float bw = 460 * pulse, bh = 150 * pulse;
        if (ui.button("play", rx - bw / 2, by - bh / 2, bw, bh, 0xFF34D058, "PLAY!", 84f)) {
            game.setScreen(new MatchScreen(game));
        }
        mapChip(b, ui, rx, by + 120);
        float sy = by + 160;
        if (ui.button("garage", rx - 280, sy, 270, 104, 0xFF8E62FF, "GARAGE", 46f)) {
            game.setScreen(new GarageScreen(game));
        }
        if (ui.button("locker", rx + 10, sy, 270, 104, 0xFFFF4FA3, "LOCKER", 46f)) {
            game.setScreen(new LockerScreen(game, LockerScreen.SKINS));
        }
        if (canAffordSomething(game.save)) alertDot(b, rx - 24, sy + 6, t);
        if (canAffordLocker(game.save)) alertDot(b, rx + 266, sy + 6, t + 0.4f);
        float sy2 = sy + 118;
        if (ui.iconButton("awards", rx - 280, sy2, 270, 80, 0xFFFFB321, null, UI.ICON_STAR, "AWARDS", 38f, true)) {
            game.setScreen(new ProfileScreen(game, ProfileScreen.AWARDS));
        }
        if (ui.button("settings", rx + 10, sy2, 270, 80, 0xFF3BA8FF, "SETTINGS", 38f)) {
            game.setScreen(new SettingsScreen(game));
        }
    }

    /** Level seal with an XP bar; tapping it opens the profile. */
    private void levelBadge(UIBatch b, UI ui, float x, float y) {
        Save s = game.save;
        int lv = s.level();
        if (ui.hit("level", x, y, 300, 92)) game.setScreen(new ProfileScreen(game, ProfileScreen.LEVELS));
        float sc = 1f + (float) Math.sin(ui.bounce("level") * Math.PI) * 0.08f;
        float cy = y + 40;
        float px = x + 42, pw = 228, ph = 58;
        b.shadow(px, cy - ph / 2 + 6, pw, ph, 29, 0x50200040, 8);
        b.shape(px + pw / 2, cy, pw, ph, 29, 0xFF2A1840, 0, 0, 0, 0, 0);
        b.textFit(b.title, lv >= Levels.MAX ? "MAX LEVEL" : "LEVEL " + lv, px + 56, cy - 10, 27f, pw - 76, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
        xpBar(b, px + 56, cy + 14, pw - 76, 14, Levels.progress(s.xp), 0xFF5EE65A);
        levelSeal(game, x + 42, cy, 40 * sc, lv, lv >= Levels.MAX ? 0xFFFF9A2B : 0xFFFFC21F);
    }

    /** Rounded progress bar (dark track) with its left edge at x and vertical center at cy. */
    static void xpBar(UIBatch b, float x, float cy, float w, float h, float k, int color) {
        b.roundRect(x, cy - h / 2, w, h, h / 2, 0xFF4A3A66);
        if (k > 0f) {
            float fw = Math.max(h, w * Math.min(1f, k));
            b.roundRect(x, cy - h / 2, fw, h, h / 2, color);
            b.roundRect(x + h * 0.3f, cy - h * 0.32f, Math.max(0f, fw - h * 0.6f), h * 0.22f, h * 0.11f, 0x60FFFFFF);
        }
    }

    /** Eight-pointed badge with the level number on it. */
    static void levelSeal(Game game, float cx, float cy, float r, int level, int color) {
        UIBatch b = game.b;
        float s = r * 1.5f, rr = r * 0.22f;
        float q = (float) (Math.PI / 4);
        b.shape(cx, cy + r * 0.14f, s + 10, s + 10, rr, 0x50200040, 0, 0, 0, 4, 0);
        b.shape(cx, cy + r * 0.14f, s + 10, s + 10, rr, 0x50200040, 0, 0, 0, 4, q);
        b.shape(cx, cy, s + 10, s + 10, rr + 4, 0xFFFFFFFF, 0, 0, 0, 0, 0);
        b.shape(cx, cy, s + 10, s + 10, rr + 4, 0xFFFFFFFF, 0, 0, 0, 0, q);
        b.shape(cx, cy, s, s, rr, color, 0, 0, 0.4f, 0, 0);
        b.shape(cx, cy, s, s, rr, color, 0, 0, 0.4f, 0, q);
        b.shape(cx, cy, r * 1.32f, r * 1.32f, r * 0.66f, UI.shade(color, 1.18f), UI.shade(color, 0.8f), r * 0.07f, 0.3f, 0, 0);
        String n = String.valueOf(level);
        b.textFit(b.title, n, cx, cy + r * 0.04f, r * 0.9f, r * 1.12f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, r * 0.1f);
    }

    private void mapChip(UIBatch b, UI ui, float cx, float cy) {
        Save s = game.save;
        String label = "MAP: " + (s.map < 0 ? "SHUFFLE" : Maps.NAME[s.map]);
        float tw = b.title.width(label, 28f);
        float w = Math.min(520f, tw + 120), h = 52;
        if (ui.hit("mapchip", cx - w / 2, cy - 40, w, 80)) game.setScreen(new LockerScreen(game, LockerScreen.MAPS));
        float sc = 1f + (float) Math.sin(ui.bounce("mapchip") * Math.PI) * 0.06f;
        float x0 = cx - w / 2 * sc;
        b.shape(cx, cy + 4, w * sc, h * sc, h / 2, 0x50200040, 0, 0, 0, 4, 0);
        b.shape(cx, cy, w * sc, h * sc, h / 2, 0xE02A1840, 0xFFFFFFFF, 3f, 0, 0, 0);
        if (s.map < 0) {
            b.shape(x0 + 36, cy, 34, 34, 8, 0xFFFFE14D, 0, 0, 0.3f, 0, 0.2f);
            b.text(b.title, "?", x0 + 36, cy + 1, 26f, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        } else {
            Maps.drawPreview(b, s.map, x0 + 36, cy, 36, 0xFFFFE14D);
        }
        b.textFit(b.title, label, x0 + 64, cy + 1, 28f, w * sc - 110, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
        ui.iconPlay(x0 + w * sc - 30, cy, 22, 0xFFFFE14D);
    }

    /** Pulsing red "!" bubble that says "you can buy something in there". */
    static void alertDot(UIBatch b, float x, float y, float t) {
        float s2 = 1f + (float) Math.abs(Math.sin(t * 4f)) * 0.15f;
        b.circle(x, y + 3, 24 * s2, 0x50200040);
        b.shape(x, y, 48 * s2, 48 * s2, 24 * s2, 0xFFFF3B5C, 0xFFFFFFFF, 4f, 0.4f, 0, 0);
        b.text(b.title, "!", x, y + 1, 34f * s2, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
    }

    /** True if a map, skin, sky, theme or trail is affordable right now. */
    static boolean canAffordLocker(Save s) {
        for (int cat = Items.MAP; cat <= Items.TRAIL; cat++) {
            int n = Items.count(cat);
            for (int i = 0; i < n; i++) if (Items.affordable(s, cat, i)) return true;
        }
        return false;
    }

    /** True if any car, upgrade or cosmetic is affordable right now. */
    static boolean canAffordSomething(Save s) {
        for (CarDef d : Cars.ALL) {
            if (Items.rule(Items.CAR, d.id) == Items.COINS && Items.affordable(s, Items.CAR, d.id)) return true;
        }
        for (int i = 0; i < 4; i++) {
            int lv = s.levels[s.selectedCar][i];
            if (lv < CarDef.MAX_LEVEL && s.coins >= Save.upgradeCost(lv)) return true;
        }
        return false;
    }

    /** Coin counter pill anchored at its right edge. Returns its width. */
    static float coinPill(Game game, float right, float cy) {
        UIBatch b = game.b;
        String s = String.valueOf(game.save.coins);
        float w = b.title.width(s, 42f) + 110;
        b.shadow(right - w, cy - 34 + 6, w, 68, 34, 0x50200040, 8);
        b.shape(right - w / 2, cy, w, 68, 34, 0xFF2A1840, 0xFFFFFFFF, 0, 0, 0, 0);
        game.ui.coin(right - w + 40, cy, 26);
        b.text(b.title, s, right - 28, cy + 2, 42f, 0xFFFFE14D, UIBatch.RIGHT, 0, 0);
        return w;
    }

    /** Rims counter pill anchored at its right edge; tapping it opens the Rims shop. Returns its width. */
    static float rimsPill(Game game, float right, float cy) {
        UIBatch b = game.b;
        UI ui = game.ui;
        String s = String.valueOf(game.save.rims);
        // read-only in the shop itself and on the results screen (which must be left through its own buttons)
        boolean inShop = game.screen() instanceof ShopScreen || game.screen() instanceof ResultsScreen;
        float w = b.title.width(s, 42f) + (inShop ? 110 : 150);
        if (!inShop && ui.hit("rimspill", right - w, cy - 40, w, 80)) game.setScreen(new ShopScreen(game, game.screen()));
        float sc = 1f + (float) Math.sin(ui.bounce("rimspill") * Math.PI) * 0.08f;
        float cx = right - w / 2;
        b.shadow(cx - w * sc / 2, cy - 34 + 6, w * sc, 68, 34, 0x50200040, 8);
        b.shape(cx, cy, w * sc, 68 * sc, 34, 0xFF2A1840, 0, 0, 0, 0, 0);
        float l = cx - w * sc / 2, r = cx + w * sc / 2;
        ui.rim(l + 40, cy, 27);
        b.text(b.title, s, r - (inShop ? 28 : 72), cy + 2, 42f, 0xFF8FE4FF, UIBatch.RIGHT, 0, 0);
        if (!inShop) {
            b.shape(r - 36, cy, 44, 44, 22, 0xFF34D058, 0xFFFFFFFF, 3f, 0.4f, 0, 0);
            b.rect(r - 47, cy - 3, 22, 6, 0xFFFFFFFF);
            b.rect(r - 39, cy - 11, 6, 22, 0xFFFFFFFF);
        }
        return w;
    }

    public boolean back() {
        return false;
    }
}
