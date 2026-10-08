package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.UIBatch;

/** Big dramatic reveal of the special car. */
public final class RevealScreen extends Screen {
    private final Screen next;
    private final Showroom room;
    private float t;
    private boolean played;

    public RevealScreen(Game game, Screen next) {
        super(game);
        this.next = next;
        room = new Showroom(game);
        CarDef d = Cars.ALL[Cars.SECRET];
        room.show(d.id, d.defPaint, d.defAccent, 11, 4, true);
        room.pedestalColor = 0xFFD23F;
        room.pedestalAccent = 0xFF4FA3;
        room.screenLift = 0.1f;
        room.spinSpeed = 1.2f;
    }

    public void enter() {
        game.save.secretSeen = true;
        game.save.markDirty();
        game.save.flush();
    }

    public void update(float dt) {
        t += dt;
        room.update(dt);
        if (!played && t > 0.6f) {
            played = true;
            game.sfx.play(Sfx.UNLOCK, 1f, 1f);
            game.sfx.play(Sfx.QUACK, 1f, 0.8f);
            room.celebrate();
            game.vibrate(250);
        }
        if (t > 0.6f && Math.random() < dt * 4) room.fx.confetti((float) Math.random() * 8 - 4, 5, (float) Math.random() * 4 - 2, 10, 0.8f);
    }

    public boolean render3d() {
        room.render();
        return true;
    }

    public void ui(float dt) {
        UIBatch b = game.b;
        float W = b.width, H = b.height, top = game.safeTop;
        // light rays
        for (int i = 0; i < 12; i++) {
            float a = (float) (i * Math.PI / 6 + t * 0.3f);
            b.shape(W / 2 + (float) Math.cos(a) * 380, H * 0.45f + (float) Math.sin(a) * 380, 760, 70, 35, 0x18FFFFFF, 0, 0, 0, 20, a);
        }
        float k = Ease.outElastic(Math.min(1f, Math.max(0f, t - 0.5f) * 1.2f));
        b.textShadow(b.title, "SECRET", W / 2, top + 150, 120f * k, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 11f, 12f, 0x70200040);
        b.textShadow(b.title, "UNLOCKED!", W / 2, top + 280, 100f * k, 0xFFFF4FA3, UIBatch.CENTER, 0xFF2A1840, 10f, 12f, 0x70200040);
        float k2 = Ease.outBack(Math.min(1f, Math.max(0f, t - 1.4f) * 2f));
        b.textShadow(b.title, Cars.ALL[Cars.SECRET].name, W / 2, H - game.safeBottom - 420, 84f * k2, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 8f, 10f, 0x70200040);
        b.text(b.body, "You found it. Quack responsibly.", W / 2, H - game.safeBottom - 340, 34f * k2, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
        if (t > 2f && game.ui.button("awesome", W / 2 - 250, H - game.safeBottom - 250, 500, 130, 0xFF34D058, "QUACK!", 70f)) {
            game.setScreen(next);
        }
    }

    public boolean back() {
        if (t > 2f) game.setScreen(next);
        return true;
    }
}
