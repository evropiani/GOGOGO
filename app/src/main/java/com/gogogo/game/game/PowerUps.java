package com.gogogo.game.game;

/**
 * Power-ups: the pickups lying on the map, the snowballs and ice blocks in flight, and what each power-up does
 * when a car uses it. A car holds at most one power-up and drives through pickups until it has used it.
 * Everything lives in fixed pools (no allocation while a match runs).
 */
public final class PowerUps {
    public static final int SNOWBALL = 0, ICE = 1, JUMP = 2, STICKY = 3, SUPER_BUMP = 4, COUNT = 5;
    public static final String[] NAME = {"SNOWBALL", "ICE BLOCK", "SUPER JUMP", "STICKY WHEELS", "SUPER BUMP"};
    /** Label on the power button. */
    public static final String[] SHORT = {"SNOWBALL", "ICE", "JUMP", "STICKY", "SUPER BUMP"};
    /** Pop-up word when the player grabs one. */
    public static final String[] GOT = {"SNOWBALL!", "ICE BLOCK!", "SUPER JUMP!", "STICKY WHEELS!", "SUPER BUMP!"};
    /** Signature color of each power-up (RGB). */
    public static final int[] COLOR = {0xDDEEFF, 0x45D8FF, 0x7BEA3C, 0xD05CFF, 0xFF7A1F};
    /** How often each kind shows up, relative to the others. */
    private static final float[] WEIGHT = {1.25f, 0.9f, 1f, 0.9f, 0.95f};

    // ---- tuning
    /** Ice block: seconds frozen, and the grip (and coasting drag) of a frozen car: it slides like a puck. */
    public static final float FREEZE_TIME = 2.5f, ICE_GRIP = 1.4f;
    /** Ice block: seeks the nearest car within this range inside a forward cone of +-30 degrees (cosine). */
    public static final float ICE_RANGE = 16f, ICE_CONE = 0.866f, ICE_SPEED = 27f;
    /** Sticky wheels: seconds, and the grip multiplier (+25%). */
    public static final float STICKY_TIME = 6f, STICKY_GRIP = 1.25f;
    /** Super jump: seconds in the air and apex height when jumping from the floor (a floaty arc). */
    public static final float JUMP_TIME = 2.6f, JUMP_HEIGHT = 12f;
    public static final float JUMP_V = 4f * JUMP_HEIGHT / JUMP_TIME, JUMP_GRAVITY = 8f * JUMP_HEIGHT / (JUMP_TIME * JUMP_TIME);
    /** Air control: push along the stick, and the top speed over the ground while in the air. */
    public static final float AIR_ACCEL = 9f, AIR_MAX = 6.5f;
    /** A car holding a super jump that loses its footing can still jump during this many seconds of its fall. */
    public static final float RESCUE_TIME = 0.4f;
    /** Snowball: speed, how far it carries, and the knockback of a hit. */
    public static final float SNOW_SPEED = 30f, SNOW_RANGE = 25f, SNOW_PUSH = 15f;
    private static final float SNOW_LIFT = 3f, SNOW_GRAVITY = 10f;
    /** Super bump: shockwave radius and the outward push at its middle (weaker towards the rim). */
    public static final float SHOCK_RADIUS = 6.5f, SHOCK_PUSH = 21f;
    /** Seconds a car hit by a power-up keeps less grip (so the push carries). */
    public static final float SLIP_TIME = 0.4f;
    /** Distance at which a car grabs a pickup. */
    public static final float PICK_RADIUS = 1.75f;
    /** Share of the pickups that show up on the safe color while it is showing. */
    private static final float SAFE_SHARE = 0.5f;

    public final Match m;
    /** False in previews: no pickups ever show up. */
    public boolean enabled = true;

    // ---- pickups lying on tiles
    public static final int MAX_PICKUPS = 8;
    public final boolean[] pickOn = new boolean[MAX_PICKUPS];
    public final int[] pickType = new int[MAX_PICKUPS], pickTile = new int[MAX_PICKUPS];
    public final float[] pickX = new float[MAX_PICKUPS], pickZ = new float[MAX_PICKUPS], pickAge = new float[MAX_PICKUPS];
    /** Pickup lying on each tile (index into the pick arrays), -1 = none. */
    public final int[] tilePick;
    public int pickCount;
    private float spawnT = 1f;

    // ---- snowballs and ice blocks in flight
    public static final int MAX_SHOTS = 24;
    public final boolean[] shotOn = new boolean[MAX_SHOTS];
    public final int[] shotType = new int[MAX_SHOTS], shotOwner = new int[MAX_SHOTS], shotTarget = new int[MAX_SHOTS];
    public final float[] shotX = new float[MAX_SHOTS], shotY = new float[MAX_SHOTS], shotZ = new float[MAX_SHOTS];
    public final float[] shotVX = new float[MAX_SHOTS], shotVY = new float[MAX_SHOTS], shotVZ = new float[MAX_SHOTS];
    public final float[] shotAge = new float[MAX_SHOTS];

    /** Where the last event happened (for events without a car, e.g. a snowball hitting the floor). */
    public float evX, evY, evZ;

    /** Totals this match (balance checks). */
    public int spawned, used;

    public PowerUps(Match m) {
        this.m = m;
        tilePick = new int[m.arena.tiles.length];
        for (int i = 0; i < tilePick.length; i++) tilePick[i] = -1;
    }

    /** Most pickups lying on the map at once: about one per 15 cars still in, 1 to 6. */
    public static int maxOnMap(int alive) {
        return Math.max(1, Math.min(6, (alive + 14) / 15));
    }

    // ------------------------------------------------------------------ per step

    /** Uses the power-ups the cars asked for (before the cars move). */
    public void useRequested(boolean canDrive) {
        for (Car c : m.cars) {
            if (!c.wantPower) continue;
            c.wantPower = false;
            if (!canDrive || !c.alive || c.power < 0 || c.frozenT > 0 || c.airborne) continue;
            if (c.falling && (c.power != JUMP || c.fallT >= RESCUE_TIME)) continue;
            use(c);
        }
    }

    /** Pickups (spawning, riding their tiles, being grabbed) and shots in flight. After the cars moved. */
    public void update(float dt) {
        if (!enabled) return;
        Arena a = m.arena;
        for (int i = 0; i < MAX_PICKUPS; i++) {
            if (!pickOn[i]) continue;
            pickAge[i] += dt;
            Arena.Tile t = a.tiles[pickTile[i]];
            // a pickup rides its tile down when it drops, and is gone with it
            if (t.state == Arena.GONE || t.state == Arena.SPAWNING || t.y < -25f) removePickup(i);
        }
        boolean live = m.phase == Match.SHOW || m.phase == Match.DROP;
        if (live) {
            spawnT -= dt;
            if (spawnT <= 0f) {
                if (pickCount < maxOnMap(m.alive)) spawnPickup();
                spawnT = m.rng.range(1.8f, 3.2f) * (m.alive <= 12 ? 1.4f : 1f);
            }
            grab();
        }
        updateShots(dt);
    }

    private void spawnPickup() {
        Arena a = m.arena;
        // mostly on the safe color (a little bonus for getting there); late in a round only there (the rest drops)
        boolean safeOnly = m.phase == Match.SHOW && (m.timer < 1.2f || m.rng.chance(SAFE_SHARE));
        for (int tries = 0; tries < 40; tries++) {
            Arena.Tile t = a.tiles[m.rng.i(a.tiles.length)];
            if (!t.exists || t.state != Arena.PRESENT || tilePick[t.index] >= 0) continue;
            if (safeOnly && t.color != m.target) continue;
            float px = t.x + m.rng.range(-0.8f, 0.8f), pz = t.z + m.rng.range(-0.8f, 0.8f);
            boolean crowded = false;
            for (Car c : m.cars) {
                if (!c.alive) continue;
                float dx = c.x - px, dz = c.z - pz;
                if (dx * dx + dz * dz < 3.5f * 3.5f) {
                    crowded = true;
                    break;
                }
            }
            if (crowded) continue;
            int slot = -1;
            for (int i = 0; i < MAX_PICKUPS; i++) {
                if (!pickOn[i]) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) return;
            pickOn[slot] = true;
            pickType[slot] = randomType();
            pickTile[slot] = t.index;
            pickX[slot] = px;
            pickZ[slot] = pz;
            pickAge[slot] = 0f;
            tilePick[t.index] = slot;
            pickCount++;
            spawned++;
            return;
        }
    }

    private int randomType() {
        float sum = 0f;
        for (int i = 0; i < COUNT; i++) sum += WEIGHT[i];
        float r = m.rng.f() * sum;
        for (int i = 0; i < COUNT; i++) {
            r -= WEIGHT[i];
            if (r < 0f) return i;
        }
        return COUNT - 1;
    }

    private void removePickup(int i) {
        pickOn[i] = false;
        if (tilePick[pickTile[i]] == i) tilePick[pickTile[i]] = -1;
        pickCount--;
    }

    /** Cars with empty hands that drive over a pickup take it. */
    private void grab() {
        Arena a = m.arena;
        float rr = PICK_RADIUS * PICK_RADIUS;
        for (int i = 0; i < MAX_PICKUPS; i++) {
            if (!pickOn[i] || a.tiles[pickTile[i]].state != Arena.PRESENT) continue;
            for (Car c : m.cars) {
                if (!c.alive || c.falling || c.airborne || c.power >= 0) continue;
                float dx = c.x - pickX[i], dz = c.z - pickZ[i];
                if (dx * dx + dz * dz < rr) {
                    c.power = pickType[i];
                    c.pickups++;
                    evX = pickX[i];
                    evY = 1.4f;
                    evZ = pickZ[i];
                    removePickup(i);
                    m.emit(Match.EV_PICKUP, c, null, c.power);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ using power-ups

    private void use(Car c) {
        int type = c.power;
        c.power = -1;
        c.powersUsed++;
        used++;
        boolean rescue = false;
        switch (type) {
            case SNOWBALL:
                launch(c, SNOWBALL, -1);
                break;
            case ICE:
                launch(c, ICE, iceTarget(c));
                break;
            case JUMP:
                rescue = c.falling;
                c.superJump();
                if (rescue) c.rescues++;
                break;
            case STICKY:
                c.stickyT = STICKY_TIME;
                break;
            default:
                shockwave(c);
                break;
        }
        evX = c.x;
        evY = c.y;
        evZ = c.z;
        m.emit(Match.EV_POWER, c, null, type);
        if (rescue) m.emit(Match.EV_RESCUE, c, null, 0);
    }

    /** The car an ice block thrown by c goes for: the nearest one in front within range, -1 if none. */
    public int iceTarget(Car c) {
        return carInFront(c, ICE_RANGE, ICE_CONE, true);
    }

    /** A car a snowball thrown by c would fly at (nearly straight ahead), -1 if none. For bots. */
    public int snowTarget(Car c) {
        return carInFront(c, SNOW_RANGE - 3f, 0.97f, false);
    }

    private int carInFront(Car c, float range, float cone, boolean skipFrozen) {
        float fx = (float) Math.sin(c.yaw), fz = (float) Math.cos(c.yaw);
        int best = -1;
        float bd = range * range;
        for (Car o : m.cars) {
            if (o == c || !o.alive || o.falling || o.airborne || (skipFrozen && o.frozenT > 0)) continue;
            float dx = o.x - c.x, dz = o.z - c.z;
            float d2 = dx * dx + dz * dz;
            if (d2 >= bd || d2 < 0.01f) continue;
            float d = (float) Math.sqrt(d2);
            if ((dx * fx + dz * fz) / d < cone) continue;
            bd = d2;
            best = o.index;
        }
        return best;
    }

    private void launch(Car c, int type, int target) {
        int s = -1;
        float oldest = -1f;
        for (int i = 0; i < MAX_SHOTS; i++) {
            if (!shotOn[i]) {
                s = i;
                break;
            }
            if (shotAge[i] > oldest) {
                oldest = shotAge[i];
                s = i;
            }
        }
        float fx = (float) Math.sin(c.yaw), fz = (float) Math.cos(c.yaw);
        shotOn[s] = true;
        shotType[s] = type;
        shotOwner[s] = c.index;
        shotTarget[s] = target;
        shotAge[s] = 0f;
        shotX[s] = c.x + fx * 1.5f;
        shotZ[s] = c.z + fz * 1.5f;
        shotY[s] = c.y + 1.3f;
        if (type == SNOWBALL) {
            shotVX[s] = fx * SNOW_SPEED + c.vx * 0.3f;
            shotVZ[s] = fz * SNOW_SPEED + c.vz * 0.3f;
            shotVY[s] = SNOW_LIFT;
        } else {
            shotY[s] = c.y + 1.0f;
            float dx = fx, dz = fz;
            if (target >= 0) {
                Car t = m.cars[target];
                float tx = t.x - shotX[s], tz = t.z - shotZ[s];
                float tl = (float) Math.sqrt(tx * tx + tz * tz);
                if (tl > 0.01f) {
                    dx = tx / tl;
                    dz = tz / tl;
                }
            }
            shotVX[s] = dx * ICE_SPEED;
            shotVZ[s] = dz * ICE_SPEED;
            shotVY[s] = 0.6f;
        }
    }

    private void updateShots(float dt) {
        Car[] cars = m.cars;
        for (int i = 0; i < MAX_SHOTS; i++) {
            if (!shotOn[i]) continue;
            shotAge[i] += dt;
            int type = shotType[i];
            if (type == ICE && shotTarget[i] >= 0) {
                // homing: turn towards the target, keep the speed
                Car t = cars[shotTarget[i]];
                if (!t.alive || t.falling || t.airborne || shotAge[i] > 1.4f) {
                    shotTarget[i] = -1;
                } else {
                    float tx = t.x - shotX[i], ty = t.y + 0.8f - shotY[i], tz = t.z - shotZ[i];
                    float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz) + 1e-4f;
                    float k = 1f - (float) Math.exp(-10f * dt);
                    shotVX[i] += (tx / tl * ICE_SPEED - shotVX[i]) * k;
                    shotVY[i] += (ty / tl * ICE_SPEED - shotVY[i]) * k;
                    shotVZ[i] += (tz / tl * ICE_SPEED - shotVZ[i]) * k;
                }
            } else {
                // snowballs arc; a stray ice block flies on and sinks once past its range
                float g = type == SNOWBALL ? SNOW_GRAVITY : (shotAge[i] > ICE_RANGE / ICE_SPEED * 1.4f ? 14f : 1.2f);
                shotVY[i] -= g * dt;
            }
            shotX[i] += shotVX[i] * dt;
            shotY[i] += shotVY[i] * dt;
            shotZ[i] += shotVZ[i] * dt;

            // hit the first car it touches (never the thrower)
            Car hit = null;
            for (Car c : cars) {
                if (c.index == shotOwner[i] || !c.alive || c.falling) continue;
                float dy = shotY[i] - (c.y + c.hop);
                if (dy < -0.6f || dy > 2.4f) continue;
                float dx = shotX[i] - c.x, dz = shotZ[i] - c.z;
                float r = c.radius + 0.4f;
                if (dx * dx + dz * dz < r * r) {
                    hit = c;
                    break;
                }
            }
            if (hit != null) {
                shotOn[i] = false;
                if (type == SNOWBALL) splat(i, hit);
                else freeze(i, hit);
                continue;
            }
            // the floor (or the abyss) ends it
            if (shotY[i] <= 0.3f && m.arena.supported(shotX[i], shotZ[i]) && shotY[i] > -0.8f) {
                shotOn[i] = false;
                evX = shotX[i];
                evY = 0.3f;
                evZ = shotZ[i];
                m.emit(Match.EV_SPLAT, owner(i), null, type);
            } else if (shotY[i] < -40f) {
                shotOn[i] = false;
            }
        }
    }

    private Car owner(int shot) {
        return m.cars[shotOwner[shot]];
    }

    private void splat(int shot, Car c) {
        float vx = shotVX[shot], vz = shotVZ[shot];
        float l = (float) Math.sqrt(vx * vx + vz * vz) + 1e-4f;
        float push = SNOW_PUSH / (float) Math.sqrt(c.mass);
        c.vx += vx / l * push;
        c.vz += vz / l * push;
        knock(c, owner(shot), SNOWBALL, 6f);
        evX = shotX[shot];
        evY = shotY[shot];
        evZ = shotZ[shot];
        m.emit(Match.EV_SPLAT, owner(shot), c, SNOWBALL);
    }

    private void freeze(int shot, Car c) {
        c.freeze();
        c.flash = 1f;
        c.lastHitBy = shotOwner[shot];
        c.lastHitTime = m.time;
        c.lastHitPower = ICE;
        evX = c.x;
        evY = c.y + 1f;
        evZ = c.z;
        m.emit(Match.EV_FREEZE, owner(shot), c, 0);
    }

    /** Super bump: everybody close by gets shoved away from the car, harder the closer they are. */
    private void shockwave(Car c) {
        float r2 = SHOCK_RADIUS * SHOCK_RADIUS;
        for (Car o : m.cars) {
            if (o == c || !o.alive || o.falling || (o.airborne && o.y > 2.5f)) continue;
            float dx = o.x - c.x, dz = o.z - c.z;
            float d2 = dx * dx + dz * dz;
            if (d2 > r2) continue;
            float d = (float) Math.sqrt(d2);
            float nx, nz;
            if (d < 0.05f) {
                nx = (float) Math.sin(c.yaw);
                nz = (float) Math.cos(c.yaw);
            } else {
                nx = dx / d;
                nz = dz / d;
            }
            float k = 1f - 0.55f * d / SHOCK_RADIUS;
            float push = SHOCK_PUSH * k / (float) Math.sqrt(o.mass);
            o.vx += nx * push;
            o.vz += nz * push;
            knock(o, c, SUPER_BUMP, 4f + 4f * k);
            m.emit(Match.EV_SHOCK, c, o, push);
        }
        c.squashV -= 9f;
        if (c.hop <= 0) {
            c.hopV = 4f;
            c.hop = 0.001f;
        }
    }

    /** Common reaction to a power-up hit: a hop, a flash, less grip for a moment, and who to thank. */
    private void knock(Car c, Car by, int type, float hop) {
        if (!c.airborne && c.hop <= 0) {
            c.hopV = hop;
            c.hop = 0.001f;
        }
        c.flash = 1f;
        c.squashV += 9f;
        c.slipT = SLIP_TIME;
        c.boostT = 0f;
        if (by != null) {
            c.lastHitBy = by.index;
            c.lastHitTime = m.time;
            c.lastHitPower = type;
        }
    }
}
