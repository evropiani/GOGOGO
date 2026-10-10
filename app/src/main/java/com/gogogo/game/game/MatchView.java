package com.gogogo.game.game;

import com.gogogo.game.engine.Camera;
import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Mesh;
import com.gogogo.game.engine.Renderer;
import com.gogogo.game.engine.Rng;
import com.gogogo.game.engine.UIBatch;

/** Draws a {@link Match} in 3D and turns its events into particles, sounds and shakes. */
public final class MatchView implements Match.Listener {
    private final Game game;
    public Match match;
    public final Particles fx = new Particles();
    public boolean quiet; // title-screen background: no sounds / shakes
    /** The car whose hood the camera sits on: drawn without topper and markers. */
    public Car hoodCar;
    /** The car the camera follows (its held power-up icon sits lower, out of the line of sight). */
    public Car camCar;
    /** Arena skin and sky (see Skins, Skies). */
    public int skin, sky;
    /** Pulls the sky decorations closer and shrinks them (small previews without a chase camera). */
    public float decorReach = 1f, decorSize = 1f;

    private final float[] base = new float[16], m = new float[16], tmp = new float[16];
    private float time;

    // sky decorations: placed once per sky, moved by formulas of time (see drawDecor)
    private static final int DECOR_MAX = 96;
    private final float[] dAng = new float[DECOR_MAX], dRad = new float[DECOR_MAX], dY = new float[DECOR_MAX];
    private final float[] dSize = new float[DECOR_MAX], dPhase = new float[DECOR_MAX], dSpeed = new float[DECOR_MAX];
    private final int[] dKind = new int[DECOR_MAX], dColor = new int[DECOR_MAX], dColor2 = new int[DECOR_MAX];
    private int decorCount, decorSky = -1;

    // world-space popups ("BONK!")
    private static final int POPS = 24;
    private final String[] popText = new String[POPS];
    private final float[] popX = new float[POPS], popY = new float[POPS], popZ = new float[POPS], popT = new float[POPS];
    private final int[] popColor = new int[POPS];
    private int popCursor;
    private final float[] proj = new float[4];

    public MatchView(Game game, Match match) {
        this.game = game;
        setMatch(match);
    }

    public void setMatch(Match match) {
        this.match = match;
        match.listener = this;
        fx.clear();
        for (int i = 0; i < POPS; i++) popT[i] = 0;
        for (int i = 0; i < WAVES; i++) waveT[i] = -1f;
    }

    public void pop(String text, float x, float y, float z, int color) {
        int i = popCursor;
        popCursor = (popCursor + 1) % POPS;
        popText[i] = text;
        popX[i] = x;
        popY[i] = y;
        popZ[i] = z;
        popT[i] = 1f;
        popColor[i] = color;
    }

    public void update(float dt) {
        time += dt;
        fx.update(dt);
        for (int i = 0; i < POPS; i++) if (popT[i] > 0) popT[i] -= dt * 0.9f;
        // boost trails
        for (Car c : match.cars) {
            if (c.boostT > 0 && !c.falling && Math.random() < 0.6) {
                float fx0 = (float) Math.sin(c.yaw), fz0 = (float) Math.cos(c.yaw);
                Trails.emit(fx, c.trail, c.x - fx0 * 1.3f, c.y + 0.6f, c.z - fz0 * 1.3f, -fx0, -fz0, time);
            }
        }
        if (skin == Skins.GOLDEN) sparkleTiles();
        powerFx(dt);
    }

    /** GOLDEN GLORY: twinkles popping up on tiles around the camera. */
    private void sparkleTiles() {
        Arena a = match.arena;
        int tries = a.n > 10 ? 2 : 1;
        for (int k = 0; k < tries; k++) {
            if (Math.random() > 0.45) continue;
            float px = game.cam.tx + (float) (Math.random() * 2 - 1) * 26f;
            float pz = game.cam.tz + (float) (Math.random() * 2 - 1) * 26f;
            Arena.Tile t = a.cellAt(px, pz);
            if (t == null || t.state != Arena.PRESENT) continue;
            fx.sparkle(t.x + (float) (Math.random() * 3.4 - 1.7), t.y + 0.25f, t.z + (float) (Math.random() * 3.4 - 1.7),
                    0.32f + (float) Math.random() * 0.2f, Math.random() < 0.7 ? 0xFFE27A : 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ events

    public void event(int type, Car a, Car b, float v) {
        Sfx s = game.sfx;
        Car p = match.player;
        switch (type) {
            case Match.EV_BUMP: {
                float mx = (a.x + b.x) / 2, mz = (a.z + b.z) / 2;
                boolean near = isNear(mx, mz, 30f);
                if (v > 9f) {
                    fx.stars(mx, 1.6f, mz, 5);
                    fx.bonk(mx, 2.2f, mz, 1.4f);
                    if (near && !quiet) s.play(Sfx.BONK, Math.min(1f, v / 14f), 0.9f + (float) Math.random() * 0.3f);
                } else {
                    fx.puff(mx, 0.6f, mz, 3, 0xFFFFFF, 2f);
                    if (near && !quiet) s.play(Sfx.BUMP, Math.min(0.8f, v / 10f), 0.85f + (float) Math.random() * 0.4f);
                }
                if ((a == p || b == p) && !quiet) {
                    game.shake(Math.min(0.9f, v * 0.06f), 0.3f);
                    game.vibrate(v > 9f ? 40 : 18);
                    if (v > 9f) pop(BONK_WORDS[(int) (Math.random() * BONK_WORDS.length)], mx, 3.2f, mz, 0xFFFFE14D);
                }
                break;
            }
            case Match.EV_BOOST:
                if (!quiet && isNear(a.x, a.z, 25f)) s.play(Sfx.BOOST, a == p ? 0.9f : 0.35f, 0.9f + (float) Math.random() * 0.2f);
                Trails.burst(fx, a.trail, a.x, a.y + 0.5f, a.z);
                break;
            case Match.EV_ELIM:
                if (!quiet) {
                    if (a == p) {
                        s.play(Sfx.FALL, 1f, 1f);
                        game.vibrate(120);
                        game.shake(0.8f, 0.5f);
                    } else if (isNear(a.x, a.z, 22f)) {
                        s.play(Sfx.FALL, 0.4f, 0.9f + (float) Math.random() * 0.4f);
                    }
                }
                if (isNear(a.x, a.z, 40f) && Math.random() < 0.35) pop(BYE_WORDS[(int) (Math.random() * BYE_WORDS.length)], a.x, 2.5f, a.z, 0xFFFFFFFF);
                break;
            case Match.EV_BONK:
                if (a == p && !quiet) {
                    pop("BONKED " + b.name.toUpperCase() + "!", a.x, 3.5f, a.z, 0xFFFF4FA3);
                    s.play(Sfx.COIN, 0.8f, 1.2f);
                }
                break;
            case Match.EV_DROP:
                if (!quiet) {
                    s.play(Sfx.DROP, 0.9f, 1f);
                    game.shake(0.35f, 0.5f);
                    game.vibrate(30);
                }
                break;
            case Match.EV_LAND:
                if (!quiet) s.play(Sfx.LAND, Math.min(0.5f, 0.15f + v * 0.02f), 0.8f + (float) Math.random() * 0.5f);
                break;
            case Match.EV_WIN:
                if (a != null) fx.confetti(a.x, 3f, a.z, 140, 1.4f);
                break;
            case Match.EV_TIE:
                if (p != null) fx.confetti(p.x, 3f, p.z, 80, 1.2f);
                break;
            case Match.EV_PICKUP:
            case Match.EV_POWER:
            case Match.EV_SPLAT:
            case Match.EV_FREEZE:
            case Match.EV_THAW:
            case Match.EV_LANDED:
            case Match.EV_SHOCK:
            case Match.EV_RESCUE:
                powerEvent(type, a, b, v);
                break;
            case Match.EV_DUCK_GET:
                if (!quiet) {
                    s.play(Sfx.QUACK, 1f, 1f);
                    game.vibrate(25);
                }
                fx.puff(match.duckX, 1f, match.duckZ, 8, 0xFFE03B, 2.5f);
                break;
            default:
                break;
        }
    }

    private static final String[] BONK_WORDS = {"BONK!", "BOING!", "WHAM!", "POW!", "BOP!", "THWACK!", "BAM!"};
    private static final String[] BYE_WORDS = {"BYE!", "NOOO!", "WHEEE!", "OOPS!", "AAAH!", "CYA!", "YEET!", "UH OH!"};

    private boolean isNear(float x, float z, float r) {
        float cx = game.cam.tx, cz = game.cam.tz;
        return (x - cx) * (x - cx) + (z - cz) * (z - cz) < r * r;
    }

    // ------------------------------------------------------------------ drawing

    public void draw() {
        Renderer r = game.r;
        Art art = game.art;
        Arena a = match.arena;
        boolean symbols = game.save.symbols;

        drawDecor(r, art, a);

        Mesh skinMesh = art.tileSkins[skin], glowMesh = art.tileGlow[skin];
        boolean jelly = skin == Skins.JELLY;
        float symFlash = Skins.symbolFlash(skin);
        boolean warn = match.phase == Match.SHOW && match.timer < 0.9f;
        for (Arena.Tile t : a.tiles) {
            if (t.state == Arena.GONE) continue;
            if (t.state == Arena.SPAWNING && t.delay > 0) continue;
            float y = t.y;
            float sq = Ease.outBounce(1f - t.squash) - 1f; // -1..0
            float flipHop = (float) Math.sin(t.flip * Math.PI) * 0.7f;
            float shakeX = 0, shakeZ = 0;
            if (warn && t.color != match.target && t.state == Arena.PRESENT) {
                shakeX = (float) Math.sin(time * 60f + t.index) * 0.08f;
                shakeZ = (float) Math.cos(time * 53f + t.index * 1.3f) * 0.08f;
            }
            float sy = 1f + sq * -0.35f;
            float sxz = 1f - sq * -0.08f;
            if (jelly) {
                float w = (float) Math.sin(time * 4.2f + t.gx * 0.9f + t.gz * 0.7f);
                sy *= 1f + w * 0.07f;
                sxz *= 1f - w * 0.012f;
            }
            int colorIdx = t.flip > 0.5f ? t.prevColor : t.color;
            int col = Palette.TILE[colorIdx % Palette.TILE.length];
            M4.trs(m, t.x + shakeX, y + flipHop, t.z + shakeZ, 0, t.rotX, t.rotZ, sxz, sy, sxz);
            r.draw(skinMesh, m, col, col, 0f);
            if (glowMesh != null) r.draw(glowMesh, m, col, col, Skins.glow(skin, t.x, t.z, time));
            if (symbols) {
                M4.trs(tmp, 0, 0, 0, 0, 0, 0, 1.15f, 1f, 1.15f);
                M4.mul(tmp, m, tmp);
                r.draw(art.symbols[colorIdx % 6], tmp, Palette.mix(col, 0xFFFFFF, 0.45f), 0, symFlash);
            }
        }

        if (match.duckTile >= 0 && !match.duckCollected && !match.duckLost) {
            Arena.Tile dt = a.tiles[match.duckTile];
            float dy = (dt.state == Arena.FALLING || dt.state == Arena.GONE) ? match.duckY : dt.y;
            M4.trs(m, match.duckX, dy + 0.02f + Math.abs((float) Math.sin(time * 3f)) * 0.12f, match.duckZ, time * 0.8f, 0, 0, 1.1f, 1.1f, 1.1f);
            r.draw(art.duck, m, 0xFFFFFF);
            if (dy > -1f) r.blob(match.duckX, dy + 0.03f, match.duckZ, 0.6f, 1f, 0, 0.35f);
        }

        for (Car c : match.cars) {
            if (c.y < -85f) continue;
            drawCar(c, c == match.player && c.alive);
        }
        drawPowerUps(r, art, a);

        fx.faceYaw = (float) Math.atan2(game.cam.ex - game.cam.tx, game.cam.ez - game.cam.tz);
        fx.draw(r, art);
    }

    public void drawCar(Car c, boolean marker) {
        Renderer r = game.r;
        Art art = game.art;
        CarDef d = c.def;
        CarRenderer.draw(r, art, c, time, base, m, c != hoodCar);
        if (c == hoodCar) marker = false;

        if (c.frozenT > 0) drawIce(r, art, c, c == hoodCar);
        if (c.power >= 0 && c != hoodCar && !c.falling) drawHeld(r, art, c);

        // shadow on the floor (in a super jump it stays, so you can see where you come down)
        Arena.Tile t = match.arena.cellAt(c.x, c.z);
        boolean floor = t != null && t.state != Arena.GONE && !(t.state == Arena.SPAWNING && t.delay > 0);
        if (floor) {
            float h = c.y + c.hop - t.y;
            if (h > -0.6f) {
                float alpha = c.airborne ? 0.4f : 0.45f * Math.max(0f, 1f - h / 6f);
                float rad = c.airborne ? 1.35f * Math.max(0.55f, 1f - h / 25f) : 1.35f;
                r.blob(c.x, t.y + 0.04f, c.z, rad, 1.35f, c.yaw, Math.min(0.45f, alpha));
            }
        }
        if (c.airborne && c.y > 1.5f) {
            // a dotted line down to the floor under the car
            float floorY = floor ? t.y : 0f;
            float top = c.y - 0.2f;
            int dots = Math.min(10, (int) ((top - floorY) / 1.3f) - 1);
            for (int k = 1; k <= dots; k++) {
                float dy = top - (k - 1) * 1.3f - (time * 3f) % 1.3f;
                M4.trs(m, c.x, dy, c.z, 0, 0, 0, 0.13f, 0.13f, 0.13f);
                r.draw(art.ball, m, PowerUps.COLOR[PowerUps.JUMP], 0, 0.35f);
            }
        }

        if (marker) {
            M4.trs(m, c.x, c.y + d.topY + 2.6f + (float) Math.abs(Math.sin(time * 5f)) * 0.6f, c.z, time * 2.5f, 0, 0, 1.3f, 1.3f, 1.3f);
            r.draw(art.marker, m, 0xFFE14D, 0xFFE14D, 0.3f);
            if (t != null && !c.falling && (!c.airborne || (floor && t.state != Arena.FALLING))) {
                float pulse = 1.55f + (float) Math.sin(time * 6f) * 0.08f;
                M4.trs(m, c.x, t.y + 0.12f, c.z, 0, 0, 0, pulse, 1f, pulse);
                r.draw(art.ring, m, 0xFFE14D, 0xFFE14D, 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------ power-ups

    // shockwave rings (super bump, super jump take-off and touchdown, freezing)
    private static final int WAVES = 12;
    private final float[] waveX = new float[WAVES], waveY = new float[WAVES], waveZ = new float[WAVES];
    private final float[] waveT = new float[WAVES], waveDur = new float[WAVES], waveR = new float[WAVES];
    private final int[] waveColor = new int[WAVES];
    private int waveCursor;

    private void wave(float x, float y, float z, float radius, float dur, int color) {
        int i = waveCursor;
        waveCursor = (waveCursor + 1) % WAVES;
        waveX[i] = x;
        waveY[i] = y;
        waveZ[i] = z;
        waveR[i] = radius;
        waveDur[i] = dur;
        waveT[i] = 0f;
        waveColor[i] = color;
    }

    private void powerEvent(int type, Car a, Car b, float v) {
        Sfx s = game.sfx;
        Car p = match.player;
        PowerUps pw = match.power;
        switch (type) {
            case Match.EV_PICKUP: {
                int col = PowerUps.COLOR[(int) v];
                for (int k = 0; k < 6; k++) {
                    fx.sparkle(pw.evX + rnd(1.2f), pw.evY + rnd(0.6f), pw.evZ + rnd(1.2f), 0.5f, col);
                }
                fx.puff(pw.evX, pw.evY, pw.evZ, 5, 0xFFFFFF, 3f);
                if (a == p && !quiet) {
                    s.play(Sfx.PICKUP, 0.9f, 1f);
                    game.vibrate(20);
                    pop(PowerUps.GOT[(int) v], a.x, 3.6f, a.z, 0xFF000000 | col);
                } else if (!quiet && isNear(a.x, a.z, 20f)) {
                    s.play(Sfx.PICKUP, 0.3f, 1.1f);
                }
                break;
            }
            case Match.EV_POWER:
                powerUsed(a, (int) v);
                break;
            case Match.EV_SPLAT: {
                boolean ice = v == PowerUps.ICE;
                boolean near = isNear(pw.evX, pw.evZ, 26f);
                if (b == null) {
                    // missed: a puff of snow, or the ice block shatters on the floor
                    if (ice) iceBurst(pw.evX, pw.evY + 0.3f, pw.evZ, 10);
                    else fx.puff(pw.evX, pw.evY, pw.evZ, 6, 0xF4FAFF, 2.5f);
                    if (near && !quiet) s.play(ice ? Sfx.FREEZE : Sfx.SPLAT, 0.35f, ice ? 1.5f : 1.2f);
                    break;
                }
                fx.puff(pw.evX, pw.evY, pw.evZ, 12, 0xF4FAFF, 5f);
                for (int k = 0; k < 10; k++) {
                    int i = fx.spawn(Particles.BALL, pw.evX, pw.evY, pw.evZ, rnd(7f), 3f + (float) Math.random() * 5f, rnd(7f),
                            0.22f + (float) Math.random() * 0.2f, 0.6f + (float) Math.random() * 0.3f, 0xFFFFFF, 18f);
                    fx.drag(i, 1.5f);
                }
                if (near && !quiet) s.play(Sfx.SPLAT, b == p || a == p ? 1f : 0.5f, 0.9f + (float) Math.random() * 0.2f);
                if (b == p && !quiet) {
                    pop("SPLAT!", b.x, 3.4f, b.z, 0xFFFFFFFF);
                    game.shake(0.5f, 0.3f);
                    game.vibrate(40);
                } else if (a == p && !quiet) {
                    pop("SPLAT!", b.x, 3.2f, b.z, 0xFFFFE14D);
                }
                break;
            }
            case Match.EV_FREEZE: {
                iceBurst(b.x, b.y + 1f, b.z, 14);
                wave(b.x, b.y + 0.1f, b.z, 2.6f, 0.4f, CarRenderer.ICE_TINT);
                if (!quiet && isNear(b.x, b.z, 26f)) s.play(Sfx.FREEZE, b == p || a == p ? 1f : 0.5f, 1f);
                if (b == p && !quiet) {
                    pop("FROZEN!", b.x, 3.6f, b.z, 0xFF7FE2FF);
                    game.shake(0.4f, 0.3f);
                    game.vibrate(60);
                } else if (a == p && !quiet) {
                    pop("FROZEN!", b.x, 3.2f, b.z, 0xFF7FE2FF);
                }
                break;
            }
            case Match.EV_THAW:
                iceBurst(a.x, a.y + 1f, a.z, 12);
                if (!quiet && isNear(a.x, a.z, 20f)) s.play(Sfx.FREEZE, a == p ? 0.6f : 0.25f, 1.6f);
                break;
            case Match.EV_LANDED:
                fx.puff(a.x, 0.3f, a.z, 8, 0xFFFFFF, 4f);
                wave(a.x, a.y + 0.1f, a.z, 3.2f, 0.35f, 0xFFFFFF);
                if (!quiet && isNear(a.x, a.z, 26f)) s.play(Sfx.THUD, a == p ? 1f : 0.45f, 0.9f + (float) Math.random() * 0.2f);
                if (a == p && !quiet) {
                    game.shake(Math.min(0.7f, v * 0.03f), 0.35f);
                    game.vibrate(35);
                }
                break;
            case Match.EV_SHOCK:
                fx.stars(b.x, 1.6f, b.z, 3);
                if (b == p && !quiet) {
                    pop("WHOA!", b.x, 3.4f, b.z, 0xFFFFB03B);
                    game.shake(0.6f, 0.35f);
                    game.vibrate(50);
                }
                break;
            case Match.EV_RESCUE:
                if (a == p && !quiet) pop("SAVED!", a.x, 2.5f, a.z, 0xFF5EE65A);
                break;
            default:
                break;
        }
    }

    private void powerUsed(Car a, int type) {
        Sfx s = game.sfx;
        boolean me = a == match.player;
        boolean near = isNear(a.x, a.z, 26f) && !quiet;
        float vol = me ? 1f : 0.45f;
        float fx0 = (float) Math.sin(a.yaw), fz0 = (float) Math.cos(a.yaw);
        switch (type) {
            case PowerUps.SNOWBALL:
                fx.puff(a.x + fx0 * 1.6f, 1.3f, a.z + fz0 * 1.6f, 4, 0xF4FAFF, 2f);
                if (near) s.play(Sfx.THROW, vol, 1f + (float) Math.random() * 0.15f);
                break;
            case PowerUps.ICE:
                for (int k = 0; k < 5; k++) fx.sparkle(a.x + fx0 * 1.6f + rnd(0.6f), 1.2f + rnd(0.4f), a.z + fz0 * 1.6f + rnd(0.6f), 0.45f, CarRenderer.ICE_TINT);
                if (near) s.play(Sfx.THROW, vol, 0.8f);
                break;
            case PowerUps.JUMP:
                fx.puff(a.x, 0.4f, a.z, 10, 0xFFFFFF, 5f);
                wave(a.x, Math.max(0f, a.y) + 0.1f, a.z, 3.4f, 0.4f, PowerUps.COLOR[PowerUps.JUMP]);
                if (near) s.play(Sfx.SPRING, vol, 1f);
                if (me && !quiet) game.vibrate(30);
                break;
            case PowerUps.STICKY:
                for (int k = 0; k < 10; k++) fx.sparkle(a.x + rnd(1.6f), 0.5f + (float) Math.random() * 0.8f, a.z + rnd(1.6f), 0.45f, CarRenderer.STICKY_GOO);
                if (near) s.play(Sfx.GOO, vol, 1f);
                if (me && !quiet) pop("STICKY!", a.x, 3.4f, a.z, 0xFFE09AFF);
                break;
            default: {
                float rr = PowerUps.SHOCK_RADIUS;
                wave(a.x, a.y + 0.2f, a.z, rr, 0.45f, PowerUps.COLOR[PowerUps.SUPER_BUMP]);
                wave(a.x, a.y + 0.5f, a.z, rr * 0.75f, 0.32f, 0xFFFFFF);
                fx.bonk(a.x, 2.6f, a.z, 1.3f);
                fx.puff(a.x, 0.5f, a.z, 12, 0xFFD8A0, 7f);
                if (!quiet && isNear(a.x, a.z, 30f)) {
                    s.play(Sfx.BOOM, me ? 1f : 0.6f, 0.95f + (float) Math.random() * 0.1f);
                    Car p = match.player;
                    if (p != null && p.alive) {
                        float dx = p.x - a.x, dz = p.z - a.z;
                        if (dx * dx + dz * dz < 14f * 14f) game.shake(me ? 0.7f : 0.35f, 0.35f);
                    }
                }
                if (me && !quiet) game.vibrate(60);
                break;
            }
        }
    }

    private void iceBurst(float x, float y, float z, int count) {
        for (int k = 0; k < count; k++) {
            int i = fx.spawn(Particles.CUBE, x + rnd(0.8f), y + rnd(0.5f), z + rnd(0.8f), rnd(6f), 2f + (float) Math.random() * 5f, rnd(6f),
                    0.16f + (float) Math.random() * 0.2f, 0.7f + (float) Math.random() * 0.4f, Math.random() < 0.5 ? 0xCFF6FF : 0x8FE0FF, 20f);
            fx.glow(i, 0.25f);
        }
    }

    private static float rnd(float r) {
        return (float) (Math.random() * 2 - 1) * r;
    }

    /** Trails and twinkles of the power-ups in play (and goo dripping off sticky wheels). */
    private void powerFx(float dt) {
        PowerUps pw = match.power;
        for (int i = 0; i < WAVES; i++) {
            if (waveT[i] >= 0f) {
                waveT[i] += dt;
                if (waveT[i] > waveDur[i]) waveT[i] = -1f;
            }
        }
        for (int i = 0; i < PowerUps.MAX_SHOTS; i++) {
            if (!pw.shotOn[i] || Math.random() > 0.7) continue;
            if (pw.shotType[i] == PowerUps.SNOWBALL) {
                int k = fx.spawn(Particles.BALL, pw.shotX[i], pw.shotY[i], pw.shotZ[i], rnd(0.5f), rnd(0.5f), rnd(0.5f), 0.16f, 0.35f, 0xFFFFFF, 0f);
                fx.drag(k, 3f);
            } else {
                fx.sparkle(pw.shotX[i] + rnd(0.3f), pw.shotY[i] + rnd(0.3f), pw.shotZ[i] + rnd(0.3f), 0.35f, CarRenderer.ICE_TINT);
            }
        }
        if (Math.random() < dt * 6f) {
            for (int i = 0; i < PowerUps.MAX_PICKUPS; i++) {
                if (!pw.pickOn[i] || Math.random() > 0.35 || !isNear(pw.pickX[i], pw.pickZ[i], 40f)) continue;
                Arena.Tile t = match.arena.tiles[pw.pickTile[i]];
                fx.sparkle(pw.pickX[i] + rnd(1f), t.y + 1.5f + rnd(0.8f), pw.pickZ[i] + rnd(1f), 0.3f, PowerUps.COLOR[pw.pickType[i]]);
            }
        }
        for (Car c : match.cars) {
            if (c.falling || !c.alive) continue;
            if (c.stickyT > 0 && Math.random() < dt * 5f && isNear(c.x, c.z, 30f)) {
                float fx0 = (float) Math.sin(c.yaw), fz0 = (float) Math.cos(c.yaw);
                float side = Math.random() < 0.5 ? -1f : 1f, along = Math.random() < 0.5 ? c.def.wheelZf : c.def.wheelZr;
                float wx = c.x + fz0 * side * c.def.wheelX + fx0 * along, wz = c.z - fx0 * side * c.def.wheelX + fz0 * along;
                if (Math.random() < 0.6) {
                    int k = fx.spawn(Particles.BALL, wx, c.y + 0.25f, wz, 0, 0.5f, 0, 0.12f + (float) Math.random() * 0.08f, 0.6f, CarRenderer.STICKY_GOO, 6f);
                    fx.drag(k, 2f);
                } else {
                    fx.sparkle(wx, c.y + 0.5f, wz, 0.3f, 0xF6D2FF);
                }
            }
            if (c.airborne && c != hoodCar && Math.random() < 0.5 && isNear(c.x, c.z, 40f)) {
                // a twinkly trail under and behind the car
                float bx = c.x - (float) Math.sin(c.yaw) * 1.2f, bz = c.z - (float) Math.cos(c.yaw) * 1.2f;
                fx.sparkle(bx + rnd(0.7f), c.y - 0.1f + rnd(0.3f), bz + rnd(0.7f), 0.35f, Math.random() < 0.5 ? 0xFFFFFF : PowerUps.COLOR[PowerUps.JUMP]);
            }
        }
    }

    /** Pickups lying on the map, snowballs and ice blocks in flight, shockwave rings. */
    private void drawPowerUps(Renderer r, Art art, Arena a) {
        PowerUps pw = match.power;
        for (int i = 0; i < PowerUps.MAX_PICKUPS; i++) {
            if (!pw.pickOn[i]) continue;
            Arena.Tile t = a.tiles[pw.pickTile[i]];
            float sc = Ease.outBack(Math.min(1f, pw.pickAge[i] / 0.4f));
            if (sc < 0.02f) continue;
            float ph = i * 1.7f;
            float y = t.y + 1.8f + (float) Math.sin(time * 2.6f + ph) * 0.22f;
            float spin = time * 1.6f + ph;
            int type = pw.pickType[i];
            float pulse = 0.25f + 0.15f * (float) Math.sin(time * 5f + ph);
            float fs = sc * 1.2f, is = sc * 1.3f;
            M4.trs(m, pw.pickX[i], y, pw.pickZ[i], spin, 0, 0, fs, fs, fs);
            r.draw(art.pickupFrame, m, 0xFFFFFF, 0xFFFFFF, pulse);
            M4.trs(m, pw.pickX[i], y, pw.pickZ[i], spin, 0, 0, is, is, is);
            r.draw(art.powerIcon[type], m, 0xFFFFFF, 0xFFFFFF, 0.1f);
            // a glowing ring on the floor in the power-up's color, and a soft shadow
            float rp = (1.45f + 0.1f * (float) Math.sin(time * 4f + ph)) * sc;
            M4.trs(m, pw.pickX[i], t.y + 0.1f, pw.pickZ[i], 0, t.rotX, t.rotZ, rp, 1f, rp);
            r.draw(art.ring, m, PowerUps.COLOR[type], PowerUps.COLOR[type], 0.45f);
            r.blob(pw.pickX[i], t.y + 0.05f, pw.pickZ[i], 0.8f * sc, 1f, 0, 0.3f);
        }
        for (int i = 0; i < PowerUps.MAX_SHOTS; i++) {
            if (!pw.shotOn[i]) continue;
            int type = pw.shotType[i];
            float age = pw.shotAge[i];
            float s = type == PowerUps.SNOWBALL ? 0.8f : 1.05f;
            M4.trs(m, pw.shotX[i], pw.shotY[i], pw.shotZ[i], age * 9f, age * 7f, 0, s, s, s);
            r.draw(art.powerIcon[type], m, 0xFFFFFF, 0xFFFFFF, type == PowerUps.ICE ? 0.2f : 0.1f);
            Arena.Tile t = a.cellAt(pw.shotX[i], pw.shotZ[i]);
            if (t != null && t.state == Arena.PRESENT && pw.shotY[i] > -0.5f) {
                r.blob(pw.shotX[i], t.y + 0.05f, pw.shotZ[i], 0.55f, 1f, 0, 0.3f * Math.max(0f, 1f - pw.shotY[i] / 8f));
            }
        }
        for (int i = 0; i < WAVES; i++) {
            if (waveT[i] < 0f) continue;
            float k = waveT[i] / waveDur[i];
            float rad = 0.6f + (waveR[i] - 0.6f) * Ease.outCubic(k);
            float th = 1f - k;
            M4.trs(m, waveX[i], waveY[i], waveZ[i], 0, 0, 0, rad, 1f + th * 5f, rad);
            r.draw(art.wave, m, waveColor[i], waveColor[i], 0.25f + 0.4f * th);
        }
    }

    /** A small icon of the held power-up hovering low over the roof (out of the way of a camera behind the car). */
    private void drawHeld(Renderer r, Art art, Car c) {
        CarDef d = c.def;
        float fx0 = (float) Math.sin(c.yaw), fz0 = (float) Math.cos(c.yaw);
        float top = Math.max(d.topY + (c.topper > 0 ? 1f : 0.1f), art.carTop[d.id]);
        boolean followed = c == camCar;
        float y = c.y + c.hop + top + (followed ? 0.4f : 0.55f) + (float) Math.sin(time * 3.4f + c.index) * 0.08f;
        float px = c.x - fx0 * 0.35f, pz = c.z - fz0 * 0.35f;
        float s = followed ? 0.5f : 0.6f;
        M4.trs(m, px, y, pz, time * 2.2f + c.index, 0, 0, s, s, s);
        r.draw(art.powerIcon[c.power], m, 0xFFFFFF, 0xFFFFFF, 0.22f + 0.1f * (float) Math.sin(time * 6f + c.index));
        // a little halo in the power-up's color
        int col = PowerUps.COLOR[c.power];
        M4.trs(m, px, y - s * 0.6f, pz, 0, 0, 0, s * 0.7f, 0.8f, s * 0.7f);
        r.draw(art.ring, m, col, col, 0.45f);
    }

    // ice shell: crystals around a frozen car in car space: x across and z along (fractions of the car's size),
    // height (fraction of its roof), size, lean outwards and lean forwards/backwards (radians)
    private static final float[] SHARD_X = {-1.05f, -0.75f, 1.05f, 0.8f, -1.05f, -0.8f, 1.05f, 0.75f, -1.15f, 1.15f, -0.3f, 0.35f};
    private static final float[] SHARD_Z = {1.0f, 1.15f, 1.0f, 1.15f, -1.0f, -1.15f, -1.0f, -1.15f, 0.05f, -0.1f, 0.15f, -0.35f};
    private static final float[] SHARD_Y = {0.1f, 0.15f, 0.1f, 0.15f, 0.1f, 0.15f, 0.1f, 0.15f, 0.25f, 0.25f, 0.95f, 0.95f};
    private static final float[] SHARD_S = {1.15f, 0.7f, 1.05f, 0.75f, 1.1f, 0.7f, 1.2f, 0.65f, 0.9f, 0.85f, 0.62f, 0.55f};
    private static final float[] SHARD_OUT = {0.75f, 0.35f, 0.75f, 0.35f, 0.75f, 0.35f, 0.75f, 0.35f, 0.95f, 0.95f, 0.25f, 0.3f};
    private static final float[] SHARD_FWD = {0.55f, 0.85f, 0.55f, 0.85f, 0.55f, 0.85f, 0.55f, 0.85f, 0f, 0f, 0.2f, 0.25f};

    private void drawIce(Renderer r, Art art, Car c, boolean hood) {
        CarDef d = c.def;
        float grow = Ease.outBack(Math.min(1f, (PowerUps.FREEZE_TIME - c.frozenT) / 0.25f));
        float shake = c.frozenT < 0.45f ? (float) Math.sin(time * 70f + c.index) * 0.05f : 0f;
        float halfZ = Math.max(d.wheelZf, -d.wheelZr) + 0.2f;
        for (int k = 0; k < SHARD_X.length; k++) {
            if (hood && SHARD_Y[k] > 0.5f) continue; // roof crystals would sit in the hood camera
            float sx = SHARD_X[k], sz = SHARD_Z[k];
            float s = SHARD_S[k] * grow;
            M4.copy(base, m);
            M4.postTranslate(m, sx * (d.wheelX + 0.1f) + shake, SHARD_Y[k] * d.topY, sz * halfZ);
            M4.postRotZ(m, -Math.signum(sx) * SHARD_OUT[k]);
            M4.postRotX(m, Math.signum(sz) * SHARD_FWD[k]);
            M4.postRotY(m, k * 0.9f);
            M4.postScale(m, s, s * 1.15f, s);
            r.draw(art.shard, m, k % 3 == 0 ? 0xE2F8FF : 0xA6E6FF, 0, 0.2f);
        }
    }

    // ------------------------------------------------------------------ sky decorations

    private static final float TAU = 6.2832f;
    private static final int K_CLOUD = 0, K_STAR = 1, K_RIBBON = 2, K_BUBBLE = 3, K_LOLLIPOP = 4, K_CANE = 5,
            K_PLANET = 6, K_RINGED = 7, K_ROCK = 8, K_EMBER = 9;
    private static final int[] PLANET_COLORS = {0xFF8A5C, 0x6BC8FF, 0xC88BFF, 0xFFD25A, 0x7CE6A0, 0xFF6FA8};
    private static final int[] PLANET_RINGS = {0xFFE6A0, 0xFFFFFF, 0xFFB0E0, 0xA0E8FF};
    private static final int[] CANDY_COLORS = {0xFF5FA8, 0x5FC8FF, 0xFFD23B, 0x7CE66A, 0xB98BFF, 0xFF8A3B};
    private static final int[] COTTON_COLORS = {0xFFC8E8, 0xC8E4FF, 0xE8D0FF, 0xFFF0C8};
    private static final int[] EMBER_COLORS = {0xFF6A1A, 0xFFA21F, 0xFFD84A};

    /**
     * Adds a decoration. rad is the distance beyond the arena edge and y the height, both scaled with the arena.
     * Anything reaching up to the arena stays far enough out that the chase camera never runs into it.
     */
    private void add(int kind, float ang, float rad, float y, float size, float phase, float speed, int c1, int c2) {
        if (decorCount >= DECOR_MAX) return;
        if (kind != K_BUBBLE && kind != K_EMBER && y + size * 3f > -3f) rad = Math.max(rad, 34f + rad * 0.3f);
        int i = decorCount++;
        dKind[i] = kind;
        dAng[i] = ang;
        dRad[i] = rad;
        dY[i] = y;
        dSize[i] = size;
        dPhase[i] = phase;
        dSpeed[i] = speed;
        dColor[i] = c1;
        dColor2[i] = c2;
    }

    /** Scatters this sky's decorations (the same layout every time). */
    private void seedDecor() {
        decorSky = sky;
        decorCount = 0;
        Rng g = new Rng(1234 + sky * 7919L);
        int c = Skies.DECOR_COLOR[sky];
        switch (Skies.DECOR[sky]) {
            case Skies.DECOR_STARS:
                for (int i = 0; i < 64; i++) {
                    add(K_STAR, g.range(0, TAU), g.range(40f, 130f), g.range(-12f, 46f), g.range(1.1f, 2.3f), g.range(0, TAU), 0.004f,
                            g.chance(0.6f) ? c : 0xFFFFFF, 0);
                }
                if (sky == Skies.AURORA) {
                    for (int i = 0; i < 7; i++) {
                        add(K_RIBBON, i / 7f * TAU + g.range(0, 0.5f), g.range(50f, 80f), g.range(-10f, 2f), g.range(26f, 38f), g.range(0, TAU), 0.008f, 0, 0);
                    }
                }
                break;
            case Skies.DECOR_BUBBLES:
                for (int i = 0; i < 64; i++) {
                    float rad = g.range(4f, 84f), size = g.range(0.9f, 2.8f);
                    if (rad < 34f) size *= 0.5f;
                    // near ones pop just under the arena, far ones rise higher
                    add(K_BUBBLE, g.range(0, TAU), rad, rad < 34f ? -2f - size : g.range(4f, 24f), size, g.f(), g.range(2.5f, 6f),
                            g.chance(0.6f) ? c : 0xE8FCFF, 0);
                }
                break;
            case Skies.DECOR_CANDY:
                for (int i = 0; i < 8; i++) {
                    add(K_LOLLIPOP, i / 8f * TAU + g.range(0, 0.4f), g.range(34f, 80f), g.range(-12f, 0f), g.range(2.6f, 3.8f), g.range(0, TAU),
                            0.008f, CANDY_COLORS[g.i(CANDY_COLORS.length)], 0);
                }
                for (int i = 0; i < 6; i++) {
                    add(K_CANE, i / 6f * TAU + 0.45f + g.range(0, 0.4f), g.range(34f, 80f), g.range(-14f, -2f), g.range(2.6f, 3.6f), g.range(0, TAU),
                            0.008f, 0, 0);
                }
                for (int i = 0; i < 14; i++) {
                    add(K_CLOUD, g.range(0, TAU), g.range(16f, 90f), g.range(-38f, -10f), g.range(1.4f, 3f), g.range(0, TAU), g.range(0.006f, 0.012f),
                            COTTON_COLORS[g.i(COTTON_COLORS.length)], 0);
                }
                break;
            case Skies.DECOR_PLANETS:
                for (int i = 0; i < 56; i++) {
                    add(K_STAR, g.range(0, TAU), g.range(40f, 130f), g.range(-14f, 46f), g.range(0.9f, 2f), g.range(0, TAU), 0.002f,
                            g.chance(0.7f) ? 0xFFFFFF : c, 0);
                }
                for (int i = 0; i < 6; i++) {
                    add(i % 2 == 0 ? K_RINGED : K_PLANET, i / 6f * TAU + g.range(0, 0.6f), g.range(46f, 96f), g.range(-8f, 16f), g.range(4f, 7.5f),
                            g.range(0, TAU), 0.005f, PLANET_COLORS[i % PLANET_COLORS.length], PLANET_RINGS[g.i(PLANET_RINGS.length)]);
                }
                break;
            case Skies.DECOR_ROCKS:
                for (int i = 0; i < 18; i++) {
                    boolean low = i % 2 == 0;
                    add(K_ROCK, i / 18f * TAU + g.range(0, 0.3f), low ? g.range(10f, 40f) : g.range(40f, 90f), low ? g.range(-30f, -14f) : g.range(-10f, 8f),
                            g.range(2.2f, 4.2f), g.range(0, TAU), g.range(0.005f, 0.012f), c, 0);
                }
                for (int i = 0; i < 72; i++) {
                    float rad = g.range(4f, 70f);
                    add(K_EMBER, g.range(0, TAU), rad, rad < 34f ? -3f : g.range(4f, 20f), g.range(0.35f, 0.75f), g.f(), g.range(3f, 7f),
                            EMBER_COLORS[g.i(EMBER_COLORS.length)], 0);
                }
                break;
            case Skies.DECOR_GRID:
                for (int i = 0; i < 36; i++) {
                    add(K_STAR, g.range(0, TAU), g.range(60f, 130f), g.range(10f, 50f), g.range(0.8f, 1.6f), g.range(0, TAU), 0.002f, 0xFFFFFF, 0);
                }
                break;
            default:
                for (int i = 0; i < 16; i++) {
                    add(K_CLOUD, i / 16f * TAU + g.range(0, 0.3f), g.range(18f, 85f), g.range(-40f, -12f), g.range(1.6f, 3.6f), g.range(0, TAU),
                            g.range(0.006f, 0.014f), c, 0);
                }
                for (int i = 0; i < (sky == Skies.BLUE_SKY ? 12 : 8); i++) {
                    add(K_CLOUD, g.range(0, TAU), g.range(70f, 125f), g.range(4f, 24f), g.range(2.6f, 4.4f), g.range(0, TAU), 0.005f, c, 0);
                }
                break;
        }
    }

    /** Big, slow things floating around and below the arena (scaled to the arena so small previews show them too). */
    private void drawDecor(Renderer r, Art art, Arena a) {
        if (sky != decorSky) seedDecor();
        float k = Math.max(0.55f, Math.min(1.25f, (a.half + 24f) / 60f));
        float half = a.half;
        float ex = game.cam.ex, ez = game.cam.ez;
        if (Skies.DECOR[sky] == Skies.DECOR_GRID) drawGrid(r, art, k);
        drawBackdrop(r, art);
        for (int i = 0; i < decorCount; i++) {
            float ph = dPhase[i];
            int kind = dKind[i];
            boolean rising = kind == K_BUBBLE || kind == K_EMBER;
            float ang = rising ? dAng[i] + (float) Math.sin(time * 0.7f + ph * 20f) * 0.03f : dAng[i] + time * dSpeed[i];
            float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
            // distance to the edge of the (square) arena along this direction, then out
            float rad = half / Math.max(0.5f, Math.max(Math.abs(ca), Math.abs(sa))) + 2f + dRad[i] * k * (dY[i] < 0 ? decorReach : 1f);
            float px = ca * rad, pz = sa * rad;
            float s = dSize[i] * k * decorSize;
            if (rising) {
                // rising from the deep, popping at the top
                float span = 70f;
                float h = (time * dSpeed[i] + ph * span) % span;
                float py = (dY[i] - span + h) * k;
                s *= Math.min(1f, (span - h) / 4f) * Math.min(1f, h / 6f);
                if (s < 0.02f) continue;
                if (kind == K_BUBBLE) {
                    M4.trs(m, px, py, pz, (float) Math.atan2(ex - px, ez - pz), 0, 0, s, s, s);
                    r.draw(art.bubble, m, dColor[i], dColor[i], 0.22f);
                } else {
                    M4.trs(m, px, py, pz, time * 2f + ph * 9f, time * 1.3f + ph, 0, s, s, s);
                    r.draw(art.cube, m, dColor[i], dColor[i], 0.6f);
                }
                continue;
            }
            float py = dY[i] * k + (float) Math.sin(time * 0.4f + ph * 6f) * 0.8f * k;
            float face = (float) Math.atan2(ex - px, ez - pz);
            switch (kind) {
                case K_STAR: {
                    float tw = s * (0.75f + 0.25f * (float) Math.sin(time * 2.4f + ph * 5f));
                    M4.trs(m, px, py, pz, face, 0, ph + time * 0.3f, tw, tw, tw);
                    r.draw(art.star, m, dColor[i], dColor[i], 0.65f);
                    break;
                }
                case K_RIBBON: {
                    float h = s * 0.6f * (1f + 0.18f * (float) Math.sin(time * 0.7f + ph));
                    M4.trs(m, px, py, pz, -ang - 1.5708f + (float) Math.sin(time * 0.3f + ph) * 0.15f, 0, 0, s, h, s * 2f);
                    r.draw(art.aurora, m, 0xFFFFFF, 0xFFFFFF, 0.12f + 0.1f * (float) Math.sin(time * 1.1f + ph * 3f));
                    break;
                }
                case K_LOLLIPOP:
                    M4.trs(m, px, py, pz, face + (float) Math.sin(time * 0.4f + ph) * 0.5f, 0, (float) Math.sin(time * 0.3f + ph) * 0.25f, s, s, s);
                    r.draw(art.lollipop, m, dColor[i], dColor[i], 0.05f);
                    break;
                case K_CANE:
                    M4.trs(m, px, py, pz, time * 0.25f + ph, 0, (float) Math.sin(time * 0.5f + ph) * 0.5f, s, s, s);
                    r.draw(art.candyCane, m, 0xFFFFFF, 0xFFFFFF, 0.05f);
                    break;
                case K_PLANET:
                case K_RINGED:
                    M4.trs(m, px, py, pz, time * 0.08f + ph, 0.3f, 0.2f, s, s, s);
                    r.draw(kind == K_RINGED ? art.ringedPlanet : art.planet, m, dColor[i], dColor2[i], 0.12f);
                    break;
                case K_ROCK:
                    M4.trs(m, px, py, pz, time * 0.12f * (i % 2 == 0 ? 1 : -1) + ph, ph * 0.7f, 0, s, s, s);
                    r.draw(art.rock, m, dColor[i], dColor[i], 0f);
                    r.draw(art.rockLava, m, 0xFFFFFF, 0xFFFFFF, 0.35f + 0.15f * (float) Math.sin(time * 2f + ph * 4f));
                    break;
                default:
                    M4.trs(m, px, py, pz, ph, 0, 0, s, s * 0.7f, s);
                    r.draw(art.puff, m, dColor[i], dColor[i], 0f);
                    break;
            }
        }
    }

    /** Suns and moons: hang in the sky ahead of the camera, behind the arena, always in view. */
    private void drawBackdrop(Renderer r, Art art) {
        switch (sky) {
            case Skies.SYNTHWAVE:
                backdrop(120f, 0f, 0.35f, 24f);
                r.draw(art.sun, m, 0xFFFFFF, 0xFFFFFF, 0.15f);
                break;
            case Skies.SUNSET:
                backdrop(120f, 0.3f, 0.4f, 15f);
                r.draw(art.sunDisc, m, 0xFFFFFF, 0xFFFFFF, 0.15f);
                break;
            case Skies.STARRY_NIGHT:
                backdrop(120f, -0.38f, 0.7f, 8f);
                r.draw(art.moon, m, 0xFFFFFF, 0xFFFFFF, 0.15f);
                break;
            default:
                break;
        }
    }

    /**
     * Places m at a spot in the sky ahead of the camera, turned yawOff to the side and lift (0..1 of half the
     * view) above the middle of the view, facing the camera. Kept above the abyss haze.
     */
    private void backdrop(float dist, float yawOff, float lift, float size) {
        Camera c = game.cam;
        float fx = c.tx - c.ex, fy = c.ty - c.ey, fz = c.tz - c.ez;
        float hl = (float) Math.sqrt(fx * fx + fz * fz);
        if (hl < 1e-3f) {
            fz = 1f;
            hl = 1f;
        }
        float yaw = (float) Math.atan2(fx, fz) + yawOff;
        float elev = Math.min(0.12f, (float) Math.atan2(fy, hl) + (float) Math.toRadians(c.fov) * 0.5f * lift);
        elev = Math.max(elev, (float) Math.asin(Math.max(-1f, Math.min(1f, (-3f - c.ey) / dist))));
        float ce = (float) Math.cos(elev);
        float px = c.ex + (float) Math.sin(yaw) * ce * dist, py = c.ey + (float) Math.sin(elev) * dist, pz = c.ez + (float) Math.cos(yaw) * ce * dist;
        M4.trs(m, px, py, pz, yaw + (float) Math.PI, elev, 0, size, size, size);
    }

    /** SYNTHWAVE: a scrolling neon grid far below the arena. */
    private void drawGrid(Renderer r, Art art, float k) {
        float oz = (time * 5f) % Art.GRID_STEP;
        M4.trs(m, 0, -22f * k, oz, 0, 0, 0, 1, 1, 1);
        r.draw(art.grid, m, Skies.DECOR_COLOR[sky], Skies.DECOR_COLOR[sky], 0.4f);
    }

    /** World popups, drawn in the 2D pass. */
    public void drawPopups(UIBatch b) {
        for (int i = 0; i < POPS; i++) {
            if (popT[i] <= 0) continue;
            float t = popT[i];
            float px = popX[i], py = popY[i] + (1f - t) * 3f, pz = popZ[i];
            if (hoodCar != null) {
                // popups on top of the hood car would sit above the camera: show them ahead of the hood
                float dx = px - hoodCar.x, dz = pz - hoodCar.z;
                if (dx * dx + dz * dz < 9f) {
                    px = hoodCar.x + (float) Math.sin(hoodCar.yaw) * 7f;
                    pz = hoodCar.z + (float) Math.cos(hoodCar.yaw) * 7f;
                    py = hoodCar.y + hoodCar.def.hoodY + 1f + (1f - t) * 1.5f;
                }
            }
            if (!game.cam.project(px, py, pz, proj)) continue;
            float x = proj[0] / b.scale, y = proj[1] / b.scale;
            float sc = Ease.outBack(Math.min(1f, (1f - t) * 6f));
            b.alpha(Math.min(1f, t * 3f));
            b.textShadow(b.title, popText[i], x, y, 40f * sc, popColor[i], UIBatch.CENTER, 0xFF2A1840, 5f, 4f, 0x60200040);
            b.alpha(1f);
        }
    }
}
