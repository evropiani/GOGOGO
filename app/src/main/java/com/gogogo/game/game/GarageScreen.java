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

    /** Opens on an item: a car on the CAR tab, or a paint / topper / wheel previewed on the driven car. */
    public GarageScreen focus(int cat, int idx) {
        Save s = s();
        if (cat == Items.CAR) {
            carIdx = idx;
            tab = 0;
            refreshCar(false);
            return this;
        }
        tab = cat == Items.PAINT ? 2 : 3;
        accentMode = false;
        wheelMode = cat == Items.WHEEL;
        if (!Items.owned(s, cat, idx)) {
            pendingKind = cat == Items.PAINT ? 0 : (cat == Items.TOPPER ? 2 : 3);
            pendingItem = idx;
            applyPreview();
        }
        return this;
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
        room.screenX = 0.23f;
        room.screenY = 0.47f;
        room.distance = 15f;
        room.render();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;

        if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) game.setScreen(new TitleScreen(game));
        ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        b.textShadow(b.title, "GARAGE", left + 100, top + 56, 56f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);
        float cw = TitleScreen.coinPill(game, W - right, top + 52);
        TitleScreen.rimsPill(game, W - right - cw - 14, top + 52);

        // car selector under the showroom (left half)
        float lw = W * 0.46f;
        float lcx = left + (lw - left) / 2f;
        CarDef d = Cars.ALL[carIdx];
        float ny = bottom - 80;
        boolean hidden = secretHidden(carIdx);
        String name = hidden ? "???" : d.name;
        float nw = Math.min(520f, lw - left - 170);
        b.shadow(lcx - nw / 2, ny - 46 + 8, nw, 92, 46, 0x50200040, 10);
        b.shape(lcx, ny, nw, 92, 46, 0xFF2A1840, 0, 0, 0, 0, 0);
        b.textFit(b.title, name, lcx, ny - 8, 46f, nw - 50, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        if (s().carOwned[carIdx] || hidden) {
            String sub = s().carOwned[carIdx] ? (s().selectedCar == carIdx ? "DRIVING THIS ONE" : "OWNED") : "SECRET";
            b.text(b.body, sub, lcx, ny + 28, 22f, s().carOwned[carIdx] ? 0xFF5EE65A : 0xFFFFE14D, UIBatch.CENTER, 0, 0);
        } else {
            int rule = Items.rule(Items.CAR, carIdx), v = Items.value(Items.CAR, carIdx);
            if (rule == Items.LEVEL) ui.iconLabel(lcx, ny + 28, nw - 60, null, UI.ICON_LOCK, "LEVEL " + v, 22f, 0xFFC9B4FF, 0, 0);
            else if (rule == Items.RIMS) ui.iconLabel(lcx, ny + 28, nw - 60, null, UI.ICON_RIM, v + " RIMS", 22f, 0xFF8FE4FF, 0, 0);
            else ui.iconLabel(lcx, ny + 28, nw - 60, null, UI.ICON_COIN, ShopScreen.num(v) + " COINS", 22f, 0xFFFFE14D, 0, 0);
        }
        if (!hidden) carTag(b, ui, lcx, ny - 68);
        float ax = nw / 2 + 50;
        if (ui.roundButton("prev", lcx - ax, ny, 42, 0xFFFFE14D)) {
            carIdx = (carIdx + Cars.ALL.length - 1) % Cars.ALL.length;
            refreshCar(true);
            game.sfx.play(Sfx.WHOOSH, 0.6f, 1.2f);
        }
        ui.iconBack(lcx - ax, ny + ui.lastRoundPress, 42, 0xFF2A1840);
        if (ui.roundButton("next", lcx + ax, ny, 42, 0xFFFFE14D)) {
            carIdx = (carIdx + 1) % Cars.ALL.length;
            refreshCar(true);
            game.sfx.play(Sfx.WHOOSH, 0.6f, 1.2f);
        }
        ui.iconPlay(lcx + ax + 4, ny + ui.lastRoundPress, 42, 0xFF2A1840);

        // tabs + content (right half)
        float px = lw + 10, pw = W - right - px;
        float ty = top + 100;
        float tw = pw / 4f;
        for (int i = 0; i < 4; i++) {
            float x = px + i * tw;
            boolean on = tab == i;
            if (ui.hit("tab" + i, x, ty, tw - 8, 70)) {
                if (tab != i) {
                    tab = i;
                    pendingKind = -1;
                    refreshCar(false);
                    ui.resetScroll("content");
                }
            }
            int col = on ? TAB_COLORS[i] : 0xFFFFFFFF;
            b.shape(x + (tw - 8) / 2, ty + 35 + (on ? 0 : 5), tw - 8, 70 - (on ? 0 : 5), 22, col, 0, 0, 0.3f, 0, 0);
            b.textFit(b.title, TABS[i], x + (tw - 8) / 2, ty + 37 + (on ? 0 : 3), 32f, tw - 24, on ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, on ? 0xFF2A1840 : 0, on ? 4f : 0);
        }
        float cy = ty + 84, ch = bottom - 20 - cy;
        b.shadow(px, cy + 8, pw, ch, 30, 0x40200040, 14);
        b.shape(px + pw / 2, cy + ch / 2, pw, ch, 30, 0xFFFFFFFF, TAB_COLORS[tab], 6f, 0f, 0, 0);
        switch (tab) {
            case 0: tabCar(b, ui, px, pw, cy, ch); break;
            case 1: tabUpgrade(b, ui, px, pw, cy, ch); break;
            case 2: tabPaint(b, ui, px, pw, cy, ch); break;
            default: tabStyle(b, ui, px, pw, cy, ch); break;
        }

        if (toastT > 0) {
            float k = Ease.outBack(Math.min(1f, (1.6f - toastT) * 5f));
            b.alpha(Math.min(1f, toastT * 3f));
            b.textShadow(b.title, toast, lcx, H * 0.3f, 60f * k, toastColor, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x60200040);
            b.alpha(1f);
        }
    }

    // ------------------------------------------------------------------ tabs (px/pw = panel left/width)

    private void statBars(UIBatch b, float x, float y, float w, int car, boolean showUpg) {
        CarDef d = Cars.ALL[car];
        for (int i = 0; i < 4; i++) {
            float ry = y + i * 50;
            b.text(b.body, CarDef.STAT_NAME[i], x, ry, 28f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            float bx = x + 150, bw = w - 150;
            b.roundRect(bx, ry - 14, bw, 28, 14, 0xFFEDE6F6);
            float base = d.stats[i] / 8f;
            float lv = showUpg ? s().levels[car][i] * 0.6f / 8f : 0f;
            b.roundRect(bx, ry - 14, Math.max(28, bw * Math.min(1f, base + lv)), 28, 14, 0xFFFFC21F);
            b.roundRect(bx, ry - 14, Math.max(28, bw * base), 28, 14, TAB_COLORS[i % 4]);
        }
    }

    private void tabCar(UIBatch b, UI ui, float px, float pw, float cy, float ch) {
        Save s = s();
        CarDef d = Cars.ALL[carIdx];
        float cx = px + pw / 2;
        if (secretHidden(carIdx)) {
            ui.iconLock(cx, cy + 100, 84, 0xFF2A1840);
            b.text(b.title, "TOP SECRET", cx, cy + 195, 52f, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
            b.text(b.body, "Nobody knows how to get this one.", cx, cy + 255, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
            b.text(b.body, "Some say you have to be a real champion...", cx, cy + 295, 26f, 0xFFA898C0, UIBatch.CENTER, 0, 0);
            return;
        }
        b.textFit(b.body, d.blurb, cx, cy + 44, 30f, pw - 60, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
        statBars(b, px + 50, cy + 100, pw - 100, carIdx, s.carOwned[carIdx]);
        float by = cy + Math.min(ch - 125, 320);
        float bw = Math.min(440f, pw - 80);
        if (s.carOwned[carIdx]) {
            if (s.selectedCar == carIdx) {
                ui.button("sel", cx - bw / 2, by, bw, 104, 0xFF34D058, "DRIVING!", 50f, false);
            } else if (ui.button("sel", cx - bw / 2, by, bw, 104, 0xFF34D058, "DRIVE THIS", 50f)) {
                s.selectedCar = carIdx;
                s.markDirty();
                s.flush();
                room.car.squashV = -8f;
                game.sfx.play(Sfx.HONK, 1f, 1f);
            }
        } else if (Items.rule(Items.CAR, carIdx) == Items.LEVEL) {
            int lv = s.level();
            b.textFit(b.body, "Reach level " + d.levelReq + " to unlock it. You are level " + lv + ".", cx, by - 24, 26f, pw - 60, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
            ui.iconButton("buy", cx - bw / 2, by, bw, 104, 0xFFB8B0C8, null, UI.ICON_LOCK, "LEVEL " + d.levelReq, 50f, false);
        } else {
            boolean afford = Items.affordable(s, Items.CAR, carIdx);
            boolean premium = Items.rule(Items.CAR, carIdx) == Items.RIMS;
            if (ui.iconButton("buy", cx - bw / 2, by, bw, 104, afford ? (premium ? 0xFF3BB8FF : 0xFFFF9A2B) : 0xFFB8B0C8,
                    "BUY", premium ? UI.ICON_RIM : UI.ICON_COIN, ShopScreen.num(premium ? d.rimsPrice : d.price), 50f, true)) {
                if (Items.buy(s, Items.CAR, carIdx)) {
                    s.selectedCar = carIdx;
                    s.markDirty();
                    s.flush();
                    room.celebrate();
                    game.sfx.play(Sfx.BUY, 1f, 1f);
                    showToast("NEW RIDE!", 0xFFFFE14D);
                } else {
                    game.sfx.play(Sfx.NOPE, 1f, 1f);
                    showToast(premium ? "NEED MORE RIMS" : "NEED MORE COINS", 0xFFFF4FA3);
                }
            }
        }
    }

    /** Ribbon over the name for the special cars: LEGENDARY for the level 100 car, PREMIUM for Rims cars. */
    private void carTag(UIBatch b, UI ui, float cx, float cy) {
        int rule = Items.rule(Items.CAR, carIdx);
        String tag;
        int col;
        if (carIdx == Cars.TITAN) {
            tag = "LEGENDARY";
            col = 0xFFFFB321;
        } else if (rule == Items.RIMS) {
            tag = "PREMIUM";
            col = 0xFF3BB8FF;
        } else if (rule == Items.LEVEL) {
            tag = "LEVEL REWARD";
            col = 0xFF8E62FF;
        } else {
            return;
        }
        float w = b.title.width(tag, 30f) + 70;
        float wob = (float) Math.sin(t * 3f) * 0.03f;
        b.shape(cx, cy + 5, w, 50, 25, 0x50200040, 0, 0, 0, 6, 0);
        b.shape(cx, cy, w, 50, 25, col, 0xFFFFFFFF, 4f, 0.4f, 0, wob);
        if (carIdx == Cars.TITAN) {
            ui.star(cx - w / 2 + 24, cy, 13, 0xFFFFFFFF);
            ui.star(cx + w / 2 - 24, cy, 13, 0xFFFFFFFF);
        }
        b.text(b.title, tag, cx, cy + 2, 30f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);
    }

    private boolean needOwned(UIBatch b, float px, float pw, float cy) {
        if (s().carOwned[carIdx]) return true;
        b.text(b.title, "BUY THIS CAR FIRST!", px + pw / 2, cy + 120, 44f, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
        return false;
    }

    private void tabUpgrade(UIBatch b, UI ui, float px, float pw, float cy, float ch) {
        if (!needOwned(b, px, pw, cy)) return;
        Save s = s();
        float rowH = Math.min(115f, (ch - 30) / 4f);
        for (int i = 0; i < 4; i++) {
            float y = cy + 18 + i * rowH + rowH / 2;
            int lv = s.levels[carIdx][i];
            b.text(b.title, CarDef.STAT_NAME[i], px + 36, y - 16, 36f, TAB_COLORS[i], UIBatch.LEFT, 0xFF2A1840, 3f);
            for (int k = 0; k < CarDef.MAX_LEVEL; k++) {
                float x = px + 42 + k * 46;
                b.shape(x + 18, y + 24, 38, 22, 11, k < lv ? 0xFFFFC21F : 0xFFEDE6F6, 0xFF2A1840, k < lv ? 3f : 0f, 0.3f, 0, 0);
            }
            float bx = px + pw - 270;
            if (lv >= CarDef.MAX_LEVEL) {
                b.text(b.title, "MAX!", bx + 115, y, 44f, 0xFF34D058, UIBatch.CENTER, 0, 0);
            } else {
                int cost = Save.upgradeCost(lv);
                boolean afford = s.coins >= cost;
                if (ui.button("up" + i, bx, y - 40, 230, 82, afford ? 0xFF34D058 : 0xFFB8B0C8, "+ " + cost, 38f)) {
                    if (s.spend(cost)) {
                        s.levels[carIdx][i]++;
                        s.markDirty();
                        Progress.refresh(s);
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
        float w = 200;
        if (ui.hit(id + "a", cx - w, y, w, 60) && second) toggleSeg(id, false);
        if (ui.hit(id + "b", cx, y, w, 60) && !second) toggleSeg(id, true);
        b.shape(cx, y + 30, w * 2 + 12, 72, 36, 0xFFEDE6F6, 0, 0, 0, 0, 0);
        float sx = second ? cx + w / 2 : cx - w / 2;
        b.shape(sx, y + 30, w, 60, 30, 0xFF8E62FF, 0, 0, 0.3f, 0, 0);
        b.text(b.title, a, cx - w / 2, y + 32, 30f, !second ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, 0, 0);
        b.text(b.title, c, cx + w / 2, y + 32, 30f, second ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, 0, 0);
    }

    private void toggleSeg(String id, boolean v) {
        if (id.equals("pm")) accentMode = v;
        else wheelMode = v;
        pendingKind = -1;
        refreshCar(false);
        game.ui.resetScroll("content");
    }

    private void tabPaint(UIBatch b, UI ui, float px, float pw, float cy, float ch) {
        if (!needOwned(b, px, pw, cy)) return;
        Save s = s();
        segmented(b, ui, px + pw / 2, cy + 18, "BODY", "ACCENT", accentMode, "pm");
        int n = Palette.PAINT.length;
        int cols = pw > 720 ? 9 : 6;
        float cell = (pw - 50) / cols;
        float gy = cy + 96;
        float buyH = pendingKind >= 0 ? 110 : 0;
        float gh = ch - 104 - buyH;
        float rowH = cell * 0.8f + 34;
        float content = (float) Math.ceil(n / (float) cols) * rowH + 10;
        float off = ui.beginScroll("content", px + 10, gy, pw - 20, gh, content);
        int current = accentMode ? s.carAccent[carIdx] : s.carPaint[carIdx];
        for (int i = 0; i < n; i++) {
            float x = px + 25 + (i % cols) * cell + cell / 2;
            float y = gy + (i / cols) * rowH + cell * 0.4f + 8 - off;
            int rgb = Palette.paintColor(i, t);
            boolean owned = s.paintOwned[i];
            boolean sel = current == i;
            boolean prev = pendingKind == (accentMode ? 1 : 0) && pendingItem == i;
            float r = Math.min(cell * 0.36f, 38f);
            if (sel || prev) b.circle(x, y, r + 8, prev ? 0xFFFFC21F : 0xFF2A1840);
            b.shape(x, y, r * 2, r * 2, r, 0xFF000000 | rgb, 0xFFFFFFFF, 4f, 0.5f, 0, 0);
            if (!owned) {
                b.circle(x, y, r, 0x50200040);
                ui.iconLock(x, y, r * 0.9f, 0xFFFFFFFF);
                LockerScreen.priceTag(ui, Items.PAINT, i, x, y + r + 20, 20f, cell - 6);
            } else {
                b.textFit(b.body, Palette.PAINT_NAME[i], x, y + r + 20, 19f, cell - 4, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
            }
            if (ui.hit("paint" + i, x - cell / 2, y - r - 6, cell, rowH) && y > gy && y < gy + gh) {
                pick(accentMode ? 1 : 0, i, owned);
            }
        }
        ui.endScroll();
        buyBar(b, ui, px, pw, cy + ch - 112);
    }

    private void tabStyle(UIBatch b, UI ui, float px, float pw, float cy, float ch) {
        if (!needOwned(b, px, pw, cy)) return;
        Save s = s();
        segmented(b, ui, px + pw / 2, cy + 18, "TOPPER", "WHEELS", wheelMode, "sm");
        String[] names = wheelMode ? Palette.WHEEL_NAME : Palette.TOPPER_NAME;
        boolean[] owned = wheelMode ? s.wheelOwned : s.topperOwned;
        int current = wheelMode ? s.carWheel[carIdx] : s.carTopper[carIdx];
        int kind = wheelMode ? 3 : 2;
        int cols = pw > 720 ? 4 : 3;
        float cw = (pw - 50) / cols, chh = 96;
        float gy = cy + 96;
        float buyH = pendingKind >= 0 ? 110 : 0;
        float gh = ch - 104 - buyH;
        float content = (float) Math.ceil(names.length / (float) cols) * (chh + 12) + 10;
        float off = ui.beginScroll("content", px + 10, gy, pw - 20, gh, content);
        for (int i = 0; i < names.length; i++) {
            float x = px + 25 + (i % cols) * cw;
            float y = gy + 6 + (i / cols) * (chh + 12) - off;
            boolean sel = current == i;
            boolean prev = pendingKind == kind && pendingItem == i;
            int fill = sel ? 0xFF8E62FF : (prev ? 0xFFFFC21F : 0xFFF3EEFA);
            b.shape(x + cw / 2 - 4, y + chh / 2, cw - 12, chh, 22, fill, 0xFF2A1840, sel || prev ? 4f : 0f, 0.25f, 0, 0);
            int tc = sel ? 0xFFFFFFFF : 0xFF2A1840;
            b.textFit(b.title, names[i], x + cw / 2 - 4, y + chh / 2 - (owned[i] ? 0 : 13), 28f, cw - 30, tc, UIBatch.CENTER, 0, 0);
            if (!owned[i]) {
                int cat = wheelMode ? Items.WHEEL : Items.TOPPER;
                LockerScreen.priceTag(ui, cat, i, x + cw / 2 - 4, y + chh / 2 + 23, 22f, cw - 30);
            }
            if (ui.hit("style" + kind + "_" + i, x, y, cw - 8, chh) && y + chh / 2 > gy && y + chh / 2 < gy + gh) {
                pick(kind, i, owned[i]);
            }
        }
        ui.endScroll();
        buyBar(b, ui, px, pw, cy + ch - 112);
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

    private void buyBar(UIBatch b, UI ui, float px, float pw, float y) {
        if (pendingKind < 0) return;
        Save s = s();
        int cat = pendingKind <= 1 ? Items.PAINT : (pendingKind == 2 ? Items.TOPPER : Items.WHEEL);
        int rule = Items.rule(cat, pendingItem), price = Items.value(cat, pendingItem);
        String name = Items.name(cat, pendingItem);
        b.textFit(b.title, name, px + 36, y + 50, 36f, pw - 380, 0xFF2A1840, UIBatch.LEFT, 0, 0);
        if (rule == Items.LEVEL) {
            ui.iconButton("buyitem", px + pw - 330, y + 6, 290, 90, 0xFFB8B0C8, null, UI.ICON_LOCK, "LEVEL " + price, 40f, false);
            return;
        }
        boolean premium = rule == Items.RIMS;
        boolean afford = Items.affordable(s, cat, pendingItem);
        if (ui.iconButton("buyitem", px + pw - 330, y + 6, 290, 90, afford ? (premium ? 0xFF3BB8FF : 0xFFFF9A2B) : 0xFFB8B0C8,
                "BUY", premium ? UI.ICON_RIM : UI.ICON_COIN, ShopScreen.num(price), 40f, true)) {
            if (Items.buy(s, cat, pendingItem)) {
                int kind = pendingKind, item = pendingItem;
                pick(kind, item, true);
                room.celebrate();
                game.sfx.play(Sfx.BUY, 1f, 1f);
                showToast("SHINY!", 0xFFFFE14D);
            } else {
                game.sfx.play(Sfx.NOPE, 1f, 1f);
                showToast(premium ? "NEED MORE RIMS" : "NEED MORE COINS", 0xFFFF4FA3);
            }
        }
    }

    public boolean back() {
        game.setScreen(new TitleScreen(game));
        return true;
    }
}
