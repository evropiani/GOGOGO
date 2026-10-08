package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** End of match: placement, coin rewards, (very rarely) a secret unlock. */
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

        add("Showing up", 20);
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
        s.matches++;
        if (won) s.wins++;
        if (tied) s.ties++;
        if (s.bestPlace == 0 || place < s.bestPlace) {
            newBest = s.matches > 1 && !won;
            s.bestPlace = place;
        }
        s.bonks += bonks;
        s.roundsSurvived += rounds;
        // the duck knows
        if (won && m.duckCollected && !s.secretUnlocked()) {
            s.unlockSecret();
            secret = true;
        }
        s.markDirty();
        s.flush();

        room = new Showroom(game);
        room.showSaved(s.selectedCar);
        room.screenY = 0.36f;
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
    }

    public boolean render3d() {
        room.render();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;

        String head = won ? "WINNER!" : (tied ? "TIE!" : "#" + place);
        int col = won ? 0xFFFFE14D : (tied ? 0xFF3BE0FF : 0xFFFFFFFF);
        float sc = Ease.outElastic(Math.min(1f, t * 1.5f));
        float hs = Math.min(130f, 150f * (W - 200) / Math.max(1f, b.title.width(head, 150f)));
        b.textShadow(b.title, head, W / 2, top + 175, hs * sc, col, UIBatch.CENTER, 0xFF2A1840, 13f, 12f, 0x70200040);
        String sub = won ? "Last car rolling!" : (tied ? "Everyone fell. Everyone wins?" : "out of " + total + " cars");
        b.text(b.body, sub, W / 2, top + 270, 36f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 5f);
        TitleScreen.coinPill(game, W - 24, top + 52);
        if (newBest && t > 1f) {
            float k2 = Ease.outBack(Math.min(1f, (t - 1f) * 3f));
            b.shape(W / 2 + 150, top + 110, 170 * k2, 56 * k2, 28, 0xFFFF3B5C, 0xFFFFFFFF, 4f, 0.4f, 0, 0f);
            b.text(b.title, "NEW BEST!", W / 2 + 150, top + 112, 28f * k2, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
        }

        // reward panel
        float pw = 620, ph = 120 + rewardCount * 56 + 40;
        float px = W / 2 - pw / 2, py = H - game.safeBottom - 300 - ph;
        float k = Ease.outBack(Math.min(1f, Math.max(0f, (t - 0.3f) * 2.5f)));
        py += (1 - k) * 600;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        for (int i = 0; i < rewardCount; i++) {
            float y = py + 50 + i * 56;
            b.text(b.body, rewardName[i], px + 40, y, 34f, 0xFF2A1840, UIBatch.LEFT, 0, 0);
            b.text(b.title, "+" + rewardVal[i], px + pw - 80, y, 38f, 0xFFE89A00, UIBatch.RIGHT, 0, 0);
            ui.coin(px + pw - 50, y, 16);
        }
        float ty = py + ph - 60;
        b.roundRect(px + 30, ty - 34, pw - 60, 4, 2, 0x202A1840);
        b.text(b.title, "TOTAL", px + 40, ty, 44f, 0xFF8E62FF, UIBatch.LEFT, 0, 0);
        b.text(b.title, "+" + (int) shown, px + pw - 80, ty, 52f, 0xFFE89A00, UIBatch.RIGHT, 0xFF2A1840, 3f);
        ui.coin(px + pw - 46, ty, 22);

        float by = H - game.safeBottom - 250;
        if (ui.button("again", W / 2 - 300, by, 600, 130, 0xFF34D058, "PLAY AGAIN", 64f)) {
            leave(new MatchScreen(game));
        }
        if (ui.button("menu", W / 2 - 300, by + 150, 290, 100, 0xFF8E62FF, "MENU", 44f)) {
            leave(new TitleScreen(game));
        }
        if (ui.button("garage", W / 2 + 10, by + 150, 290, 100, 0xFFFF9A2B, "GARAGE", 44f)) {
            leave(new GarageScreen(game));
        }
    }

    private void leave(Screen next) {
        if (secret) game.setScreen(new RevealScreen(game, next));
        else game.setScreen(next);
    }

    public boolean back() {
        leave(new TitleScreen(game));
        return true;
    }
}
