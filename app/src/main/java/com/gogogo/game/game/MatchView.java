package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Renderer;
import com.gogogo.game.engine.UIBatch;

/** Draws a {@link Match} in 3D and turns its events into particles, sounds and shakes. */
public final class MatchView implements Match.Listener {
    private final Game game;
    public Match match;
    public final Particles fx = new Particles();
    public boolean quiet; // title-screen background: no sounds / shakes
    /** The car whose hood the camera sits on: drawn without topper and markers. */
    public Car hoodCar;

    private final float[] base = new float[16], m = new float[16], tmp = new float[16];
    private final float[] cloudX = new float[14], cloudY = new float[14], cloudZ = new float[14], cloudS = new float[14];
    private float time;

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
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < cloudX.length; i++) {
            double a = i / (double) cloudX.length * Math.PI * 2 + r.nextDouble() * 0.3;
            float d = 60f + r.nextFloat() * 60f;
            cloudX[i] = (float) Math.cos(a) * d;
            cloudZ[i] = (float) Math.sin(a) * d;
            cloudY[i] = -12f - r.nextFloat() * 30f;
            cloudS[i] = 1.6f + r.nextFloat() * 2.2f;
        }
    }

    public void setMatch(Match match) {
        this.match = match;
        match.listener = this;
        fx.clear();
        for (int i = 0; i < POPS; i++) popT[i] = 0;
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
                fx.puff(c.x - fx0 * 1.3f, c.y + 0.6f, c.z - fz0 * 1.3f, 1, Math.random() < 0.5 ? 0xFFB23B : 0xFFE14D, 1.5f);
            }
        }
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] += dt * 0.8f;
            if (cloudX[i] > 130f) cloudX[i] = -130f;
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
                fx.puff(a.x, 0.5f, a.z, 6, 0xFFFFFF, 3f);
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

        // clouds below / around the arena
        for (int i = 0; i < cloudX.length; i++) {
            M4.trs(m, cloudX[i], cloudY[i] + (float) Math.sin(time * 0.4f + i) * 0.8f, cloudZ[i], i * 0.7f, 0, 0, cloudS[i], cloudS[i] * 0.7f, cloudS[i]);
            r.draw(art.cloud, m, 0xFFFFFF);
        }

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
            int colorIdx = t.flip > 0.5f ? t.prevColor : t.color;
            int col = Palette.TILE[colorIdx % Palette.TILE.length];
            M4.trs(m, t.x + shakeX, y + flipHop, t.z + shakeZ, 0, t.rotX, t.rotZ, sxz, sy, sxz);
            r.draw(art.tile, m, col, col, 0f);
            if (symbols) {
                M4.trs(tmp, 0, 0, 0, 0, 0, 0, 1.15f, 1f, 1.15f);
                M4.mul(tmp, m, tmp);
                r.draw(art.symbols[colorIdx % 6], tmp, Palette.mix(col, 0xFFFFFF, 0.45f), 0, 0f);
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

        fx.draw(r, art);
    }

    public void drawCar(Car c, boolean marker) {
        Renderer r = game.r;
        Art art = game.art;
        CarDef d = c.def;
        CarRenderer.draw(r, art, c, time, base, m, c != hoodCar);
        if (c == hoodCar) marker = false;

        // shadow on the floor
        Arena.Tile t = match.arena.cellAt(c.x, c.z);
        if (t != null && t.state != Arena.GONE && !(t.state == Arena.SPAWNING && t.delay > 0)) {
            float h = c.y + c.hop - t.y;
            if (h > -0.6f) {
                float alpha = 0.45f * Math.max(0f, 1f - h / 6f);
                r.blob(c.x, t.y + 0.04f, c.z, 1.35f, 1.35f, c.yaw, Math.min(0.45f, alpha));
            }
        }

        if (marker) {
            M4.trs(m, c.x, c.y + d.topY + 2.6f + (float) Math.abs(Math.sin(time * 5f)) * 0.6f, c.z, time * 2.5f, 0, 0, 1.3f, 1.3f, 1.3f);
            r.draw(art.marker, m, 0xFFE14D, 0xFFE14D, 0.3f);
            if (t != null && !c.falling) {
                float pulse = 1.55f + (float) Math.sin(time * 6f) * 0.08f;
                M4.trs(m, c.x, t.y + 0.12f, c.z, 0, 0, 0, pulse, 1f, pulse);
                r.draw(art.ring, m, 0xFFE14D, 0xFFE14D, 0.5f);
            }
        }
    }

    /** World popups, drawn in the 2D pass. */
    public void drawPopups(UIBatch b) {
        for (int i = 0; i < POPS; i++) {
            if (popT[i] <= 0) continue;
            float t = popT[i];
            if (!game.cam.project(popX[i], popY[i] + (1f - t) * 3f, popZ[i], proj)) continue;
            float x = proj[0] / b.scale, y = proj[1] / b.scale;
            float sc = Ease.outBack(Math.min(1f, (1f - t) * 6f));
            b.alpha(Math.min(1f, t * 3f));
            b.textShadow(b.title, popText[i], x, y, 40f * sc, popColor[i], UIBatch.CENTER, 0xFF2A1840, 5f, 4f, 0x60200040);
            b.alpha(1f);
        }
    }
}
