package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/**
 * Dress up the driver: hats, hair, faces, skin tones, shirts, pants, shoes and things to hold, and the emotes it
 * plays on the results screen. The driver stands next to the selected car on the pedestal; tap an item to try it on
 * (an emote to watch it), then buy or wear it. RANDOMIZE throws a random look together from what you own.
 */
public final class DriverScreen extends Screen {
    private static final int[] TAB_COLORS = {
            0xFFFF4FA3, 0xFFFF9A2B, 0xFFFFB321, 0xFF34D058, 0xFF3BA8FF, 0xFF8E62FF, 0xFFFF5C7A, 0xFF2EBFB0, 0xFFFF6A3A};
    /** The tabs: one per wardrobe part, then the emotes (card 0 is the car's own emote, then the victory emotes). */
    private static final int EMOTES = Outfits.PARTS, TABS = Outfits.PARTS + 1;
    private static final String[] TAB_NAME = {"HAT", "HAIR", "FACE", "SKIN", "SHIRT", "PANTS", "SHOES", "HANDS", "EMOTES"};

    private final Showroom room;
    private final Driver driver = new Driver();
    private final int[] look = new int[Outfits.PARTS];
    private int tab;
    /** Item tried on in the current tab. */
    private int sel;
    private float t, cheerT;
    private String toast;
    private float toastT;
    private int toastColor;
    /** Emotes tab: seconds until the emote being looked at plays again. */
    private float replayT;
    /** Card to scroll the grid to when it is drawn next (focus()), -1 = none. */
    private int scrollTo = -1;
    private final String[] cardId = new String[1 + Emotes.COUNT], tabId = new String[TABS];

    public DriverScreen(Game game) {
        super(game);
        Save s = game.save;
        room = new Showroom(game);
        room.showSaved(s.selectedCar);
        room.stand = driver;
        room.spinSpeed = 0f;
        // the car turned three quarters to the camera, the driver in front of it on the right
        room.carX = -1.15f;
        room.carZ = -0.55f;
        room.carYaw = 0.75f;
        room.standX = 1.65f;
        room.standZ = 1.05f;
        room.standYaw = -0.25f;
        room.standScale = 1.15f;
        driver.mode = Driver.STANDING;
        sel = s.look[tab];
        applyPreview();
        driver.reset();
        driver.mode = Driver.STANDING;
        game.ui.resetScroll("wardrobe");
        for (int i = 0; i < cardId.length; i++) cardId[i] = "drv" + i;
        for (int i = 0; i < TABS; i++) tabId[i] = "dtab" + i;
        if ((s.seen & Save.SEEN_DRIVER) == 0) {
            s.seen |= Save.SEEN_DRIVER;
            s.markDirty();
            s.flush();
        }
    }

    /** Opens on an item (a wardrobe category of Items, or a victory emote) to try on. */
    public DriverScreen focus(int cat, int idx) {
        setTab(Math.max(0, Math.min(TABS - 1, cat - Items.HAT)));
        sel = tab == EMOTES ? idx + 1 : idx;
        scrollTo = sel;
        applyPreview();
        return this;
    }

    /** Items category of the tab (the emotes tab: Items.EMOTE, whose item i is card i + 1). */
    private int cat() {
        return Items.HAT + tab;
    }

    private void setTab(int i) {
        tab = i;
        if (tab == EMOTES) {
            sel = 0;
            Save s = game.save;
            if ((s.seen & Save.SEEN_EMOTES) == 0) {
                s.seen |= Save.SEEN_EMOTES;
                s.markDirty();
                s.flush();
            }
        } else {
            sel = game.save.look[tab];
        }
        applyPreview();
        game.ui.resetScroll("wardrobe");
    }

    /** The saved look with the item being tried on swapped in; on the emotes tab the emote picked plays. */
    private void applyPreview() {
        System.arraycopy(game.save.look, 0, look, 0, Outfits.PARTS);
        if (tab == EMOTES) {
            driver.setLook(look);
            playPreview();
            return;
        }
        driver.stopEmote();
        look[tab] = sel;
        driver.setLook(look);
    }

    /** The emote of the card picked on the emotes tab. */
    private int previewMove() {
        return sel == 0 ? Emotes.signature(game.save.selectedCar) : Emotes.VICTORY[sel - 1];
    }

    private void playPreview() {
        int m = previewMove();
        cheerT = 0f;
        driver.aim = 0.25f;
        driver.play(m);
        replayT = Emotes.DUR[m] + 1.4f;
        game.sfx.play(Emotes.SOUND[m], 0.7f * Emotes.VOLUME[m], Emotes.PITCH[m]);
    }

    private void showToast(String msg, int color) {
        toast = msg;
        toastT = 1.6f;
        toastColor = color;
    }

    /** Arms up for a moment (bought, equipped, randomized). */
    private void cheer() {
        driver.stopEmote();
        cheerT = 0.9f;
        driver.kick(Driver.STRETCH, 2f);
        room.fx.confetti(room.standX, 2.4f, room.standZ, 40, 0.9f);
    }

    public void update(float dt) {
        t += dt;
        if (toastT > 0) toastT -= dt;
        // the pedestal sways gently instead of spinning round
        room.spin = 0.15f + (float) Math.sin(t * 0.45f) * 0.22f;
        if (cheerT > 0) {
            cheerT -= dt;
            float wave = (float) Math.sin(t * 14f) * 0.25f;
            driver.pose(Driver.L_ARM, 2.8f, 1f);
            driver.pose(Driver.R_ARM, 2.8f, 1f);
            driver.pose(Driver.L_ARM_OUT, 0.5f + wave, 1f);
            driver.pose(Driver.R_ARM_OUT, 0.5f - wave, 1f);
            driver.pose(Driver.RISE, 0.15f + Math.abs(wave), 1f);
            if (cheerT <= 0) driver.relax();
        }
        if (tab == EMOTES && !driver.emoting() && cheerT <= 0) {
            // the emote picked plays over and over
            replayT -= dt;
            if (replayT <= 0) playPreview();
        }
        room.update(dt);
    }

    public boolean render3d() {
        float W = game.b.width;
        float left = game.safeLeft + 20, lw = W * 0.46f;
        room.screenX = (left + (lw - left) / 2f) / W;
        room.screenY = 0.54f;
        // keep the car and the driver inside the left column whatever the screen shape
        float colW = Math.max(300f, lw - left);
        room.distance = 10.5f * (game.b.height / 720f) * (697f / colW);
        room.height = 4.4f;
        room.render();
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
        b.textShadow(b.title, "DRIVER", left + 100, top + 56, 56f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 6f, 6f, 0x50200040);
        float pr = W - right;
        float cw = TitleScreen.coinPill(game, pr, top + 52);
        TitleScreen.rimsPill(game, pr - cw - 14, top + 52);

        float lw = W * 0.46f;
        // tapping the driver gives it a shove
        float sceneY = top + 190;
        if (ui.hit("poke", left, sceneY, lw - left, bottom - 130 - sceneY)) {
            driver.jolt(14f, (float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f);
            game.sfx.play(Sfx.POP, 0.7f, 0.8f + (float) Math.random() * 0.4f);
        }
        if (ui.button("random", left + 4, top + 104, 250, 74, 0xFFFFB321, "RANDOMIZE", 30f)) randomize();
        if (tab == EMOTES && driver.emoting()) {
            // the emote's word over the driver
            int m = driver.emote;
            float k = Ease.outBack(Math.min(1f, driver.emoteT * 6f));
            b.alpha(Math.min(1f, (Emotes.DUR[m] - driver.emoteT) * 3f));
            b.textShadow(b.title, Emotes.WORD[m], left + (lw - left) * 0.62f, top + 230, 44f * k, Emotes.WORD_COLOR[m], UIBatch.CENTER,
                    0xFF2A1840, 5f, 5f, 0x60200040);
            b.alpha(1f);
        }
        infoBar(b, ui, s, left, lw, bottom - 112);

        // tabs + item grid (right side)
        float px = lw + 10, pw = W - right - px;
        float ty = top + 100;
        float tw = pw / TABS;
        for (int i = 0; i < TABS; i++) {
            float x = px + i * tw;
            boolean on = tab == i;
            if (ui.hit(tabId[i], x, ty - 6, tw - 6, 82) && tab != i) {
                setTab(i);
                game.sfx.play(Sfx.POP, 0.6f, 1.2f);
            }
            int col = on ? TAB_COLORS[i] : 0xFFFFFFFF;
            b.shape(x + (tw - 6) / 2, ty + 35 + (on ? 0 : 5), tw - 6, 70 - (on ? 0 : 5), 20, col, 0, 0, 0.3f, 0, 0);
            b.textFit(b.title, TAB_NAME[i], x + (tw - 6) / 2, ty + 37 + (on ? 0 : 3), 26f, tw - 14,
                    on ? 0xFFFFFFFF : 0xFF8A7AA0, UIBatch.CENTER, on ? 0xFF2A1840 : 0, on ? 3f : 0);
            if (i == EMOTES && !on && (s.seen & Save.SEEN_EMOTES) == 0) {
                newBadge(b, x + tw - 26, ty + 6, 0.8f, t);
            } else if (!on && canAfford(s, i)) {
                b.circle(x + tw - 20, ty + 12, 8, 0xFFFF3B5C, 0xFFFFFFFF, 2.5f);
            }
        }
        float cy = ty + 84, ch = bottom - 20 - cy;
        b.shadow(px, cy + 8, pw, ch, 30, 0x40200040, 14);
        b.shape(px + pw / 2, cy + ch / 2, pw, ch, 30, 0xFFFFFFFF, TAB_COLORS[tab], 6f, 0f, 0, 0);

        int cat = cat();
        int n = tab == EMOTES ? 1 + Emotes.COUNT : Items.count(cat);
        int cols = pw >= 760 ? 4 : 3;
        float gap = 12;
        float cardW = (pw - 36 - gap * (cols - 1)) / cols, cardH = 150;
        float gy = cy + 10, gh = ch - 20;
        float content = (float) Math.ceil(n / (float) cols) * (cardH + gap) + 10;
        if (scrollTo >= 0) {
            // bring the card focus() opened on into view
            float row = (scrollTo / cols) * (cardH + gap);
            ui.setScroll("wardrobe", Math.max(0f, Math.min(content - gh, row - (gh - cardH) / 2f)));
            scrollTo = -1;
        }
        float off = ui.beginScroll("wardrobe", px + 8, gy, pw - 16, gh, content);
        for (int i = 0; i < n; i++) {
            float x = px + 18 + (i % cols) * (cardW + gap);
            float y = gy + 8 + (i / cols) * (cardH + gap) - off;
            if (y > gy + gh || y + cardH < gy) continue;
            boolean tappable = y + cardH / 2 > gy && y + cardH / 2 < gy + gh;
            if (tab == EMOTES) emoteCard(b, ui, s, i, x, y, cardW, cardH, tappable);
            else card(b, ui, s, cat, i, x, y, cardW, cardH, tappable);
        }
        ui.endScroll();

        if (toastT > 0) {
            float k = Ease.outBack(Math.min(1f, (1.6f - toastT) * 5f));
            b.alpha(Math.min(1f, toastT * 3f));
            b.textShadow(b.title, toast, left + (lw - left) / 2, H * 0.36f, 60f * k, toastColor, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x60200040);
            b.alpha(1f);
        }
    }

    private void card(UIBatch b, UI ui, Save s, int cat, int i, float x, float y, float w, float h, boolean tappable) {
        String id = cardId[i];
        if (ui.hit(id, x, y, w, h) && tappable && sel != i) {
            sel = i;
            applyPreview();
            driver.kick(Driver.STRETCH, -1.5f);
            driver.kick(Driver.TILT, (float) Math.random() * 4f - 2f);
            game.sfx.play(Sfx.POP, 0.7f, 1f + (float) Math.random() * 0.3f);
        }
        boolean owned = Items.owned(s, cat, i);
        boolean equipped = Items.selected(s, cat) == i;
        boolean picked = sel == i;
        float sc = 1f + (float) Math.sin(ui.bounce(id) * Math.PI) * 0.05f;
        float cx = x + w / 2, cy = y + h / 2;
        int fill = picked ? 0xFFFFF3C4 : (owned ? 0xFFF3EEFA : 0xFFE6E0EE);
        int border = picked ? 0xFFFFC21F : (equipped ? 0xFF34D058 : 0);
        b.shape(cx, cy + 4, w * sc, h * sc, 22, 0x30200040, 0, 0, 0, 4, 0);
        b.shape(cx, cy, w * sc, h * sc, 22, fill, border, border != 0 ? 5f : 0f, 0.15f, 0, 0);
        boolean premium = Items.rule(cat, i) == Items.RIMS;
        if (premium) {
            // premium items get a sparkle
            ui.star(x + w - 22, y + h - 58, 10 + (float) Math.abs(Math.sin(t * 3f + i)) * 4f, 0xFF8FE4FF);
        }

        float iy = y + 48;
        b.circle(cx, iy, 40, owned ? 0xFFFFFFFF : 0xFFF4F0F8);
        icon(game, tab, i, cx, iy, 70, t);
        if (!owned) {
            b.circle(cx, iy, 40, 0x50FFFFFF);
            b.circle(x + 26, y + 24, 17, 0xFF2A1840);
            ui.iconLock(x + 26, y + 24, 19, 0xFFFFFFFF);
        }
        if (equipped) {
            b.circle(x + w - 24, y + 24, 18, 0xFF34D058, 0xFFFFFFFF, 3f);
            ui.iconCheck(x + w - 24, y + 24, 20, 0xFFFFFFFF);
        }
        b.textFit(b.title, Items.name(cat, i), cx, y + 106, 23f, w - 18, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        float sy = y + 132;
        if (equipped) b.text(b.body, "WEARING", cx, sy, 19f, 0xFF2EAE4C, UIBatch.CENTER, 0, 0);
        else if (owned) b.text(b.body, "OWNED", cx, sy, 19f, 0xFF9A8AB0, UIBatch.CENTER, 0, 0);
        else LockerScreen.priceTag(ui, cat, i, cx, sy, 20f, w - 24);
    }

    /**
     * An emote card: card 0 is the car's own emote (always there), the others the victory emotes. Tapping one plays
     * it on the driver.
     */
    private void emoteCard(UIBatch b, UI ui, Save s, int i, float x, float y, float w, float h, boolean tappable) {
        String id = cardId[i];
        if (ui.hit(id, x, y, w, h) && tappable) {
            sel = i;
            applyPreview();
        }
        int v = i - 1;
        boolean own = i == 0 || Items.owned(s, Items.EMOTE, v);
        boolean equipped = i == 0 || Items.equipped(s, Items.EMOTE, v);
        boolean picked = sel == i;
        float sc = 1f + (float) Math.sin(ui.bounce(id) * Math.PI) * 0.05f;
        float cx = x + w / 2, cy = y + h / 2;
        int fill = picked ? 0xFFFFF3C4 : (own ? 0xFFF3EEFA : 0xFFE6E0EE);
        int border = picked ? 0xFFFFC21F : (equipped ? 0xFF34D058 : 0);
        b.shape(cx, cy + 4, w * sc, h * sc, 22, 0x30200040, 0, 0, 0, 4, 0);
        b.shape(cx, cy, w * sc, h * sc, 22, fill, border, border != 0 ? 5f : 0f, 0.15f, 0, 0);
        if (i > 0 && Items.rule(Items.EMOTE, v) == Items.RIMS) {
            ui.star(x + w - 22, y + h - 58, 10 + (float) Math.abs(Math.sin(t * 3f + i)) * 4f, 0xFF8FE4FF);
        }
        float iy = y + 48;
        b.circle(cx, iy, 40, own ? 0xFFFFFFFF : 0xFFF4F0F8);
        if (i == 0) {
            int car = s.selectedCar;
            EmoteIcons.signature(game, car, s.carPaint[car], s.carAccent[car], cx, iy, 62, t);
        } else {
            int m = Emotes.VICTORY[v];
            EmoteIcons.move(game, m, cx, iy, 74, EmoteIcons.color(m), t);
        }
        if (!own) {
            b.circle(cx, iy, 40, 0x50FFFFFF);
            b.circle(x + 26, y + 24, 17, 0xFF2A1840);
            ui.iconLock(x + 26, y + 24, 19, 0xFFFFFFFF);
        }
        if (equipped) {
            b.circle(x + w - 24, y + 24, 18, 0xFF34D058, 0xFFFFFFFF, 3f);
            ui.iconCheck(x + w - 24, y + 24, 20, 0xFFFFFFFF);
        }
        String name = i == 0 ? Emotes.NAME[Emotes.signature(s.selectedCar)] : Emotes.victoryName(v);
        b.textFit(b.title, name, cx, y + 106, 23f, w - 18, 0xFF2A1840, UIBatch.CENTER, 0, 0);
        float sy = y + 132;
        if (i == 0) b.text(b.body, "THE CAR'S OWN", cx, sy, 19f, 0xFFE8702A, UIBatch.CENTER, 0, 0);
        else if (equipped) b.text(b.body, "EQUIPPED", cx, sy, 19f, 0xFF2EAE4C, UIBatch.CENTER, 0, 0);
        else if (own) b.text(b.body, "OWNED", cx, sy, 19f, 0xFF9A8AB0, UIBatch.CENTER, 0, 0);
        else LockerScreen.priceTag(ui, Items.EMOTE, v, cx, sy, 20f, w - 24);
    }

    /** Name of the emote picked and its EQUIP / BUY button. */
    private void emoteBar(UIBatch b, UI ui, Save s, float left, float lw, float y) {
        float bw = 270, bh = 96;
        float bx = lw - 6 - bw;
        float nw = bx - 16 - left;
        int v = sel - 1;
        b.shadow(left, y + 8, nw, bh, 30, 0x50200040, 10);
        b.shape(left + nw / 2, y + bh / 2, nw, bh, 30, 0xE02A1840, 0, 0, 0, 0, 0);
        b.textFit(b.title, Emotes.NAME[previewMove()], left + 26, y + 36, 36f, nw - 50, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
        String sub = sel == 0 ? "EMOTE OF THE " + Cars.ALL[s.selectedCar].name : Items.CAT_NAME[Items.EMOTE];
        b.textFit(b.body, sub, left + 26, y + 72, 24f, nw - 50, 0xFFD8CCEA, UIBatch.LEFT, 0, 0);
        if (sel == 0) {
            ui.iconButton("wear", bx, y, bw, bh, 0xFF34D058, null, UI.ICON_CHECK, "ALWAYS ON", 32f, false);
            return;
        }
        if (Items.owned(s, Items.EMOTE, v)) {
            boolean on = Items.equipped(s, Items.EMOTE, v);
            if (on ? ui.iconButton("wear", bx, y, bw, bh, 0xFF34D058, null, UI.ICON_CHECK, "EQUIPPED", 34f, true)
                    : ui.button("wear", bx, y, bw, bh, 0xFF3BA8FF, "EQUIP", 44f)) {
                Items.equip(s, v, !on);
                game.sfx.play(Sfx.POP, 1f, on ? 0.7f : 1.1f);
                showToast(on ? "TAKEN OFF" : "SHOWTIME!", on ? 0xFFB8B0C8 : 0xFF5EE65A);
            }
            return;
        }
        int rule = Items.rule(Items.EMOTE, v), val = Items.value(Items.EMOTE, v);
        if (rule == Items.LEVEL) {
            ui.iconButton("wear", bx, y, bw, bh, 0xFFB8B0C8, null, UI.ICON_LOCK, "LEVEL " + val, 40f, false);
            return;
        }
        boolean premium = rule == Items.RIMS;
        boolean afford = Items.affordable(s, Items.EMOTE, v);
        int col = afford ? (premium ? 0xFF3BB8FF : 0xFFFF9A2B) : 0xFFB8B0C8;
        if (ui.iconButton("wear", bx, y, bw, bh, col, "BUY", premium ? UI.ICON_RIM : UI.ICON_COIN, ShopScreen.num(val), 40f, true)) {
            if (Items.buy(s, Items.EMOTE, v)) {
                game.sfx.play(Sfx.BUY, 1f, 1f);
                game.sfx.play(Sfx.UNLOCK, 0.7f, 1.1f);
                room.fx.confetti(room.standX, 2.4f, room.standZ, 40, 0.9f);
                playPreview();
                showToast("NEW MOVE!", 0xFFFFE14D);
            } else {
                game.sfx.play(Sfx.NOPE, 1f, 1f);
                showToast(premium ? "NEED MORE RIMS" : "NEED MORE COINS", 0xFFFF4FA3);
            }
        }
    }

    /** A small tilted "NEW" tag (things that came with drivers and emotes, until they have been looked at). */
    static void newBadge(UIBatch b, float cx, float cy, float s, float t) {
        float k = s * (1f + (float) Math.abs(Math.sin(t * 4f)) * 0.08f);
        b.shape(cx, cy + 3 * k, 64 * k, 32 * k, 12 * k, 0x50200040, 0, 0, 0, 3, -0.2f);
        b.shape(cx, cy, 64 * k, 32 * k, 12 * k, 0xFFFF3B5C, 0xFFFFFFFF, 3f * k, 0.4f, 0, -0.2f);
        b.text(b.title, "NEW", cx, cy + 1, 22f * k, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
    }

    /** Name of the item tried on and its WEAR / BUY button, along the bottom of the left side. */
    private void infoBar(UIBatch b, UI ui, Save s, float left, float lw, float y) {
        if (tab == EMOTES) {
            emoteBar(b, ui, s, left, lw, y);
            return;
        }
        int cat = cat();
        float bw = 270, bh = 96;
        float bx = lw - 6 - bw;
        float nw = bx - 16 - left;
        b.shadow(left, y + 8, nw, bh, 30, 0x50200040, 10);
        b.shape(left + nw / 2, y + bh / 2, nw, bh, 30, 0xE02A1840, 0, 0, 0, 0, 0);
        b.textFit(b.title, Items.name(cat, sel), left + 26, y + 36, 36f, nw - 50, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
        b.textFit(b.body, Items.CAT_NAME[cat], left + 26, y + 72, 24f, nw - 50, 0xFFD8CCEA, UIBatch.LEFT, 0, 0);

        boolean owned = Items.owned(s, cat, sel);
        if (Items.selected(s, cat) == sel) {
            ui.iconButton("wear", bx, y, bw, bh, 0xFF34D058, null, UI.ICON_CHECK, "WEARING", 36f, true);
            return;
        }
        if (owned) {
            if (ui.button("wear", bx, y, bw, bh, 0xFF34D058, "WEAR IT", 44f)) {
                Items.select(s, cat, sel);
                cheer();
                game.sfx.play(Sfx.POP, 1f, 0.9f);
                game.sfx.play(Sfx.WHOOSH, 0.5f, 1.3f);
                showToast("LOOKING GOOD!", 0xFF5EE65A);
            }
            return;
        }
        int rule = Items.rule(cat, sel), v = Items.value(cat, sel);
        if (rule == Items.LEVEL) {
            ui.iconButton("wear", bx, y, bw, bh, 0xFFB8B0C8, null, UI.ICON_LOCK, "LEVEL " + v, 40f, false);
            return;
        }
        boolean premium = rule == Items.RIMS;
        boolean afford = Items.affordable(s, cat, sel);
        int col = afford ? (premium ? 0xFF3BB8FF : 0xFFFF9A2B) : 0xFFB8B0C8;
        if (ui.iconButton("wear", bx, y, bw, bh, col, "BUY", premium ? UI.ICON_RIM : UI.ICON_COIN, ShopScreen.num(v), 40f, true)) {
            if (Items.buy(s, cat, sel)) {
                Items.select(s, cat, sel);
                cheer();
                game.sfx.play(Sfx.BUY, 1f, 1f);
                game.sfx.play(Sfx.UNLOCK, 0.7f, 1.1f);
                showToast("FRESH!", 0xFFFFE14D);
            } else {
                game.sfx.play(Sfx.NOPE, 1f, 1f);
                showToast(premium ? "NEED MORE RIMS" : "NEED MORE COINS", 0xFFFF4FA3);
            }
        }
    }

    /** A random look from what the player owns, worn right away. */
    private void randomize() {
        Save s = game.save;
        for (int p = 0; p < Outfits.PARTS; p++) {
            boolean[] owned = s.lookOwned[p];
            int n = 0;
            for (boolean o : owned) if (o) n++;
            int k = (int) (Math.random() * n);
            for (int i = 0; i < owned.length; i++) {
                if (owned[i] && k-- == 0) {
                    s.look[p] = i;
                    break;
                }
            }
        }
        s.markDirty();
        s.flush();
        if (tab != EMOTES) sel = s.look[tab];
        applyPreview();
        cheer();
        game.sfx.play(Sfx.WHOOSH, 0.7f, 1.1f);
        game.sfx.play(Sfx.POP, 0.9f, 1.2f);
        showToast("SHUFFLED!", 0xFFFFC21F);
    }

    /** True if something of this tab (a wardrobe part, or the emotes) could be bought right now. */
    static boolean canAfford(Save s, int part) {
        int cat = Items.HAT + part;
        for (int i = 0; i < Items.count(cat); i++) if (Items.affordable(s, cat, i)) return true;
        return false;
    }

    /** True if any wardrobe item or emote could be bought right now. */
    static boolean canAffordSomething(Save s) {
        for (int p = 0; p < TABS; p++) if (canAfford(s, p)) return true;
        return false;
    }

    public boolean back() {
        game.setScreen(new TitleScreen(game));
        return true;
    }

    // ------------------------------------------------------------------ item pictures (also used by other screens)

    private static final int INK = 0xFF2A1840, PEACH = 0xFFF6C7A1;

    /** Small picture of a wardrobe item (part, item), about s units across, centered at (cx, cy). */
    static void icon(Game game, int part, int i, float cx, float cy, float s, float t) {
        UIBatch b = game.b;
        float k = s / 100f;
        int col = 0xFF000000 | Outfits.COLOR[part][i];
        switch (part) {
            case Outfits.HAT: hatIcon(game, i, cx, cy, k, col); break;
            case Outfits.HAIR: hairIcon(game, i, cx, cy, k, col); break;
            case Outfits.FACE: faceIcon(game, i, cx, cy, k, PEACH); break;
            case Outfits.SKIN:
                faceIcon(game, Outfits.FC_SMILE, cx, cy, k, col);
                break;
            case Outfits.SHIRT: shirtIcon(game, i, cx, cy, k, col); break;
            case Outfits.PANTS: pantsIcon(game, i, cx, cy, k, col); break;
            case Outfits.SHOES: shoeIcon(game, i, cx, cy, k, col); break;
            default: heldIcon(game, i, cx, cy, k, t); break;
        }
    }

    private static void head(UIBatch b, float cx, float cy, float k, int skin) {
        b.shape(cx, cy + 10 * k, 64 * k, 60 * k, 30 * k, skin, INK, 2.5f * k, 0.3f, 0, 0);
        b.circle(cx - 11 * k, cy + 10 * k, 4 * k, INK);
        b.circle(cx + 11 * k, cy + 10 * k, 4 * k, INK);
    }

    private static void hatIcon(Game game, int i, float cx, float cy, float k, int col) {
        UIBatch b = game.b;
        UI ui = game.ui;
        head(b, cx, cy + 8 * k, k, PEACH);
        float hy = cy - 8 * k;
        switch (i) {
            case Outfits.H_NONE:
                b.shape(cx, hy - 4 * k, 60 * k, 26 * k, 13 * k, 0xFF7A4A2A, 0, 0, 0.3f, 0, 0);
                break;
            case Outfits.H_COWBOY:
                b.shape(cx, hy - 10 * k, 46 * k, 34 * k, 12 * k, col, INK, 2f * k, 0.3f, 0, 0);
                b.shape(cx, hy + 4 * k, 96 * k, 14 * k, 7 * k, UI.shade(col, 0.85f), INK, 2f * k, 0.3f, 0, 0);
                break;
            case Outfits.H_CONE:
                for (int j = 0; j < 4; j++) {
                    float w = (40 - j * 9) * k;
                    b.shape(cx, hy - j * 12 * k, w, 14 * k, 4 * k, j == 1 ? 0xFFFFFFFF : col, 0, 0, 0.3f, 0, 0);
                }
                break;
            case Outfits.H_TOPHAT:
                b.shape(cx + 6 * k, hy - 18 * k, 30 * k, 36 * k, 4 * k, col, 0xFF6A6080, 2f * k, 0.3f, 0, -0.25f);
                b.shape(cx + 2 * k, hy - 2 * k, 48 * k, 8 * k, 4 * k, col, 0xFF6A6080, 2f * k, 0.3f, 0, -0.25f);
                break;
            case Outfits.H_CROWN:
                b.roundRect(cx - 28 * k, hy - 8 * k, 56 * k, 18 * k, 4 * k, col);
                for (int j = -1; j <= 1; j++) b.shape(cx + j * 19 * k, hy - 12 * k, 16 * k, 16 * k, 3 * k, col, 0, 0, 0.3f, 0, 0.785f);
                b.circle(cx, hy + 1 * k, 5 * k, 0xFFFF2E4A);
                b.circle(cx - 17 * k, hy + 1 * k, 4 * k, 0xFF3BB8FF);
                b.circle(cx + 17 * k, hy + 1 * k, 4 * k, 0xFF34D058);
                break;
            case Outfits.H_SPACE:
                b.circle(cx, cy + 12 * k, 44 * k, 0xFFF4F6FF, INK, 2.5f * k);
                b.shape(cx, cy + 12 * k, 60 * k, 44 * k, 20 * k, 0xFFFFC83D, 0, 0, 0.6f, 0, 0);
                b.line(cx + 18 * k, cy - 30 * k, cx + 18 * k, cy - 44 * k, 3 * k, 0xFF9AA2B4);
                b.circle(cx + 18 * k, cy - 45 * k, 5 * k, 0xFFFF3B4F);
                break;
            case Outfits.H_HELMET:
                b.shape(cx, cy + 2 * k, 76 * k, 66 * k, 30 * k, col, INK, 2.5f * k, 0.4f, 0, 0);
                b.shape(cx, cy + 14 * k, 52 * k, 26 * k, 10 * k, PEACH, 0, 0, 0, 0, 0);
                b.circle(cx - 11 * k, cy + 14 * k, 4 * k, INK);
                b.circle(cx + 11 * k, cy + 14 * k, 4 * k, INK);
                b.rect(cx - 3 * k, cy - 30 * k, 6 * k, 30 * k, 0xFFFF3B4F);
                break;
            case Outfits.H_SHARK:
                b.shape(cx, cy + 2 * k, 76 * k, 66 * k, 30 * k, col, INK, 2.5f * k, 0.4f, 0, 0);
                b.shape(cx, cy - 32 * k, 18 * k, 18 * k, 3 * k, col, 0, 0, 0.3f, 0, 0.785f);
                b.shape(cx, cy + 14 * k, 52 * k, 28 * k, 12 * k, PEACH, 0xFFFFFFFF, 3f * k, 0, 0, 0);
                b.circle(cx - 11 * k, cy + 14 * k, 4 * k, INK);
                b.circle(cx + 11 * k, cy + 14 * k, 4 * k, INK);
                break;
            default: {
                // caps, beanies and the like: a dome with a brim or a band
                b.shape(cx, hy - 6 * k, 66 * k, 40 * k, 20 * k, col, INK, 2f * k, 0.35f, 0, 0);
                if (i == Outfits.H_CAP || i == Outfits.H_HARD || i == Outfits.H_BUCKET) {
                    b.shape(cx + (i == Outfits.H_CAP ? 14 : 0) * k, hy + 10 * k, (i == Outfits.H_CAP ? 52 : 80) * k, 10 * k, 5 * k,
                            UI.shade(col, 0.85f), INK, 2f * k, 0.3f, 0, 0);
                } else {
                    b.roundRect(cx - 33 * k, hy + 6 * k, 66 * k, 10 * k, 5 * k, UI.shade(col, 0.8f));
                }
                if (i == Outfits.H_BEANIE) b.circle(cx, hy - 28 * k, 9 * k, 0xFFFFFFFF);
                if (i == Outfits.H_PROPELLER) {
                    b.line(cx, hy - 26 * k, cx, hy - 34 * k, 3 * k, 0xFFC8CCD8);
                    b.shape(cx, hy - 36 * k, 44 * k, 7 * k, 3 * k, 0xFFFF3B4F, 0, 0, 0, 0, 0.2f);
                }
                if (i == Outfits.H_VIKING) {
                    for (int sd = -1; sd <= 1; sd += 2) {
                        b.shape(cx + sd * 36 * k, hy - 16 * k, 12 * k, 30 * k, 6 * k, 0xFFFFF2D8, INK, 1.5f * k, 0, 0, sd * 0.6f);
                    }
                }
                break;
            }
        }
    }

    private static void hairIcon(Game game, int i, float cx, float cy, float k, int col) {
        UIBatch b = game.b;
        float hy = cy - 4 * k;
        if (i == Outfits.HR_AFRO || i == Outfits.HR_RAINBOW) {
            for (int j = 0; j < 7; j++) {
                float a = (float) (Math.PI + j * Math.PI / 6);
                int c = i == Outfits.HR_RAINBOW ? 0xFF000000 | Palette.hsv(j / 7f, 0.55f, 1f) : col;
                b.circle(cx + (float) Math.cos(a) * 32 * k, hy + 6 * k + (float) Math.sin(a) * 30 * k, 16 * k, c);
            }
            b.circle(cx, hy - 6 * k, 30 * k, i == Outfits.HR_RAINBOW ? 0xFFFFE15C : col);
        }
        if (i == Outfits.HR_LONG) b.shape(cx, cy + 14 * k, 76 * k, 64 * k, 22 * k, col, 0, 0, 0.3f, 0, 0);
        if (i == Outfits.HR_PIGTAILS) {
            b.circle(cx - 38 * k, cy + 10 * k, 12 * k, col);
            b.circle(cx + 38 * k, cy + 10 * k, 12 * k, col);
        }
        if (i == Outfits.HR_PONY) b.shape(cx + 34 * k, cy + 12 * k, 16 * k, 36 * k, 8 * k, col, 0, 0, 0.3f, 0, -0.3f);
        head(b, cx, cy + 4 * k, k, PEACH);
        switch (i) {
            case Outfits.HR_BALD:
                b.circle(cx - 12 * k, hy - 4 * k, 6 * k, 0x80FFFFFF);
                break;
            case Outfits.HR_SPIKY:
            case Outfits.HR_FLAME:
                for (int j = -2; j <= 2; j++) {
                    int c = i == Outfits.HR_FLAME ? (j % 2 == 0 ? 0xFFFF3B1F : 0xFFFFD23B) : col;
                    b.shape(cx + j * 12 * k, hy - 12 * k - (2 - Math.abs(j)) * 4 * k, 16 * k, 16 * k, 2 * k, c, 0, 0, 0.3f, 0, 0.785f);
                }
                b.shape(cx, hy - 2 * k, 62 * k, 22 * k, 11 * k, i == Outfits.HR_FLAME ? 0xFFFF8A1F : col, 0, 0, 0.3f, 0, 0);
                break;
            case Outfits.HR_MOHAWK:
                b.shape(cx, hy - 2 * k, 60 * k, 18 * k, 9 * k, UI.shade(col, 0.55f), 0, 0, 0.3f, 0, 0);
                b.shape(cx, hy - 16 * k, 14 * k, 34 * k, 6 * k, col, INK, 1.5f * k, 0.3f, 0, 0);
                break;
            case Outfits.HR_POMP:
                b.shape(cx + 4 * k, hy - 12 * k, 62 * k, 26 * k, 13 * k, col, INK, 1.5f * k, 0.4f, 0, -0.15f);
                break;
            case Outfits.HR_CURLS:
                for (int j = -2; j <= 2; j++) b.circle(cx + j * 13 * k, hy - 6 * k + Math.abs(j) * 3 * k, 10 * k, col);
                break;
            case Outfits.HR_AFRO:
            case Outfits.HR_RAINBOW:
                break;
            default: {
                b.shape(cx, hy - 4 * k, 64 * k, 28 * k, 14 * k, col, INK, 1.5f * k, 0.3f, 0, 0);
                if (i == Outfits.HR_MOP) {
                    for (int j = -2; j <= 2; j++) b.circle(cx + j * 11 * k, hy + 6 * k, 8 * k, col);
                }
                break;
            }
        }
    }

    private static void faceIcon(Game game, int i, float cx, float cy, float k, int skin) {
        UIBatch b = game.b;
        UI ui = game.ui;
        b.circle(cx, cy, 36 * k, skin, INK, 2.5f * k);
        float ey = cy - 6 * k, ex = 12 * k, my = cy + 14 * k;
        // eyes
        switch (i) {
            case Outfits.FC_SHADES:
                b.roundRect(cx - 28 * k, ey - 7 * k, 56 * k, 14 * k, 6 * k, 0xFF14101E);
                break;
            case Outfits.FC_GRIN:
                b.line(cx - ex - 6 * k, ey, cx - ex, ey - 5 * k, 3 * k, INK);
                b.line(cx - ex, ey - 5 * k, cx - ex + 6 * k, ey, 3 * k, INK);
                b.line(cx + ex - 6 * k, ey, cx + ex, ey - 5 * k, 3 * k, INK);
                b.line(cx + ex, ey - 5 * k, cx + ex + 6 * k, ey, 3 * k, INK);
                break;
            case Outfits.FC_WINK:
                b.circle(cx + ex, ey, 5 * k, INK);
                b.line(cx - ex - 6 * k, ey, cx - ex + 6 * k, ey, 3 * k, INK);
                break;
            case Outfits.FC_PATCH:
                b.circle(cx + ex, ey, 5 * k, INK);
                b.circle(cx - ex, ey, 9 * k, 0xFF14101E);
                b.line(cx - 34 * k, ey - 12 * k, cx + 34 * k, ey - 2 * k, 2.5f * k, 0xFF14101E);
                break;
            case Outfits.FC_STARS:
                ui.star(cx - ex, ey, 9 * k, 0xFFFFD23B);
                ui.star(cx + ex, ey, 9 * k, 0xFFFFD23B);
                break;
            case Outfits.FC_LASER:
                b.circle(cx - ex, ey, 8 * k, 0xFFFF1F2E);
                b.circle(cx + ex, ey, 8 * k, 0xFFFF1F2E);
                b.circle(cx - ex, ey, 3 * k, 0xFFFFF0F0);
                b.circle(cx + ex, ey, 3 * k, 0xFFFFF0F0);
                break;
            case Outfits.FC_SHOCKED:
                b.circle(cx - ex, ey, 8 * k, 0xFFFFFFFF, INK, 2f * k);
                b.circle(cx + ex, ey, 8 * k, 0xFFFFFFFF, INK, 2f * k);
                b.circle(cx - ex, ey, 3.5f * k, INK);
                b.circle(cx + ex, ey, 3.5f * k, INK);
                break;
            default:
                b.circle(cx - ex, ey, 5 * k, INK);
                b.circle(cx + ex, ey, 5 * k, INK);
                break;
        }
        if (i == Outfits.FC_NERD) {
            b.circle(cx - ex, ey, 10 * k, 0, 0xFF3A2A20, 2.5f * k);
            b.circle(cx + ex, ey, 10 * k, 0, 0xFF3A2A20, 2.5f * k);
        }
        if (i == Outfits.FC_GRUMPY || i == Outfits.FC_LASER) {
            b.line(cx - ex - 8 * k, ey - 14 * k, cx - ex + 6 * k, ey - 9 * k, 3 * k, INK);
            b.line(cx + ex + 8 * k, ey - 14 * k, cx + ex - 6 * k, ey - 9 * k, 3 * k, INK);
        }
        // mouth
        switch (i) {
            case Outfits.FC_GRUMPY:
                b.line(cx - 10 * k, my + 4 * k, cx, my, 3 * k, INK);
                b.line(cx, my, cx + 10 * k, my + 4 * k, 3 * k, INK);
                break;
            case Outfits.FC_SHOCKED:
                b.shape(cx, my + 2 * k, 12 * k, 16 * k, 6 * k, 0xFF5A1A2A, 0, 0, 0, 0, 0);
                break;
            case Outfits.FC_GRIN:
                b.shape(cx, my, 30 * k, 14 * k, 7 * k, 0xFFFFFFFF, INK, 2f * k, 0, 0, 0);
                break;
            case Outfits.FC_SILLY:
                b.line(cx - 10 * k, my, cx + 10 * k, my, 3 * k, INK);
                b.shape(cx + 3 * k, my + 6 * k, 10 * k, 12 * k, 5 * k, 0xFFFF6F8F, 0, 0, 0, 0, 0);
                break;
            case Outfits.FC_MUSTACHE:
                b.shape(cx - 8 * k, my - 3 * k, 18 * k, 8 * k, 4 * k, 0xFF4A2A1A, 0, 0, 0, 0, 0.25f);
                b.shape(cx + 8 * k, my - 3 * k, 18 * k, 8 * k, 4 * k, 0xFF4A2A1A, 0, 0, 0, 0, -0.25f);
                break;
            default:
                b.line(cx - 10 * k, my - 2 * k, cx - 4 * k, my + 2 * k, 3 * k, INK);
                b.line(cx - 4 * k, my + 2 * k, cx + 4 * k, my + 2 * k, 3 * k, INK);
                b.line(cx + 4 * k, my + 2 * k, cx + 10 * k, my - 2 * k, 3 * k, INK);
                break;
        }
        if (i == Outfits.FC_CLOWN) b.circle(cx, cy + 4 * k, 7 * k, 0xFFFF2E3A);
    }

    private static void shirtIcon(Game game, int i, float cx, float cy, float k, int col) {
        UIBatch b = game.b;
        UI ui = game.ui;
        int sleeve = Outfits.SLEEVE[i] == 0 ? PEACH : 0xFF000000 | Outfits.SLEEVE[i];
        int len = Outfits.SLEEVE_LEN[i];
        if (len < 2) {
            float sl = len == 0 ? 36 : 22;
            for (int sd = -1; sd <= 1; sd += 2) {
                b.shape(cx + sd * 30 * k, cy - 8 * k + sl * 0.4f * k, 16 * k, sl * k, 6 * k, sleeve, INK, 2f * k, 0.2f, 0, sd * 0.55f);
            }
        }
        b.shape(cx, cy + 4 * k, 50 * k, 60 * k, 10 * k, col, INK, 2.5f * k, 0.3f, 0, 0);
        b.shape(cx, cy - 24 * k, 18 * k, 8 * k, 4 * k, PEACH, 0, 0, 0, 0, 0);
        switch (i) {
            case Outfits.SH_STRIPES:
                for (int j = 0; j < 3; j++) b.rect(cx - 23 * k, cy - 8 * k + j * 13 * k, 46 * k, 5 * k, 0xFFFFFFFF);
                break;
            case Outfits.SH_SWEATER:
                for (int j = 0; j < 4; j++) b.line(cx - 22 * k + j * 11 * k, cy + 2 * k + (j % 2) * 6 * k, cx - 11 * k + j * 11 * k, cy + 2 * k + ((j + 1) % 2) * 6 * k, 3 * k, 0xFFFFE14D);
                break;
            case Outfits.SH_HAWAII:
                for (int j = 0; j < 4; j++) b.circle(cx - 14 * k + (j % 2) * 26 * k, cy - 6 * k + (j / 2) * 22 * k, 5 * k, j % 2 == 0 ? 0xFFFFE14D : 0xFFFF6FB5);
                break;
            case Outfits.SH_OVERALLS:
                b.rect(cx - 24 * k, cy - 25 * k, 48 * k, 18 * k, 0xFFF4F4FF);
                b.rect(cx - 16 * k, cy - 25 * k, 6 * k, 18 * k, col);
                b.rect(cx + 10 * k, cy - 25 * k, 6 * k, 18 * k, col);
                break;
            case Outfits.SH_JERSEY:
                b.text(b.title, "1", cx, cy + 6 * k, 34 * k, INK, UIBatch.CENTER, 0, 0);
                break;
            case Outfits.SH_RACING:
                b.rect(cx - 25 * k, cy - 24 * k, 7 * k, 56 * k, 0xFFFF3B4F);
                b.rect(cx + 18 * k, cy - 24 * k, 7 * k, 56 * k, 0xFFFF3B4F);
                break;
            case Outfits.SH_HERO:
                ui.star(cx, cy + 2 * k, 13 * k, 0xFFFFD23B);
                break;
            case Outfits.SH_TUX:
                b.shape(cx, cy - 6 * k, 16 * k, 34 * k, 4 * k, 0xFFFFFFFF, 0, 0, 0, 0, 0);
                b.shape(cx - 6 * k, cy - 20 * k, 12 * k, 8 * k, 4 * k, 0xFFFF2E4A, 0, 0, 0, 0, 0);
                b.shape(cx + 6 * k, cy - 20 * k, 12 * k, 8 * k, 4 * k, 0xFFFF2E4A, 0, 0, 0, 0, 0);
                break;
            case Outfits.SH_DISCO:
                for (int j = 0; j < 6; j++) b.circle(cx - 16 * k + (j % 3) * 16 * k, cy - 8 * k + (j / 3) * 20 * k, 4 * k, j % 2 == 0 ? 0xFFFF6FD8 : 0xFF7FF0FF);
                break;
            case Outfits.SH_SPACE:
                b.roundRect(cx - 12 * k, cy - 4 * k, 24 * k, 14 * k, 3 * k, 0xFFC8CED8);
                b.circle(cx - 6 * k, cy + 3 * k, 3 * k, 0xFFFF3B4F);
                b.circle(cx + 6 * k, cy + 3 * k, 3 * k, 0xFF3BB8FF);
                break;
            case Outfits.SH_HOODIE:
                b.roundRect(cx - 14 * k, cy + 14 * k, 28 * k, 12 * k, 4 * k, UI.shade(col, 0.85f));
                break;
            default:
                break;
        }
    }

    private static void pantsIcon(Game game, int i, float cx, float cy, float k, int col) {
        UIBatch b = game.b;
        boolean shorts = Outfits.SHORTS[i];
        float legH = shorts ? 30 : 58;
        if (shorts) {
            b.shape(cx - 13 * k, cy + 18 * k, 14 * k, 30 * k, 6 * k, PEACH, INK, 2f * k, 0, 0, 0);
            b.shape(cx + 13 * k, cy + 18 * k, 14 * k, 30 * k, 6 * k, PEACH, INK, 2f * k, 0, 0, 0);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            b.shape(cx + sd * 13 * k, cy - 20 * k + legH / 2 * k, 24 * k, legH * k, 6 * k, col, INK, 2.5f * k, 0.3f, 0, 0);
            if (Outfits.PANTS_STYLE[i] == 1) b.rect(cx + sd * 13 * k + sd * 8 * k - 2 * k, cy - 14 * k, 4 * k, (legH - 8) * k, 0xFFFFFFFF);
            if (Outfits.PANTS_STYLE[i] == 2) {
                for (int j = 0; j < 4; j++) b.rect(cx + sd * 13 * k - 10 * k + (j % 2) * 10 * k, cy - 12 * k + j * 12 * k, 10 * k, 6 * k, INK);
            }
        }
        b.shape(cx, cy - 22 * k, 52 * k, 14 * k, 5 * k, UI.shade(col, 0.85f), INK, 2.5f * k, 0.3f, 0, 0);
    }

    private static void shoeIcon(Game game, int i, float cx, float cy, float k, int col) {
        UIBatch b = game.b;
        for (int sd = -1; sd <= 1; sd += 2) {
            float x = cx + sd * 22 * k, y = cy + 8 * k;
            float len = i == Outfits.S_CLOWN ? 48 : 34;
            boolean tall = i == Outfits.S_BOOTS || i == Outfits.S_RAIN || i == Outfits.S_HIGHTOPS || i == Outfits.S_ROCKET
                    || i == Outfits.S_SKATES;
            if (tall) b.shape(x - 6 * k, y - 14 * k, 18 * k, 30 * k, 6 * k, col, INK, 2f * k, 0.3f, 0, 0);
            if (i == Outfits.S_FLIPFLOPS) {
                b.shape(x, y + 6 * k, len * k, 8 * k, 4 * k, col, INK, 2f * k, 0, 0, 0);
                b.line(x - 4 * k, y + 2 * k, x + 4 * k, y - 6 * k, 3 * k, 0xFF3BB8FF);
                continue;
            }
            b.shape(x + 4 * k, y, len * k, 18 * k, 9 * k, col, INK, 2f * k, 0.35f, 0, 0);
            b.rect(x + 4 * k - len / 2 * k + 3 * k, y + 6 * k, (len - 6) * k, 4 * k, i == Outfits.S_GOLD ? 0xFFFFFFFF : 0xFF3A3550);
            if (i == Outfits.S_BUNNY) {
                b.shape(x + 8 * k, y - 14 * k, 6 * k, 16 * k, 3 * k, 0xFFFFFFFF, INK, 1.5f * k, 0, 0, 0.3f);
                b.shape(x + 16 * k, y - 12 * k, 6 * k, 16 * k, 3 * k, 0xFFFFFFFF, INK, 1.5f * k, 0, 0, 0.6f);
            }
            if (i == Outfits.S_SKATES) {
                b.circle(x - 6 * k, y + 13 * k, 5 * k, 0xFFFFD23B);
                b.circle(x + 12 * k, y + 13 * k, 5 * k, 0xFFFFD23B);
            }
            if (i == Outfits.S_ROCKET) b.shape(x - 18 * k, y - 4 * k, 14 * k, 8 * k, 4 * k, 0xFFFF8A1F, 0, 0, 0, 0, 0);
            if (i == Outfits.S_GOLD) b.shape(x - 10 * k, y - 10 * k, 6 * k, 14 * k, 3 * k, 0xFFFFFFFF, 0, 0, 0, 0, -0.6f);
        }
    }

    private static void heldIcon(Game game, int i, float cx, float cy, float k, float t) {
        UIBatch b = game.b;
        UI ui = game.ui;
        switch (i) {
            case Outfits.HD_NONE:
                // an open hand
                b.circle(cx, cy + 6 * k, 18 * k, PEACH, INK, 2f * k);
                for (int j = -1; j <= 2; j++) b.shape(cx + j * 8 * k - 4 * k, cy - 14 * k, 7 * k, 18 * k, 3.5f * k, PEACH, INK, 1.5f * k, 0, 0, 0);
                break;
            case Outfits.HD_FINGER:
                b.shape(cx, cy + 10 * k, 34 * k, 30 * k, 8 * k, 0xFF3BB8FF, INK, 2f * k, 0.3f, 0, 0);
                b.shape(cx + 6 * k, cy - 16 * k, 12 * k, 34 * k, 6 * k, 0xFF3BB8FF, INK, 2f * k, 0.3f, 0, 0);
                b.text(b.title, "#1", cx - 2 * k, cy + 12 * k, 16 * k, 0xFFFFE14D, UIBatch.CENTER, 0, 0);
                break;
            case Outfits.HD_ICECREAM:
                b.shape(cx, cy + 14 * k, 18 * k, 34 * k, 4 * k, 0xFFE8A65A, INK, 2f * k, 0, 0, 0);
                b.circle(cx, cy - 8 * k, 14 * k, 0xFFFF8AC0, INK, 2f * k);
                b.circle(cx, cy - 24 * k, 11 * k, 0xFFFFF6E0, INK, 2f * k);
                b.circle(cx, cy - 36 * k, 4 * k, 0xFFFF2244);
                break;
            case Outfits.HD_BANANA:
                for (int j = 0; j < 6; j++) {
                    float a = -1.1f + j * 0.44f;
                    b.circle(cx + (float) Math.sin(a) * 24 * k, cy - (float) Math.cos(a) * 10 * k + 4 * k, (8 - Math.abs(j - 2.5f) * 1.6f) * k, 0xFFFFE14D);
                }
                break;
            case Outfits.HD_CHICKEN:
                b.shape(cx, cy + 6 * k, 22 * k, 36 * k, 11 * k, 0xFFFFE14D, INK, 2f * k, 0.3f, 0, 0);
                b.circle(cx, cy - 20 * k, 10 * k, 0xFFFFE14D, INK, 2f * k);
                b.shape(cx + 9 * k, cy - 19 * k, 10 * k, 5 * k, 2 * k, 0xFFFF8A1F, 0, 0, 0, 0, 0);
                b.rect(cx - 2 * k, cy - 32 * k, 4 * k, 6 * k, 0xFFFF2E3A);
                break;
            case Outfits.HD_BALLOON:
                b.line(cx, cy + 34 * k, cx, cy, 2 * k, 0xFFFFFFFF);
                b.shape(cx, cy - 14 * k, 34 * k, 40 * k, 17 * k, 0xFFFF2E4A, INK, 2f * k, 0.4f, 0, 0);
                break;
            case Outfits.HD_PHONE:
                b.roundRect(cx - 12 * k, cy - 22 * k, 24 * k, 44 * k, 5 * k, 0xFF2A2433);
                b.roundRect(cx - 9 * k, cy - 18 * k, 18 * k, 34 * k, 3 * k, 0xFF6FE8FF);
                break;
            case Outfits.HD_TACO:
                b.shape(cx, cy + 4 * k, 50 * k, 30 * k, 15 * k, 0xFFFFC94D, INK, 2f * k, 0.3f, 0, 0);
                for (int j = -1; j <= 1; j++) b.circle(cx + j * 13 * k, cy - 10 * k, 7 * k, j == 0 ? 0xFFFF4040 : 0xFF5ED64A);
                break;
            case Outfits.HD_LOLLIPOP:
                b.line(cx, cy + 34 * k, cx, cy, 3 * k, 0xFFFFFFFF);
                b.circle(cx, cy - 12 * k, 20 * k, 0xFFFF4FA3, INK, 2f * k);
                b.circle(cx, cy - 12 * k, 11 * k, 0, 0xFFFFFFFF, 3f * k);
                break;
            case Outfits.HD_FLAG:
                b.line(cx - 16 * k, cy + 34 * k, cx - 16 * k, cy - 30 * k, 3 * k, 0xFFD8DEEA);
                for (int j = 0; j < 8; j++) {
                    b.rect(cx - 14 * k + (j % 4) * 9 * k, cy - 30 * k + (j / 4) * 9 * k, 9 * k, 9 * k, (j + j / 4) % 2 == 0 ? INK : 0xFFFFFFFF);
                }
                break;
            case Outfits.HD_TROPHY:
                b.shape(cx, cy - 10 * k, 34 * k, 30 * k, 12 * k, 0xFFFFC83D, INK, 2f * k, 0.4f, 0, 0);
                b.rect(cx - 4 * k, cy + 4 * k, 8 * k, 14 * k, 0xFFFFC83D);
                b.roundRect(cx - 14 * k, cy + 18 * k, 28 * k, 10 * k, 3 * k, 0xFF3A3550);
                ui.star(cx, cy - 10 * k, 7 * k, 0xFFFFF0A0);
                break;
            case Outfits.HD_MEGAPHONE:
                b.shape(cx + 4 * k, cy - 4 * k, 44 * k, 20 * k, 6 * k, 0xFFFFFFFF, INK, 2f * k, 0.3f, 0, -0.35f);
                b.circle(cx + 22 * k, cy - 12 * k, 13 * k, 0xFFFF3B4F, INK, 2f * k);
                b.rect(cx - 12 * k, cy + 4 * k, 7 * k, 18 * k, 0xFF3A3550);
                break;
            case Outfits.HD_WAND:
                b.line(cx - 14 * k, cy + 30 * k, cx + 6 * k, cy - 10 * k, 4 * k, 0xFF2A2433);
                ui.star(cx + 10 * k, cy - 18 * k, 16 * k, 0xFFFFD23B);
                b.circle(cx + 26 * k, cy - 32 * k, 3 * k * (1f + (float) Math.sin(t * 6f) * 0.4f), 0xFFFFFFFF);
                break;
            default: // the diamond
                b.shape(cx, cy - 8 * k, 44 * k, 16 * k, 4 * k, 0xFFBFF4FF, INK, 2f * k, 0.5f, 0, 0);
                b.shape(cx, cy + 8 * k, 30 * k, 30 * k, 4 * k, 0xFF6FCBFF, INK, 2f * k, 0.5f, 0, 0.785f);
                b.circle(cx - 10 * k, cy - 10 * k, 3 * k * (1f + (float) Math.sin(t * 5f) * 0.5f), 0xFFFFFFFF);
                break;
        }
    }
}
