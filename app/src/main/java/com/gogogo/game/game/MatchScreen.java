package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.Input;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** The actual game: HUD, controls, spectating, pause. */
public final class MatchScreen extends Screen {
    private static final int OWNER_STICK = 2, OWNER_BOOST = 3;

    private final Match match;
    private final MatchView view;
    private final Match.Options opt;

    private boolean paused;
    private float camX, camZ, camH = 30f;
    private boolean camInit;
    private Input.Pointer stick;
    private float stickX, stickY; // base position
    private float knobX, knobY;
    private float boostPress;

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

    private static final String[] SAFE_WORDS = {"SAFE!", "PHEW!", "NICE!", "CLUTCH!", "COZY!"};

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
        match = new Match(System.nanoTime(), opt, s);
        view = new MatchView(game, match);
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
                if (v > 0) {
                    announce(String.valueOf((int) v), 0xFFFFFFFF, 0.8f);
                    s.play(Sfx.BEEP, 0.9f, 1f);
                } else {
                    announce("GO!", 0xFF5EE65A, 0.9f);
                    s.play(Sfx.GO, 1f, 1f);
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
                if (p != null && p.alive) {
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
            case Match.EV_LAST_ONE:
                break;
            case Match.EV_WIN:
                overT = 0;
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
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ update

    public void update(float dt) {
        if (paused) return;
        Car p = match.player;
        handleControls(dt);
        if (skipping) {
            if (match.fastForward(900)) skipping = false;
        } else {
            match.update(dt);
        }
        view.update(dt);
        if (bigText != null) {
            bigT += dt;
            if (bigT > bigDur) bigText = null;
        }
        if (subT > 0) subT -= dt;
        if (bannerPop > 0) bannerPop = Math.max(0f, bannerPop - dt * 3f);
        if (duckFlash > 0) duckFlash = Math.max(0f, duckFlash - dt * 0.6f);
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

        if (match.phase == Match.OVER && overT > 3.2f && !resultsShown) {
            resultsShown = true;
            finish(false);
        }
        if (p != null && !p.alive && deadT > 1.6f && !spectating && specIndex < 0) {
            // stay on the elimination panel; spectate on request
        }
    }

    private void handleControls(float dt) {
        Car p = match.player;
        if (p == null) return;
        UIBatch b = game.b;
        float bx = boostX(), by = boostY(), br = 92f;
        // claim pointers
        for (Input.Pointer ptr : game.input.pointers) {
            if (!ptr.justDown || ptr.owner != 0) continue;
            float dx = ptr.x - bx, dy = ptr.y - by;
            if (dx * dx + dy * dy < (br + 30) * (br + 30) && p.alive) {
                ptr.owner = OWNER_BOOST;
                p.wantBoost = true;
                boostPress = 1f;
                game.vibrate(15);
            } else if (ptr.y > game.safeTop + 230 && stick == null && p.alive && !(spectating || deadT >= 0)) {
                ptr.owner = OWNER_STICK;
                stick = ptr;
                stickX = ptr.x;
                stickY = ptr.y;
                knobX = ptr.x;
                knobY = ptr.y;
            }
        }
        if (stick != null && (!stick.down || stick.owner != OWNER_STICK)) stick = null;
        if (boostPress > 0) boostPress = Math.max(0f, boostPress - dt * 4f);
        if (stick != null && p.alive) {
            float dx = stick.x - stickX, dy = stick.y - stickY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float maxR = 95f;
            if (len > maxR) {
                // drag the base along so the stick never feels stuck
                stickX += dx / len * (len - maxR);
                stickY += dy / len * (len - maxR);
                dx = stick.x - stickX;
                dy = stick.y - stickY;
                len = maxR;
            }
            knobX = stickX + dx;
            knobY = stickY + dy;
            float mag = Math.min(1f, len / maxR);
            if (len > 4f) {
                mag = Math.max(0.35f, mag);
                p.inX = dx / Math.max(1e-3f, (float) Math.sqrt(dx * dx + dy * dy)) * mag;
                p.inZ = dy / Math.max(1e-3f, (float) Math.sqrt(dx * dx + dy * dy)) * mag;
            } else {
                p.inX = p.inZ = 0;
            }
        } else if (p.bot == null) {
            p.inX = p.inZ = 0;
        }
    }

    private float boostX() {
        return game.save.leftHanded ? 130f : game.b.width - 130f;
    }

    private float boostY() {
        return game.b.height - game.safeBottom - 170f;
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
        float dt = 1f / 60f;
        float tx = 0, tz = 0;
        if (t != null) {
            tx = t.x + t.vx * 0.25f;
            tz = t.z + t.vz * 0.25f;
            if (t.falling) {
                tx = t.x;
                tz = t.z;
            }
        }
        float wantH = 30f + (t != null ? Math.min(6f, t.speed() * 0.25f) : 0f);
        if (match.phase == Match.INTRO) wantH = 30f + (1f - Math.min(1f, match.phaseT / Match.INTRO_TIME)) * 40f;
        if (match.phase == Match.OVER) wantH = 22f;
        if (match.phase == Match.DROP && match.phaseT < 1.6f) wantH += 7f;
        if (!camInit) {
            camX = tx;
            camZ = tz;
            camH = 70f;
            camInit = true;
        }
        camX = Ease.approach(camX, tx, 5f, dt);
        camZ = Ease.approach(camZ, tz, 5f, dt);
        camH = Ease.approach(camH, wantH, 2.5f, dt);
        float ty = 0f;
        if (t != null && t.falling) ty = Math.max(-25f, t.y * 0.5f);
        game.cam.fov = 55f;
        game.cam.set(camX, camH + ty, camZ + camH * 0.68f, camX, ty, camZ);
        game.beginWorld();
        view.draw();
        return true;
    }

    // ------------------------------------------------------------------ HUD

    public void ui(float dt) {
        UIBatch b = game.b;
        UI ui = game.ui;
        float W = b.width, H = b.height, top = game.safeTop;
        Car p = match.player;

        drawDanger(b, W, H);
        view.drawPopups(b);
        drawNameTags(b);

        if (!paused) drawBanner(b, W, top);

        // pause button
        if (!paused && match.phase != Match.OVER) {
            if (ui.roundButton("pause", 62, top + 66, 44, 0xFFFFFFFF)) {
                paused = true;
                game.input.reset();
            }
            ui.iconPause(62, top + 66 + ui.lastRoundPress, 44, 0xFF2A1840);
        }

        // alive counter
        float acx = W - 24;
        String al = String.valueOf(match.alive);
        float aw = b.title.width(al, 44f) + 100;
        b.shadow(acx - aw, top + 32 + 6, aw, 68, 34, 0x50200040, 8);
        b.shape(acx - aw / 2, top + 66, aw, 68, 34, 0xFF2A1840, 0, 0, 0, 0, 0);
        drawCarIcon(b, acx - aw + 42, top + 66, 1f);
        b.text(b.title, al, acx - 24, top + 68, 44f, 0xFFFFFFFF, UIBatch.RIGHT, 0, 0);
        if (match.duckCollected) {
            float s = 1f + duckFlash * 0.6f;
            drawDuckIcon(b, 62, top + 150, s);
        }

        // controls
        if (p != null && p.alive && match.phase != Match.OVER && !paused) {
            if (stick != null) {
                b.circle(stickX, stickY, 100, 0x30FFFFFF, 0x80FFFFFF, 5);
                b.circle(knobX, knobY + 6, 52, 0x40200040);
                b.shape(knobX, knobY, 104, 104, 52, 0xFFFFFFFF, 0xFF2A1840, 0, 0.4f, 0, 0);
            } else if (match.phase == Match.INTRO || match.round <= 1) {
                if (match.phase == Match.INTRO && game.save.matches < 3) {
                    float ty = H * 0.62f;
                    b.shape(W / 2, ty, W - 80, 150, 40, 0xD02A1840, 0, 0, 0, 0, 0);
                    b.text(b.title, "GET ON THE COLOR SHOWN UP TOP", W / 2, ty - 28, 34f, 0xFFFFE14D, UIBatch.CENTER, 0, 0);
                    b.text(b.body, "before the timer runs out. Last car standing wins!", W / 2, ty + 30, 30f, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
                }
                b.alpha(0.6f + (float) Math.sin(game.time * 4f) * 0.3f);
                b.text(b.body, "DRAG ANYWHERE TO DRIVE", W / 2, H - game.safeBottom - 60, 32f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 4f);
                b.alpha(1f);
            }
            drawBoost(b, p);
        }

        // big announcement
        if (bigText != null) {
            float k = bigT / bigDur;
            float sc = Ease.outElastic(Math.min(1f, bigT * 2.2f));
            float fade = k > 0.75f ? 1f - (k - 0.75f) / 0.25f : 1f;
            b.alpha(fade);
            float size = (bigText.length() <= 3 ? 190f : 110f) * sc;
            b.textShadow(b.title, bigText, W / 2, H * 0.40f, size, bigColor, UIBatch.CENTER, 0xFF2A1840, size * 0.08f, 12f, 0x70200040);
            b.alpha(1f);
        }
        if (subT > 0 && bigText == null) {
            float sc = Ease.outBack(Math.min(1f, (2.2f - subT) * 4f));
            b.alpha(Math.min(1f, subT * 2f));
            b.textShadow(b.title, subText, W / 2, H * 0.40f, 90f * sc, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 8f, 10f, 0x70200040);
            b.alpha(1f);
        }

        // eliminated panel
        if (p != null && !p.alive && match.phase != Match.OVER && deadT > 1.5f && !paused) drawEliminated(b, ui, W, H);
        else if (p != null && !p.alive && match.phase == Match.OVER && !resultsShown && !paused) {
            // wait for results
        }
        if (skipping) {
            b.rect(0, 0, W, H, 0x80200040);
            b.textShadow(b.title, "FAST FORWARD...", W / 2, H / 2, 70f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 7f, 8f, 0x70200040);
        }

        if (paused) drawPause(b, ui, W, H);
    }

    /** Red pulsing edges when the timer is running out and you're on the wrong color. */
    private void drawDanger(UIBatch b, float W, float H) {
        Car p = match.player;
        if (p == null || !p.alive || match.phase != Match.SHOW || match.timer > 1.6f) return;
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

    private void drawBanner(UIBatch b, float W, float top) {
        float cy = top + 66;
        float bw = 360, bh = 92;
        float pop = 1f + Ease.outElastic(1f - bannerPop) * 0f + bannerPop * 0.25f;
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
        b.shadow(W / 2 - w / 2, cy - h / 2 + 10, w, h, h / 2, 0x60200040, 12);
        b.shape(W / 2, cy + 7, w, h, h / 2, UI.shade(fill, 0.7f), 0, 0, 0, 0, 0);
        b.shape(W / 2, cy, w, h, h / 2, fill, 0xFFFFFFFF, 6f, 0.5f, 0, 0);
        int textCol = fill == 0xFFFFFFFF || fill == 0xFFFFE14D ? 0xFF2A1840 : 0xFFFFFFFF;
        int outline = textCol == 0xFFFFFFFF ? 0xFF2A1840 : 0;
        if (sym >= 0) {
            drawSymbol(b, W / 2 - w / 2 + 52, cy, 28 * pop, sym, 0xFFFFFFFF);
            b.textFit(b.title, label, W / 2 + 22, cy + 2, 60f * pop, w - 130, textCol, UIBatch.CENTER, outline, 6f);
        } else {
            b.textFit(b.title, label, W / 2, cy + 2, 56f * pop, w - 50, textCol, UIBatch.CENTER, outline, 6f);
        }

        // timer bar
        if (match.phase == Match.SHOW || match.phase == Match.DROP) {
            float k = match.phase == Match.SHOW ? match.timer / Math.max(0.01f, match.timerMax) : 0f;
            float tw = 420, th = 30, ty = cy + h / 2 + 34;
            b.shape(W / 2, ty + 4, tw, th, th / 2, 0x50200040, 0, 0, 0, 0, 0);
            b.shape(W / 2, ty, tw, th, th / 2, 0xFF2A1840, 0, 0, 0, 0, 0);
            float fw = (tw - 10) * k;
            int bar = k > 0.5f ? 0xFF5EE65A : (k > 0.25f ? 0xFFFFC21F : 0xFFFF3B5C);
            if (fw > 4) {
                float wob = match.timer < 1f ? (float) Math.sin(game.time * 40f) * 2f : 0f;
                b.shape(W / 2 - (tw - 10) / 2 + fw / 2, ty + wob, fw, th - 10, (th - 10) / 2, bar, 0, 0, 0.6f, 0, 0);
            }
            String secs = match.phase == Match.SHOW ? String.format(java.util.Locale.US, "%.1f", Math.max(0f, match.timer)) : "0.0";
            b.text(b.title, secs, W / 2 + tw / 2 + 16, ty + 2, 40f, 0xFFFFFFFF, UIBatch.LEFT, 0xFF2A1840, 5f);
            b.text(b.body, "ROUND " + match.round, W / 2 - tw / 2 - 16, ty + 2, 30f, 0xFFFFFFFF, UIBatch.RIGHT, 0xFF2A1840, 4f);
        }
    }

    private void drawBoost(UIBatch b, Car p) {
        float x = boostX(), y = boostY(), r = 92f;
        float ready = p.boostCd <= 0 ? 1f : 1f - p.boostCd / p.boostCooldown;
        boolean ok = p.boostCd <= 0;
        float press = boostPress * 10f;
        float pulse = ok ? 1f + (float) Math.sin(game.time * 6f) * 0.04f : 1f;
        float rr = r * pulse;
        b.shadow(x - rr, y - rr + 14, rr * 2, rr * 2, rr, 0x50200040, 10);
        b.circle(x, y + 12, rr, ok ? 0xFFC0306A : 0xFF6A6080);
        b.shape(x, y + press, rr * 2, rr * 2, rr, ok ? 0xFFFF4FA3 : 0xFF9A90B0, 0xFFFFFFFF, 7f, 0.5f, 0, 0);
        if (!ok) {
            // fill from the bottom as it recharges
            float fh = rr * 2 * ready;
            b.clip(x - rr, y + rr - fh + press, rr * 2, fh);
            b.shape(x, y + press, rr * 2 - 14, rr * 2 - 14, rr - 7, 0xFFFF7CC0, 0, 0, 0, 0, 0);
            b.unclip();
        }
        b.text(b.title, "BOOST", x, y + press + 4, 40f, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, 5f);
    }

    private void drawNameTags(UIBatch b) {
        // names for cars near the camera target
        Car t = camTarget();
        if (t == null || match.phase == Match.INTRO) return;
        float[] pr = proj2;
        for (int k = 0; k < 3; k++) {
            Car best = null;
            float bd = 90f;
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
        float pw = 600, ph = spectating ? 0 : 430;
        if (!spectating) {
            float px = W / 2 - pw / 2, py = H * 0.5f - ph / 2 + (1 - k) * 300;
            b.alpha(Math.min(1f, (deadT - 1.5f) * 3f));
            ui.panel(px, py, pw, ph, 0xFFFFFFFF);
            b.text(b.title, "ELIMINATED!", W / 2, py + 70, 66f, 0xFFFF4FA3, UIBatch.CENTER, 0xFF2A1840, 6f);
            b.text(b.title, "#" + p.place, W / 2, py + 165, 110f, 0xFF2A1840, UIBatch.CENTER, 0, 0);
            b.text(b.body, "of " + match.total + "  -  survived " + p.roundsSurvived + " rounds", W / 2, py + 240, 30f, 0xFF7A6A90, UIBatch.CENTER, 0, 0);
            b.alpha(1f);
            if (ui.button("spectate", px + 30, py + 290, 260, 110, 0xFF3BA8FF, "WATCH", 48f)) {
                spectating = true;
            }
            if (ui.button("continue", px + 310, py + 290, 260, 110, 0xFF34D058, "NEXT", 48f)) {
                finish(true);
            }
        } else {
            // spectator bar
            Car t = camTarget();
            float y = H - game.safeBottom - 120;
            b.shadow(30, y - 50 + 8, W - 60, 100, 50, 0x50200040, 10);
            b.shape(W / 2, y, W - 60, 100, 50, 0xFF2A1840, 0, 0, 0, 0, 0);
            b.textFit(b.body, "WATCHING " + (t == null ? "" : t.name.toUpperCase()), W / 2 - 60, y + 2, 32f, 300, 0xFFFFFFFF, UIBatch.CENTER, 0, 0);
            if (ui.button("nextcar", W - 300, y - 40, 120, 80, 0xFF8E62FF, ">", 50f)) nextSpectate();
            if (ui.button("skip", W - 170, y - 40, 120, 80, 0xFFFF9A2B, "SKIP", 34f)) skipping = true;
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
        float pw = 560, ph = dev ? 760 : 520;
        float px = W / 2 - pw / 2, py = H / 2 - ph / 2;
        ui.panel(px, py, pw, ph, 0xFFFFFFFF);
        b.text(b.title, "PAUSED", W / 2, py + 75, 80f, 0xFF8E62FF, UIBatch.CENTER, 0xFF2A1840, 6f);
        if (ui.button("resume", px + 60, py + 160, pw - 120, 130, 0xFF34D058, "RESUME", 60f)) paused = false;
        if (ui.button("quit", px + 60, py + 320, pw - 120, 120, 0xFFFF4FA3, "GIVE UP", 52f)) {
            paused = false;
            finish(true);
        }
        if (dev) {
            float y = py + 480;
            if (ui.button("cw", px + 40, y, 230, 100, 0xFFFFC21F, "WIN NOW", 36f)) {
                paused = false;
                Cheats.instantWin(match);
            }
            if (ui.button("cd", px + 290, y, 230, 100, 0xFFFFC21F, "DUCK HERE", 36f)) {
                paused = false;
                Cheats.duckHere(match);
            }
            if (ui.button("cs", px + 40, y + 120, 230, 100, 0xFFFFC21F, "SKIP RND", 36f)) {
                paused = false;
                match.timer = 0.01f;
            }
            if (ui.button("cg", px + 290, y + 120, 230, 100, match.player != null && match.player.god ? 0xFF34D058 : 0xFFFFC21F, "GOD", 36f)) {
                if (match.player != null) match.player.god = !match.player.god;
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
            if (!resultsShown) {
                resultsShown = true;
                finish(false);
            }
            return true;
        }
        paused = !paused;
        game.input.reset();
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
