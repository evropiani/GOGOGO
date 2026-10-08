package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** Buy, upgrade and pimp your cars. */
public final class GarageScreen extends Screen {
    private static final String[] TABS = {"CAR", "UPGRADE", "PAINT", "STYLE"};
    private static final int[] TAB_COLORS = {0xFF34D058, 0xFFFF9A2B, 0xFFFF4FA3, 0xFF3BA8FF};

    private final Showroom room;
    private int carIdx;
    private int tab;
    private boolean accentMode;   // paint tab: body vs accent
    private boolean wheelMode;    // style tab: toppers vs wheels
    private float t;
    private String toast;
    private float toastT;
    private int toastColor;

    // pending purchase (cosmetic preview)
    private int pendingKind = -1; // 0 paint, 1 accent, 2 topper, 3 wheel
    private int pendingItem;

    public GarageScreen(Game game) {
        super(game);
        room = new Showroom(game);
        carIdx = game.save.selectedCar;
        refreshCar(false);
    }

    private Save s() {
        return game.save;
    }

    private boolean secretHidden(int i) {
        return Cars.ALL[i].secret && !s().carOwned[i];
    }

    private void refreshCar(boolean animate) {
        Save s = s();
        if (secretHidden(carIdx)) {
            room.show(carIdx, 15, 15, 0, 0, animate);
            room.car.paint = 15;
        } else {
            room.show(carIdx, s.carPaint[carIdx], s.carAccent[carIdx], s.carTopper[carIdx], s.carWheel[carIdx], animate);
        }
        pendingKind = -1;
    }

    private void applyPreview() {
        Save s = s();
        int paint = s.carPaint[carIdx], accent = s.carAccent[carIdx], top = s.carTopper[carIdx], wheel = s.carWheel[carIdx];
        if (pendingKind == 0) paint = pendingItem;
        if (pendingKind == 1) accent = pendingItem;
        if (pendingKind == 2) top = pendingItem;
        if (pendingKind == 3) wheel = pendingItem;
        room.car.setLook(paint, accent, top, wheel);
        room.car.squashV = -5f;
    }

    private void showToast(String msg, int color) {
        toast = msg;
        toastT = 1.6f;
        toastColor = color;
    }

    public void update(float dt) {
        t += dt;
        room.update(dt);
        if (toastT > 0) toastT -= dt;
    }

    public boolean render3d() {
        room.screenY = 0.235f;
        room.render();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;

        if (ui.roundButton("back", 62, top + 66, 44, 0xFFFFFFFF)) game.setScreen(new TitleScreen(game));
        ui.iconBack(62, top + 66 + ui.lastRoundPress, 44, 0xFF2A1840);
        b.textShadow(b.title, "GARAGE", W / 2 - 40, top + 68, 60f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 6f, 6f, 0x50200040);
        TitleScreen.coinPill(game, W - 24, top + 66);

        // car selector
        CarDef d = Cars.ALL[carIdx];
        float ny = top + 610;
        boolean hidden = secretHidden(carIdx);
        String name = hidden ? "???" : d.name;
        b.shadow(110, ny - 50 + 8, W - 220, 100, 50, 0x50200040, 10);
        b.shape(W / 2, ny, W - 220, 100, 50, 0xFF2A1840, 0, 0, 0, 0, 0);
        b.textFit(b.title, name, W / 2, ny - 6, 50f, W - 300, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        String sub = s().carOwned[carIdx] ? (s().selectedCar == carIdx ? "DRIVING THIS ONE" : "OWNED") : (hidden ? "SECRET" : d.price + " COINS");
        b.text(b.body, sub, W / 2, ny + 32, 24f, s().carOwned[carIdx] ? 0xFF5EE65A : 0xFFFFE14D, UIBatch.CENTER, 0, 0);
        if (ui.roundButton("prev", 62, ny, 46, 0xFFFFE14D)) {
            carIdx = (carIdx + Cars.ALL.length - 1) % Cars.ALL.length;
            refreshCar(true);
            game.sfx.play(Sfx.WHOOSH, 0.6f, 1.2f);
        }
        ui.iconBack(62, ny + ui.lastRoundPress, 46, 0xFF2A1840);
        if (ui.roundButton("next", W - 62, ny, 46, 0xFFFFE14D)) {
            carIdx = (carIdx + 1) % Cars.ALL.length;
            refreshCar(true);
            game.sfx.play(Sfx.WHOOSH, 0.6f, 1.2f);
        }
        ui.iconPlay(W - 58, ny + ui.lastRoundPress, 46, 0xFF2A1840);

        // tabs
        float ty = ny + 75;
        float tw = (W - 60) / 4f;
        for (int i = 0; i < 4; i++) {
            float x = 30 + i * tw;
            boolean on = tab == i;
            if (ui.hit("tab" + i, x, ty, tw - 8, 76)) {
                if (tab != i) {
                    tab = i;
                    pendingKind = -1;
                    refreshCar(false);
                    ui.resetScroll("content");
                }
            }
            int col = on ? TAB_COLORS[i] : 0xFFFFFFFF;
            b.shape(x + (tw - 8) / 2, ty + 38 + (on ? 0 : 6), tw - 8, 76 - (on ? 0 : 6), 22, col, 0xFF2A1840, on ? 0 : 0, 0.3f, 0, 0);
            b.textFit(b.title, TABS[i], x + (tw - 8) / 2, ty + 40 + (on ? 0 : 3), 34f, tw - 24, on ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, on ? 0xFF2A1840 : 0, on ? 4f : 0);
        }

        // content panel
        float cy = ty + 90, ch = H - game.safeBottom - 24 - cy;
        b.shadow(24, cy + 8, W - 48, ch, 30, 0x40200040, 14);
        b.shape(W / 2, cy + ch / 2, W - 48, ch, 30, 0xFFFFFFFF, TAB_COLORS[tab], 6f, 0f, 0, 0);
        switch (tab) {
            case 0: tabCar(b, ui, cy, ch, W); break;
            case 1: tabUpgrade(b, ui, cy, ch, W); break;
            case 2: tabPaint(b, ui, cy, ch, W); break;
            default: tabStyle(b, ui, cy, ch, W); break;
        }

        if (toastT > 0) {
            float k = Ease.outBack(Math.min(1f, (1.6f - toastT) * 5f));
            b.alpha(Math.min(1f, toastT * 3f));
            b.textShadow(b.title, toast, W / 2, top + 480, 64f * k, toastColor, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x60200040);
            b.alpha(1f);
        }
    }

    // ------------------------------------------------------------------ tabs

    private void statBars(UIBatch b, float x, float y, float w, int car, boolean showUpg) {
        CarDef d = Cars.ALL[car];
        for (int i = 0; i < 4; i++) {
            float ry = y + i * 52;
            b.text(b.body, CarDef.STAT_NAME[i], x, ry, 28f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            float bx = x + 150, bw = w - 150;
            b.roundRect(bx, ry - 14, bw, 28, 14, 0xFFEDE6F6);
            float base = d.stats[i] / 8f;
            float lv = showUpg ? s().levels[car][i] * 0.6f / 8f : 0f;
            b.roundRect(bx, ry - 14, Math.max(28, bw * Math.min(1f, base + lv)), 28, 14, 0xFFFFC21F);
            b.roundRect(bx, ry - 14, Math.max(28, bw * base), 28, 14, TAB_COLORS[i % 4]);
        }
    }

    private void tabCar(UIBatch b, UI ui, float cy, float ch, float W) {
        Save s = s();
        CarDef d = Cars.ALL[carIdx];
        boolean hidden = secretHidden(carIdx);
        if (hidden) {
            ui.iconLock(W / 2, cy + 110, 90, 0xFF2A1840);
            b.text(b.title, "TOP SECRET", W / 2, cy + 210, 54f, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
            b.text(b.body, "Nobody knows how to get this one.", W / 2, cy + 270, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
            b.text(b.body, "Some say you have to be a real champion...", W / 2, cy + 310, 26f, 0xFFA898C0, UIBatch.CENTER, 0, 0);
            return;
        }
        b.text(b.body, d.blurb, W / 2, cy + 50, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
        statBars(b, 70, cy + 110, W - 140, carIdx, s.carOwned[carIdx]);
        float by = cy + Math.min(ch - 140, 340);
        if (s.carOwned[carIdx]) {
            if (s.selectedCar == carIdx) {
                ui.button("sel", W / 2 - 220, by, 440, 110, 0xFF34D058, "DRIVING!", 52f, false);
            } else if (ui.button("sel", W / 2 - 220, by, 440, 110, 0xFF34D058, "DRIVE THIS", 52f)) {
                s.selectedCar = carIdx;
                s.markDirty();
                s.flush();
                room.car.squashV = -8f;
                game.sfx.play(Sfx.HONK, 1f, 1f);
            }
        } else {
            boolean afford = s.coins >= d.price;
            if (ui.button("buy", W / 2 - 220, by, 440, 110, afford ? 0xFFFF9A2B : 0xFFB8B0C8, "BUY  " + d.price, 52f)) {
                if (s.spend(d.price)) {
                    s.carOwned[carIdx] = true;
                    s.selectedCar = carIdx;
                    s.markDirty();
                    s.flush();
                    room.celebrate();
                    game.sfx.play(Sfx.BUY, 1f, 1f);
                    showToast("NEW RIDE!", 0xFFFFE14D);
                } else {
                    game.sfx.play(Sfx.NOPE, 1f, 1f);
                    showToast("NEED MORE COINS", 0xFFFF4FA3);
                }
            }
        }
    }

    private boolean needOwned(UIBatch b, float cy, float W) {
        if (s().carOwned[carIdx]) return true;
        b.text(b.title, "BUY THIS CAR FIRST!", W / 2, cy + 120, 44f, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
        return false;
    }

    private void tabUpgrade(UIBatch b, UI ui, float cy, float ch, float W) {
        if (!needOwned(b, cy, W)) return;
        Save s = s();
        float rowH = Math.min(120f, (ch - 40) / 4f);
        for (int i = 0; i < 4; i++) {
            float y = cy + 30 + i * rowH + rowH / 2;
            int lv = s.levels[carIdx][i];
            b.text(b.title, CarDef.STAT_NAME[i], 60, y - 16, 38f, TAB_COLORS[i], UIBatch.LEFT, 0xFF2A1840, 3f);
            for (int k = 0; k < CarDef.MAX_LEVEL; k++) {
                float px = 66 + k * 46;
                b.shape(px + 18, y + 26, 38, 22, 11, k < lv ? 0xFFFFC21F : 0xFFEDE6F6, 0xFF2A1840, k < lv ? 3f : 0f, 0.3f, 0, 0);
            }
            if (lv >= CarDef.MAX_LEVEL) {
                b.text(b.title, "MAX!", W - 150, y, 44f, 0xFF34D058, UIBatch.CENTER, 0, 0);
            } else {
                int cost = Save.upgradeCost(lv);
                boolean afford = s.coins >= cost;
                if (ui.button("up" + i, W - 290, y - 44, 230, 88, afford ? 0xFF34D058 : 0xFFB8B0C8, "+ " + cost, 40f)) {
                    if (s.spend(cost)) {
                        s.levels[carIdx][i]++;
                        s.markDirty();
                        s.flush();
                        room.car.squashV = -9f;
                        room.fx.stars(0, 2f, 0, 8);
                        game.sfx.play(Sfx.BUY, 1f, 1f + lv * 0.08f);
                        showToast(CarDef.STAT_NAME[i] + " UP!", 0xFF5EE65A);
                    } else {
                        game.sfx.play(Sfx.NOPE, 1f, 1f);
                        showToast("NEED MORE COINS", 0xFFFF4FA3);
                    }
                }
            }
        }
    }

    private void segmented(UIBatch b, UI ui, float cx, float y, String a, String c, boolean second, String id) {
        float w = 220;
        if (ui.hit(id + "a", cx - w, y, w, 64) && second) {
            toggleSeg(id, false);
        }
        if (ui.hit(id + "b", cx, y, w, 64) && !second) {
            toggleSeg(id, true);
        }
        b.shape(cx, y + 32, w * 2 + 12, 76, 38, 0xFFEDE6F6, 0, 0, 0, 0, 0);
        float sx = second ? cx + w / 2 : cx - w / 2;
        b.shape(sx, y + 32, w, 64, 32, 0xFF8E62FF, 0, 0, 0.3f, 0, 0);
        b.text(b.title, a, cx - w / 2, y + 34, 32f, !second ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, 0, 0);
        b.text(b.title, c, cx + w / 2, y + 34, 32f, second ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, 0, 0);
    }

    private void toggleSeg(String id, boolean v) {
        if (id.equals("pm")) accentMode = v;
        else wheelMode = v;
        pendingKind = -1;
        refreshCar(false);
        game.ui.resetScroll("content");
    }

    private void tabPaint(UIBatch b, UI ui, float cy, float ch, float W) {
        if (!needOwned(b, cy, W)) return;
        Save s = s();
        segmented(b, ui, W / 2, cy + 24, "BODY", "ACCENT", accentMode, "pm");
        int n = Palette.PAINT.length;
        int cols = 6;
        float cell = (W - 100) / cols;
        float gy = cy + 110;
        float buyH = pendingKind >= 0 ? 120 : 0;
        float gh = ch - 120 - buyH;
        float content = (float) Math.ceil(n / (float) cols) * (cell + 40);
        float off = ui.beginScroll("content", 30, gy, W - 60, gh, content);
        int current = accentMode ? s.carAccent[carIdx] : s.carPaint[carIdx];
        for (int i = 0; i < n; i++) {
            float x = 50 + (i % cols) * cell + cell / 2;
            float y = gy + (i / cols) * (cell + 40) + cell / 2 + 10 - off;
            int rgb = Palette.paintColor(i, t);
            boolean owned = s.paintOwned[i];
            boolean sel = current == i;
            boolean prev = pendingKind == (accentMode ? 1 : 0) && pendingItem == i;
            float r = cell * 0.38f;
            if (sel || prev) b.circle(x, y, r + 9, prev ? 0xFFFFC21F : 0xFF2A1840);
            b.shape(x, y, r * 2, r * 2, r, 0xFF000000 | rgb, 0xFFFFFFFF, 4f, 0.5f, 0, 0);
            if (!owned) {
                b.circle(x, y, r, 0x50200040);
                ui.iconLock(x, y, r * 0.9f, 0xFFFFFFFF);
                b.text(b.body, String.valueOf(Palette.PAINT_PRICE[i]), x, y + r + 22, 22f, 0xFFE89A00, UIBatch.CENTER, 0, 0);
            } else {
                b.textFit(b.body, Palette.PAINT_NAME[i], x, y + r + 22, 20f, cell - 4, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
            }
            if (ui.hit("paint" + i, x - cell / 2, y - cell / 2, cell, cell) && y > gy && y < gy + gh) {
                pick(accentMode ? 1 : 0, i, owned);
            }
        }
        ui.endScroll();
        buyBar(b, ui, W, cy + ch - 125);
    }

    private void tabStyle(UIBatch b, UI ui, float cy, float ch, float W) {
        if (!needOwned(b, cy, W)) return;
        Save s = s();
        segmented(b, ui, W / 2, cy + 24, "TOPPER", "WHEELS", wheelMode, "sm");
        String[] names = wheelMode ? Palette.WHEEL_NAME : Palette.TOPPER_NAME;
        int[] prices = wheelMode ? Palette.WHEEL_PRICE : Palette.TOPPER_PRICE;
        boolean[] owned = wheelMode ? s.wheelOwned : s.topperOwned;
        int current = wheelMode ? s.carWheel[carIdx] : s.carTopper[carIdx];
        int kind = wheelMode ? 3 : 2;
        int cols = 3;
        float cw = (W - 100) / cols, chh = 110;
        float gy = cy + 110;
        float buyH = pendingKind >= 0 ? 120 : 0;
        float gh = ch - 120 - buyH;
        float content = (float) Math.ceil(names.length / (float) cols) * (chh + 14) + 10;
        float off = ui.beginScroll("content", 30, gy, W - 60, gh, content);
        for (int i = 0; i < names.length; i++) {
            float x = 50 + (i % cols) * cw;
            float y = gy + 6 + (i / cols) * (chh + 14) - off;
            boolean sel = current == i;
            boolean prev = pendingKind == kind && pendingItem == i;
            int fill = sel ? 0xFF8E62FF : (prev ? 0xFFFFC21F : 0xFFF3EEFA);
            b.shape(x + cw / 2 - 4, y + chh / 2, cw - 12, chh, 22, fill, 0xFF2A1840, sel || prev ? 4f : 0f, 0.25f, 0, 0);
            int tc = sel ? 0xFFFFFFFF : 0xFF2A1840;
            b.textFit(b.title, names[i], x + cw / 2 - 4, y + chh / 2 - (owned[i] ? 0 : 14), 30f, cw - 30, tc, UIBatch.CENTER, 0, 0);
            if (!owned[i]) {
                ui.coin(x + cw / 2 - 40, y + chh / 2 + 24, 12);
                b.text(b.body, String.valueOf(prices[i]), x + cw / 2 - 24, y + chh / 2 + 25, 24f, 0xFFE89A00, UIBatch.LEFT, 0, 0);
            }
            if (ui.hit("style" + kind + "_" + i, x, y, cw - 8, chh) && y + chh / 2 > gy && y + chh / 2 < gy + gh) {
                pick(kind, i, owned[i]);
            }
        }
        ui.endScroll();
        buyBar(b, ui, W, cy + ch - 125);
    }

    private void pick(int kind, int item, boolean owned) {
        Save s = s();
        if (owned) {
            if (kind == 0) s.carPaint[carIdx] = item;
            else if (kind == 1) s.carAccent[carIdx] = item;
            else if (kind == 2) s.carTopper[carIdx] = item;
            else s.carWheel[carIdx] = item;
            s.markDirty();
            s.flush();
            pendingKind = -1;
            refreshCar(false);
            room.car.squashV = -6f;
            game.sfx.play(Sfx.POP, 0.8f, 1f + (float) Math.random() * 0.3f);
        } else {
            pendingKind = kind;
            pendingItem = item;
            applyPreview();
            game.sfx.play(Sfx.POP, 0.6f, 1.3f);
        }
    }

    private void buyBar(UIBatch b, UI ui, float W, float y) {
        if (pendingKind < 0) return;
        Save s = s();
        int price;
        String name;
        if (pendingKind <= 1) {
            price = Palette.PAINT_PRICE[pendingItem];
            name = Palette.PAINT_NAME[pendingItem];
        } else if (pendingKind == 2) {
            price = Palette.TOPPER_PRICE[pendingItem];
            name = Palette.TOPPER_NAME[pendingItem];
        } else {
            price = Palette.WHEEL_PRICE[pendingItem];
            name = Palette.WHEEL_NAME[pendingItem];
        }
        b.textFit(b.title, name, 60, y + 50, 38f, 230, 0xFF2A1840, UIBatch.LEFT, 0, 0);
        boolean afford = s.coins >= price;
        if (ui.button("buyitem", W - 360, y + 4, 300, 96, afford ? 0xFFFF9A2B : 0xFFB8B0C8, "BUY " + price, 42f)) {
            if (s.spend(price)) {
                if (pendingKind <= 1) s.paintOwned[pendingItem] = true;
                else if (pendingKind == 2) s.topperOwned[pendingItem] = true;
                else s.wheelOwned[pendingItem] = true;
                int kind = pendingKind, item = pendingItem;
                pick(kind, item, true);
                room.celebrate();
                game.sfx.play(Sfx.BUY, 1f, 1f);
                showToast("SHINY!", 0xFFFFE14D);
            } else {
                game.sfx.play(Sfx.NOPE, 1f, 1f);
                showToast("NEED MORE COINS", 0xFFFF4FA3);
            }
        }
    }

    public boolean back() {
        game.setScreen(new TitleScreen(game));
        return true;
    }
}
