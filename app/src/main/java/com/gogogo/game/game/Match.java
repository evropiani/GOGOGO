package com.gogogo.game.game;

import com.gogogo.game.engine.Rng;

import java.util.ArrayList;

/** One round-based elimination match: rules, timing, bots, collisions. Rendering-free. */
public final class Match {
    public static final int INTRO = 0, SHOW = 1, DROP = 2, OVER = 4;

    public static final int EV_COUNT = 0, EV_ROUND = 1, EV_DROP = 2, EV_LAND = 3, EV_ELIM = 4, EV_BUMP = 5,
            EV_BOOST = 6, EV_WIN = 7, EV_TIE = 8, EV_BONK = 9, EV_DUCK_SPAWN = 10, EV_DUCK_GET = 11, EV_TICK = 12,
            EV_LAST_ONE = 13;
    /**
     * Power-up events (see PowerUps; where it happened is in power.evX/evY/evZ): EV_PICKUP a grabbed value's type,
     * EV_POWER a used value's type, EV_SPLAT a snowball (value = type: an ice block shatters) hit car b or the floor
     * (b = null), EV_FREEZE an ice block from a froze b, EV_THAW a broke out of the ice, EV_LANDED a came down from a
     * super jump (value = speed), EV_SHOCK a's super bump shoved b (value = push), EV_RESCUE a jumped out of a fall.
     */
    public static final int EV_PICKUP = 14, EV_POWER = 15, EV_SPLAT = 16, EV_FREEZE = 17, EV_THAW = 18, EV_LANDED = 19,
            EV_SHOCK = 20, EV_RESCUE = 21;
    /** Car a started its emote (value = the Emotes move). */
    public static final int EV_EMOTE = 22;

    public interface Listener {
        void event(int type, Car a, Car b, float value);
    }

    /** Match settings (including developer toggles). */
    public static final class Options {
        public int bots = 99;
        /** Arena layout (see Maps). */
        public int map;
        /** If > 0: a plain size x size grid instead of the map (small previews). */
        public int size;
        public boolean god, freezeTimer, superSpeed, dumbBots, forceDuck, slowTimer, autopilot;
        public boolean attract; // title screen background (no player)
        /** No power-ups on the map (previews). */
        public boolean noPowerUps;
    }

    public static final float STEP = 1f / 60f;
    public static final float INTRO_TIME = 3.4f;
    /** Start lights: red, then yellow, then green when the first round starts (at INTRO_TIME). */
    public static final float LIGHT_RED = INTRO_TIME - 1.7f, LIGHT_YELLOW = INTRO_TIME - 0.85f;
    public static final float DROP_TIME = 2.2f;
    /** Seconds between two emotes: the player's (the EMOTE button), a bot's. */
    public static final float EMOTE_CD = 3f, BOT_EMOTE_CD = 8f;

    public final Arena arena;
    public final Rng rng;
    public final Options opt;
    /** Pickups on the map and power-ups in flight. */
    public final PowerUps power;
    public Car[] cars;
    public Car player;
    public Listener listener;

    public int phase = INTRO;
    public float phaseT;
    public int round;
    public int target;
    public int numColors = 3;
    public float timer, timerMax;
    public float time;
    public int alive;
    public int total;
    public Car winner;
    /** Set by the screen: the player won while driving in HOOD view. */
    public boolean hoodWin;
    public boolean tie;
    public final ArrayList<Car> tieGroup = new ArrayList<Car>();
    public int lastTick = -1;

    // the duck
    public int duckTile = -1;
    public boolean duckSpawned, duckCollected, duckLost;
    public float duckX, duckY, duckZ, duckVy;

    /** Where the camera is (set by the view each frame): emotes play towards it. */
    public float viewX, viewZ;
    /** Bot taunts: their own random sequence (the match's stays as it was), and how many may start soon. */
    private final Rng fun;
    private float tauntBudget = 2f;

    private float acc;
    private float finishT;
    private int countdown = 4;
    private final ArrayList<Car> justFell = new ArrayList<Car>();

    public Match(long seed, Options opt, Save save) {
        this.rng = new Rng(seed);
        this.opt = opt;
        int mapId = Math.max(0, Math.min(Maps.COUNT - 1, opt.map));
        arena = opt.size > 0 ? new Arena(opt.size, null) : new Arena(Maps.size(mapId), Maps.mask(mapId));
        power = new PowerUps(this);
        power.enabled = !opt.noPowerUps;
        int n = opt.attract ? Math.max(2, opt.bots) : opt.bots + 1;
        cars = new Car[n];
        total = n;
        String[] names = BotNames.pick(rng, n);
        int k = 0;
        if (!opt.attract) {
            int id = save.selectedCar;
            CarDef d = Cars.ALL[id];
            player = new Car(0, d, "YOU", true, save.levels[id]);
            player.setLook(save.carPaint[id], save.carAccent[id], save.carTopper[id], save.carWheel[id]);
            if (opt.superSpeed) {
                player.maxSpeed *= 1.6f;
                player.accel *= 1.6f;
                player.boostCooldown = 0.5f;
            }
            player.god = opt.god;
            player.manual = true;
            player.trail = save.trail;
            player.driver.setLook(save.look);
            if (opt.autopilot) player.bot = new Bot(rng, 0.9f);
            cars[k++] = player;
        }
        for (; k < n; k++) {
            int id;
            do {
                id = rng.i(Cars.ALL.length);
            } while (!Cars.forBots(id));
            // every bot gets its own skill, and its car its own upgrades (each stat on its own)
            float skill = Bot.randomSkill(rng);
            int[] lv = new int[4];
            for (int i = 0; i < 4; i++) lv[i] = Bot.randomLevel(rng, skill);
            Car c = new Car(k, Cars.ALL[id], names[k], false, lv);
            c.bot = new Bot(rng, skill);
            c.bot.dumb = opt.dumbBots;
            int paint = rng.chance(0.5f) ? Cars.ALL[id].defPaint : botPaint();
            int accent = rng.chance(0.6f) ? Cars.ALL[id].defAccent : botPaint();
            int topper = rng.chance(0.45f) ? 0 : rng.i(Palette.TOPPER_NAME.length);
            int wheel = rng.chance(0.6f) ? 0 : rng.i(Palette.WHEEL_NAME.length);
            c.setLook(paint, accent, topper, wheel);
            c.trail = rng.chance(0.3f) ? rng.i(Trails.COUNT) : 0;
            cars[k] = c;
        }
        fun = new Rng(seed * 17L + 3L);
        // the bots' drivers dress up from their own sequence (the match's rng stays as it was)
        Rng looks = new Rng(seed * 31L + 7L);
        for (Car c : cars) {
            if (!c.isPlayer) Outfits.random(looks, c.driver.look);
            c.driver.settle();
        }
        placeCars();
        alive = n;
        numColors = 3;
        target = rng.i(numColors);
        arena.assignColors(rng, numColors, target, 1f / numColors, 3);
    }

    private int botPaint() {
        int p;
        do {
            p = rng.i(Palette.PAINT.length);
        } while (!Palette.botPaint(p));
        return p;
    }

    private void placeCars() {
        int nt = arena.count;
        int[] order = new int[nt];
        for (int i = 0, k = 0; i < arena.tiles.length; i++) if (arena.tiles[i].exists) order[k++] = i;
        for (int i = nt - 1; i > 0; i--) {
            int j = rng.i(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        int ti = 0;
        for (int i = 0; i < cars.length; i++) {
            Car c = cars[i];
            Arena.Tile t;
            if (c.isPlayer) {
                // somewhere near the middle (on maps with a hole there: the closest tile not at its rim)
                t = arena.nearest(rng.range(-1.5f, 1.5f) * Arena.PITCH, rng.range(-1.5f, 1.5f) * Arena.PITCH, true);
            } else {
                t = arena.tiles[order[ti++ % nt]];
            }
            c.x = t.x + rng.range(-1.2f, 1.2f);
            c.z = t.z + rng.range(-1.2f, 1.2f);
            c.yaw = rng.range(-3.14f, 3.14f);
            // the player starts facing open floor, not a hole
            for (int k = 0; k < 16 && c.isPlayer && !clearAhead(c, 12f); k++) c.yaw = rng.range(-3.14f, 3.14f);
        }
        // separate overlaps
        for (int it = 0; it < 10; it++) collide(0f, false);
    }

    private boolean clearAhead(Car c, float d) {
        return arena.clearLine(c.x, c.z, c.x + (float) Math.sin(c.yaw) * d, c.z + (float) Math.cos(c.yaw) * d, 1.2f);
    }

    // ------------------------------------------------------------------ rules

    public static int colorsFor(int round) {
        return round <= 3 ? 3 : (round <= 7 ? 4 : (round <= 11 ? 5 : 6));
    }

    public float timeFor(int round) {
        float t = 7.0f;
        for (int i = 2; i <= round; i++) if (i % 3 != 0) t *= 0.92f;
        float min = alive <= 10 ? 1.6f : 2.0f;
        t = Math.max(min, t);
        if (opt.slowTimer) t *= 2f;
        return t;
    }

    private float targetShare(int round) {
        float p = 1f / numColors;
        if (round >= 12) p = 0.13f;
        if (round >= 16) p = 0.095f;
        if (round >= 20) p = 0.07f;
        if (alive <= 12) p = Math.min(p, 0.1f);
        if (alive <= 4) p = Math.min(p, 0.06f);
        return p;
    }

    private void startRound() {
        round++;
        for (Car c : cars) {
            if (c.alive && round > 1) c.roundsSurvived++;
            c.aliveAtRoundStart = c.alive;
        }
        numColors = colorsFor(round);
        int newTarget = rng.i(numColors);
        if (newTarget == target && rng.chance(0.6f)) newTarget = (newTarget + 1 + rng.i(numColors - 1)) % numColors;
        target = newTarget;
        arena.assignColors(rng, numColors, target, targetShare(round), 3);
        for (Arena.Tile t : arena.tiles) t.crowd = 0;
        {
            float cx = player != null && player.alive ? player.x : 0f;
            float cz = player != null && player.alive ? player.z : 0f;
            arena.respawn(rng, cx, cz);
        }
        timerMax = timeFor(round);
        timer = timerMax;
        if (player != null) {
            player.onTarget = false;
            player.safeAt = -1f;
        }
        lastTick = -1;
        phase = SHOW;
        phaseT = 0;
        emit(EV_ROUND, null, null, round);
        maybeSpawnDuck();
    }

    private void maybeSpawnDuck() {
        if (duckSpawned || opt.attract || player == null || !player.alive) return;
        if (round < 5 && !opt.forceDuck) return;
        if (!opt.forceDuck && !rng.chance(0.04f)) return;
        // somewhere a bit away from the player
        for (int tries = 0; tries < 60; tries++) {
            Arena.Tile t = arena.tiles[rng.i(arena.tiles.length)];
            if (!t.exists) continue;
            float dx = t.x - player.x, dz = t.z - player.z;
            float d = dx * dx + dz * dz;
            if (d > 80f && d < 600f) {
                duckTile = t.index;
                duckSpawned = true;
                duckX = t.x + rng.range(-1f, 1f);
                duckZ = t.z + rng.range(-1f, 1f);
                duckY = 0;
                emit(EV_DUCK_SPAWN, null, null, 0);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ simulation

    public void update(float dt) {
        acc += dt;
        int steps = 0;
        while (acc >= STEP && steps < 6) {
            step(STEP);
            acc -= STEP;
            steps++;
        }
        if (acc > STEP * 6) acc = 0;
    }

    public void step(float dt) {
        time += dt;
        phaseT += dt;
        switch (phase) {
            case INTRO: {
                // start lights: EV_COUNT 3 = red, 2 = yellow, 0 = green (go)
                int n = phaseT >= LIGHT_YELLOW ? 2 : (phaseT >= LIGHT_RED ? 3 : 4);
                if (n < countdown) {
                    countdown = n;
                    emit(EV_COUNT, null, null, n);
                }
                if (phaseT >= INTRO_TIME) {
                    emit(EV_COUNT, null, null, 0);
                    startRound();
                }
                break;
            }
            case SHOW: {
                if (!opt.freezeTimer) timer -= dt;
                trackClutch();
                if (timer <= LATE_TIME && timer + dt > LATE_TIME) markLate();
                if (timer <= GLOAT_TIME && timer + dt > GLOAT_TIME) gloatAtPlayer();
                int sec = (int) Math.ceil(timer);
                if (sec != lastTick && sec <= 3 && sec >= 1) {
                    lastTick = sec;
                    emit(EV_TICK, null, null, sec);
                }
                if (timer <= 0) {
                    timer = 0;
                    phase = DROP;
                    phaseT = 0;
                    if (player != null && player.alive && player.onTarget && player.safeAt >= 0f && player.safeAt < CLUTCH_TIME) {
                        player.clutches++;
                    }
                    arena.dropAllBut(rng, target);
                    emit(EV_DROP, null, null, 0);
                }
                break;
            }
            case DROP:
                if (phaseT >= 1.3f && phaseT - dt < 1.3f) closeCalls();
                if (phaseT >= DROP_TIME && alive > 1) startRound();
                break;
            default:
                break;
        }

        boolean canDrive = phase == SHOW || phase == DROP;
        for (Car c : cars) {
            if (c.bot != null && c.alive) c.bot.think(this, c, dt);
        }
        stepEmotes(dt);
        power.useRequested(canDrive);
        for (Car c : cars) {
            if (!c.alive && c.y < -80f) {
                if (c.driver.flung) c.driver.tumble(dt);
                continue;
            }
            c.step(arena, dt, canDrive && c.alive);
            // out once it loses its footing; holding a super jump buys a moment to jump out of the fall
            if (c.alive && c.falling && (c.power != PowerUps.JUMP || c.fallT >= PowerUps.RESCUE_TIME || !canDrive)) justFell.add(c);
            if (c.justBoosted) {
                c.boosts++;
                emit(EV_BOOST, c, null, 0);
            }
            if (c.justLanded) emit(EV_LANDED, c, null, c.landSpeed);
            if (c.justThawed) emit(EV_THAW, c, null, 0);
        }
        collide(dt, true);
        power.update(dt);
        arena.landed = 0;
        arena.update(dt);
        if (arena.landed > 0) emit(EV_LAND, null, null, arena.landed);
        updateDuck(dt);

        if (!justFell.isEmpty()) {
            int after = alive - justFell.size();
            for (Car c : justFell) {
                c.alive = false;
                c.elimTime = time;
                c.place = after + 1;
                c.driver.fling(c);
                if (c.lastHitBy >= 0 && time - c.lastHitTime < 2.2f) {
                    Car k = cars[c.lastHitBy];
                    if (k.alive) {
                        k.bonks++;
                        if (c.lastHitPower == PowerUps.NITRO) k.nitroBonks++;
                        k.lastBonkTime = time;
                        emit(EV_BONK, k, c, 0);
                        // a bot that knocks someone off rubs it in (almost always when it was the player: just as the
                        // camera comes over to it)
                        if (c == player) taunt(k, 1.05f + fun.f() * 0.2f, 0.85f, true);
                        else taunt(k, 0.3f + fun.f() * 0.4f, 0.2f, false);
                    }
                }
                emit(EV_ELIM, c, null, after + 1);
                if (c == player) laughAtPlayer();
            }
            alive = after;
            justFell.clear();
            if (alive == 1 && phase != OVER) emit(EV_LAST_ONE, null, null, 0);
        }

        if (phase != OVER && phase != INTRO) {
            if (alive <= 0) {
                tie = true;
                tieGroup.clear();
                for (Car c : cars) if (c.aliveAtRoundStart) tieGroup.add(c);
                if (tieGroup.isEmpty()) {
                    float last = 0;
                    for (Car c : cars) last = Math.max(last, c.elimTime);
                    for (Car c : cars) if (c.elimTime >= last - 1f) tieGroup.add(c);
                }
                for (Car c : tieGroup) c.place = 1;
                phase = OVER;
                phaseT = 0;
                emit(EV_TIE, null, null, tieGroup.size());
            } else if (alive == 1) {
                // (not while the last one is still trying to jump out of a fall)
                if (!lastOneFalling()) finishT += dt;
                if (finishT > 1.6f) {
                    for (Car c : cars) {
                        if (c.alive) {
                            winner = c;
                            c.place = 1;
                        }
                    }
                    phase = OVER;
                    phaseT = 0;
                    emit(EV_WIN, winner, null, 0);
                    // the winner shows off
                    winner.emoteCd = 0f;
                    winner.tauntIn = 0.7f;
                }
            } else {
                finishT = 0;
            }
        }
    }

    private boolean lastOneFalling() {
        for (Car c : cars) if (c.alive && c.falling) return true;
        return false;
    }

    /** Reaching the target color with less than this many seconds left counts as a clutch save. */
    public static final float CLUTCH_TIME = 0.5f;

    private void trackClutch() {
        Car p = player;
        if (p == null || !p.alive || p.falling) return;
        Arena.Tile t = arena.cellAt(p.x, p.z);
        boolean on = t != null && t.exists && t.color == target && !p.airborne;
        if (on && !p.onTarget) p.safeAt = Math.max(0f, timer);
        p.onTarget = on;
    }

    private void updateDuck(float dt) {
        if (duckTile < 0 || duckCollected || duckLost) return;
        Arena.Tile t = arena.tiles[duckTile];
        if (t.state == Arena.FALLING && t.delay <= 0 || t.state == Arena.GONE) {
            duckVy -= Arena.GRAVITY * dt;
            duckY += duckVy * dt;
            if (duckY < -40f) duckLost = true;
            return;
        }
        if (player != null && player.alive && !player.falling) {
            float dx = player.x - duckX, dz = player.z - duckZ;
            if (dx * dx + dz * dz < 2.2f) {
                duckCollected = true;
                emit(EV_DUCK_GET, player, null, 0);
            }
        }
    }

    private void collide(float dt, boolean events) {
        Car[] cs = cars;
        int n = cs.length;
        for (int i = 0; i < n; i++) {
            Car a = cs[i];
            if (!a.alive || a.falling || a.airborne) continue;
            for (int j = i + 1; j < n; j++) {
                Car b = cs[j];
                if (!b.alive || b.falling || b.airborne) continue;
                float dx = b.x - a.x, dz = b.z - a.z;
                float rr = a.radius + b.radius;
                float d2 = dx * dx + dz * dz;
                if (d2 >= rr * rr) continue;
                float d = (float) Math.sqrt(d2);
                float nx, nz;
                if (d < 1e-4f) {
                    nx = 1;
                    nz = 0;
                    d = 1e-4f;
                } else {
                    nx = dx / d;
                    nz = dz / d;
                }
                float ia = 1f / a.mass, ib = 1f / b.mass, tot = ia + ib;
                float overlap = rr - d;
                a.x -= nx * overlap * ia / tot;
                a.z -= nz * overlap * ia / tot;
                b.x += nx * overlap * ib / tot;
                b.z += nz * overlap * ib / tot;
                if (!events) continue;
                float vrel = (b.vx - a.vx) * nx + (b.vz - a.vz) * nz;
                if (vrel < 0) {
                    boolean boost = a.boostT > 0 || b.boostT > 0;
                    boolean nitro = a.nitroT > 0 || b.nitroT > 0;
                    float factor = boost ? 2.3f : (nitro ? 1.15f * PowerUps.NITRO_RAM : 1.15f);
                    float j0 = -(1f + 0.55f) * vrel / tot * factor;
                    j0 += 0.5f / tot; // always a little bouncy
                    a.vx -= j0 * ia * nx;
                    a.vz -= j0 * ia * nz;
                    b.vx += j0 * ib * nx;
                    b.vz += j0 * ib * nz;
                    float strength = -vrel * factor;
                    if (strength > 3f) {
                        // (rammed by a car burning nitro counts as a nitro hit)
                        a.lastHitBy = b.index;
                        a.lastHitTime = time;
                        a.lastHitPower = b.nitroT > 0 ? PowerUps.NITRO : -1;
                        b.lastHitBy = a.index;
                        b.lastHitTime = time;
                        b.lastHitPower = a.nitroT > 0 ? PowerUps.NITRO : -1;
                        float hop = Math.min(5f, strength * 0.25f);
                        if (a.hop <= 0) a.hopV = hop * ib / tot * 2f;
                        if (b.hop <= 0) b.hopV = hop * ia / tot * 2f;
                        if (a.hopV > 0) a.hop = 0.001f;
                        if (b.hopV > 0) b.hop = 0.001f;
                        a.flash = Math.min(1f, strength * 0.06f);
                        b.flash = Math.min(1f, strength * 0.06f);
                        a.squashV += strength * 0.5f;
                        b.squashV += strength * 0.5f;
                        a.jolt(strength, nx, nz);
                        b.jolt(strength, -nx, -nz);
                        emit(EV_BUMP, a, b, strength);
                        // shoving the player around is worth a taunt now and then
                        if (strength > 9f && player != null && (a == player || b == player)) {
                            taunt(a == player ? b : a, 0.45f, 0.3f, true);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ emotes

    /** Bots that only reach the target color with less than this many seconds left had a close call. */
    private static final float LATE_TIME = 0.8f;
    /** With this many seconds left, bots on the right color near a player on the wrong one may taunt it. */
    private static final float GLOAT_TIME = 1.8f;

    /** The EMOTE button, queued taunts, cooldowns; and every emoting driver turns to the camera. */
    private void stepEmotes(float dt) {
        tauntBudget = Math.min(2.5f, tauntBudget + dt * 0.22f);
        for (Car c : cars) {
            if (c.emoteCd > 0) c.emoteCd -= dt;
            if (c.tauntIn > 0) {
                c.tauntIn -= dt;
                if (c.tauntIn <= 0) {
                    c.tauntIn = 0f;
                    emote(c);
                }
            }
            if (c.wantEmote) {
                c.wantEmote = false;
                emote(c);
            }
            if (c.driver.emote >= 0) c.driver.aim = aimOf(c);
        }
    }

    /** Plays the car's signature emote if it can (alive, on its wheels, not frozen, not cooling down). */
    public boolean emote(Car c) {
        if (!c.alive || c.falling || c.frozenT > 0 || c.driver.flung || c.emoteCd > 0) return false;
        int move = Emotes.signature(c.def.id);
        c.driver.aim = aimOf(c);
        c.driver.play(move);
        c.emoteCd = c.isPlayer ? EMOTE_CD : BOT_EMOTE_CD;
        c.emotes++;
        if (time - c.lastBonkTime < 3f) c.gloats++;
        emit(EV_EMOTE, c, null, move);
        return true;
    }

    /** Which way the camera is from the car, as a turn from where it is heading. */
    private float aimOf(Car c) {
        float a = (float) Math.atan2(viewX - c.x, viewZ - c.z) - c.yaw;
        while (a > Math.PI) a -= 2f * (float) Math.PI;
        while (a < -Math.PI) a += 2f * (float) Math.PI;
        return a;
    }

    /**
     * Maybe queues a taunt (its emote, delay seconds from now) for a bot. Taunts share a budget so they stay a treat;
     * important ones (aimed at the player) may overdraw it a little.
     */
    private void taunt(Car c, float delay, float chance, boolean important) {
        if (c == null || c.isPlayer || c.bot == null || !c.alive || c.falling) return;
        if (c.tauntIn > 0 || c.emoteCd > 0 || c.driver.emoting()) return;
        if (tauntBudget < (important ? -1f : 1f) || fun.f() > chance) return;
        tauntBudget -= 1f;
        c.tauntIn = delay;
    }

    private boolean onTarget(Car c) {
        Arena.Tile t = arena.cellAt(c.x, c.z);
        return t != null && t.exists && t.state == Arena.PRESENT && t.color == target && !c.airborne;
    }

    private void markLate() {
        for (Car c : cars) c.late = c.alive && !c.isPlayer && !onTarget(c);
    }

    /** Bots that made it at the very last moment cheer about it. */
    private void closeCalls() {
        for (Car c : cars) {
            if (c.late && c.alive) taunt(c, fun.range(0f, 0.5f), 0.2f, false);
            c.late = false;
        }
    }

    /** The player is on the wrong color with time running out: bots close by that are safe rub it in. */
    private void gloatAtPlayer() {
        Car p = player;
        if (p == null || !p.alive || onTarget(p)) return;
        for (Car c : cars) {
            if (c == p || !c.alive) continue;
            float dx = c.x - p.x, dz = c.z - p.z;
            if (dx * dx + dz * dz < 12f * 12f && onTarget(c)) taunt(c, fun.range(0f, 0.6f), 0.35f, true);
        }
    }

    /** The player just fell: a few bots close by have a laugh. */
    private void laughAtPlayer() {
        Car p = player;
        int n = 0;
        for (Car c : cars) {
            if (n >= 3 || c == p || !c.alive) continue;
            float dx = c.x - p.x, dz = c.z - p.z;
            if (dx * dx + dz * dz < 16f * 16f && c.tauntIn <= 0) {
                taunt(c, 0.25f + n * 0.25f + fun.f() * 0.15f, 0.5f, true);
                if (c.tauntIn > 0) n++;
            }
        }
    }

    void emit(int type, Car a, Car b, float v) {
        if (listener != null) listener.event(type, a, b, v);
    }

    /** Runs the rest of the match instantly (skip button). Returns true when done. */
    public boolean fastForward(int maxSteps) {
        Listener l = listener;
        listener = null;
        for (int i = 0; i < maxSteps && phase != OVER; i++) step(STEP);
        listener = l;
        if (phase == OVER && l != null) {
            if (tie) l.event(EV_TIE, null, null, tieGroup.size());
            else if (winner != null) l.event(EV_WIN, winner, null, 0);
        }
        return phase == OVER;
    }

    public Car leader() {
        if (winner != null) return winner;
        for (Car c : cars) if (c.alive) return c;
        return null;
    }
}
