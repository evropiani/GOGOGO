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

        fx.faceYaw = (float) Math.atan2(game.cam.ex - game.cam.tx, game.cam.ez - game.cam.tz);
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
