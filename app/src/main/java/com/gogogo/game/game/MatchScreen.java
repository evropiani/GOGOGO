package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.Input;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** The actual game (landscape): HUD, steering/pedal buttons, camera modes, spectating, pause. */
public final class MatchScreen extends Screen {
    private static final int OWNER_CTRL = 2;
    private static final int C_LEFT = 0, C_RIGHT = 1, C_BRAKE = 2, C_GAS = 3, C_BOOST = 4, C_POWER = 5, CONTROLS = 6;
    /** Radius of the power-up button (above BRAKE). */
    private static final float POWER_R = 58f;
    public static final String[] CAM_NAME = {"NEAR", "FAR", "HOOD"};

    private final Match match;
    private final MatchView view;
    private final Match.Options opt;

    private boolean paused;
    private boolean camMenu;
    private float menuX, menuY, menuTop, camBtnX; // camera menu panel area and the button column
    private final boolean[] held = new boolean[CONTROLS];
    private final float[] press = new float[CONTROLS];
    private final float[] cx = new float[CONTROLS], cy = new float[CONTROLS];

    // camera state
    private boolean camInit;
    private float camYaw;
    private float ex, ey, ez, tx, ty, tz;
    private float orbit;

    // announcements
    private String bigText;
    private int bigColor;
    private float bigT, bigDur;
    private String subText;
    private float subT;

    private float deadT = -1f;   // time since player eliminated
    private boolean spectating;
    private int specIndex = -1;
    private float overT = -1f;
    private boolean skipping;
    private float bannerPop;
    private int lastRoundSeen;
    private int lastAliveAnnounce = 100;
    private float duckFlash;
    private boolean resultsShown;
    private float hintT;
    private boolean usedGas;
    private float powerPop;    // power button bounce after a pickup
    private float powerHintT;  // "TAP!" over the power button (first matches)
    private int devPower;      // next power-up the developer button hands out

    private static final String[] SAFE_WORDS = {"SAFE!", "PHEW!", "NICE!", "CLUTCH!", "COZY!"};
    /** Start light colors: red, yellow, green. */
    static final int[] LIGHT_COLORS = {0xFFFF3B3B, 0xFFFFD21F, 0xFF34D058};

    public MatchScreen(Game game) {
        super(game);
        opt = new Match.Options();
        Save s = game.save;
        if (s.dev) {
            opt.god = s.devFlags[Cheats.GOD];
            opt.freezeTimer = s.devFlags[Cheats.FREEZE];
            opt.superSpeed = s.devFlags[Cheats.SPEED];
            opt.dumbBots = s.devFlags[Cheats.DUMB];
            opt.forceDuck = s.devFlags[Cheats.DUCK];
            opt.slowTimer = s.devFlags[Cheats.SLOW];
            if (s.devFlags[Cheats.FEW]) opt.bots = 9;
            opt.autopilot = s.devFlags[Cheats.AUTO];
        }
        long seed = System.nanoTime();
        opt.map = Maps.pick(s, new com.gogogo.game.engine.Rng(seed ^ 0x5EEDL));
        Themes.apply(s.theme);
        match = new Match(seed, opt, s);
        view = new MatchView(game, match);
        view.skin = s.skin;
        view.sky = s.sky;
        final Match.Listener inner = view;
        match.listener = new Match.Listener() {
            public void event(int type, Car a, Car b, float v) {
                inner.event(type, a, b, v);
                onEvent(type, a, b, v);
            }
        };
    }

    public void enter() {
        game.sfx.music(Sfx.MUSIC_GAME);
    }

    public int skyId() {
        return view.sky;
    }

    public boolean allowNotes() {
        return false;
    }

    /** The map this match is played on (see Maps). */
    public int mapId() {
        return opt.map;
    }

    // ------------------------------------------------------------------ events → HUD

    private void announce(String text, int color, float dur) {
        bigText = text;
        bigColor = color;
        bigT = 0;
        bigDur = dur;
    }

    private void onEvent(int type, Car a, Car b, float v) {
        Sfx s = game.sfx;
        switch (type) {
            case Match.EV_COUNT:
                // start lights: red GO!, yellow GO!, green GO! with a beep for each light
                if (v == 3) {
                    announce("GO!", LIGHT_COLORS[0], 0.8f);
                    s.play(Sfx.START_RED, 1f, 1f);
                } else if (v == 2) {
                    announce("GO!", LIGHT_COLORS[1], 0.8f);
                    s.play(Sfx.START_YELLOW, 1f, 1f);
                } else {
                    announce("GO!", LIGHT_COLORS[2], 1.0f);
                    s.play(Sfx.START_GO, 1f, 1f);
                    game.vibrate(30);
                }
                break;
            case Match.EV_ROUND:
                bannerPop = 1f;
                if (match.round > 1) {
                    int c = 0xFF000000 | Palette.TILE[match.target];
                    announce(Palette.TILE_NAME[match.target] + "!", c, 1.0f);
                }
                s.play(Sfx.WHOOSH, 0.7f, 1f + Math.min(0.5f, match.round * 0.02f));
                break;
            case Match.EV_TICK:
                s.play(Sfx.TICK, 0.8f, 1f + (3 - v) * 0.12f);
                break;
            case Match.EV_DROP: {
                announce("DROP!", 0xFFFF4FA3, 0.7f);
                Car p = match.player;
                if (p != null && p.alive && !p.airborne) {
                    Arena.Tile t = match.arena.cellAt(p.x, p.z);
                    if (t != null && t.color == match.target) {
                        view.pop(SAFE_WORDS[(int) (Math.random() * SAFE_WORDS.length)], p.x, 3.4f, p.z, 0xFF5EE65A);
                        s.play(Sfx.COIN, 0.5f, 1.4f);
                    }
                }
                break;
            }
            case Match.EV_ELIM:
                if (a == match.player) {
                    deadT = 0;
                    announce("WHOOPS!", 0xFFFF4FA3, 1.4f);
                    s.play(Sfx.LOSE, 0.9f, 1f);
                }
                break;
            case Match.EV_WIN:
                overT = 0;
                match.hoodWin = a == match.player && game.save.camMode == 2;
                if (a == match.player) {
                    announce("YOU WIN!", 0xFFFFE14D, 3f);
                    s.play(Sfx.WIN, 1f, 1f);
                    game.vibrate(200);
                } else {
                    announce(a.name.toUpperCase() + " WINS!", 0xFFFFFFFF, 3f);
                }
                break;
            case Match.EV_TIE:
                overT = 0;
                announce("TIE!", 0xFF3BE0FF, 3f);
                if (match.player != null && match.tieGroup.contains(match.player)) s.play(Sfx.WIN, 1f, 0.9f);
                break;
            case Match.EV_DUCK_GET:
                duckFlash = 1f;
                break;
            case Match.EV_PICKUP:
                if (a == match.player) {
                    powerPop = 1f;
                    if (game.save.matches < 5) powerHintT = 2.5f;
                }
                break;
            case Match.EV_POWER:
                if (a == match.player) powerHintT = 0f;
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ controls

    private boolean controlsActive() {
        Car p = match.player;
        return p != null && p.alive && !paused && match.phase != Match.OVER && !skipping;
    }

    /** Lays out the control centers for the current screen size and handedness. */
    private void layoutControls() {
        UIBatch b = game.b;
        float W = b.width, H = b.height;
        float sl = game.safeLeft, sr = game.safeRight;
        if (game.save.leftHanded) {
            float t = sl;
            sl = sr;
            sr = t;
        }
        float left = sl + 20, right = sr + 20, bottom = H - game.safeBottom;
        cx[C_LEFT] = left + 100;
        cx[C_RIGHT] = left + 290;
        cy[C_LEFT] = cy[C_RIGHT] = bottom - 120;
        cx[C_GAS] = W - right - 100;
        cy[C_GAS] = bottom - 140;
        cx[C_BRAKE] = W - right - 280;
        cy[C_BRAKE] = bottom - 105;
        cx[C_BOOST] = W - right - 100;
        cy[C_BOOST] = bottom - 350;
        // power-ups: right above BRAKE
        cx[C_POWER] = cx[C_BRAKE];
        cy[C_POWER] = cy[C_BRAKE] - 75 - 22 - POWER_R;
        if (game.save.leftHanded) {
            for (int i = 0; i < CONTROLS; i++) cx[i] = W - cx[i];
            // keep left/right arrows in screen order
            float t = cx[C_LEFT];
            cx[C_LEFT] = cx[C_RIGHT];
            cx[C_RIGHT] = t;
        }
    }

    /** Nearest control to a point, or -1 if none is within reach. */
    private int controlAt(float x, float y) {
        int best = -1;
        float bd = 150f * 150f;
        for (int i = 0; i < CONTROLS; i++) {
            if (i == C_POWER && !powerShown()) continue;
            float dx = x - cx[i], dy = y - cy[i];
            float d = dx * dx + dy * dy;
            if (i == C_BOOST || i == C_POWER) d *= 1.4f; // smaller targets than the pedals
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    public void preInput() {
        layoutControls();
        if (!controlsActive()) return;
        Car p = match.player;
        for (Input.Pointer ptr : game.input.pointers) {
            if (ptr.owner != 0) continue;
            // fingers already resting on a control when the match appears count too
            boolean fresh = ptr.justDown || (ptr.down && match.phase == Match.INTRO);
            if (!fresh) continue;
            if (camMenu) {
                if (ptr.x > menuX && ptr.y > menuTop && ptr.y < menuY) continue; // the menu handles it
                if (ptr.justDown && !(ptr.x > camBtnX && ptr.y < menuTop)) camMenu = false;
            }
            int c = controlAt(ptr.x, ptr.y);
            if (c < 0) continue;
            ptr.owner = OWNER_CTRL;
            if (c == C_BOOST && ptr.justDown && p.boostCd <= 0) {
                p.wantBoost = true;
                game.vibrate(15);
            }
            if (c == C_POWER && ptr.justDown && p.power >= 0 && p.frozenT <= 0) {
                p.wantPower = true;
                game.vibrate(15);
            }
        }
    }

    private void handleControls(float dt) {
        Car p = match.player;
        for (int i = 0; i < CONTROLS; i++) held[i] = false;
        if (controlsActive()) {
            for (Input.Pointer ptr : game.input.pointers) {
                if (ptr.owner != OWNER_CTRL || !ptr.down) continue;
                int c = controlAt(ptr.x, ptr.y);
                if (c >= 0) held[c] = true;
            }
        }
        for (int i = 0; i < CONTROLS; i++) press[i] = Ease.approach(press[i], held[i] ? 1f : 0f, 25f, dt);
        if (p == null) return;
        p.steer = (held[C_RIGHT] ? 1f : 0f) - (held[C_LEFT] ? 1f : 0f);
        p.throttle = held[C_BRAKE] ? -1f : (held[C_GAS] ? 1f : 0f);
        if (held[C_GAS] && match.phase != Match.INTRO) usedGas = true;
    }

    // ------------------------------------------------------------------ update

    private float frameDt = 1f / 60f;

    public void update(float dt) {
        frameDt = dt;
        if (paused) return;
        handleControls(dt);
        if (skipping) {
            if (match.fastForward(900)) skipping = false;
        } else {
            match.update(dt);
        }
        view.update(dt);
        hintT += dt;
        if (bigText != null) {
            bigT += dt;
            if (bigT > bigDur) bigText = null;
        }
        if (subT > 0) subT -= dt;
        if (bannerPop > 0) bannerPop = Math.max(0f, bannerPop - dt * 3f);
        if (duckFlash > 0) duckFlash = Math.max(0f, duckFlash - dt * 0.6f);
        if (powerPop > 0) powerPop = Math.max(0f, powerPop - dt * 3f);
        if (powerHintT > 0) powerHintT -= dt;
        if (deadT >= 0) deadT += dt;
        if (overT >= 0) overT += dt;

        // alive milestones
        if (match.phase == Match.SHOW && match.round != lastRoundSeen) {
            lastRoundSeen = match.round;
            int al = match.alive;
            String msg = null;
            if (al <= 2 && lastAliveAnnounce > 2) msg = "FINAL 2!";
            else if (al <= 3 && lastAliveAnnounce > 3) msg = "FINAL 3!";
            else if (al <= 10 && lastAliveAnnounce > 10) msg = "TOP 10!";
            else if (al <= 25 && lastAliveAnnounce > 25) msg = "TOP 25!";
            else if (al <= 50 && lastAliveAnnounce > 50) msg = "HALF GONE!";
            if (msg != null) {
                subText = msg;
                subT = 2.2f;
                lastAliveAnnounce = al;
            }
        }

        if (match.phase == Match.OVER && overT > 3.2f && !resultsShown) finish(false);
    }

    // ------------------------------------------------------------------ camera / 3D

    private Car lastCam;

    private Car camTarget() {
        Car p = match.player;
        Car t;
        if (p != null && (p.alive || deadT < 1.8f)) t = p;
        else if (match.winner != null) t = match.winner;
        else if (match.tie && p != null && match.tieGroup.contains(p)) t = p;
        else if (specIndex >= 0 && match.cars[specIndex].alive) t = match.cars[specIndex];
        else {
            t = match.leader();
            if (t != null) specIndex = t.index;
        }
        if (t == null) t = lastCam;
        lastCam = t;
        return t;
    }

    public boolean render3d() {
        Car t = camTarget();
        float dt = frameDt;
        int mode = game.save.camMode;
        float fov;
        float wex, wey, wez, wtx, wty, wtz;
        view.hoodCar = null;
        view.camCar = t;
        game.cam.near = 0.5f;
        if (t == null) {
            wex = 0; wey = 60; wez = 50; wtx = 0; wty = 0; wtz = 0;
            fov = 55f;
        } else if (match.phase == Match.OVER && (match.winner != null || match.tie)) {
            // celebration orbit
            orbit += dt * 0.5f;
            wex = t.x + (float) Math.sin(orbit) * 11f;
            wez = t.z + (float) Math.cos(orbit) * 11f;
            wey = Math.max(t.y, -30f) + 6f;
            wtx = t.x;
            wty = Math.max(t.y, -30f) + 1f;
            wtz = t.z;
            fov = 55f;
        } else {
            if (!t.falling) {
                float rate = mode == 2 ? 40f : 4.5f;
                camYaw = camYaw + Ease.wrapAngle(t.yaw - camYaw) * (1f - (float) Math.exp(-rate * dt));
            }
            float fx = (float) Math.sin(camYaw), fz = (float) Math.cos(camYaw);
            float baseY = t.falling ? Math.max(-30f, t.y * 0.6f) : 0f;
            // high up in a super jump: rise with the car and look down at where it is going to land
            float air = t.airborne ? Math.max(0f, t.y) : 0f;
            // a fall pulls the camera back (not yet while a super jump can still save the car)
            if (t.falling && !t.alive) mode = 1;
            boolean drop = match.phase == Match.DROP && match.phaseT < 1.6f;
            if (mode == 2) {
                CarDef d = t.def;
                wex = t.x + fx * d.hoodZ;
                wez = t.z + fz * d.hoodZ;
                wey = t.y + t.hop + d.hoodY * (1f + Math.max(0f, Math.min(0.35f, t.squash))); // follow the body stretch
                wtx = wex + fx * 12f;
                wtz = wez + fz * 12f;
                wty = wey - 2.3f - Math.min(4f, air * 0.5f);
                fov = 70f;
                game.cam.near = 0.2f;
                // keep the player's marker during the bird's-eye part of the intro
                if (match.phase != Match.INTRO || match.phaseT > Match.INTRO_TIME * 0.8f) view.hoodCar = t;
            } else if (mode == 0) {
                float back = 8.5f + (drop ? 2f : 0f), up = 4.4f + (drop ? 1.5f : 0f);
                wex = t.x - fx * back;
                wez = t.z - fz * back;
                wey = baseY + up + air * 1.05f;
                wtx = t.x + fx * 3f;
                wtz = t.z + fz * 3f;
                wty = baseY + 1.0f + air * 0.45f;
                fov = 62f;
            } else {
                float back = 15f + (drop ? 4f : 0f), up = 12f + (drop ? 5f : 0f);
                wex = t.x - fx * back;
                wez = t.z - fz * back;
                wey = baseY + up + air;
                wtx = t.x + fx * 4f;
                wtz = t.z + fz * 4f;
                wty = baseY + air * 0.5f;
                fov = 58f;
            }
            if (match.phase == Match.INTRO) {
                // swoop down from a bird's-eye view behind the car (the car can't turn yet)
                float k = Ease.inOutSine(Math.min(1f, match.phaseT / (Match.INTRO_TIME * 0.9f)));
                wex = Ease.lerp(t.x - fx * 30f, wex, k);
                wey = Ease.lerp(75f, wey, k);
                wez = Ease.lerp(t.z - fz * 30f, wez, k);
                wtx = Ease.lerp(t.x, wtx, k);
                wty = Ease.lerp(0f, wty, k);
                wtz = Ease.lerp(t.z, wtz, k);
            }
        }
        if (!camInit) {
            camInit = true;
            if (t != null) camYaw = t.yaw;
            ex = wex; ey = wey; ez = wez; tx = wtx; ty = wty; tz = wtz;
        }
        boolean rigid = view.hoodCar != null || match.phase == Match.INTRO;
        float k = rigid ? 1f : 1f - (float) Math.exp(-9f * dt);
        ex += (wex - ex) * k;
        ey += (wey - ey) * k;
        ez += (wez - ez) * k;
        tx += (wtx - tx) * k;
        ty += (wty - ty) * k;
        tz += (wtz - tz) * k;
        game.cam.fov = Ease.approach(game.cam.fov, fov, 10f, dt);
        game.cam.set(ex, ey, ez, tx, ty, tz);
        game.shakeScale = view.hoodCar != null ? 0.25f : 1f;
        game.beginWorld();
        view.draw();
        return true;
    }

    // ------------------------------------------------------------------ HUD

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;
        float left = game.safeLeft + 20, right = game.safeRight + 20;
        Car p = match.player;
        layoutControls();

        drawDanger(b, W, H);
        drawFrost(b, W, H);
        view.drawPopups(b);
        if (view.hoodCar == null || view.hoodCar != match.player) drawNameTags(b);

        if (!paused) {
            drawLights(b, W, top, false);
            drawBanner(b, W, top);
            drawLights(b, W, top, true);
        }

        // pause + alive counter (top left)
        if (!paused && match.phase != Match.OVER) {
            if (ui.roundButton("pause", left + 40, top + 52, 38, 0xFFFFFFFF)) {
                paused = true;
                camMenu = false;
            }
            ui.iconPause(left + 40, top + 52 + ui.lastRoundPress, 38, 0xFF2A1840);
        }
        String al = String.valueOf(match.alive);
        float aw = b.title.width(al, 40f) + 96;
        float ax = left + 100;
        b.shadow(ax, top + 52 - 30 + 6, aw, 60, 30, 0x50200040, 8);
        b.shape(ax + aw / 2, top + 52, aw, 60, 30, 0xFF2A1840, 0, 0, 0, 0, 0);
        drawCarIcon(b, ax + 40, top + 52, 0.95f);
        b.text(b.title, al, ax + aw - 22, top + 54, 40f, 0xFFFFFFFF, UIBatch.RIGHT, 0, 0);
        if (match.duckCollected) drawDuckIcon(b, ax + aw + 42, top + 52, 1f + duckFlash * 0.6f);

        // controls
        if (controlsActive()) {
            drawControls(b, p);
            if (match.phase == Match.INTRO && game.save.matches < 3) {
                float hy = H * 0.6f;
                float hw = Math.max(420f, Math.min(900f, W - 2 * (Math.max(game.safeLeft, game.safeRight) + 20 + 190)));
                b.shape(W / 2, hy, hw, 140, 40, 0xD02A1840, 0, 0, 0, 0, 0);
                b.textFit(b.title, "GET ON THE COLOR SHOWN UP TOP", W / 2, hy - 26, 38f, hw - 40, 0xFFFFE14D, UIBatch.CENTER, 0, 0);
                b.textFit(b.body, "before the timer runs out. Last car standing wins!", W / 2, hy + 30, 30f, hw - 40, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
            } else if (match.round <= 1 && !usedGas && lightsShown < 1f) {
                // the start light hangs here during the intro; the hint fades in once it has gone
                b.alpha((0.6f + (float) Math.sin(game.time * 4f) * 0.3f) * (1f - lightsShown));
                b.text(b.body, "ARROWS TO STEER  -  HOLD GAS TO GO", W / 2, top + 178, 28f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
                b.alpha(1f);
            }
        }

        // camera button + menu (top right), drawn over the controls
        if (!paused && match.phase != Match.OVER) drawCameraButton(b, ui, W, top, right);

        // big announcement
        if (bigText != null) {
            float k = bigT / bigDur;
            float sc = Ease.outElastic(Math.min(1f, bigT * 2.2f));
            float fade = k > 0.75f ? 1f - (k - 0.75f) / 0.25f : 1f;
            b.alpha(fade);
            float size = bigText.length() <= 3 ? 170f : 104f;
            float maxW = W - game.safeLeft - game.safeRight - 60f, tw = b.title.width(bigText, size);
            if (tw > maxW) size *= maxW / tw; // long bot names
            size *= sc;
            b.textShadow(b.title, bigText, W / 2, H * 0.40f, size, bigColor, UIBatch.CENTER, 0xFF2A1840, size * 0.08f, 12f, 0x70200040);
            b.alpha(1f);
        }
        if (subT > 0 && bigText == null) {
            float sc = Ease.outBack(Math.min(1f, (2.2f - subT) * 4f));
            b.alpha(Math.min(1f, subT * 2f));
            b.textShadow(b.title, subText, W / 2, H * 0.40f, 86f * sc, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 8f, 10f, 0x70200040);
            b.alpha(1f);
        }

        if (p != null && !p.alive && match.phase != Match.OVER && deadT > 1.5f && !paused) drawEliminated(b, ui, W, H);
        if (skipping) {
            b.rect(0, 0, W, H, 0x80200040);
            b.textShadow(b.title, "FAST FORWARD...", W / 2, H / 2, 70f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x70200040);
        }
        if (paused) drawPause(b, ui, W, H);
    }

    private void drawCameraButton(UIBatch b, UI ui, float W, float top, float right) {
        float bx = W - right - 40, by = top + 52;
        camBtnX = bx - 60;
        menuTop = by + 40;
        if (ui.roundButton("cam", bx, by, 38, camMenu ? 0xFFFFE14D : 0xFFFFFFFF)) camMenu = !camMenu;
        drawCameraIcon(b, bx, by + ui.lastRoundPress, 1f);
        b.text(b.body, CAM_NAME[game.save.camMode], bx - 52, by + 2, 26f, 0xFFFFFFFF, UIBatch.RIGHT, 0xFF2A1840, 4f);
        if (!camMenu) return;
        float pw = 220, py = by + 50, step = 76f;
        // keep the list above BOOST when it sits in the same corner
        if (!game.save.leftHanded) step = Math.max(56f, Math.min(76f, (cy[C_BOOST] - 74f - py - 24f) / 3f));
        float ph = 3 * step + 24;
        float px = W - right - pw;
        menuX = px - 10;
        menuTop = py - 10;
        menuY = py + ph + 10;
        ui.block(px, py, pw, ph);
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        for (int i = 0; i < 3; i++) {
            boolean on = game.save.camMode == i;
            if (ui.button("cam" + i, px + 14, py + 12 + i * step, pw - 28, step - 10, on ? 0xFF8E62FF : 0xFF3BA8FF, CAM_NAME[i], Math.min(34f, step * 0.5f))) {
                game.save.camMode = i;
                game.save.markDirty();
                game.save.flush();
                camMenu = false;
                camInit = false; // snap to the new view
            }
        }
    }

    static void drawCameraIcon(UIBatch b, float x, float y, float s) {
        b.roundRect(x - 22 * s, y - 13 * s, 44 * s, 30 * s, 7 * s, 0xFF2A1840);
        b.roundRect(x - 10 * s, y - 20 * s, 20 * s, 10 * s, 3 * s, 0xFF2A1840);
        b.circle(x, y + 2 * s, 10 * s, 0xFFFFFFFF);
        b.circle(x, y + 2 * s, 6 * s, 0xFF2A1840);
    }

    private void drawControls(UIBatch b, Car p) {
        // steering arrows
        for (int i = C_LEFT; i <= C_RIGHT; i++) {
            float x = cx[i], y = cy[i], s = 150f;
            float pr = press[i] * 9f;
            b.shadow(x - s / 2, y - s / 2 + 12, s, s, 40, 0x50200040, 10);
            b.shape(x, y + 10, s, s, 40, 0xC0303050, 0, 0, 0, 0, 0);
            b.shape(x, y + pr, s, s, 40, held[i] ? 0xFFFFE14D : 0xE0FFFFFF, 0xFF2A1840, 5f, 0.35f, 0, 0);
            float d = i == C_LEFT ? -1f : 1f;
            float ax = x + d * 10, ay = y + pr;
            b.line(ax + d * 22, ay, ax - d * 18, ay - 34, 22, 0xFF2A1840);
            b.line(ax + d * 22, ay, ax - d * 18, ay + 34, 22, 0xFF2A1840);
        }
        // pedals
        drawPedal(b, C_BRAKE, 140, 150, 0xFFFF4F6A, "BRAKE", 30f);
        drawPedal(b, C_GAS, 150, 220, 0xFF34D058, "GAS", 46f);
        if (powerShown()) drawPowerButton(b, p);
        // boost
        float x = cx[C_BOOST], y = cy[C_BOOST], r = 68f;
        boolean ok = p.boostCd <= 0;
        float ready = ok ? 1f : 1f - p.boostCd / p.boostCooldown;
        float pr = press[C_BOOST] * 8f;
        float rr = r * (ok ? 1f + (float) Math.sin(game.time * 6f) * 0.04f : 1f);
        b.shadow(x - rr, y - rr + 12, rr * 2, rr * 2, rr, 0x50200040, 10);
        b.circle(x, y + 10, rr, ok ? 0xFFC0306A : 0xFF6A6080);
        b.shape(x, y + pr, rr * 2, rr * 2, rr, ok ? 0xFFFF4FA3 : 0xFF9A90B0, 0xFFFFFFFF, 6f, 0.5f, 0, 0);
        if (!ok) {
            float fh = rr * 2 * ready;
            b.clip(x - rr, y + rr - fh + pr, rr * 2, fh);
            b.shape(x, y + pr, rr * 2 - 12, rr * 2 - 12, rr - 6, 0xFFFF7CC0, 0, 0, 0, 0, 0);
            b.unclip();
        }
        b.text(b.title, "BUMP", x, y + pr + 3, 32f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
    }

    private static final String[] DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String[] DEV_POWER = {"+SNOWBALL", "+ICE", "+JUMP", "+STICKY", "+S.BUMP"};

    /** The power button waits until the first-match hint over the controls has gone. */
    private boolean powerShown() {
        return !(match.phase == Match.INTRO && game.save.matches < 3);
    }

    /** Round power-up button above BRAKE: the held item's icon and name, or a dim empty slot. */
    private void drawPowerButton(UIBatch b, Car p) {
        float x = cx[C_POWER], y = cy[C_POWER], r = POWER_R;
        float pr = press[C_POWER] * 8f;
        int type = p.power;
        if (type < 0) {
            // empty: a dim slot (sticky wheels that are running drain around it)
            b.circle(x, y + 8, r, 0x40200040);
            b.shape(x, y, r * 2, r * 2, r, 0x70302A48, 0x70FFFFFF, 4f, 0, 0, 0);
            if (p.stickyT > 0) {
                float k = p.stickyT / PowerUps.STICKY_TIME;
                float fh = (r * 2 - 12) * k;
                b.clip(x - r, y + r - 6 - fh, r * 2, fh);
                b.shape(x, y, r * 2 - 12, r * 2 - 12, r - 6, UIBatch.withAlpha(0xFF000000 | CarRenderer.STICKY_GOO, 0.55f), 0, 0, 0, 0, 0);
                b.unclip();
                drawPowerIcon(b, PowerUps.STICKY, x, y - r * 0.16f, r * 0.36f);
                b.text(b.title, DIGITS[Math.min(9, (int) Math.ceil(p.stickyT))], x, y + r * 0.5f, 24f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
            } else {
                b.text(b.title, "POWER", x, y + 2, 24f, 0x90FFFFFF, UIBatch.CENTER, 0, 0);
            }
            return;
        }
        int col = 0xFF000000 | PowerUps.COLOR[type];
        boolean frozen = p.frozenT > 0;
        boolean rescue = type == PowerUps.JUMP && p.falling; // jump out of the fall, now!
        float pop = powerPop;
        float rr = r * (1f + 0.04f * (float) Math.sin(game.time * 6f) + pop * 0.22f + (rescue ? 0.14f * Math.abs((float) Math.sin(game.time * 22f)) : 0f));
        if (rescue) b.shape(x, y + pr, rr * 2.9f, rr * 2.9f, rr * 1.45f, UIBatch.withAlpha(col, 0.55f), 0, 0, 0, 26f, 0);
        b.shadow(x - rr, y - rr + 12, rr * 2, rr * 2, rr, 0x50200040, 10);
        b.circle(x, y + 10, rr, UI.shade(col, 0.6f));
        b.shape(x, y + pr, rr * 2, rr * 2, rr, frozen ? 0xFF9AB4C8 : col, 0xFFFFFFFF, 6f, 0.5f, 0, 0);
        drawPowerIcon(b, type, x, y + pr - rr * 0.17f, rr * 0.4f);
        b.textFit(b.title, rescue ? "JUMP!" : PowerUps.SHORT[type], x, y + pr + rr * 0.52f, 24f, rr * 1.55f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
        if (frozen) {
            // iced over until the car thaws
            b.shape(x, y + pr, rr * 2 - 8, rr * 2 - 8, rr - 4, 0x60DFF6FF, 0, 0, 0, 0, 0);
            b.line(x - rr * 0.5f, y + pr - rr * 0.2f, x - rr * 0.1f, y + pr + rr * 0.15f, 4f, 0xC0FFFFFF);
            b.line(x - rr * 0.1f, y + pr + rr * 0.15f, x + rr * 0.45f, y + pr - rr * 0.3f, 4f, 0xC0FFFFFF);
        }
        if (powerHintT > 0 && !frozen) {
            float a = Math.min(1f, powerHintT * 3f) * (0.65f + 0.35f * (float) Math.sin(game.time * 9f));
            b.alpha(a);
            b.textShadow(b.title, "TAP!", x, y - rr - 26, 34f, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 5f, 4f, 0x60200040);
            b.alpha(1f);
        }
    }

    /** Flat icon of a power-up (s = about its radius), for buttons and lists. */
    static void drawPowerIcon(UIBatch b, int type, float cx, float cy, float s) {
        int ink = 0xFF2A1840;
        switch (type) {
            case PowerUps.SNOWBALL:
                b.circle(cx, cy, s * 1.08f, ink);
                b.circle(cx, cy, s, 0xFFFFFFFF);
                b.shape(cx + s * 0.18f, cy + s * 0.32f, s * 1.5f, s * 1.1f, s * 0.55f, 0xFFD2E6FA, 0, 0, 0, 0, 0);
                b.circle(cx, cy - s * 0.12f, s * 0.78f, 0xFFFFFFFF);
                b.circle(cx - s * 0.36f, cy - s * 0.38f, s * 0.2f, 0xFFE4F0FC);
                b.circle(cx + s * 0.34f, cy + s * 0.1f, s * 0.16f, 0xFFE4F0FC);
                break;
            case PowerUps.ICE:
                b.shape(cx, cy, s * 1.75f, s * 1.75f, s * 0.36f, 0xFF9CE4FF, ink, s * 0.12f, 0.35f, 0, 0.26f);
                b.shape(cx - s * 0.1f, cy - s * 0.1f, s * 1.05f, s * 1.05f, s * 0.22f, 0xFFDDF8FF, 0, 0, 0, 0, 0.26f);
                b.line(cx - s * 0.42f, cy - s * 0.05f, cx - s * 0.12f, cy - s * 0.42f, s * 0.14f, 0xFFFFFFFF);
                break;
            case PowerUps.JUMP:
                // up arrow over a spring
                for (int i = 0; i < 2; i++) {
                    float w = i == 0 ? s * 0.56f : s * 0.36f;
                    int c = i == 0 ? ink : 0xFFFFFFFF;
                    b.line(cx, cy - s * 0.95f, cx - s * 0.7f, cy - s * 0.25f, w, c);
                    b.line(cx, cy - s * 0.95f, cx + s * 0.7f, cy - s * 0.25f, w, c);
                    b.line(cx, cy - s * 0.8f, cx, cy + s * 0.25f, w, c);
                }
                for (int i = 0; i < 2; i++) {
                    float w = i == 0 ? s * 0.34f : s * 0.16f;
                    int c = i == 0 ? ink : 0xFFE4E8F0;
                    b.line(cx - s * 0.45f, cy + s * 0.5f, cx + s * 0.45f, cy + s * 0.68f, w, c);
                    b.line(cx + s * 0.45f, cy + s * 0.68f, cx - s * 0.45f, cy + s * 0.86f, w, c);
                }
                break;
            case PowerUps.STICKY:
                // a drop of goo
                for (int i = 0; i < 2; i++) {
                    float o = i == 0 ? s * 0.12f : 0f;
                    int c = i == 0 ? ink : 0xFFF0B8FF;
                    b.shape(cx, cy - s * 0.18f, s * 1.05f + o * 1.6f, s * 1.05f + o * 1.6f, s * 0.12f, c, 0, 0, 0, 0, (float) Math.PI / 4);
                    b.circle(cx, cy + s * 0.3f, s * 0.74f + o, c);
                }
                b.circle(cx - s * 0.26f, cy + s * 0.2f, s * 0.16f, 0xFFFFFFFF);
                break;
            default:
                // super bump: a burst
                for (int i = 0; i < 2; i++) {
                    float o = i == 0 ? s * 0.12f : 0f;
                    int c = i == 0 ? ink : 0xFFFFE14D;
                    for (int k = 0; k < 4; k++) {
                        b.shape(cx, cy, s * 2.05f + o * 2f, s * 0.46f + o * 2f, s * 0.23f + o, c, 0, 0, 0, 0, (float) (k * Math.PI / 4));
                    }
                }
                b.circle(cx, cy, s * 0.42f, 0xFFFFFFFF);
                break;
        }
    }

    /** Frosty screen edges while the player is frozen. */
    private void drawFrost(UIBatch b, float W, float H) {
        Car p = match.player;
        if (p == null || !p.alive || p.frozenT <= 0) return;
        float k = Math.min(1f, p.frozenT * 3f) * Math.min(1f, (PowerUps.FREEZE_TIME - p.frozenT) * 6f);
        int c = UIBatch.withAlpha(0xFFDDF6FF, 0.7f * k);
        float e = 40f;
        b.shape(W / 2, -e / 2, W + 200, e * 3, 0, c, 0, 0, 0, 50f, 0);
        b.shape(W / 2, H + e / 2, W + 200, e * 3, 0, c, 0, 0, 0, 50f, 0);
        b.shape(-e / 2, H / 2, e * 3, H + 200, 0, c, 0, 0, 0, 50f, 0);
        b.shape(W + e / 2, H / 2, e * 3, H + 200, 0, c, 0, 0, 0, 50f, 0);
    }

    private void drawPedal(UIBatch b, int c, float w, float h, int color, String label, float size) {
        float x = cx[c], y = cy[c];
        float pr = press[c] * 10f;
        b.shadow(x - w / 2, y - h / 2 + 12, w, h, 34, 0x50200040, 10);
        b.shape(x, y + 10, w, h, 34, UI.shade(color, 0.62f), 0, 0, 0, 0, 0);
        b.shape(x, y + pr, w, h, 34, held[c] ? UI.shade(color, 1.25f) : color, 0xFFFFFFFF, 5f, 0.45f, 0, 0);
        // grip ridges
        for (int i = -1; i <= 1; i++) b.roundRect(x - w * 0.3f, y + pr + h * 0.18f + i * 18 + 10, w * 0.6f, 7, 3.5f, 0x40FFFFFF);
        b.textFit(b.title, label, x, y + pr - h * 0.2f, size, w - 20, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
    }

    /** Red pulsing edges when the timer is running out and you're on the wrong color. */
    private void drawDanger(UIBatch b, float W, float H) {
        Car p = match.player;
        if (p == null || !p.alive || p.airborne || match.phase != Match.SHOW || match.timer > 1.6f) return;
        Arena.Tile t = match.arena.cellAt(p.x, p.z);
        if (t != null && t.color == match.target && t.state == Arena.PRESENT) return;
        float pulse = 0.55f + 0.45f * (float) Math.sin(game.time * 18f);
        int c = UIBatch.withAlpha(0xFFFF2050, 0.55f * pulse);
        float e = 46f;
        b.shape(W / 2, -e / 2, W + 200, e * 3, 0, c, 0, 0, 0, 60f, 0);
        b.shape(W / 2, H + e / 2, W + 200, e * 3, 0, c, 0, 0, 0, 60f, 0);
        b.shape(-e / 2, H / 2, e * 3, H + 200, 0, c, 0, 0, 0, 60f, 0);
        b.shape(W + e / 2, H / 2, e * 3, H + 200, 0, c, 0, 0, 0, 60f, 0);
    }

    private float lightsShown; // 0..1 how much of the start light is on screen (the steering hint waits for it)

    /**
     * Cartoon traffic light under the banner: drops in for the intro and counts red, yellow, green.
     * Drawn twice per frame: behind the banner while it drops in (front = false), and in front of the
     * round timer for the moment after GO while it fades out (front = true).
     */
    private void drawLights(UIBatch b, float W, float top, boolean front) {
        float since; // seconds since GO, or -1 before it
        if (match.phase == Match.INTRO) since = -1f;
        else if (match.round == 1 && match.phase == Match.SHOW && match.phaseT < 0.6f) since = match.phaseT;
        else {
            lightsShown = 0f;
            return;
        }
        if (front != (since >= 0)) return;
        float t = since < 0 ? match.phaseT : Match.INTRO_TIME + since;
        int lit = t >= Match.INTRO_TIME ? 2 : (t >= Match.LIGHT_YELLOW ? 1 : (t >= Match.LIGHT_RED ? 0 : -1));
        float litAt = lit == 2 ? Match.INTRO_TIME : (lit == 1 ? Match.LIGHT_YELLOW : Match.LIGHT_RED);
        float pop = lit >= 0 ? Math.max(0f, 1f - (t - litAt) / 0.35f) : 0f; // 1 the moment a lamp switches on
        float drop = Ease.outBack(Math.min(1f, t / 0.45f));
        float away = since > 0.25f ? Ease.inCubic(Math.min(1f, (since - 0.25f) / 0.33f)) : 0f;
        lightsShown = 1f - away;
        float sc = 1f + 0.12f * away; // puffs out as it fades
        float r = 30f * sc, gap = 82f * sc, hw = 3 * gap + 18f * sc, hh = 2 * r + 28f * sc;
        float cx = W / 2 + (float) Math.sin(t * 45f) * 4f * pop * pop;
        float cy = top + 100 + 44 - (1f - drop) * 100f;
        b.alpha(Math.min(1f, t * 6f) * (1f - away));
        int body = 0xFF2A1840;
        if (!front) {
            // straps up behind the banner
            b.roundRect(cx - 70 - 7, cy - hh / 2 - 44, 14, 52, 5, UI.shade(body, 0.75f));
            b.roundRect(cx + 70 - 7, cy - hh / 2 - 44, 14, 52, 5, UI.shade(body, 0.75f));
        }
        b.shadow(cx - hw / 2, cy - hh / 2 + 10, hw, hh, hh / 2, 0x60200040, 12);
        b.shape(cx, cy + 7, hw, hh, 28 * sc, 0xFF140A24, 0, 0, 0, 0, 0);
        b.shape(cx, cy, hw, hh, 28 * sc, body, 0xFFFFFFFF, 5f, 0.3f, 0, 0);
        for (int i = 0; i < 3; i++) {
            float lx = cx + (i - 1) * gap;
            // sunken socket under a little hood
            b.circle(lx, cy - 4, r + 9, 0xFF46316A);
            b.circle(lx, cy + 2, r + 7, 0xFF140A24);
            if (i == lit) continue;
            b.circle(lx, cy + 3, r, UI.shade(LIGHT_COLORS[i], 0.3f));
            b.shape(lx - r * 0.3f, cy - r * 0.2f, r * 0.6f, r * 0.34f, r * 0.17f, 0x30FFFFFF, 0, 0, 0, 1f, -0.5f);
        }
        if (lit >= 0) {
            float lx = cx + (lit - 1) * gap, ly = cy + 3;
            int col = LIGHT_COLORS[lit];
            float s = 1f + 0.3f * pop * pop;
            float g = r * (1.9f + 0.8f * pop);
            b.shape(lx, ly, g * 2, g * 2, g, UIBatch.withAlpha(col, 0.55f), 0, 0, 0, 30f, 0);
            b.shape(lx, ly, r * 2 * s, r * 2 * s, r * s, UI.shade(col, 1.12f), UI.shade(col, 1.5f), 4f, 0.35f, 0, 0);
            b.shape(lx - r * 0.3f * s, ly - r * 0.34f * s, r * 0.78f * s, r * 0.42f * s, r * 0.21f * s, 0xC8FFFFFF, 0, 0, 0, 1.5f, -0.5f);
            if (pop > 0) {
                float rr = r * (1.15f + (1f - pop) * 1.5f);
                b.shape(lx, ly, rr * 2, rr * 2, rr, 0, UIBatch.withAlpha(0xFFFFFFFF, pop), 6f * pop + 1f, 0, 0, 0);
            }
        }
        b.alpha(1f);
    }

    private void drawBanner(UIBatch b, float W, float top) {
        float cy0 = top + 52;
        float bw = 340, bh = 84;
        float pop = 1f + bannerPop * 0.25f;
        String label;
        int fill;
        int sym = -1;
        if (match.phase == Match.INTRO) {
            label = "GET READY";
            fill = 0xFFFFFFFF;
        } else if (match.phase == Match.OVER) {
            label = match.tie ? "TIE!" : "WINNER!";
            fill = 0xFFFFE14D;
        } else {
            label = Palette.TILE_NAME[match.target];
            fill = 0xFF000000 | Palette.TILE[match.target];
            sym = match.target;
        }
        float w = bw * pop, h = bh * pop;
        b.shadow(W / 2 - w / 2, cy0 - h / 2 + 10, w, h, h / 2, 0x60200040, 12);
        b.shape(W / 2, cy0 + 7, w, h, h / 2, UI.shade(fill, 0.7f), 0, 0, 0, 0, 0);
        b.shape(W / 2, cy0, w, h, h / 2, fill, 0xFFFFFFFF, 6f, 0.5f, 0, 0);
        int textCol = fill == 0xFFFFFFFF || fill == 0xFFFFE14D ? 0xFF2A1840 : 0xFFFFFFFF;
        int outline = textCol == 0xFFFFFFFF ? 0xFF2A1840 : 0;
        if (sym >= 0) {
            drawSymbol(b, W / 2 - w / 2 + 50, cy0, 26 * pop, sym, 0xFFFFFFFF);
            b.textFit(b.title, label, W / 2 + 22, cy0 + 2, 56f * pop, w - 130, textCol, UIBatch.CENTER, outline, 6f);
        } else {
            b.textFit(b.title, label, W / 2, cy0 + 2, 52f * pop, w - 50, textCol, UIBatch.CENTER, outline, 6f);
        }

        if (match.phase == Match.SHOW || match.phase == Match.DROP) {
            float k = match.phase == Match.SHOW ? match.timer / Math.max(0.01f, match.timerMax) : 0f;
            float tw = 400, th = 28, ty0 = cy0 + h / 2 + 28;
            b.shape(W / 2, ty0 + 4, tw, th, th / 2, 0x50200040, 0, 0, 0, 0, 0);
            b.shape(W / 2, ty0, tw, th, th / 2, 0xFF2A1840, 0, 0, 0, 0, 0);
            float fw = (tw - 10) * k;
            int bar = k > 0.5f ? 0xFF5EE65A : (k > 0.25f ? 0xFFFFC21F : 0xFFFF3B5C);
            if (fw > 4) {
                float wob = match.timer < 1f ? (float) Math.sin(game.time * 40f) * 2f : 0f;
                b.shape(W / 2 - (tw - 10) / 2 + fw / 2, ty0 + wob, fw, th - 10, (th - 10) / 2, bar, 0, 0, 0.6f, 0, 0);
            }
            String secs = match.phase == Match.SHOW ? String.format(java.util.Locale.US, "%.1f", Math.max(0f, match.timer)) : "0.0";
            b.text(b.title, secs, W / 2 + tw / 2 + 16, ty0 + 2, 38f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 5f);
            b.text(b.body, "ROUND " + match.round, W / 2 - tw / 2 - 16, ty0 + 2, 28f, 0xFFFFFFFF, UIBatch.RIGHT, 0xFF2A1840, 4f);
        }
    }

    private void drawNameTags(UIBatch b) {
        Car t = camTarget();
        if (t == null || match.phase == Match.INTRO) return;
        float[] pr = proj2;
        for (int k = 0; k < 3; k++) {
            Car best = null;
            float bd = 120f;
            for (Car c : match.cars) {
                if (!c.alive || c == match.player || c == tagged[0] || c == tagged[1]) continue;
                float dx = c.x - t.x, dz = c.z - t.z;
                float d = dx * dx + dz * dz;
                if (d < bd && d > 2f) {
                    bd = d;
                    best = c;
                }
            }
            tagged[k] = best;
            if (best == null || !game.cam.project(best.x, best.y + best.def.topY + 1.0f, best.z, pr)) continue;
            b.alpha(0.75f);
            b.text(b.body, best.name, pr[0] / b.scale, pr[1] / b.scale, 22f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 3f);
            b.alpha(1f);
        }
        tagged[0] = tagged[1] = tagged[2] = null;
    }

    private final float[] proj2 = new float[2];
    private final Car[] tagged = new Car[3];

    private void drawEliminated(UIBatch b, UI ui, float W, float H) {
        Car p = match.player;
        float k = Ease.outBack(Math.min(1f, (deadT - 1.5f) * 3f));
        if (!spectating) {
            float pw = 640, ph = 380;
            float px = W / 2 - pw / 2, py = H * 0.5f - ph / 2 + 30 + (1 - k) * 300;
            b.alpha(Math.min(1f, (deadT - 1.5f) * 3f));
            ui.panel(px, py, pw, ph, 0xFFFFFFFF);
            b.text(b.title, "ELIMINATED!", W / 2, py + 62, 62f, 0xFFFF4FA3, UIBatch.CENTER, 0xFF2A1840, 6f);
            b.text(b.title, "#" + p.place, W / 2, py + 148, 100f, 0xFF2A1840, UIBatch.CENTER, 0, 0);
            b.text(b.body, "of " + match.total + "  -  survived " + p.roundsSurvived + " rounds", W / 2, py + 218, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
            b.alpha(1f);
            if (ui.button("spectate", px + 30, py + 255, 280, 100, 0xFF3BA8FF, "WATCH", 46f)) spectating = true;
            if (ui.button("continue", px + 330, py + 255, 280, 100, 0xFF34D058, "NEXT", 46f)) finish(true);
        } else {
            Car t = camTarget();
            float bw = 780, y = H - game.safeBottom - 70;
            b.shadow(W / 2 - bw / 2, y - 45 + 8, bw, 90, 45, 0x50200040, 10);
            b.shape(W / 2, y, bw, 90, 45, 0xFF2A1840, 0, 0, 0, 0, 0);
            b.textFit(b.body, "WATCHING " + (t == null ? "" : t.name.toUpperCase()), W / 2 - 120, y + 2, 32f, 460, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
            if (ui.button("nextcar", W / 2 + bw / 2 - 270, y - 36, 120, 72, 0xFF8E62FF, ">", 46f)) nextSpectate();
            if (ui.button("skip", W / 2 + bw / 2 - 140, y - 36, 120, 72, 0xFFFF9A2B, "SKIP", 32f)) skipping = true;
        }
    }

    private void nextSpectate() {
        int n = match.cars.length;
        for (int i = 1; i <= n; i++) {
            int k = (Math.max(0, specIndex) + i) % n;
            if (match.cars[k].alive) {
                specIndex = k;
                return;
            }
        }
    }

    private void drawPause(UIBatch b, UI ui, float W, float H) {
        b.rect(0, 0, W, H, 0x90200040);
        ui.block(0, 0, W, H);
        boolean dev = game.save.dev;
        float pw = 760, ph = dev ? 470 : 330;
        float px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "PAUSED", W / 2, py + 64, 72f, 0xFF8E62FF, UIBatch.CENTER, 0xFF2A1840, 6f);
        if (ui.button("resume", px + 40, py + 130, 330, 120, 0xFF34D058, "RESUME", 56f)) paused = false;
        if (ui.button("quit", px + 390, py + 130, 330, 120, 0xFFFF4FA3, "GIVE UP", 50f)) {
            paused = false;
            finish(true);
        }
        if (dev) {
            float y = py + 290, bw = 128, gap = (pw - 80 - 5 * bw) / 4f;
            if (ui.button("cw", px + 40, y, bw, 100, 0xFFFFC21F, "WIN NOW", 30f)) {
                paused = false;
                Cheats.instantWin(match);
            }
            if (ui.button("cd", px + 40 + (bw + gap), y, bw, 100, 0xFFFFC21F, "DUCK HERE", 28f)) {
                paused = false;
                Cheats.duckHere(match);
            }
            if (ui.button("cs", px + 40 + 2 * (bw + gap), y, bw, 100, 0xFFFFC21F, "SKIP RND", 30f)) {
                paused = false;
                match.timer = 0.01f;
            }
            boolean god = match.player != null && match.player.god;
            if (ui.button("cg", px + 40 + 3 * (bw + gap), y, bw, 100, god ? 0xFF34D058 : 0xFFFFC21F, "GOD", 30f)) {
                if (match.player != null) match.player.god = !match.player.god;
            }
            if (ui.button("cp", px + 40 + 4 * (bw + gap), y, bw, 100, 0xFFFFC21F, DEV_POWER[devPower], 26f)) {
                // hand the player the next power-up (cycles through all of them)
                Car pl = match.player;
                if (pl != null && pl.alive) {
                    pl.power = devPower;
                    powerPop = 1f;
                }
                devPower = (devPower + 1) % PowerUps.COUNT;
                paused = false;
            }
        }
    }

    /** Ends the match for the player and shows results. */
    private boolean finished;

    private void finish(boolean early) {
        if (finished) return;
        finished = true;
        resultsShown = true;
        Car p = match.player;
        if (p != null && p.alive && match.phase != Match.OVER) {
            // gave up while alive: counts as being eliminated right now
            p.place = match.alive;
        }
        game.setScreen(new ResultsScreen(game, match, early));
    }

    public boolean back() {
        if (match.phase == Match.OVER) {
            finish(false);
            return true;
        }
        if (camMenu) {
            camMenu = false;
            return true;
        }
        paused = !paused;
        return true;
    }

    public void pause() {
        if (match.phase != Match.OVER) paused = true;
    }

    // ------------------------------------------------------------------ small icons

    static void drawCarIcon(UIBatch b, float cx, float cy, float s) {
        b.shape(cx, cy - 6 * s, 30 * s, 16 * s, 7 * s, 0xFFFF4FA3, 0, 0, 0, 0, 0);
        b.shape(cx, cy + 4 * s, 46 * s, 18 * s, 8 * s, 0xFFFF4FA3, 0, 0, 0, 0, 0);
        b.circle(cx - 13 * s, cy + 14 * s, 7 * s, 0xFFFFFFFF);
        b.circle(cx + 13 * s, cy + 14 * s, 7 * s, 0xFFFFFFFF);
    }

    static void drawDuckIcon(UIBatch b, float cx, float cy, float s) {
        b.shape(cx, cy + 6 * s, 44 * s, 28 * s, 14 * s, 0xFFFFE03B, 0xFF2A1840, 3, 0, 0, 0);
        b.circle(cx + 10 * s, cy - 12 * s, 13 * s, 0xFFFFE03B, 0xFF2A1840, 3);
        b.shape(cx + 25 * s, cy - 10 * s, 14 * s, 8 * s, 4 * s, 0xFFFF8A1F, 0, 0, 0, 0, 0);
        b.circle(cx + 13 * s, cy - 15 * s, 2.5f * s, 0xFF2A1840);
    }

    static void drawSymbol(UIBatch b, float cx, float cy, float r, int sym, int color) {
        switch (sym) {
            case 0:
                b.circle(cx, cy, r * 0.8f, color);
                break;
            case 1:
                for (int i = 0; i < 5; i++) {
                    float a = (float) (i * Math.PI * 2 / 5 - Math.PI / 2);
                    b.shape(cx + (float) Math.cos(a) * r * 0.42f, cy + (float) Math.sin(a) * r * 0.42f, r * 0.95f, r * 0.34f, r * 0.12f, color, 0, 0, 0, 0, a);
                }
                b.circle(cx, cy, r * 0.4f, color);
                break;
            case 2:
                b.shape(cx, cy, r * 1.25f, r * 1.25f, r * 0.12f, color, 0, 0, 0, 0, (float) Math.PI / 4);
                break;
            case 3: {
                float h = r * 1.5f;
                b.line(cx - r * 0.8f, cy + h * 0.38f, cx + r * 0.8f, cy + h * 0.38f, r * 0.35f, color);
                b.line(cx - r * 0.8f, cy + h * 0.38f, cx, cy - h * 0.5f, r * 0.35f, color);
                b.line(cx + r * 0.8f, cy + h * 0.38f, cx, cy - h * 0.5f, r * 0.35f, color);
                b.shape(cx, cy + r * 0.15f, r * 0.8f, r * 0.6f, r * 0.1f, color, 0, 0, 0, 0, 0);
                break;
            }
            case 4:
                b.roundRect(cx - r * 0.9f, cy - r * 0.3f, r * 1.8f, r * 0.6f, r * 0.1f, color);
                b.roundRect(cx - r * 0.3f, cy - r * 0.9f, r * 0.6f, r * 1.8f, r * 0.1f, color);
                break;
            default:
                for (int i = 0; i < 3; i++) {
                    b.shape(cx, cy, r * 1.6f, r * 0.92f, r * 0.08f, color, 0, 0, 0, 0, (float) (i * Math.PI / 3));
                }
                break;
        }
    }
}
