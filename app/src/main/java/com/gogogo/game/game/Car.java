package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;

/** A car in a match (player or bot): arcade physics state plus cosmetic animation state. */
public final class Car {
    public final int index;
    public final CarDef def;
    public final String name;
    public final boolean isPlayer;
    public Bot bot;

    // look
    public int paint, accent, topper, wheel;
    public int paintRgb, accentRgb;

    // tuning (derived from stats)
    public float maxSpeed, accel, turnRate, grip, boostImpulse, boostCooldown, mass, radius;

    // physics
    public float x, z, y, vx, vz, vy, yaw;
    public float inX, inZ;        // desired direction (length 0..1), used by bots
    /** Button controls (the player): steer -1 left .. 1 right, throttle 1 gas .. -1 brake/reverse. */
    public boolean manual;
    public float steer, throttle;
    public float manualTurn;
    public boolean wantBoost;
    public boolean alive = true, falling;
    public float boostT, boostCd;
    public float hop, hopV;       // visual hop
    public float rotX, rotZ, spinX, spinZ;
    public float fallT;

    // fx
    public float squash, squashV;
    public float flash;
    public float wheelSpin, steerVis;
    public float topX, topZ, topVX, topVZ;
    public float tilt;            // lean into turns
    public float lastAx, lastAz;

    // bookkeeping
    public int place;
    public float elimTime = -1f;
    public int lastHitBy = -1;
    public float lastHitTime = -10f;
    public int bonks;
    public int roundsSurvived;
    /** Boosts used and last-moment saves this match (player stats). */
    public int boosts, clutches;
    /** Round timer value when the car last rolled onto the target color (-1 = not on it). */
    public float safeAt = -1f;
    public boolean onTarget;
    /** Boost trail style (see Trails). */
    public int trail;
    public boolean god;
    public boolean aliveAtRoundStart = true;

    // power-ups (see PowerUps)
    /** Held power-up (PowerUps type), -1 = none. */
    public int power = -1;
    /** Set to use the held power-up this step (like wantBoost). */
    public boolean wantPower;
    /** Seconds left: frozen in an ice block, on sticky wheels, sliding after a power-up hit. */
    public float frozenT, stickyT, slipT;
    /** In the air after a super jump (no floor check until it comes down). */
    public boolean airborne;
    public float airT;
    /** Set for one step: came down from a super jump (landSpeed = how hard), thawed out of an ice block. */
    public boolean justLanded, justThawed;
    public float landSpeed;
    /** Power-up that gave the last hit (lastHitBy), -1 = a plain bump. */
    public int lastHitPower = -1;
    /** Power-ups picked up and used, and falls saved by a super jump, this match. */
    public int pickups, powersUsed, rescues;

    public Car(int index, CarDef def, String name, boolean isPlayer, int[] lv) {
        this.index = index;
        this.def = def;
        this.name = name;
        this.isPlayer = isPlayer;
        float sp = def.stat(CarDef.SPEED, lv[0]);
        float gr = def.stat(CarDef.GRIP, lv[1]);
        float bo = def.stat(CarDef.BOOST, lv[2]);
        float we = def.stat(CarDef.WEIGHT, lv[3]);
        maxSpeed = 10.5f + sp * 1.25f;
        accel = 22f + sp * 2.2f;
        turnRate = 3.6f + gr * 0.5f;
        grip = 4.5f + gr * 1.1f;
        boostImpulse = 9f + bo * 1.3f;
        boostCooldown = Math.max(1.4f, 4.2f - bo * 0.35f);
        mass = 0.9f + we * 0.18f;
        manualTurn = 2.3f + gr * 0.22f;
        radius = 1.08f;
    }

    public void setLook(int paint, int accent, int topper, int wheel) {
        this.paint = paint;
        this.accent = accent;
        this.topper = topper;
        this.wheel = wheel;
    }

    public float speed() {
        return (float) Math.sqrt(vx * vx + vz * vz);
    }

    /** Physics for one fixed step. Returns true if the car just lost its footing. */
    public boolean step(Arena arena, float dt, boolean canDrive) {
        justLanded = false;
        justThawed = false;
        if (falling) {
            justBoosted = false;
            wantBoost = false;
            fallT += dt;
            vy -= Arena.GRAVITY * dt;
            y += vy * dt;
            x += vx * dt;
            z += vz * dt;
            vx *= (float) Math.exp(-0.6f * dt);
            vz *= (float) Math.exp(-0.6f * dt);
            rotX += spinX * dt;
            rotZ += spinZ * dt;
            return false;
        }

        if (stickyT > 0) stickyT = Math.max(0f, stickyT - dt);
        if (slipT > 0) slipT = Math.max(0f, slipT - dt);
        if (frozenT > 0) {
            frozenT -= dt;
            if (frozenT <= 0) {
                frozenT = 0;
                justThawed = true;
            }
        }
        boolean frozen = frozenT > 0;
        if (frozen) canDrive = false;
        if (airborne) return stepAir(arena, dt, canDrive);

        float fx = (float) Math.sin(yaw), fz = (float) Math.cos(yaw);
        float mag = (float) Math.sqrt(inX * inX + inZ * inZ);
        if (!canDrive) mag = 0;
        if (mag > 1f) mag = 1f;
        float thr = 0f;
        float yawRate = 0f;
        boolean braking = false, reversing = false;
        if (manual && bot == null) {
            float st = canDrive ? Math.max(-1f, Math.min(1f, steer)) : 0f;
            float th = canDrive ? Math.max(-1f, Math.min(1f, throttle)) : 0f;
            float fwd0 = vx * fx + vz * fz;
            float dir = fwd0 < -0.3f ? -1f : 1f;
            float rate = manualTurn * Math.min(1f, 0.35f + Math.abs(fwd0) / 6f);
            // steering right turns clockwise seen from above (yaw decreases)
            yawRate = -st * rate * dir;
            yaw = Ease.wrapAngle(yaw + yawRate * dt);
            fx = (float) Math.sin(yaw);
            fz = (float) Math.cos(yaw);
            if (th > 0) thr = th;
            else if (th < 0) {
                if (fwd0 > 0.6f) braking = true;
                else reversing = true;
            }
        } else if (mag > 0.08f) {
            float target = (float) Math.atan2(inX, inZ);
            float diff = Ease.wrapAngle(target - yaw);
            float maxTurn = turnRate * dt * (0.55f + 0.45f * mag);
            float turn = Math.max(-maxTurn, Math.min(maxTurn, diff * 12f * dt));
            yaw = Ease.wrapAngle(yaw + turn);
            yawRate = turn / dt;
            float c = (float) Math.cos(diff);
            thr = mag * Math.max(0.2f, c);
            fx = (float) Math.sin(yaw);
            fz = (float) Math.cos(yaw);
        }
        steerVis = Ease.approach(steerVis, Math.max(-0.5f, Math.min(0.5f, yawRate * 0.12f)), 12f, dt);

        // throttle
        float ax = fx * accel * thr, az = fz * accel * thr;
        if (reversing) {
            ax = -fx * accel * 0.55f;
            az = -fz * accel * 0.55f;
        }
        vx += ax * dt;
        vz += az * dt;

        // boost
        if (boostCd > 0) boostCd -= dt;
        if (boostT > 0) boostT -= dt;
        boolean boosted = false;
        if (wantBoost && canDrive && boostCd <= 0) {
            vx += fx * boostImpulse;
            vz += fz * boostImpulse;
            boostT = 0.42f;
            boostCd = boostCooldown;
            squashV -= 6f;
            boosted = true;
        }
        wantBoost = false;
        justBoosted = boosted;

        // grip: kill lateral velocity, drag forward velocity
        float fwd = vx * fx + vz * fz;
        float latx = vx - fx * fwd, latz = vz - fz * fwd;
        float gr = grip;
        if (stickyT > 0) gr *= PowerUps.STICKY_GRIP;
        if (slipT > 0) gr *= 0.3f;
        if (frozen) gr = PowerUps.ICE_GRIP;
        float g = (float) Math.exp(-gr * dt * (boostT > 0 ? 0.35f : 1f));
        latx *= g;
        latz *= g;
        float drag = frozen ? PowerUps.ICE_GRIP : (braking ? 7f : (thr > 0.05f || reversing ? 0.35f : 2.6f));
        fwd *= (float) Math.exp(-drag * dt);
        if (reversing && fwd < -maxSpeed * 0.45f) fwd = -maxSpeed * 0.45f;
        vx = fx * fwd + latx;
        vz = fz * fwd + latz;

        float cap = maxSpeed * (boostT > 0 ? 1.65f : 1f);
        float sp = (float) Math.sqrt(vx * vx + vz * vz);
        if (sp > cap) {
            float k = Ease.approach(sp, cap, boostT > 0 ? 2f : 6f, dt) / sp;
            vx *= k;
            vz *= k;
        }

        x += vx * dt;
        z += vz * dt;

        // visual: wheel spin, lean, topper wobble spring driven by acceleration (all stuck while frozen)
        float accX = (vx - lastVx) / dt, accZ = (vz - lastVz) / dt;
        lastVx = vx;
        lastVz = vz;
        if (!frozen) {
            wheelSpin += fwd * dt / Math.max(0.2f, def.wheelR);
            tilt = Ease.approach(tilt, -yawRate * sp * 0.006f, 8f, dt);
            // local acceleration in car frame
            float aFwd = accX * fx + accZ * fz;
            float aSide = accX * fz - accZ * fx;
            topVX += (-topX * 90f - topVX * 7f - aSide * 0.05f) * dt;
            topVZ += (-topZ * 90f - topVZ * 7f - aFwd * 0.05f) * dt;
            topX += topVX * dt;
            topZ += topVZ * dt;
            topX = Math.max(-0.7f, Math.min(0.7f, topX));
            topZ = Math.max(-0.7f, Math.min(0.7f, topZ));
        }

        // squash spring + hop
        squashV += (-squash * 160f - squashV * 9f) * dt;
        squash += squashV * dt;
        if (hop > 0 || hopV != 0) {
            hopV -= 30f * dt;
            hop += hopV * dt;
            if (hop <= 0) {
                hop = 0;
                if (hopV < -3f) squashV -= hopV * 0.6f;
                hopV = 0;
            }
        }
        if (flash > 0) flash = Math.max(0f, flash - dt * 4f);

        if (!god && !arena.supported(x, z)) {
            falling = true;
            vy = Math.min(0f, vy);
            spinX = (float) (Math.random() * 6 - 3) + fwd * 0.2f;
            spinZ = (float) (Math.random() * 6 - 3);
            return true;
        }
        if (god && !arena.supported(x, z)) {
            // stay inside the grid
            x = Math.max(-arena.half + 1f, Math.min(arena.half - 1f, x));
            z = Math.max(-arena.half + 1f, Math.min(arena.half - 1f, z));
        }
        return false;
    }

    public boolean justBoosted;
    private float lastVx, lastVz;

    // ------------------------------------------------------------------ power-up effects

    /** Super jump: up into a long floaty arc. Also works in the first moments of a fall (cancels it). */
    public void superJump() {
        if (falling) {
            // rescued: the fall is undone, the tumble settles in the air
            falling = false;
            fallT = 0f;
            spinX = spinZ = 0f;
        }
        airborne = true;
        airT = 0f;
        vy = PowerUps.JUMP_V;
        hop = 0f;
        hopV = 0f;
        boostT = 0f;
        vx *= 0.55f;
        vz *= 0.55f;
        squashV += 9f;
    }

    /** Ice block: stuck in place (mostly) and frozen solid for a while. */
    public void freeze() {
        frozenT = PowerUps.FREEZE_TIME;
        vx *= 0.35f;
        vz *= 0.35f;
        boostT = 0f;
        wantBoost = false;
        stickyT = 0f;
    }

    /** One step in the air: limited air control, a floaty arc, landing or falling at the bottom of it. */
    private boolean stepAir(Arena arena, float dt, boolean canDrive) {
        airT += dt;
        float yawRate = 0f, thr = 0f;
        if (canDrive) {
            if (manual && bot == null) {
                float st = Math.max(-1f, Math.min(1f, steer));
                yawRate = -st * manualTurn * 0.8f;
                yaw = Ease.wrapAngle(yaw + yawRate * dt);
                thr = Math.max(-1f, Math.min(1f, throttle));
            } else {
                float mag = Math.min(1f, (float) Math.sqrt(inX * inX + inZ * inZ));
                if (mag > 0.08f) {
                    float diff = Ease.wrapAngle((float) Math.atan2(inX, inZ) - yaw);
                    float maxTurn = turnRate * 0.8f * dt;
                    float turn = Math.max(-maxTurn, Math.min(maxTurn, diff * 10f * dt));
                    yaw = Ease.wrapAngle(yaw + turn);
                    yawRate = turn / dt;
                    thr = mag * Math.max(0f, (float) Math.cos(diff));
                }
            }
        }
        float fx = (float) Math.sin(yaw), fz = (float) Math.cos(yaw);
        steerVis = Ease.approach(steerVis, Math.max(-0.5f, Math.min(0.5f, yawRate * 0.12f)), 12f, dt);
        if (thr > 0) {
            vx += fx * PowerUps.AIR_ACCEL * thr * dt;
            vz += fz * PowerUps.AIR_ACCEL * thr * dt;
        } else if (thr < 0) {
            float k = (float) Math.exp(2.5f * thr * dt);
            vx *= k;
            vz *= k;
        }
        float k = (float) Math.exp(-0.35f * dt);
        vx *= k;
        vz *= k;
        float sp = (float) Math.sqrt(vx * vx + vz * vz);
        if (sp > PowerUps.AIR_MAX) {
            vx *= PowerUps.AIR_MAX / sp;
            vz *= PowerUps.AIR_MAX / sp;
        }
        x += vx * dt;
        z += vz * dt;
        vy -= PowerUps.JUMP_GRAVITY * dt;
        y += vy * dt;
        lastVx = vx;
        lastVz = vz;

        // nose up on the way up, down on the way down; wheels spin free
        rotX = Ease.approach(rotX, Math.max(-0.35f, Math.min(0.35f, -vy * 0.02f)), 5f, dt);
        rotZ = Ease.approach(rotZ, 0f, 5f, dt);
        tilt = Ease.approach(tilt, 0f, 6f, dt);
        wheelSpin += 14f * dt;
        squashV += (-squash * 160f - squashV * 9f) * dt;
        squash += squashV * dt;
        if (flash > 0) flash = Math.max(0f, flash - dt * 4f);
        if (boostCd > 0) boostCd -= dt;
        if (boostT > 0) boostT -= dt;
        wantBoost = false;
        justBoosted = false;

        if (y <= 0f && vy < 0f) {
            airborne = false;
            if (god || arena.supported(x, z)) {
                landSpeed = -vy;
                y = 0f;
                vy = 0f;
                rotX = rotZ = 0f;
                squashV -= Math.min(16f, landSpeed * 0.8f);
                justLanded = true;
                return false;
            }
            // nothing underneath: down it goes
            falling = true;
            fallT = 0f;
            spinX = (float) (Math.random() * 6 - 3);
            spinZ = (float) (Math.random() * 6 - 3);
            return true;
        }
        return false;
    }
}
