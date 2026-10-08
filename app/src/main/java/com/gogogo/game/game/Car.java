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
    public float inX, inZ;        // desired direction (length 0..1)
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
    public boolean god;
    public boolean aliveAtRoundStart = true;

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
        if (falling) {
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

        float fx = (float) Math.sin(yaw), fz = (float) Math.cos(yaw);
        float mag = (float) Math.sqrt(inX * inX + inZ * inZ);
        if (!canDrive) mag = 0;
        if (mag > 1f) mag = 1f;
        float thr = 0f;
        float yawRate = 0f;
        if (mag > 0.08f) {
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
        float g = (float) Math.exp(-grip * dt * (boostT > 0 ? 0.35f : 1f));
        latx *= g;
        latz *= g;
        float drag = thr > 0.05f ? 0.35f : 2.6f;
        fwd *= (float) Math.exp(-drag * dt);
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

        // visual: wheel spin, lean, topper wobble spring driven by acceleration
        wheelSpin += fwd * dt / Math.max(0.2f, def.wheelR);
        float accX = (vx - lastVx) / dt, accZ = (vz - lastVz) / dt;
        lastVx = vx;
        lastVz = vz;
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
}
