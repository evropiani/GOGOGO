package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** Maps, arena skins, skies, tile themes and boost trails: preview, buy and equip. */
public final class LockerScreen extends Screen {
    public static final int MAPS = 0, SKINS = 1, SKIES = 2, THEMES = 3, TRAILS = 4;
    private static final String[] TABS = {"MAPS", "SKINS", "SKIES", "THEMES", "TRAILS"};
    private static final int[] TAB_COLORS = {0xFF34D058, 0xFFFF9A2B, 0xFF3BA8FF, 0xFFFF4FA3, 0xFF8E62FF};

    private ArenaPreview preview;
    private int tab;
    /** Item being previewed in the current tab (-1 = shuffle on MAPS). */
    private int sel;
    private float t;
    private String toast;
    private float toastT;
    private int toastColor;
    private float burstT = -1f, burstX, burstY;

    public LockerScreen(Game game, int tab) {
        super(game);
        this.tab = Math.max(MAPS, Math.min(TRAILS, tab));
        sel = Items.selected(game.save, cat());
        game.ui.resetScroll("locker");
    }

    /** Opens on a specific item of the current tab. */
    public LockerScreen focus(int idx) {
        sel = idx;
        if (preview != null) applyPreview();
        return this;
    }

    private int cat() {
        return Items.MAP + tab;
    }

    public void enter() {
        // built here (not in the constructor) so the previous screen keeps its theme during the wipe
        if (preview == null) preview = new ArenaPreview(game);
        applyPreview();
    }

    public void exit() {
        Themes.apply(game.save.theme);
    }

    private void setTab(int i) {
        tab = i;
        sel = Items.selected(game.save, cat());
        applyPreview();
        game.ui.resetScroll("locker");
    }

    private void applyPreview() {
        Save s = game.save;
        int skin = s.skin, sky = s.sky, theme = s.theme, trail = s.trail;
        switch (tab) {
            case SKINS: skin = sel; break;
            case SKIES: sky = sel; break;
            case THEMES: theme = sel; break;
            case TRAILS: trail = sel; break;
            default: break;
        }
        preview.set(skin, sky, theme, trail);
    }

    public int skyId() {
        return preview != null ? preview.sky : game.save.sky;
    }

    private void showToast(String msg, int color) {
        toast = msg;
        toastT = 1.6f;
        toastColor = color;
    }

    public void update(float dt) {
        t += dt;
        if (toastT > 0) toastT -= dt;
        if (burstT >= 0) {
            burstT += dt;
            if (burstT > 0.9f) burstT = -1f;
        }
        if (preview != null && tab != MAPS) preview.update(dt);
    }

    public boolean render3d() {
        if (preview == null || tab == MAPS) return false;
        float W = game.b.width;
        float left = game.safeLeft + 20, lw = W * 0.46f;
        preview.screenX = (left + (lw - left) / 2f) / W;
        preview.screenY = 0.47f;
        // keep the mini arena inside the left column whatever the screen shape
        float colW = lw - left;
        preview.distance = Math.max(36f, Math.min(90f, 46f * (game.b.height / 720f) * (690f / Math.max(300f, colW))));
        preview.render();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        Save s = game.save;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;

        if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) game.setScreen(new TitleScreen(game));
        ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        b.textShadow(b.title, "LOCKER", left + 100, top + 56, 56f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);
        float pr = W - right;
        float cw = TitleScreen.coinPill(game, pr, top + 52);
        TitleScreen.rimsPill(game, pr - cw - 14, top + 52);

        float lw = W * 0.46f;
        if (tab == MAPS) mapPanel(b, ui, left, top + 108, lw - left, bottom - 136 - (top + 108));
        infoBar(b, ui, s, left, lw, bottom - 112);

        // tabs + item grid (right side)
        float px = lw + 10, pw = W - right - px;
        float ty = top + 100;
        float tw = pw / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            float x = px + i * tw;
            boolean on = tab == i;
            if (ui.hit("ltab" + i, x, ty - 6, tw - 8, 82) && tab != i) {
                setTab(i);
                game.sfx.play(Sfx.POP, 0.6f, 1.2f);
            }
            int col = on ? TAB_COLORS[i] : 0xFFFFFFFF;
            b.shape(x + (tw - 8) / 2, ty + 35 + (on ? 0 : 5), tw - 8, 70 - (on ? 0 : 5), 22, col, 0, 0, 0.3f, 0, 0);
            b.textFit(b.title, TABS[i], x + (tw - 8) / 2, ty + 37 + (on ? 0 : 3), 30f, tw - 22, on ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, on ? 0xFF2A1840 : 0, on ? 4f : 0);
        }
        float cy = ty + 84, ch = bottom - 20 - cy;
        b.shadow(px, cy + 8, pw, ch, 30, 0x40200040, 14);
        b.shape(px + pw / 2, cy + ch / 2, pw, ch, 30, 0xFFFFFFFF, TAB_COLORS[tab], 6f, 0f, 0, 0);

        int cat = cat();
        int n = Items.count(cat);
        int first = tab == MAPS ? -1 : 0;
        int cols = pw >= 640 ? 3 : 2;
        float gap = 14;
        float cardW = (pw - 40 - gap * (cols - 1)) / cols, cardH = 158;
        float gy = cy + 12, gh = ch - 24;
        int count = n - first;
        float content = (float) Math.ceil(count / (float) cols) * (cardH + gap) + 10;
        float off = ui.beginScroll("locker", px + 8, gy, pw - 16, gh, content);
        for (int k = 0; k < count; k++) {
            int i = k + first;
            float x = px + 20 + (k % cols) * (cardW + gap);
            float y = gy + 8 + (k / cols) * (cardH + gap) - off;
            if (y > gy + gh || y + cardH < gy) continue;
            card(b, ui, s, cat, i, x, y, cardW, cardH, y + cardH / 2 > gy && y + cardH / 2 < gy + gh);
        }
        ui.endScroll();

        if (burstT >= 0) burst(b, ui);
        if (toastT > 0) {
            float k = Ease.outBack(Math.min(1f, (1.6f - toastT) * 5f));
            b.alpha(Math.min(1f, toastT * 3f));
            b.textShadow(b.title, toast, left + (lw - left) / 2, H * 0.32f, 60f * k, toastColor, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x60200040);
            b.alpha(1f);
        }
    }

    private void card(UIBatch b, UI ui, Save s, int cat, int i, float x, float y, float w, float h, boolean tappable) {
        String id = "item" + i;
        if (ui.hit(id, x, y, w, h) && tappable && sel != i) {
            sel = i;
            applyPreview();
            game.sfx.play(Sfx.POP, 0.7f, 1f + (float) Math.random() * 0.3f);
        }
        boolean owned = i < 0 || Items.owned(s, cat, i);
        boolean equipped = Items.selected(s, cat) == i;
        boolean picked = sel == i;
        float sc = 1f + (float) Math.sin(ui.bounce(id) * Math.PI) * 0.05f;
        float cx = x + w / 2, cy = y + h / 2;
        int fill = picked ? 0xFFFFF3C4 : (owned ? 0xFFF3EEFA : 0xFFE6E0EE);
        int border = picked ? 0xFFFFC21F : (equipped ? 0xFF34D058 : 0);
        b.shape(cx, cy + 4, w * sc, h * sc, 22, 0x30200040, 0, 0, 0, 4, 0);
        b.shape(cx, cy, w * sc, h * sc, 22, fill, border, border != 0 ? 5f : 0f, 0.15f, 0, 0);

        // picture
        float iy = y + 52;
        b.shape(cx, iy, Math.min(w - 24, 150), 84, 18, owned ? 0xFFFFFFFF : 0xFFF4F0F8, 0, 0, 0, 0, 0);
        itemIcon(game, cat, i, cx, iy, 78, t);
        if (!owned) {
            b.shape(cx, iy, Math.min(w - 24, 150), 84, 18, 0x50FFFFFF, 0, 0, 0, 0, 0);
            b.circle(x + 30, y + 28, 20, 0xFF2A1840);
            ui.iconLock(x + 30, y + 28, 22, 0xFFFFFFFF);
        }
        if (equipped) {
            b.circle(x + w - 26, y + 26, 21, 0xFF34D058, 0xFFFFFFFF, 3f);
            ui.iconCheck(x + w - 26, y + 26, 24, 0xFFFFFFFF);
        }

        // name + status
        String name = i < 0 ? "SHUFFLE" : Items.name(cat, i);
        b.textFit(b.title, name, cx, y + 112, 26f, w - 24, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        float sy = y + 140;
        if (equipped) {
            b.text(b.body, "EQUIPPED", cx, sy, 21f, 0xFF2EAE4C, UIBatch.CENTER, 0, 0);
        } else if (owned) {
            b.text(b.body, i < 0 ? "RANDOM MAP" : "OWNED", cx, sy, 21f, 0xFF9A8AB0, UIBatch.CENTER, 0, 0);
        } else {
            priceTag(ui, cat, i, cx, sy, 22f, w - 30);
        }
    }

    /** "LEVEL 12" with a lock, a coin price or a Rims price, centered at (cx, cy). */
    static void priceTag(UI ui, int cat, int i, float cx, float cy, float size, float maxW) {
        int rule = Items.rule(cat, i), v = Items.value(cat, i);
        if (rule == Items.COINS) ui.iconLabel(cx, cy, maxW, null, UI.ICON_COIN, ShopScreen.num(v), size, 0xFFE89A00, 0, 0);
        else if (rule == Items.RIMS) ui.iconLabel(cx, cy, maxW, null, UI.ICON_RIM, ShopScreen.num(v), size, 0xFF2A9FD8, 0, 0);
        else if (rule == Items.LEVEL) ui.iconLabel(cx, cy, maxW, null, UI.ICON_LOCK, "LEVEL " + v, size, 0xFF8E62FF, 0, 0);
        else ui.iconLabel(cx, cy, maxW, null, UI.ICON_NONE, Items.unlockLabel(cat, i), size, 0xFF8E62FF, 0, 0);
    }

    /** Name + blurb of the previewed item and its EQUIP / BUY button, along the bottom of the left side. */
    private void infoBar(UIBatch b, UI ui, Save s, float left, float lw, float y) {
        int cat = cat();
        float bw = 270, bh = 96;
        float bx = lw - 6 - bw;
        float nw = bx - 16 - left;
        String name = sel < 0 ? "SHUFFLE" : Items.name(cat, sel);
        String blurb = sel < 0 ? "A random map you own, every match." : blurb(tab, sel);
        b.shadow(left, y + 8, nw, bh, 30, 0x50200040, 10);
        b.shape(left + nw / 2, y + bh / 2, nw, bh, 30, 0xE02A1840, 0, 0, 0, 0, 0);
        b.textFit(b.title, name, left + 26, y + 34, 38f, nw - 50, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
        b.textFit(b.body, blurb, left + 26, y + 70, 24f, nw - 50, 0xFFD8CCEA, UIBatch.LEFT, 0, 0);

        boolean owned = sel < 0 || Items.owned(s, cat, sel);
        boolean equipped = Items.selected(s, cat) == sel;
        if (equipped) {
            ui.iconButton("act", bx, y, bw, bh, 0xFF34D058, null, UI.ICON_CHECK, "EQUIPPED", 36f, true);
            return;
        }
        if (owned) {
            if (ui.button("act", bx, y, bw, bh, 0xFF34D058, "EQUIP", 46f)) {
                Items.select(s, cat, sel);
                game.sfx.play(Sfx.POP, 1f, 0.9f);
                game.sfx.play(Sfx.WHOOSH, 0.5f, 1.3f);
                showToast("EQUIPPED!", 0xFF5EE65A);
                startBurst(bx + bw / 2, y + bh / 2);
            }
            return;
        }
        int rule = Items.rule(cat, sel), v = Items.value(cat, sel);
        if (rule == Items.LEVEL) {
            ui.iconButton("act", bx, y, bw, bh, 0xFFB8B0C8, null, UI.ICON_LOCK, "LEVEL " + v, 40f, false);
            return;
        }
        boolean premium = rule == Items.RIMS;
        boolean afford = Items.affordable(s, cat, sel);
        int col = afford ? (premium ? 0xFF3BB8FF : 0xFFFF9A2B) : 0xFFB8B0C8;
        if (ui.iconButton("act", bx, y, bw, bh, col, "BUY", premium ? UI.ICON_RIM : UI.ICON_COIN, ShopScreen.num(v), 40f, true)) {
            if (Items.buy(s, cat, sel)) {
                Items.select(s, cat, sel);
                game.sfx.play(Sfx.BUY, 1f, 1f);
                game.sfx.play(Sfx.UNLOCK, 0.7f, 1.1f);
                showToast("UNLOCKED!", 0xFFFFE14D);
                startBurst(bx + bw / 2, y + bh / 2);
            } else {
                game.sfx.play(Sfx.NOPE, 1f, 1f);
                showToast(premium ? "NEED MORE RIMS" : "NEED MORE COINS", 0xFFFF4FA3);
            }
        }
    }

    private static String blurb(int tab, int i) {
        switch (tab) {
            case MAPS: return Maps.BLURB[i];
            case SKINS: return Skins.BLURB[i];
            case SKIES: return Skies.BLURB[i];
            case THEMES: return Themes.BLURB[i];
            default: return Trails.BLURB[i];
        }
    }

    private static int[] tileCount;

    /** MAPS tab: the layout drawn big on a card. */
    private void mapPanel(UIBatch b, UI ui, float x, float y, float w, float h) {
        if (tileCount == null) {
            tileCount = new int[Maps.COUNT];
            for (int i = 0; i < Maps.COUNT; i++) {
                boolean[] m = Maps.mask(i);
                for (boolean on : m) if (on) tileCount[i]++;
            }
        }
        ui.panel(x, y, w, h, 0xFFFFFFFF);
        float size = Math.min(w - 60, h - 70);
        float cx = x + w / 2, cy = y + 24 + size / 2;
        int id = sel;
        if (id < 0) {
            // shuffle: flip through the owned maps
            int owned = 0;
            for (int i = 0; i < Maps.COUNT; i++) if (game.save.mapOwned[i]) owned++;
            int k = ((int) (t * 1.6f)) % Math.max(1, owned);
            id = 0;
            for (int i = 0; i < Maps.COUNT; i++) {
                if (game.save.mapOwned[i] && k-- == 0) {
                    id = i;
                    break;
                }
            }
        }
        b.shape(cx, cy, size + 20, size + 20, 26, 0xFFF3EEFA, 0, 0, 0, 0, 0);
        int[] tiles = Palette.TILE;
        Maps.drawPreview(b, id, cx, cy, size, 0xFF000000 | tiles[(id + 2) % tiles.length]);
        if (sel < 0) {
            float k = 1f + (float) Math.sin(t * 4f) * 0.06f;
            b.shape(cx, cy, 120 * k, 120 * k, 30, 0xFFFFE14D, 0xFF2A1840, 6f, 0.3f, 0, 0.15f);
            b.text(b.title, "?", cx, cy + 4, 96f * k, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        }
        String info = Maps.size(id) + " x " + Maps.size(id) + "  -  " + tileCount[id] + " TILES";
        b.textFit(b.body, sel < 0 ? "SURPRISE ME!" : info, cx, y + h - 26, 26f, w - 40, 0xFF8A7AA0, UIBatch.CENTER, 0, 0);
    }

    private void startBurst(float x, float y) {
        burstT = 0f;
        burstX = x;
        burstY = y;
    }

    /** Stars flying out of the button after a purchase. */
    private void burst(UIBatch b, UI ui) {
        float k = burstT / 0.9f;
        b.alpha(1f - k * k);
        for (int i = 0; i < 14; i++) {
            float a = i * 0.449f + 0.3f;
            float d = Ease.outCubic(k) * (120 + (i % 3) * 50);
            float x = burstX + (float) Math.cos(a) * d * 1.3f, y = burstY + (float) Math.sin(a) * d - k * 40;
            ui.star(x, y, 16 + (i % 4) * 4, TAB_COLORS[i % TAB_COLORS.length]);
        }
        b.alpha(1f);
    }

    public boolean back() {
        game.setScreen(new TitleScreen(game));
        return true;
    }

    // ------------------------------------------------------------------ item pictures (also used by other screens)

    private static final int[] SKIN_EDGE = {
            0xFFC8C4D4, 0xFFFF7EB6, 0xFF9A6234, 0xFFB24A2A, 0xFF46D07A, 0xFF1A1030, 0xFFA8E4FF, 0xFF4A3A66, 0xFF9A6A30, 0xFFE8A800};
    private static final int[] SKIN_TRIM = {
            0xFFFFFFFF, 0xFFFFFFFF, 0xFFE8C08A, 0xFFE07A50, 0xFFC8FFD8, 0xFF39FFEA, 0xFFFFFFFF, 0xFF2A1840, 0xFFD8A860, 0xFFFFF0A0};
    private static final float[] SKIN_ROUND = {0.3f, 0.45f, 0.5f, 0.08f, 0.5f, 0.25f, 0.35f, 0f, 0.1f, 0.3f};
    private static final int[] WHEEL_HUB = {
            0xFFFFFFFF, 0xFFFF7EB6, 0xFF39FF7A, 0xFFE8A060, 0xFFFFD23F, 0xFFFFA040, 0, 0xFFB8C0D0, 0xFFFF6A1F, 0xFFA8F0FF};
    private static final int[][] TRAIL_COLORS = {
            {0xFFFFFFFF, 0xFFE6DEF0}, {0xFFFFB23B, 0xFFFF4A1F}, {0xFFA8ECFF, 0xFFFFFFFF}, {0xFFFFE14D, 0xFFFFFFFF},
            {0xFFFF4FA3, 0xFFFF9AC8}, {0xFFFF4F4F, 0xFF3BB8FF}, {0xFF8FE4FF, 0xFFFFF27A}, {0xFFFF4FA3, 0xFF5EE65A},
            {0xFFFF3B3B, 0xFF34D058}, {0xFFFFD23F, 0xFFFFA800}};
    private static int[][] skyColors;

    /** Small picture of any unlockable item, about s units across, centered at (cx, cy). */
    static void itemIcon(Game game, int cat, int i, float cx, float cy, float s, float t) {
        UIBatch b = game.b;
        UI ui = game.ui;
        switch (cat) {
            case Items.CAR:
                carIcon(b, cx, cy, s, 0xFF000000 | Palette.paintColor(Cars.ALL[i].defPaint, t), 0xFF000000 | Palette.paintColor(Cars.ALL[i].defAccent, t));
                break;
            case Items.PAINT:
                b.circle(cx, cy, s * 0.36f, 0xFFFFFFFF);
                b.shape(cx, cy, s * 0.6f, s * 0.6f, s * 0.3f, 0xFF000000 | Palette.paintColor(i, t), 0, 0, 0.5f, 0, 0);
                break;
            case Items.TOPPER:
                b.roundRect(cx - s * 0.36f, cy + s * 0.14f, s * 0.72f, s * 0.12f, s * 0.06f, 0xFF2A1840);
                b.roundRect(cx - s * 0.22f, cy - s * 0.32f, s * 0.44f, s * 0.5f, s * 0.08f, 0xFF8E62FF);
                b.rect(cx - s * 0.22f, cy + s * 0.02f, s * 0.44f, s * 0.1f, 0xFFFF4FA3);
                ui.star(cx + s * 0.2f, cy - s * 0.3f, s * 0.14f, 0xFFFFE14D);
                break;
            case Items.WHEEL: {
                int hub = WHEEL_HUB[i % WHEEL_HUB.length];
                if (hub == 0) hub = 0xFF000000 | Palette.rainbow(t);
                b.circle(cx, cy + s * 0.05f, s * 0.4f, 0x40200040);
                b.circle(cx, cy, s * 0.4f, 0xFF2A2A3A);
                b.shape(cx, cy, s * 0.48f, s * 0.48f, s * 0.24f, hub, 0xFF2A1840, s * 0.03f, 0.4f, 0, 0);
                b.circle(cx, cy, s * 0.07f, 0xFF2A1840);
                break;
            }
            case Items.MAP:
                if (i < 0) {
                    b.shape(cx, cy, s * 0.62f, s * 0.62f, s * 0.14f, 0xFFFFE14D, 0xFF2A1840, s * 0.05f, 0.3f, 0, 0.15f);
                    b.text(b.title, "?", cx, cy + s * 0.03f, s * 0.5f, 0xFF2A1840, UIBatch.CENTER, 0, 0);
                } else {
                    Maps.drawPreview(b, i, cx, cy, s * 0.92f, 0xFF8E62FF);
                }
                break;
            case Items.SKIN:
                skinIcon(b, cx, cy, s, i);
                break;
            case Items.SKY:
                skyIcon(game, cx, cy, s, i, t);
                break;
            case Items.THEME: {
                int[] c = Themes.COLORS[i];
                for (int k = 0; k < 6; k++) {
                    float x = cx + (k % 3 - 1) * s * 0.3f, y = cy + (k / 3 - 0.5f) * s * 0.34f;
                    b.circle(x, y + s * 0.02f, s * 0.15f, 0x40200040);
                    b.shape(x, y, s * 0.28f, s * 0.28f, s * 0.14f, 0xFF000000 | c[k], 0xFFFFFFFF, s * 0.025f, 0.4f, 0, 0);
                }
                break;
            }
            default:
                trailIcon(game, cx, cy, s, i, t);
                break;
        }
    }

    private static void carIcon(UIBatch b, float cx, float cy, float s, int body, int accent) {
        float k = s / 60f;
        b.shape(cx, cy + 22 * k, 52 * k, 10 * k, 5 * k, 0x40200040, 0, 0, 0, 3, 0);
        b.shape(cx - 2 * k, cy - 8 * k, 30 * k, 18 * k, 8 * k, accent, 0, 0, 0.3f, 0, 0);
        b.roundRect(cx - 12 * k, cy - 13 * k, 18 * k, 8 * k, 3 * k, 0xC0FFFFFF);
        b.shape(cx, cy + 5 * k, 50 * k, 20 * k, 9 * k, body, 0, 0, 0.4f, 0, 0);
        b.circle(cx - 14 * k, cy + 15 * k, 8 * k, 0xFF2A1840);
        b.circle(cx + 14 * k, cy + 15 * k, 8 * k, 0xFF2A1840);
        b.circle(cx - 14 * k, cy + 15 * k, 3.5f * k, 0xFFE8E0F0);
        b.circle(cx + 14 * k, cy + 15 * k, 3.5f * k, 0xFFE8E0F0);
    }

    private static void skinIcon(UIBatch b, float cx, float cy, float s, int i) {
        float q = s * 0.4f;
        int[] tiles = Palette.TILE;
        for (int k = 0; k < 4; k++) {
            float x = cx + (k % 2 == 0 ? -1 : 1) * s * 0.22f, y = cy + (k < 2 ? -1 : 1) * s * 0.22f;
            float r = q * SKIN_ROUND[i];
            b.shape(x, y + s * 0.03f, q, q, r, UI.shade(SKIN_EDGE[i], 0.7f), 0, 0, 0, 0, 0);
            b.shape(x, y, q, q, r, SKIN_EDGE[i], 0, 0, 0.3f, 0, 0);
            b.shape(x, y, q * 0.68f, q * 0.68f, r * 0.68f, 0xFF000000 | tiles[(k * 2 + i) % tiles.length], SKIN_TRIM[i], s * 0.025f, 0.25f, 0, 0);
        }
    }

    private static void skyIcon(Game game, float cx, float cy, float s, int i, float t) {
        UIBatch b = game.b;
        if (skyColors == null) {
            // read the real gradient colors out of Skies, then let the frame's sky re-apply
            skyColors = new int[Skies.COUNT][3];
            for (int k = 0; k < Skies.COUNT; k++) {
                Skies.apply(game.r, k);
                skyColors[k][0] = rgb(game.r.bgTop);
                skyColors[k][1] = rgb(game.r.bgMid);
                skyColors[k][2] = rgb(game.r.bgBottom);
            }
            Skies.invalidate();
        }
        int[] c = skyColors[i];
        float w = s * 1.2f, h = s * 0.84f, r = s * 0.12f;
        float x = cx - w / 2, y = cy - h / 2;
        b.roundRect(x, y, w, r * 2 + 2, r, c[0]);
        b.roundRect(x, y + h - r * 2 - 2, w, r * 2 + 2, r, c[2]);
        int strips = 8;
        float sh = (h - r * 2) / strips;
        for (int k = 0; k < strips; k++) {
            float f = (k + 0.5f) / strips;
            int col = f < 0.5f ? Palette.mix(c[0], c[1], f * 2f) : Palette.mix(c[1], c[2], f * 2f - 1f);
            b.rect(x, y + r + k * sh, w, sh + 0.8f, 0xFF000000 | col);
        }
        int dc = 0xFF000000 | Skies.DECOR_COLOR[i];
        switch (Skies.DECOR[i]) {
            case Skies.DECOR_STARS:
                for (int k = 0; k < 5; k++) {
                    float tw = 0.7f + 0.3f * (float) Math.sin(t * 3f + k * 1.7f);
                    game.ui.star(x + w * (0.15f + k * 0.18f), y + h * (0.22f + (k % 3) * 0.22f), s * 0.06f * tw, dc);
                }
                break;
            case Skies.DECOR_BUBBLES:
                for (int k = 0; k < 4; k++) {
                    float by = y + h * (0.8f - ((t * 0.25f + k * 0.27f) % 1f) * 0.65f);
                    b.circle(x + w * (0.2f + k * 0.2f), by, s * (0.05f + (k % 2) * 0.03f), 0, dc, s * 0.018f);
                }
                break;
            case Skies.DECOR_PLANETS:
                b.circle(x + w * 0.68f, y + h * 0.4f, s * 0.13f, dc);
                b.shape(x + w * 0.68f, y + h * 0.4f, s * 0.42f, s * 0.07f, s * 0.035f, 0xC0FFFFFF, 0, 0, 0, 0, -0.35f);
                b.circle(x + w * 0.25f, y + h * 0.3f, s * 0.04f, 0xFFFFFFFF);
                break;
            case Skies.DECOR_ROCKS:
                b.shape(x + w * 0.3f, y + h * 0.55f, s * 0.2f, s * 0.15f, s * 0.05f, dc, 0, 0, 0, 0, 0.4f);
                b.shape(x + w * 0.72f, y + h * 0.4f, s * 0.14f, s * 0.11f, s * 0.04f, dc, 0, 0, 0, 0, -0.3f);
                b.circle(x + w * 0.5f, y + h * 0.78f, s * 0.05f, 0xFFFFA21F);
                break;
            case Skies.DECOR_GRID:
                for (int k = 0; k < 3; k++) b.rect(x + 2, y + h * (0.62f + k * 0.11f), w - 4, s * 0.02f, dc);
                b.circle(x + w * 0.5f, y + h * 0.42f, s * 0.15f, 0xFFFFD23F);
                break;
            case Skies.DECOR_CANDY:
                b.circle(x + w * 0.28f, y + h * 0.4f, s * 0.08f, dc);
                b.circle(x + w * 0.7f, y + h * 0.6f, s * 0.07f, 0xFFFFFFFF);
                b.shape(x + w * 0.5f, y + h * 0.3f, s * 0.2f, s * 0.06f, s * 0.03f, 0xFF5EE65A, 0, 0, 0, 0, 0.6f);
                break;
            default: // clouds
                b.shape(x + w * 0.32f, y + h * 0.42f, s * 0.36f, s * 0.15f, s * 0.075f, dc, 0, 0, 0, 0, 0);
                b.shape(x + w * 0.7f, y + h * 0.66f, s * 0.3f, s * 0.12f, s * 0.06f, dc, 0, 0, 0, 0, 0);
                break;
        }
    }

    private static int rgb(float[] c) {
        return ((int) (Math.min(1f, c[0]) * 255) << 16) | ((int) (Math.min(1f, c[1]) * 255) << 8) | (int) (Math.min(1f, c[2]) * 255);
    }

    private static void trailIcon(Game game, float cx, float cy, float s, int i, float t) {
        UIBatch b = game.b;
        UI ui = game.ui;
        int[] c = TRAIL_COLORS[i % TRAIL_COLORS.length];
        carIcon(b, cx + s * 0.26f, cy, s * 0.66f, 0xFFFF4FA3, 0xFFFF8AC8);
        for (int k = 0; k < 4; k++) {
            float f = ((t * 1.2f + k * 0.25f) % 1f);
            float x = cx - s * 0.04f - f * s * 0.5f, y = cy + s * 0.08f + (float) Math.sin(f * 6f + k) * s * 0.06f;
            float r = s * (0.14f - f * 0.06f);
            int col = c[k % 2];
            switch (i) {
                case 2: // bubbles
                    b.circle(x, y, r, 0x30FFFFFF, col, s * 0.02f);
                    break;
                case 3: case 9: // stars, gold
                    if (i == 9) ui.coin(x, y, r * 0.9f);
                    else ui.star(x, y, r * 1.3f, col);
                    break;
                case 4: // hearts
                    b.circle(x - r * 0.35f, y - r * 0.2f, r * 0.5f, col);
                    b.circle(x + r * 0.35f, y - r * 0.2f, r * 0.5f, col);
                    b.shape(x, y + r * 0.1f, r * 0.8f, r * 0.8f, r * 0.12f, col, 0, 0, 0, 0, 0.785f);
                    break;
                case 5: // rainbow
                    b.circle(x, y, r, 0xFF000000 | Palette.hsv(k * 0.2f + t * 0.3f, 0.7f, 1f));
                    break;
                case 6: // lightning
                    b.line(x - r, y - r, x + r * 0.2f, y, s * 0.03f, col);
                    b.line(x + r * 0.2f, y, x - r * 0.3f, y + r, s * 0.03f, col);
                    break;
                case 7: // confetti
                    b.shape(x, y, r * 1.2f, r * 0.5f, r * 0.1f, 0xFF000000 | Palette.hsv(k * 0.27f, 0.7f, 1f), 0, 0, 0, 0, t * 4f + k);
                    break;
                case 8: { // traffic lights
                    int[] l = MatchScreen.LIGHT_COLORS;
                    b.circle(x, y, r * 0.9f, l[k % 3]);
                    break;
                }
                default: // puff, fire
                    b.circle(x, y, r, col);
                    break;
            }
        }
    }
}
