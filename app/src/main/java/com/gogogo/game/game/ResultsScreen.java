package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

import java.util.ArrayList;

/** End of match: placement, coin rewards, XP and level ups, achievements. */
public final class ResultsScreen extends Screen {
    private final Showroom room;
    private final int place, total, rounds, bonks;
    private final boolean won, tied;
    private final int[] rewardVal = new int[5];
    private final String[] rewardName = new String[5];
    private int rewardCount;
    private final int coinsTotal;
    private float t;
    private float shown;
    private int lastTickCoins;
    private boolean secret;
    private boolean newBest;
    /** XP, level ups and achievements from this match. */
    private final Progress.Outcome outcome;

    // XP presentation: the bar fills from the old XP to the new one, pausing for a burst at every level up
    private final float xpStart, xpRate;
    private float xpShown, xpPause;
    private int lvShown;
    private float lvBurstT = -1f;
    private int lastTickXp;
    /** Level rewards revealed so far, and how many of the match's achievements are showing. */
    private final ArrayList<Levels.Reward> unlocked = new ArrayList<Levels.Reward>();
    private int achShown;
    private final ArrayList<Levels.Reward> nextRewards;

    public ResultsScreen(Game game, Match m, boolean early) {
        super(game);
        Car p = m.player;
        Save s = game.save;
        place = Math.max(1, p.place == 0 ? m.alive : p.place);
        total = m.total;
        rounds = p.roundsSurvived;
        bonks = p.bonks;
        won = m.winner == p;
        tied = m.tie && m.tieGroup.contains(p);

        // quitting during the start lights doesn't count as a match
        boolean counts = m.round > 0;
        if (counts) add("Showing up", 20);
        if (rounds > 0) add("Rounds survived x" + rounds, rounds * 6);
        if (bonks > 0) add("Bonks x" + bonks, bonks * 15);
        int bonus = 0;
        String bonusName = null;
        if (won) { bonus = 300; bonusName = "WINNER BONUS"; }
        else if (tied) { bonus = 180; bonusName = "TIE BONUS"; }
        else if (place <= 3) { bonus = 120; bonusName = "Top 3"; }
        else if (place <= 10) { bonus = 70; bonusName = "Top 10"; }
        else if (place <= 25) { bonus = 35; bonusName = "Top 25"; }
        else if (place <= 50) { bonus = 15; bonusName = "Top 50"; }
        if (bonusName != null) add(bonusName, bonus);
        int sum = 0;
        for (int i = 0; i < rewardCount; i++) sum += rewardVal[i];
        coinsTotal = sum;

        s.addCoins(sum);
        newBest = s.matches >= 1 && !won && (s.bestPlace == 0 || place < s.bestPlace);
        Progress.MatchResult r = new Progress.MatchResult();
        r.won = won;
        r.tied = tied;
        r.place = place;
        r.total = total;
        r.rounds = rounds;
        r.bonks = bonks;
        r.boosts = p.boosts;
        r.clutches = p.clutches;
        r.map = m.opt.map;
        r.car = p.def.id;
        r.hoodCam = m.hoodWin;
        r.coins = sum;
        if (counts) {
            outcome = Progress.commitMatch(s, r);
        } else {
            outcome = new Progress.Outcome();
            outcome.oldXp = outcome.newXp = s.xp;
            outcome.oldLevel = outcome.newLevel = Levels.levelFor(s.xp);
        }
        xpShown = outcome.oldXp;
        lvShown = outcome.oldLevel;
        xpRate = Math.max(90f, (outcome.newXp - outcome.oldXp) / 1.5f);
        xpStart = 0.8f + sum / Math.max(120f, sum * 0.9f) + 0.3f;
        nextRewards = Levels.rewardsAt(Math.min(Levels.MAX, outcome.newLevel + 1));
        if (won && m.duckCollected && !s.secretUnlocked()) {
            s.unlockSecret();
            secret = true;
        }
        s.markDirty();
        s.flush();

        room = new Showroom(game);
        room.showSaved(s.selectedCar);
        room.screenY = 0.52f;
        room.screenX = 0.27f;
        room.distance = 15f;
        if (won || tied) room.pedestalColor = 0xFFD23F;
    }

    private void add(String name, int v) {
        rewardName[rewardCount] = name;
        rewardVal[rewardCount] = v;
        rewardCount++;
    }

    public void enter() {
        game.sfx.music(Sfx.MUSIC_MENU);
        if (won || tied) room.celebrate();
    }

    public void update(float dt) {
        t += dt;
        room.update(dt);
        float target = t > 0.8f ? Math.min(coinsTotal, (t - 0.8f) * Math.max(120f, coinsTotal * 0.9f)) : 0;
        shown = target;
        if ((int) shown / 10 != lastTickCoins / 10 && (int) shown < coinsTotal) game.sfx.play(Sfx.COIN, 0.35f, 1f + shown / Math.max(1f, coinsTotal) * 0.6f);
        lastTickCoins = (int) shown;
        if ((won || tied) && Math.random() < dt * 2) room.fx.confetti((float) Math.random() * 6 - 3, 4, (float) Math.random() * 4 - 2, 12, 0.7f);
        updateXp(dt);
    }

    private void updateXp(float dt) {
        if (lvBurstT >= 0) {
            lvBurstT += dt;
            if (lvBurstT > 2.2f) lvBurstT = -1f;
        }
        if (t < xpStart) return;
        if (xpPause > 0) {
            xpPause -= dt;
        } else if (xpShown < outcome.newXp) {
            xpShown = Math.min(outcome.newXp, xpShown + xpRate * dt);
            int x = (int) xpShown;
            if (x / 20 != lastTickXp / 20) game.sfx.play(Sfx.CLICK, 0.3f, 1.2f + Levels.progress(x) * 0.6f);
            lastTickXp = x;
            int lv = Levels.levelFor(x);
            if (lv > lvShown) {
                while (lvShown < lv) Levels.rewardsAt(++lvShown, unlocked);
                lvBurstT = 0f;
                xpPause = 0.7f;
                room.celebrate();
                game.sfx.play(Sfx.UNLOCK, 1f, 1f);
                game.vibrate(80);
            }
        }
        // this match's achievements pop in one after another
        float ta = t - xpStart;
        if (achShown < outcome.achievements.size() && ta > 0.4f + achShown * 0.5f) {
            achShown++;
            game.sfx.play(Sfx.COIN, 0.8f, 1.3f);
        }
    }

    public boolean render3d() {
        room.render();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20, bottom = H - game.safeBottom;
        float lcx = left + (W * 0.5f - left) / 2f; // left column center (car side)

        String head = won ? "WINNER!" : (tied ? "TIE!" : "#" + place);
        int col = won ? 0xFFFFE14D : (tied ? 0xFF3BE0FF : 0xFFFFFFFF);
        float sc = Ease.outElastic(Math.min(1f, t * 1.5f));
        float hs = Math.min(130f, 150f * (W * 0.46f) / Math.max(1f, b.title.width(head, 150f)));
        b.textShadow(b.title, head, lcx, top + 95, hs * sc, col, UIBatch.CENTER, 0xFF2A1840, 12f, 11f, 0x70200040);
        String sub = won ? "Last car rolling!" : (tied ? "Everyone fell. Everyone wins?" : "out of " + total + " cars");
        b.text(b.body, sub, lcx, top + 182, 34f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 5f);
        float cw = TitleScreen.coinPill(game, W - right, top + 52);
        TitleScreen.rimsPill(game, W - right - cw - 14, top + 52);
        if (newBest && t > 1f) {
            float k2 = Ease.outBack(Math.min(1f, (t - 1f) * 3f));
            float bx = lcx + Math.min(260f, b.title.width(head, hs) / 2 + 80);
            b.shape(bx, top + 40, 170 * k2, 54 * k2, 27, 0xFFFF3B5C, 0xFFFFFFFF, 4f, 0.4f, 0, 0f);
            b.text(b.title, "NEW BEST!", bx, top + 42, 27f * k2, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        }

        xpCard(b, lcx, bottom - 124, Math.min(560f, W * 0.5f - left - 40));
        if (lvBurstT >= 0) levelBurst(b, game.ui, lcx, bottom - 172);

        // reward panel (right column)
        float rx = W * 0.5f + 10, rw = W - right - rx;
        float pw = Math.min(640f, rw), px = rx + (rw - pw) / 2;
        float py = top + 96;
        float by = bottom - 214;
        // squeeze the rows on short screens so a row of NEW STUFF chips still fits under the panel
        float rowH = Math.max(32f, Math.min(42f, (by - 14 - py - 14 - (52 + CHIP_H + 18) - 96) / Math.max(1, rewardCount)));
        float ph = 30 + rewardCount * rowH + 66;
        float k = Ease.outBack(Math.min(1f, Math.max(0f, (t - 0.3f) * 2.5f)));
        float sx = (1 - k) * 700;
        ui.panel(px + sx, py, pw, ph, 0xFFFFFFFF);
        for (int i = 0; i < rewardCount; i++) {
            float y = py + 36 + i * rowH;
            String v = "+" + rewardVal[i];
            b.textFit(b.body, rewardName[i], px + sx + 36, y, 30f, pw - 128 - b.title.width(v, 34f), 0xFF2A1840, UIBatch.LEFT, 0, 0);
            b.text(b.title, v, px + sx + pw - 76, y, 34f, 0xFFE89A00, UIBatch.RIGHT, 0, 0);
            ui.coin(px + sx + pw - 48, y, 15);
        }
        float ty = py + ph - 38;
        b.roundRect(px + sx + 28, ty - 28, pw - 56, 4, 2, 0x202A1840);
        b.text(b.title, "TOTAL", px + sx + 36, ty, 40f, 0xFF8E62FF, UIBatch.LEFT, 0, 0);
        b.text(b.title, "+" + (int) shown, px + sx + pw - 76, ty, 46f, 0xFFE89A00, UIBatch.RIGHT, 0xFF2A1840, 3f);
        ui.coin(px + sx + pw - 44, ty, 21);

        float bw = Math.min(600f, rw);
        float bx = rx + (rw - bw) / 2;
        float ny = py + ph + 14;
        if (by - 14 - ny >= 52 + CHIP_H + 12) newPanel(b, ui, px, ny, pw, Math.min(by - 14 - ny, newPanelHeight(b, pw)));

        if (ui.button("again", bx, by, bw, 108, 0xFF34D058, "PLAY AGAIN", 58f)) {
            leave(new MatchScreen(game));
        }
        if (ui.button("menu", bx, by + 122, bw / 2 - 10, 86, 0xFF8E62FF, "MENU", 42f)) {
            leave(new TitleScreen(game));
        }
        if (ui.button("garage", bx + bw / 2 + 10, by + 122, bw / 2 - 10, 86, 0xFFFF9A2B, "GARAGE", 42f)) {
            leave(new GarageScreen(game));
        }
    }

    /** Level seal, "+XP" counter and the filling XP bar, under the car. */
    private void xpCard(UIBatch b, float cx, float y, float w) {
        float h = 104, x = cx - w / 2;
        float k = Ease.outBack(Math.min(1f, Math.max(0f, (t - 0.5f) * 2.5f)));
        y += (1f - k) * 200;
        b.shadow(x, y + 8, w, h, 34, 0x50200040, 10);
        b.shape(cx, y + h / 2, w, h, 34, 0xE62A1840, 0, 0, 0, 0, 0);
        int lv = lvShown;
        boolean max = lv >= Levels.MAX;
        float pop = lvBurstT >= 0 ? 1f + 0.4f * (float) Math.sin(Math.min(1f, lvBurstT * 2.5f) * Math.PI) : 1f;
        float tx = x + 124, tw = w - 124 - 28;
        String lt = max ? "MAX LEVEL" : "LEVEL " + lv;
        b.textFit(b.title, lt, tx, y + 34, 32f, tw * 0.55f, 0xFFFFFFFF, UIBatch.LEFT, 0, 0);
        int gained = Math.max(0, (int) xpShown - outcome.oldXp);
        if (t > xpStart - 0.2f) b.text(b.title, "+" + gained + " XP", x + w - 28, y + 34, 32f, 0xFF5EE65A, UIBatch.RIGHT, 0, 0);
        float fill = max ? 1f : (xpShown - Levels.xpAt(lv)) / (float) Levels.xpToNext(lv);
        TitleScreen.xpBar(b, tx, y + 72, tw, 24, fill, max ? 0xFFFFC21F : 0xFF5EE65A);
        TitleScreen.levelSeal(game, x + 62, y + h / 2, 40 * pop, lv, max ? 0xFFFF9A2B : 0xFFFFC21F);
    }

    /** Big "LEVEL UP!" with stars flying out. */
    private void levelBurst(UIBatch b, UI ui, float cx, float cy) {
        float a = lvBurstT > 1.7f ? Math.max(0f, (2.2f - lvBurstT) / 0.5f) : 1f;
        b.alpha(a);
        float sk = Math.min(1f, lvBurstT / 0.9f);
        for (int i = 0; i < 16; i++) {
            float ang = i * 0.3927f + 0.2f;
            float d = Ease.outCubic(sk) * (170 + (i % 3) * 60);
            ui.star(cx + (float) Math.cos(ang) * d * 1.4f, cy + (float) Math.sin(ang) * d * 0.7f, (14 + (i % 4) * 5) * (1f - sk * 0.5f),
                    i % 3 == 0 ? 0xFFFFE14D : (i % 3 == 1 ? 0xFFFF4FA3 : 0xFF3BE0FF));
        }
        float k = Ease.outElastic(Math.min(1f, lvBurstT * 1.4f));
        b.textShadow(b.title, "LEVEL UP!", cx, cy, 92f * k, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 10f, 10f, 0x70200040);
        b.alpha(1f);
    }

    // chip layout cursor for newPanel
    private float chipX, chipY, chipX0, chipMaxX, chipBottom;
    private static final float CHIP_H = 62;

    /** Height newPanel() needs for everything it has to show at the moment. */
    private float newPanelHeight(UIBatch b, float pw) {
        boolean any = achShown > 0 || !unlocked.isEmpty();
        if (!any && lvShown >= Levels.MAX) return 130;
        float x0 = 20, maxX = pw - 20, x = x0;
        int rows = 1;
        int n = any ? achShown + unlocked.size() : nextRewards.size();
        for (int i = 0; i < n; i++) {
            float w;
            if (!any) w = ProfileScreen.rewardChipWidth(game, nextRewards.get(i), CHIP_H);
            else if (i < achShown) w = achChipWidth(b, outcome.achievements.get(i));
            else w = ProfileScreen.rewardChipWidth(game, unlocked.get(i - achShown), CHIP_H);
            w = Math.min(maxX - x0, w);
            if (x + w > maxX && x > x0) {
                rows++;
                x = x0;
            }
            x += w + 10;
        }
        return 56 + rows * (CHIP_H + 10) + 4;
    }

    /** Starts a new chip row if needed; false if the chip no longer fits in the panel. */
    private boolean placeChip(float w) {
        if (chipX + w > chipMaxX && chipX > chipX0) {
            chipX = chipX0;
            chipY += CHIP_H + 10;
        }
        return chipY + CHIP_H / 2 <= chipBottom;
    }

    /** What this match unlocked (level rewards, achievements), or what the next level brings. */
    private void newPanel(UIBatch b, UI ui, float px, float py, float pw, float ph) {
        boolean any = achShown > 0 || !unlocked.isEmpty();
        boolean xpDone = t > xpStart && xpShown >= outcome.newXp && xpPause <= 0;
        if (!any && !xpDone) return;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        chipX0 = chipX = px + 20;
        chipMaxX = px + pw - 20;
        chipY = py + 52 + CHIP_H / 2;
        chipBottom = py + ph - 8;
        if (!any) {
            if (lvShown >= Levels.MAX) {
                b.textFit(b.title, "LEGENDARY!", px + pw / 2, py + ph / 2 - 16, 40f, pw - 40, 0xFFFFB321, UIBatch.CENTER, 0xFF2A1840, 3f);
                b.textFit(b.body, "You reached the max level. Respect.", px + pw / 2, py + ph / 2 + 24, 26f, pw - 40, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
                return;
            }
            b.text(b.title, "NEXT UP: LEVEL " + (lvShown + 1), px + 24, py + 26, 28f, 0xFF8E62FF, UIBatch.LEFT, 0, 0);
            b.alpha(0.8f);
            for (int i = 0; i < nextRewards.size(); i++) {
                Levels.Reward r = nextRewards.get(i);
                float w = Math.min(chipMaxX - chipX0, ProfileScreen.rewardChipWidth(game, r, CHIP_H));
                if (!placeChip(w)) break;
                chipX += ProfileScreen.rewardChip(game, r, chipX, chipY, CHIP_H, w, t) + 10;
            }
            b.alpha(1f);
            return;
        }
        b.text(b.title, "NEW STUFF!", px + 24, py + 26, 28f, 0xFFFF4FA3, UIBatch.LEFT, 0, 0);
        int total = achShown + unlocked.size(), drawn = 0;
        for (int i = 0; i < achShown; i++) {
            int a = outcome.achievements.get(i);
            float w = Math.min(chipMaxX - chipX0, achChipWidth(b, a));
            if (!placeChip(w)) break;
            achChip(b, ui, a, chipX, chipY, w);
            chipX += w + 10;
            drawn++;
        }
        if (drawn == achShown) {
            for (int i = 0; i < unlocked.size(); i++) {
                Levels.Reward r = unlocked.get(i);
                float w = Math.min(chipMaxX - chipX0, ProfileScreen.rewardChipWidth(game, r, CHIP_H));
                if (!placeChip(w)) break;
                chipX += ProfileScreen.rewardChip(game, r, chipX, chipY, CHIP_H, w, t) + 10;
                drawn++;
            }
        }
        if (drawn < total) b.text(b.title, "+" + (total - drawn) + " MORE", px + pw - 24, py + 26, 26f, 0xFF8E62FF, UIBatch.RIGHT, 0, 0);
    }

    private float achChipWidth(UIBatch b, int a) {
        float tw = Math.max(b.title.width(Achievements.name(a), CHIP_H * 0.38f), b.body.width("ACHIEVEMENT", CHIP_H * 0.27f));
        return CHIP_H * 0.95f + tw + 22;
    }

    private void achChip(UIBatch b, UI ui, int a, float x, float cy, float w) {
        float h = CHIP_H;
        b.shape(x + w / 2, cy + 3, w, h, h * 0.3f, 0x30200040, 0, 0, 0, 3, 0);
        b.shape(x + w / 2, cy, w, h, h * 0.3f, 0xFFFFF0D0, 0xFFFFB321, 3f, 0.15f, 0, 0);
        ui.medal(x + h * 0.5f, cy - h * 0.06f, h * 0.27f, 0xFFFFB321);
        float tx = x + h * 0.95f, aw = w - h * 0.95f - 12;
        b.textFit(b.body, "ACHIEVEMENT", tx, cy - h * 0.2f, h * 0.27f, aw, 0xFF9A8AB0, UIBatch.LEFT, 0, 0);
        b.textFit(b.title, Achievements.name(a), tx, cy + h * 0.14f, h * 0.38f, aw, 0xFF2A1840, UIBatch.LEFT, 0, 0);
    }

    /** Leaving before the show is over: whatever wasn't presented yet becomes regular pop-ups. */
    private void announceRest() {
        Progress.Outcome o = new Progress.Outcome();
        o.oldLevel = lvShown;
        o.newLevel = outcome.newLevel;
        for (int l = lvShown + 1; l <= outcome.newLevel; l++) Levels.rewardsAt(l, o.rewards);
        for (int i = achShown; i < outcome.achievements.size(); i++) o.achievements.add(outcome.achievements.get(i));
        if (o.newLevel > o.oldLevel || !o.achievements.isEmpty()) Progress.announce(o);
        lvShown = outcome.newLevel;
        achShown = outcome.achievements.size();
    }

    private void leave(Screen next) {
        announceRest();
        if (secret) game.setScreen(new RevealScreen(game, next));
        else game.setScreen(next);
    }

    /** Pop-ups left over from earlier wait until this screen's own XP show is over. */
    public boolean allowNotes() {
        return t > xpStart && xpShown >= outcome.newXp && xpPause <= 0 && achShown >= outcome.achievements.size();
    }

    public boolean back() {
        leave(new TitleScreen(game));
        return true;
    }
}
