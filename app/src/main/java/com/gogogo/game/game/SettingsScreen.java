package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** Toggles, code entry, credits. */
public final class SettingsScreen extends Screen {
    private static final String[] ROWS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};

    private boolean codeOpen;
    private final StringBuilder code = new StringBuilder();
    private float shakeT;
    private String msg;
    private int msgColor;
    private float msgT;
    private boolean confirmReset;
    private float t;

    public SettingsScreen(Game game) {
        super(game);
    }

    public void update(float dt) {
        t += dt;
        if (shakeT > 0) shakeT -= dt;
        if (msgT > 0) msgT -= dt;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        Save s = game.save;
        float W = b.width, H = b.height, top = game.safeTop;
        b.rect(0, 0, W, H, 0x30200040);

        if (!codeOpen && !confirmReset) {
            if (ui.roundButton("back", 62, top + 66, 44, 0xFFFFFFFF)) game.setScreen(new TitleScreen(game));
            ui.iconBack(62, top + 66 + ui.lastRoundPress, 44, 0xFF2A1840);
        }
        b.textShadow(b.title, "SETTINGS", W / 2, top + 68, 64f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 6f, 6f, 0x50200040);

        float px = 40, pw = W - 80, py = top + 140;
        String[] labels = {"Sound", "Music", "Vibration", "Camera shake", "Color symbols", "Boost on left"};
        float rowH = 92;
        float ph = labels.length * rowH + 40;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        boolean[] vals = {s.sound, s.music, s.vibration, s.shake, s.symbols, s.leftHanded};
        for (int i = 0; i < labels.length; i++) {
            float y = py + 20 + i * rowH + rowH / 2;
            b.text(b.title, labels[i], px + 40, y, 40f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            boolean nv = ui.toggle("tg" + i, px + pw - 170, y - 32, 130, 64, vals[i]);
            if (nv != vals[i]) {
                switch (i) {
                    case 0: s.sound = nv; break;
                    case 1: s.music = nv; game.sfx.refreshMusic(); break;
                    case 2: s.vibration = nv; if (nv) game.platform.vibrate(40); break;
                    case 3: s.shake = nv; break;
                    case 4: s.symbols = nv; break;
                    default: s.leftHanded = nv; break;
                }
                s.markDirty();
                s.flush();
            }
            if (i < labels.length - 1) b.rect(px + 30, py + 20 + (i + 1) * rowH, pw - 60, 3, 0x152A1840);
        }

        float by = py + ph + 30;
        if (ui.button("code", px, by, pw, 110, 0xFF8E62FF, "ENTER CODE", 52f)) {
            codeOpen = true;
            code.setLength(0);
        }
        by += 130;
        if (s.dev) {
            if (ui.button("dev", px, by, pw, 110, 0xFFFFC21F, "CHEATS", 52f)) game.setScreen(new CheatScreen(game));
            by += 130;
        }
        if (ui.button("reset", px + pw / 2 - 170, by, 340, 90, 0xFFFF4FA3, "RESET PROGRESS", 34f)) confirmReset = true;

        float cy = H - game.safeBottom - 120;
        b.text(b.body, "GO! GO! GO!  v" + Version.NAME, W / 2, cy, 28f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);
        b.text(b.body, "Fonts: Luckiest Guy (Apache 2.0), Lilita One (OFL)", W / 2, cy + 40, 22f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);

        if (codeOpen) codePanel(b, ui, W, H);
        if (confirmReset) resetPanel(b, ui, W, H);
    }

    private void codePanel(UIBatch b, UI ui, float W, float H) {
        b.rect(0, 0, W, H, 0xA0200040);
        ui.block(0, 0, W, H);
        float pw = W - 40, ph = 820;
        float px = 20, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "SECRET CODE", W / 2, py + 70, 60f, 0xFF8E62FF, UIBatch.CENTER, 0xFF2A1840, 4f);
        float sx = shakeT > 0 ? (float) Math.sin(t * 70f) * 16f * shakeT / 0.5f : 0f;
        b.shape(W / 2 + sx, py + 170, pw - 80, 100, 26, 0xFF2A1840, 0, 0, 0, 0, 0);
        String shown = code.length() == 0 ? "" : code.toString();
        boolean caret = ((int) (t * 2)) % 2 == 0;
        b.textFit(b.title, shown + (caret && code.length() < 14 ? "_" : ""), W / 2 + sx, py + 172, 60f, pw - 120, 0xFFFFE14D, UIBatch.CENTER, 0, 0);
        if (msgT > 0) {
            b.alpha(Math.min(1f, msgT * 2f));
            b.text(b.title, msg, W / 2, py + 255, 40f, msgColor, UIBatch.CENTER, 0xFF2A1840, 3f);
            b.alpha(1f);
        }

        // keyboard
        float ky = py + 310;
        float kw = (pw - 40) / 10f, kh = 92;
        String digits = "1234567890";
        drawKeyRow(b, ui, digits, px + 20, ky, kw, kh);
        for (int r = 0; r < ROWS.length; r++) {
            String row = ROWS[r];
            float rx = px + 20 + (10 - row.length()) * kw / 2;
            drawKeyRow(b, ui, row, rx, ky + (r + 1) * (kh + 8), kw, kh);
        }
        float fy = ky + 4 * (kh + 8) + 10;
        if (ui.button("kdel", px + 30, fy, 200, 96, 0xFFFF4FA3, "DEL", 40f) && code.length() > 0) code.setLength(code.length() - 1);
        if (ui.button("kclose", px + 245, fy, 200, 96, 0xFFB8B0C8, "CLOSE", 40f)) codeOpen = false;
        if (ui.button("kok", px + pw - 230, fy, 200, 96, 0xFF34D058, "OK", 46f)) submit();
    }

    private void drawKeyRow(UIBatch b, UI ui, String keys, float x, float y, float kw, float kh) {
        for (int i = 0; i < keys.length(); i++) {
            char c = keys.charAt(i);
            float kx = x + i * kw;
            String id = "key" + c;
            boolean down = ui.isPressed(id);
            if (ui.hit(id, kx + 3, y, kw - 6, kh) && code.length() < 14) {
                code.append(c);
            }
            float press = down ? 4 : 0;
            b.shape(kx + kw / 2, y + kh / 2 + 5, kw - 8, kh - 6, 14, 0xFFC8BEDC, 0, 0, 0, 0, 0);
            b.shape(kx + kw / 2, y + kh / 2 + press, kw - 8, kh - 10, 14, 0xFFF3EEFA, 0, 0, 0.2f, 0, 0);
            b.text(b.title, String.valueOf(c), kx + kw / 2, y + kh / 2 + press, 40f, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        }
    }

    private void submit() {
        Save s = game.save;
        int r = Cheats.check(code.toString());
        if (r == Cheats.CODE_CAR) {
            boolean had = s.secretUnlocked();
            s.unlockSecret();
            codeOpen = false;
            if (!had) {
                game.setScreen(new RevealScreen(game, new GarageScreen(game)));
            } else {
                flash("ALREADY YOURS!", 0xFF34D058);
                codeOpen = true;
            }
        } else if (r == Cheats.CODE_DEV) {
            s.dev = true;
            s.markDirty();
            s.flush();
            game.sfx.play(Sfx.UNLOCK, 1f, 1.2f);
            flash("UNLOCKED!", 0xFF34D058);
            code.setLength(0);
        } else {
            shakeT = 0.5f;
            game.sfx.play(Sfx.NOPE, 1f, 1f);
            game.vibrate(60);
            flash("NOPE!", 0xFFFF4FA3);
            code.setLength(0);
        }
    }

    private void flash(String m, int color) {
        msg = m;
        msgColor = color;
        msgT = 1.6f;
    }

    private void resetPanel(UIBatch b, UI ui, float W, float H) {
        b.rect(0, 0, W, H, 0xA0200040);
        ui.block(0, 0, W, H);
        float pw = 600, ph = 400, px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "RESET ALL?", W / 2, py + 80, 64f, 0xFFFF4FA3, UIBatch.CENTER, 0xFF2A1840, 4f);
        b.text(b.body, "Coins, cars and upgrades will be gone.", W / 2, py + 160, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
        if (ui.button("rno", px + 40, py + 240, 250, 110, 0xFF34D058, "NO!", 50f)) confirmReset = false;
        if (ui.button("ryes", px + pw - 290, py + 240, 250, 110, 0xFFFF4FA3, "YES", 50f)) {
            boolean dev = game.save.dev;
            game.save.reset();
            game.save.dev = dev;
            game.save.markDirty();
            game.save.flush();
            confirmReset = false;
            game.sfx.play(Sfx.POP, 1f, 0.7f);
        }
    }

    public boolean back() {
        if (codeOpen) {
            codeOpen = false;
            return true;
        }
        if (confirmReset) {
            confirmReset = false;
            return true;
        }
        game.setScreen(new TitleScreen(game));
        return true;
    }
}
