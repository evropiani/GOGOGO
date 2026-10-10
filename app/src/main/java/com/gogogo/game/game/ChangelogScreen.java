package com.gogogo.game.game;

import com.gogogo.game.engine.Font;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

import java.util.ArrayList;

/** UPDATE LOG: what every version of the game added, newest first. */
public final class ChangelogScreen extends Screen {
    private static final float TEXT = 30f, LINE = 40f;
    private final boolean fromSettings;
    /** Wrapped bullet lines per version, rebuilt when the card width changes. */
    private final ArrayList<ArrayList<String>> lines = new ArrayList<ArrayList<String>>();
    private float wrapWidth = -1f;

    /** fromSettings: back goes to the settings, otherwise to the title screen. */
    public ChangelogScreen(Game game, boolean fromSettings) {
        super(game);
        this.fromSettings = fromSettings;
    }

    /** Splits text into lines no wider than maxW (one line per bullet start, continuation lines after). */
    static void wrap(Font f, String s, float size, float maxW, ArrayList<String> out) {
        StringBuilder line = new StringBuilder();
        for (String w : s.split(" ")) {
            int len = line.length();
            if (len > 0) line.append(' ');
            line.append(w);
            if (len > 0 && f.width(line, size) > maxW) {
                line.setLength(len);
                out.add(line.toString());
                line.setLength(0);
                line.append(w);
            }
        }
        if (line.length() > 0) out.add(line.toString());
    }

    private void layout(float textW) {
        if (Math.abs(textW - wrapWidth) < 1f) return;
        wrapWidth = textW;
        lines.clear();
        for (int v = 0; v < Changelog.VERSION.length; v++) {
            ArrayList<String> l = new ArrayList<String>();
            for (String item : Changelog.ITEMS[v]) {
                int first = l.size();
                wrap(game.b.body, item, TEXT, textW, l);
                // mark the first line of every bullet so it gets the star
                l.set(first, "*" + l.get(first));
            }
            lines.add(l);
        }
    }

    public void update(float dt) {}

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;
        b.rect(0, 0, W, H, 0x40200040);
        if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) back();
        ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        b.textShadow(b.title, "UPDATE LOG", left + 100, top + 56, 56f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);

        float cw = Math.min(1100f, W - left - right - 40), cx = (W - cw) / 2;
        float textW = cw - 120;
        layout(textW);
        float y0 = top + 104, viewH = bottom - 12 - y0;
        float content = 0;
        for (int v = 0; v < lines.size(); v++) content += cardHeight(v) + 24;
        float off = ui.beginScroll("log", 0, y0, W, viewH, content);
        float y = y0 - off;
        for (int v = 0; v < lines.size(); v++) {
            float h = cardHeight(v);
            if (y + h > y0 - 10 && y < y0 + viewH + 10) card(b, ui, v, cx, y, cw, h);
            y += h + 24;
        }
        ui.endScroll();
    }

    private float cardHeight(int v) {
        return 96 + lines.get(v).size() * LINE + 16;
    }

    private void card(UIBatch b, UI ui, int v, float x, float y, float w, float h) {
        boolean current = v == 0;
        ui.panel(x, y, w, h, 0xFFFFFFFF);
        String ver = "v" + Changelog.VERSION[v];
        float pw = b.title.width(ver, 36f) + 44;
        b.shape(x + 30 + pw / 2, y + 50, pw, 58, 29, current ? 0xFF34D058 : 0xFF8E62FF, 0, 0, 0.3f, 0, 0);
        b.text(b.title, ver, x + 30 + pw / 2, y + 52, 36f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);
        b.textFit(b.title, Changelog.TITLE[v], x + 54 + pw, y + 52, 40f, w - pw - 90, 0xFF2A1840, UIBatch.LEFT, 0, 0);
        ArrayList<String> l = lines.get(v);
        float ly = y + 96 + LINE / 2;
        for (int i = 0; i < l.size(); i++) {
            String s = l.get(i);
            if (s.charAt(0) == '*') {
                ui.star(x + 56, ly, 14, current ? 0xFFFFC21F : 0xFFB8A8D8);
                s = s.substring(1);
            }
            b.text(b.body, s, x + 84, ly, TEXT, 0xFF4A3A60, UIBatch.LEFT, 0, 0);
            ly += LINE;
        }
    }

    public boolean back() {
        game.setScreen(fromSettings ? new SettingsScreen(game) : new TitleScreen(game));
        return true;
    }
}
