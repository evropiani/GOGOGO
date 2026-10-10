package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

import java.util.ArrayList;

/** Shown once after the game was updated: what's new in this version. */
public final class WhatsNewScreen extends Screen {
    private static final float TEXT = 28f, LINE = 37f;
    private final Showroom room;
    private final ArrayList<String> lines = new ArrayList<String>();
    private float wrapWidth = -1f;
    private float t;

    public WhatsNewScreen(Game game) {
        super(game);
        room = new Showroom(game);
        room.showSaved(game.save.selectedCar);
        room.screenX = 0.22f;
        room.screenY = 0.55f;
        room.distance = 15f;
        room.pedestalColor = 0xFFD23F;
    }

    public void enter() {
        game.sfx.music(Sfx.MUSIC_MENU);
        game.save.lastVersion = Version.NAME;
        game.save.markDirty();
        game.save.flush();
        room.celebrate();
    }

    public void update(float dt) {
        t += dt;
        room.update(dt);
        if (Math.random() < dt * 1.5) room.fx.confetti((float) Math.random() * 6 - 3, 4, (float) Math.random() * 4 - 2, 10, 0.7f);
    }

    public boolean render3d() {
        room.render();
        return true;
    }

    /** Level-up and achievement pop-ups wait for the title screen. */
    public boolean allowNotes() {
        return false;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;

        float k = Ease.outElastic(Math.min(1f, t * 1.4f));
        float lcx = left + (W * 0.44f - left) / 2f;
        b.textShadow(b.title, "WHAT'S NEW", lcx, top + 80, 92f * k, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 9f, 10f, 0x70200040);
        b.text(b.body, "GO! GO! GO!  v" + Version.NAME, lcx, top + 150, 32f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);

        // the list (right)
        float px = W * 0.44f, pw = W - right - px;
        float py = top + 30, ph = bottom - 150 - py;
        float kp = Ease.outBack(Math.min(1f, Math.max(0f, (t - 0.2f) * 2.5f)));
        px += (1f - kp) * 800;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.textFit(b.title, Changelog.TITLE[0], px + pw / 2, py + 48, 50f, pw - 60, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
        float textW = pw - 110;
        if (Math.abs(textW - wrapWidth) > 1f) {
            wrapWidth = textW;
            lines.clear();
            for (String item : Changelog.ITEMS[0]) {
                int first = lines.size();
                ChangelogScreen.wrap(b.body, item, TEXT, textW, lines);
                lines.set(first, "*" + lines.get(first));
            }
        }
        float ly0 = py + 96, viewH = ph - 110;
        float off = ui.beginScroll("whatsnew", px + 10, ly0, pw - 20, viewH, lines.size() * LINE + 10);
        float ly = ly0 + LINE / 2 - off;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.charAt(0) == '*') {
                ui.star(px + 44, ly, 13, 0xFFFFC21F);
                s = s.substring(1);
            }
            b.text(b.body, s, px + 70, ly, TEXT, 0xFF4A3A60, UIBatch.LEFT, 0, 0);
            ly += LINE;
        }
        ui.endScroll();

        // buttons
        float by = bottom - 128;
        float bw = Math.min(520f, pw * 0.62f);
        if (ui.button("go", px + 10, by, bw, 112, 0xFF34D058, "LET'S GO!", 56f)) game.setScreen(new TitleScreen(game));
        if (ui.button("log", px + 30 + bw, by + 12, pw - bw - 40, 88, 0xFF8E62FF, "UPDATE LOG", 34f)) {
            game.setScreen(new ChangelogScreen(game, false));
        }
    }

    public boolean back() {
        game.setScreen(new TitleScreen(game));
        return true;
    }
}
