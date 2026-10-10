package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** The Rims shop: swap Rims for coins, browse premium items, learn how to earn Rims. No real payments yet. */
public final class ShopScreen extends Screen {
    private static final int[] PACK_RIMS = {10, 50, 120};
    private static final int[] PACK_COINS = {800, 4500, 12000};
    private static final String[] PACK_NAME = {"HANDFUL", "BUCKET", "TRUCKLOAD"};

    private final Screen back;
    private final int[] pickCat, pickIdx;
    private int confirm = -1;
    private float t;
    private String toast;
    private float toastT;
    private int toastColor;

    /** back: the screen to return to (null = title). */
    public ShopScreen(Game game, Screen back) {
        super(game);
        this.back = back;
        game.ui.resetScroll("shop");
        int n = 0;
        for (int cat = 0; cat < Items.CATEGORIES; cat++) {
            for (int i = 0; i < Items.count(cat); i++) if (Items.rule(cat, i) == Items.RIMS) n++;
        }
        pickCat = new int[n];
        pickIdx = new int[n];
        n = 0;
        for (int cat = 0; cat < Items.CATEGORIES; cat++) {
            for (int i = 0; i < Items.count(cat); i++) {
                if (Items.rule(cat, i) != Items.RIMS) continue;
                pickCat[n] = cat;
                pickIdx[n] = i;
                n++;
            }
        }
    }

    public void update(float dt) {
        t += dt;
        if (toastT > 0) toastT -= dt;
    }

    private void showToast(String msg, int color) {
        toast = msg;
        toastT = 1.6f;
        toastColor = color;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        Save s = game.save;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;
        b.rect(0, 0, W, H, 0x30200040);
        boolean wasEnabled = ui.enabled;
        if (confirm >= 0) ui.enabled = false;

        if (ui.roundButton("back", left + 40, top + 52, 38, 0xFFFFFFFF)) leave();
        ui.iconBack(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        b.textShadow(b.title, "RIMS SHOP", left + 100, top + 56, 56f, 0xFF8FE4FF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);
        float pr = W - right;
        float cw = TitleScreen.coinPill(game, pr, top + 52);
        TitleScreen.rimsPill(game, pr - cw - 14, top + 52);

        // ---- left column: balance + how to get Rims
        float px = left + 10, lw = Math.min(430f, W * 0.3f);
        float y = top + 110;
        b.shadow(px, y + 8, lw, 150, 28, 0x50200040, 14);
        b.shape(px + lw / 2, y + 75, lw, 150, 28, 0xFF2A1840, 0xFF8FE4FF, 5f, 0, 0, 0);
        float pop = 1f + (float) Math.sin(t * 3f) * 0.04f;
        ui.rim(px + 78, y + 75, 50 * pop);
        b.text(b.body, "YOUR RIMS", px + 148, y + 44, 26f, 0xFFB8A8D8, UIBatch.LEFT, 0, 0);
        b.textFit(b.title, num(s.rims), px + 148, y + 98, 64f, lw - 170, 0xFF8FE4FF, UIBatch.LEFT, 0, 0);

        y += 172;
        float gh = Math.max(250f, bottom - 16 - y);
        ui.panel(px, y, lw, gh, 0xFFFFFFFF);
        b.textFit(b.title, "GET MORE RIMS", px + lw / 2, y + 42, 36f, lw - 40, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
        b.textFit(b.body, "Rims packs are coming", px + lw / 2, y + 86, 26f, lw - 40, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        b.textFit(b.body, "to the store soon!", px + lw / 2, y + 116, 26f, lw - 40, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        b.roundRect(px + 28, y + 142, lw - 56, 3, 1.5f, 0x202A1840);
        b.textFit(b.title, "FREE RIMS", px + 30, y + 172, 28f, lw - 60, 0xFF3BA8FF, UIBatch.LEFT, 0, 0);
        float ry = y + 218;
        TitleScreen.levelSeal(game, px + 54, ry, 22, 5, 0xFFFFC21F);
        b.textFit(b.body, "Every 5 levels: +5, +10 or +25", px + 92, ry, 24f, lw - 110, 0xFF2A1840, UIBatch.LEFT, 0, 0);
        ry += 56;
        ui.medal(px + 54, ry - 4, 18, 0xFFFFB321);
        b.textFit(b.body, "Big achievements pay Rims too", px + 92, ry, 24f, lw - 110, 0xFF2A1840, UIBatch.LEFT, 0, 0);
        int next = nextRimsLevel(s.level());
        if (next > 0 && ry + 50 < y + gh - 10) {
            ry += 50;
            b.textFit(b.body, "Next: LEVEL " + next + " gives +" + Levels.rimsAt(next), px + lw / 2, ry, 24f, lw - 40, 0xFF8E62FF, UIBatch.CENTER, 0, 0);
        }

        // ---- right column: coin packs + premium picks (scrolls on short screens)
        float rx = px + lw + 28, rw = W - right - rx;
        float y0 = top + 100, viewH = bottom - 8 - y0;
        int cols = rw > 900 ? 3 : 2;
        int rows = (pickCat.length + cols - 1) / cols;
        float content = 60 + 248 + 70 + rows * 100 + 20;
        float off = ui.beginScroll("shop", rx - 10, y0, rw + 20, viewH, content);
        y = y0 - off;
        b.textShadow(b.title, "SWAP RIMS FOR COINS", rx + 4, y + 30, 34f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 4f, 4f, 0x50200040);
        y += 60;
        float cardW = (rw - 40) / 3f;
        for (int i = 0; i < 3; i++) {
            float x = rx + i * (cardW + 20);
            ui.panel(x, y, cardW, 236, 0xFFFFFFFF);
            b.textFit(b.body, PACK_NAME[i], x + cardW / 2, y + 22, 22f, cardW - 30, 0xFFA898C0, UIBatch.CENTER, 0, 0);
            coinPile(ui, x + cardW / 2, y + 92, i, t);
            b.textFit(b.title, num(PACK_COINS[i]) + " COINS", x + cardW / 2, y + 140, 38f, cardW - 30, 0xFFFFC21F, UIBatch.CENTER, 0xFF2A1840, 4f);
            boolean afford = s.rims >= PACK_RIMS[i];
            if (ui.iconButton("pack" + i, x + 20, y + 158, cardW - 40, 70, afford ? 0xFF3BB8FF : 0xFFB8B0C8, null, UI.ICON_RIM, String.valueOf(PACK_RIMS[i]), 38f, true)) {
                if (afford) {
                    confirm = i;
                } else {
                    game.sfx.play(Sfx.NOPE, 1f, 1f);
                    showToast("NEED MORE RIMS", 0xFFFF4FA3);
                }
            }
        }
        y += 248;
        b.textShadow(b.title, "PREMIUM PICKS", rx + 4, y + 36, 34f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 4f, 4f, 0x50200040);
        y += 70;
        float pw = (rw - (cols - 1) * 16) / cols;
        for (int k = 0; k < pickCat.length; k++) {
            float x = rx + (k % cols) * (pw + 16), yy = y + (k / cols) * 100;
            int cat = pickCat[k], idx = pickIdx[k];
            String id = "pick" + k;
            boolean visible = yy + 88 > y0 && yy < y0 + viewH;
            if (ui.hit(id, x, yy, pw, 88) && visible) open(cat, idx);
            float sc = 1f + (float) Math.sin(ui.bounce(id) * Math.PI) * 0.05f;
            boolean owned = Items.owned(s, cat, idx);
            b.shadow(x, yy + 6, pw, 88, 22, 0x40200040, 8);
            b.shape(x + pw / 2, yy + 44, pw * sc, 88 * sc, 22, 0xFFFFFFFF, owned ? 0xFF34D058 : 0xFF8FE4FF, 4f, 0.15f, 0, 0);
            b.circle(x + 50, yy + 44, 34, 0xFFEFE8FA);
            LockerScreen.itemIcon(game, cat, idx, x + 50, yy + 44, 54, t);
            float tw = pw - 100 - 96;
            b.textFit(b.title, Items.name(cat, idx), x + 96, yy + 32, 28f, tw + 40, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            b.textFit(b.body, Items.CAT_NAME[cat], x + 96, yy + 64, 21f, tw, 0xFFA898C0, UIBatch.LEFT, 0, 0);
            if (owned) {
                b.circle(x + pw - 36, yy + 60, 18, 0xFF34D058);
                ui.iconCheck(x + pw - 36, yy + 60, 22, 0xFFFFFFFF);
            } else {
                ui.iconLabel(x + pw - 52, yy + 62, 90, null, UI.ICON_RIM, String.valueOf(Items.value(cat, idx)), 26f, 0xFF2A9FD8, 0, 0);
            }
        }
        ui.endScroll();
        ui.enabled = wasEnabled;

        if (confirm >= 0) confirmPanel(b, ui, W, H);

        if (toastT > 0) {
            float k = Ease.outBack(Math.min(1f, (1.6f - toastT) * 5f));
            b.alpha(Math.min(1f, toastT * 3f));
            b.textShadow(b.title, toast, W / 2, H * 0.5f, 72f * k, toastColor, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x60200040);
            b.alpha(1f);
        }
    }

    private void confirmPanel(UIBatch b, UI ui, float W, float H) {
        Save s = game.save;
        b.rect(0, 0, W, H, 0xA0200040);
        ui.block(0, 0, W, H);
        float pw = 660, ph = 400, px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "SWAP?", W / 2, py + 66, 62f, 0xFF8E62FF, UIBatch.CENTER, 0xFF2A1840, 4f);
        float ry = py + 160;
        ui.iconLabel(W / 2 - 150, ry, 240, null, UI.ICON_RIM, String.valueOf(PACK_RIMS[confirm]), 52f, 0xFF2A9FD8, 0xFF2A1840, 0);
        ui.iconPlay(W / 2, ry, 44, 0xFFA898C0);
        ui.iconLabel(W / 2 + 160, ry, 260, null, UI.ICON_COIN, num(PACK_COINS[confirm]), 52f, 0xFFE89A00, 0xFF2A1840, 0);
        b.text(b.body, "You'll have " + num(s.rims - PACK_RIMS[confirm]) + " Rims left.", W / 2, py + 232, 28f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
        if (ui.button("cno", px + 50, py + ph - 140, 250, 100, 0xFFB8B0C8, "NO", 48f)) confirm = -1;
        if (ui.button("cyes", px + pw - 300, py + ph - 140, 250, 100, 0xFF34D058, "SWAP!", 48f)) {
            if (s.spendRims(PACK_RIMS[confirm])) {
                s.addCoins(PACK_COINS[confirm]);
                s.flush();
                game.sfx.play(Sfx.BUY, 1f, 1f);
                game.sfx.play(Sfx.COIN, 0.8f, 1.2f);
                showToast("+" + num(PACK_COINS[confirm]) + " COINS!", 0xFFFFE14D);
            } else {
                game.sfx.play(Sfx.NOPE, 1f, 1f);
                showToast("NEED MORE RIMS", 0xFFFF4FA3);
            }
            confirm = -1;
        }
    }

    /** Bigger packs get bigger piles. */
    private static void coinPile(UI ui, float cx, float cy, int size, float t) {
        int n = 2 + size * 2;
        for (int i = 0; i < n; i++) {
            float a = i * 2.4f;
            float rr = 10 + i * 3.2f;
            float x = cx + (float) Math.cos(a) * rr * 0.9f;
            float y = cy + 6 - i * 3f + (float) Math.sin(a) * rr * 0.3f;
            ui.coin(x, y, 18 + size * 2);
        }
        float bob = (float) Math.sin(t * 3f + size) * 3f;
        ui.coin(cx, cy - 10 - size * 4 + bob, 22 + size * 2);
    }

    private static int nextRimsLevel(int level) {
        for (int l = level + 1; l <= Levels.MAX; l++) if (Levels.rimsAt(l) > 0) return l;
        return 0;
    }

    /** Jumps to where the item can be bought: the garage for car stuff, the locker for the rest. */
    private void open(int cat, int idx) {
        if (cat <= Items.WHEEL) game.setScreen(new GarageScreen(game).focus(cat, idx));
        else game.setScreen(new LockerScreen(game, cat - Items.MAP).focus(idx));
    }

    private void leave() {
        game.setScreen(back != null ? back : new TitleScreen(game));
    }

    /** 12000 -> "12,000". */
    static String num(int n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        if (n < 0) sb.append('-');
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append(',');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    public boolean back() {
        if (confirm >= 0) {
            confirm = -1;
            return true;
        }
        leave();
        return true;
    }
}
