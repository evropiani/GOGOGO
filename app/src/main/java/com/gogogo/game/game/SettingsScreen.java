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
        float left = game.safeLeft + 20, right = game.safeRight + 20;
        b.rect(0, 0, W, H, 0x30200040);

        if (!codeOpen && !confirmReset) {
            if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) game.setScreen(new TitleScreen(game));
            ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        }
        b.textShadow(b.title, "SETTINGS", left + 100, top + 56, 56f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);

        // toggles (left)
        float px = left + 20, pw = Math.min(700f, W * 0.52f), py = top + 110;
        String[] labels = {"Sound", "Music", "Vibration", "Camera shake", "Color symbols", "Swap controls"};
        float rowH = 84;
        float ph = labels.length * rowH + 24;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        boolean[] vals = {s.sound, s.music, s.vibration, s.shake, s.symbols, s.leftHanded};
        for (int i = 0; i < labels.length; i++) {
            float y = py + 12 + i * rowH + rowH / 2;
            b.text(b.title, labels[i], px + 36, y, 38f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            boolean nv = ui.toggle("tg" + i, px + pw - 160, y - 30, 124, 60, vals[i]);
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
            if (i < labels.length - 1) b.rect(px + 28, py + 12 + (i + 1) * rowH, pw - 56, 3, 0x152A1840);
        }

        // buttons (right)
        float rx = px + pw + 40, rw = W - right - rx;
        float bw = Math.min(460f, rw), bx = rx + (rw - bw) / 2;
        float by = py;
        if (ui.button("code", bx, by, bw, 104, 0xFF8E62FF, "ENTER CODE", 48f)) {
            codeOpen = true;
            code.setLength(0);
        }
        by += 120;
        if (ui.button("log", bx, by, bw, 104, 0xFF3BA8FF, "UPDATE LOG", 48f)) game.setScreen(new ChangelogScreen(game, true));
        by += 120;
        if (s.dev) {
            if (ui.button("dev", bx, by, bw, 104, 0xFFFFC21F, "CHEATS", 48f)) game.setScreen(new CheatScreen(game));
            by += 120;
        }
        if (ui.button("reset", bx + bw / 2 - 170, by, 340, 88, 0xFFFF4FA3, "RESET PROGRESS", 32f)) confirmReset = true;

        float cy = H - game.safeBottom - 70;
        b.text(b.body, "GO! GO! GO!  v" + Version.NAME, rx + rw / 2, cy, 26f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);
        b.textFit(b.body, "Fonts: Luckiest Guy (Apache 2.0), Lilita One (OFL)", rx + rw / 2, cy + 36, 21f, rw, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);

        if (codeOpen) codePanel(b, ui, W, H);
        if (confirmReset) resetPanel(b, ui, W, H);
    }

    private void codePanel(UIBatch b, UI ui, float W, float H) {
        b.rect(0, 0, W, H, 0xA0200040);
        ui.block(0, 0, W, H);
        float pw = Math.min(1000f, W - 40), ph = 680;
        float px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "SECRET CODE", W / 2, py + 50, 52f, 0xFF8E62FF, UIBatch.CENTER, 0xFF2A1840, 4f);
        float sx = shakeT > 0 ? (float) Math.sin(t * 70f) * 16f * shakeT / 0.5f : 0f;
        float fw = Math.min(620f, pw - 80);
        b.shape(W / 2 + sx, py + 125, fw, 82, 24, 0xFF2A1840, 0, 0, 0, 0, 0);
        boolean caret = ((int) (t * 2)) % 2 == 0;
        b.textFit(b.title, code + (caret && code.length() < 14 ? "_" : ""), W / 2 + sx, py + 127, 54f, fw - 40, 0xFFFFE14D, UIBatch.CENTER, 0, 0);
        if (msgT > 0) {
            b.alpha(Math.min(1f, msgT * 2f));
            b.text(b.title, msg, W / 2, py + 190, 36f, msgColor, UIBatch.CENTER, 0xFF2A1840, 3f);
            b.alpha(1f);
        }

        // keyboard
        float ky = py + 222;
        float kw = Math.min(86f, (pw - 40) / 10f), kh = 72;
        float kx0 = W / 2 - kw * 5;
        drawKeyRow(b, ui, "1234567890", kx0, ky, kw, kh);
        for (int r = 0; r < ROWS.length; r++) {
            String row = ROWS[r];
            drawKeyRow(b, ui, row, kx0 + (10 - row.length()) * kw / 2, ky + (r + 1) * (kh + 6), kw, kh);
        }
        float fy = ky + 4 * (kh + 6) + 6;
        float bw2 = 200;
        if (ui.button("kdel", W / 2 - bw2 * 1.5f - 20, fy, bw2, 84, 0xFFFF4FA3, "DEL", 38f) && code.length() > 0) code.setLength(code.length() - 1);
        if (ui.button("kclose", W / 2 - bw2 / 2, fy, bw2, 84, 0xFFB8B0C8, "CLOSE", 38f)) codeOpen = false;
        if (ui.button("kok", W / 2 + bw2 / 2 + 20, fy, bw2, 84, 0xFF34D058, "OK", 44f)) submit();
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
        float pw = 640, ph = 380, px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "RESET ALL?", W / 2, py + 75, 62f, 0xFFFF4FA3, UIBatch.CENTER, 0xFF2A1840, 4f);
        b.text(b.body, "Coins, cars and upgrades will be gone.", W / 2, py + 150, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
        if (ui.button("rno", px + 50, py + 225, 250, 110, 0xFF34D058, "NO!", 50f)) confirmReset = false;
        if (ui.button("ryes", px + pw - 300, py + 225, 250, 110, 0xFFFF4FA3, "YES", 50f)) {
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
