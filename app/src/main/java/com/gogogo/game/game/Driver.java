package com.gogogo.game.game;

/**
 * A car's cartoon driver: the look it wears (see Outfits) and a floppy ragdoll pose. The pose is a set of joints,
 * each pulled by a spring towards a target: the base pose (driving, standing, flailing) blended with overrides that
 * emotes set joint by joint (pose(), relax()), and shoved around by the car's acceleration, bumps and landings.
 * When its car is eliminated the driver is flung out and tumbles on its own through world space (fling()).
 * Allocated once per car; nothing here allocates per step.
 */
public final class Driver {
    // ---- joints: angles in radians, offsets in driver units ("its left" = the driver's own left)
    /** Torso: bend forward (+), lean to its left (+), turn to its left (+). */
    public static final int LEAN = 0, SIDE = 1, TWIST = 2;
    /** Head (on the torso): nod down (+), tilt to its left (+), look to its left (+). */
    public static final int NOD = 3, TILT = 4, LOOK = 5;
    /** Arms: swing forward / up (+), raise out sideways (+), bend the elbow (+). */
    public static final int L_ARM = 6, L_ARM_OUT = 7, L_ELBOW = 8, R_ARM = 9, R_ARM_OUT = 10, R_ELBOW = 11;
    /** Legs: swing forward (+), spread out (+), bend the knee (+). */
    public static final int L_LEG = 12, L_LEG_OUT = 13, L_KNEE = 14, R_LEG = 15, R_LEG_OUT = 16, R_KNEE = 17;
    /**
     * The whole body around the hips: rise up out of the seat (+), shift to its left (+), move forward (+), turn to
     * its left (+), stretch (+) or squash (-).
     */
    public static final int RISE = 18, SHIFT = 19, REACH = 20, TURN = 21, STRETCH = 22;
    public static final int JOINTS = 23;

    // ---- skeleton in driver units (one unit is one car unit at seat scale 1): the hips are the origin,
    // +y up, +z forward, +x to the driver's left
    public static final float NECK_Y = 0.5f, HEAD_R = 0.36f, HEAD_Y = 0.33f;
    public static final float SHOULDER_X = 0.29f, SHOULDER_Y = 0.42f, UPPER_ARM = 0.21f, FOREARM = 0.19f, HAND_R = 0.085f;
    public static final float HIP_X = 0.13f, HIP_Y = -0.06f, THIGH = 0.24f, SHIN = 0.22f, SOLE = 0.07f;
    /** Height of the hips over the floor when standing straight. */
    public static final float STAND_Y = -HIP_Y + THIGH + SHIN + SOLE;
    /** The steering wheel in front of a seated driver (center height, distance forward, tilt back from upright). */
    public static final float WHEEL_Y = 0.43f, WHEEL_Z = 0.36f, WHEEL_TILT = 1.0f, WHEEL_R = 0.17f;
    /** A HATCH seat: the hips sit this far below the rim of the hole, which has this radius. */
    public static final float HATCH_DEPTH = 0.27f, HATCH_R = 0.37f;

    /** Base poses: sitting in a car (or a showroom), standing on the floor. */
    public static final int SEATED = 0, STANDING = 1;

    // spring stiffness and damping per joint: torso, head (bobbly), arms (loose), legs, body
    private static final float[] K = {
            140, 140, 120, 90, 90, 80, 60, 60, 70, 60, 60, 70, 80, 80, 90, 80, 80, 90, 130, 120, 120, 100, 230};
    private static final float[] C = {
            10, 10, 10, 4.5f, 4.5f, 5, 5, 5, 6, 5, 5, 6, 7, 7, 8, 7, 7, 8, 12, 12, 12, 10, 9};
    private static final float[] MIN = {
            -1.3f, -1.1f, -1.6f, -1.2f, -1.2f, -1.7f, -1.4f, -0.4f, -0.1f, -1.4f, -0.4f, -0.1f,
            -1.2f, -0.3f, -0.1f, -1.2f, -0.3f, -0.1f, -0.8f, -1.6f, -1.2f, -3.3f, -0.45f};
    private static final float[] MAX = {
            1.4f, 1.1f, 1.6f, 1.2f, 1.2f, 1.7f, 3.4f, 2.6f, 2.6f, 3.4f, 2.6f, 2.6f,
            2.2f, 1.6f, 2.6f, 2.2f, 1.6f, 2.6f, 1.6f, 1.6f, 1.6f, 3.3f, 0.6f};

    /** What it wears: one item per Outfits part. */
    public final int[] look = new int[Outfits.PARTS];
    public int mode = SEATED;
    /** Not drawn (an empty seat: the hidden car in the garage). */
    public boolean hidden;

    /** Current joint values and their speeds. */
    public final float[] pos = new float[JOINTS], vel = new float[JOINTS];
    /** Pose overrides (emotes): the target of joint j is the base pose blended towards over[j] by weight[j] (0..1). */
    public final float[] over = new float[JOINTS], weight = new float[JOINTS];
    private final float[] target = new float[JOINTS];

    /** Own clock (idle animations), seconds of flailing left after a hard knock, where it is looking around. */
    public float time, flailT;
    private float lookAt, lookT, lastHopV;

    // ---- flung out of the car: world position (of the hips), velocity, spin
    public boolean flung;
    public float wx, wy, wz, wvx, wvy, wvz, wYaw, wPitch, wRoll, scale = 1f;
    private float spinYaw, spinPitch, spinRoll;

    public Driver() {
        Outfits.defaults(look);
    }

    public void setLook(int[] l) {
        System.arraycopy(l, 0, look, 0, Outfits.PARTS);
    }

    public boolean holding() {
        return look[Outfits.HELD] != Outfits.HD_NONE;
    }

    // ------------------------------------------------------------------ poses (for emotes)

    /** Pulls a joint towards value, blended with the base pose by w (0..1); stays until changed or relaxed. */
    public void pose(int joint, float value, float w) {
        over[joint] = value;
        weight[joint] = w;
    }

    /** Lets go of every override: back to the base pose (the springs ease into it). */
    public void relax() {
        for (int j = 0; j < JOINTS; j++) weight[j] = 0f;
    }

    /** Kicks a joint (adds speed): slaps, shakes, jolts. */
    public void kick(int joint, float speed) {
        vel[joint] += speed;
    }

    /** Snaps every joint to the current targets (no easing in): a fresh driver sits still right away. */
    public void settle() {
        baseTargets(false, 0f);
        for (int j = 0; j < JOINTS; j++) {
            pos[j] = target[j];
            vel[j] = 0f;
        }
    }

    // ------------------------------------------------------------------ stepping

    /** One fixed step riding in a car: aFwd and aLeft are the car's acceleration along it and to its left. */
    public void ride(Car c, float dt, float aFwd, float aLeft) {
        time += dt;
        if (flailT > 0) flailT -= dt;
        mode = SEATED;
        // a hard knock (a bump, a hit, a shove): the driver goes flying around in the seat for a moment
        float a2 = aFwd * aFwd + aLeft * aLeft;
        if (a2 > 160f * 160f) {
            float a = (float) Math.sqrt(a2);
            flailT = Math.max(flailT, Math.min(1.1f, 0.35f + a / 900f));
            kick(NOD, rnd(6f));
            kick(TILT, rnd(6f));
            kick(STRETCH, -1.5f);
        }
        if (c.falling) flailT = 0.5f;
        baseTargets(c.airborne, dt);
        // steering: lean and look into the turn
        float steer = c.steerVis;
        target[TWIST] += steer * 0.5f;
        target[LOOK] += steer * 0.9f;
        if (!holding()) target[R_ARM_OUT] -= steer * 0.3f;
        target[L_ARM_OUT] += steer * 0.3f;
        // speed: tuck in a little
        float sp = Math.min(1f, c.speed() / 18f);
        target[LEAN] += sp * 0.12f;
        // bumps lift the car: the driver sinks into the seat, landings squash it
        float aUp = (c.hopV - lastHopV) / dt;
        lastHopV = c.hopV;
        if (aUp > 0f) {
            vel[RISE] -= aUp * 0.004f;
            vel[STRETCH] -= aUp * 0.006f;
        }
        if (c.justLanded) {
            vel[STRETCH] -= Math.min(4f, c.landSpeed * 0.18f);
            vel[NOD] += Math.min(6f, c.landSpeed * 0.25f);
            flailT = Math.max(flailT, 0.3f);
        }
        // inertia: thrown back when speeding up, to the outside of a turn
        float fl = -aFwd, fs = -aLeft;
        springs(dt, fl * 1.2f, fs * 1.0f, fl * 1.0f, fs * 0.9f, fl * 0.5f, fs * 0.5f);
    }

    /** One step in a showroom (no car moving it): idle fidgets only. */
    public void idle(float dt) {
        time += dt;
        if (flailT > 0) flailT -= dt;
        baseTargets(false, dt);
        springs(dt, 0, 0, 0, 0, 0, 0);
    }

    /** Thrown out of an eliminated car: leaves its seat on a tumbling arc of its own. */
    public void fling(Car c) {
        if (flung) return;
        CarDef d = c.def;
        float fx = (float) Math.sin(c.yaw), fz = (float) Math.cos(c.yaw);
        // seat position in the world (the car's left is (fz, -fx))
        wx = c.x + fx * d.seatZ + fz * d.seatX;
        wz = c.z + fz * d.seatZ - fx * d.seatX;
        wy = c.y + c.hop + d.seatY;
        wvx = c.vx * 0.35f + rnd(2.5f);
        wvz = c.vz * 0.35f + rnd(2.5f);
        wvy = Math.max(0f, c.vy) + 9f + (float) Math.random() * 4f;
        wYaw = c.yaw;
        wPitch = 0f;
        wRoll = 0f;
        spinYaw = rnd(5f);
        spinPitch = rnd(7f);
        spinRoll = rnd(6f);
        scale = d.seatScale;
        flung = true;
        flailT = 99f;
        for (int j = 0; j < JOINTS; j++) vel[j] += rnd(4f);
    }

    /** One step tumbling through the air after a fling. */
    public void tumble(float dt) {
        if (wy < -95f) return;
        time += dt;
        wvy -= Arena.GRAVITY * 0.8f * dt;
        wx += wvx * dt;
        wy += wvy * dt;
        wz += wvz * dt;
        float k = (float) Math.exp(-0.4f * dt);
        wvx *= k;
        wvz *= k;
        wYaw += spinYaw * dt;
        wPitch += spinPitch * dt;
        wRoll += spinRoll * dt;
        mode = STANDING;
        baseTargets(false, dt);
        springs(dt, 0, 0, 0, 0, 0, 0);
    }

    /** Back in the seat (a new match, a showroom). */
    public void reset() {
        flung = false;
        flailT = 0f;
        relax();
        settle();
    }

    /** Fills target[] with the base pose for the mode, flailing and arms-up variations, then the overrides. */
    private void baseTargets(boolean air, float dt) {
        float t = time;
        float[] g = target;
        for (int j = 0; j < JOINTS; j++) g[j] = 0f;
        boolean held = holding();
        // breathing and looking around
        g[STRETCH] = (float) Math.sin(t * 2.3f) * 0.025f;
        lookT -= dt;
        if (lookT <= 0f) {
            lookT = 1.5f + (float) Math.random() * 3f;
            lookAt = Math.random() < 0.4 ? 0f : rnd(0.7f);
        }
        g[LOOK] = lookAt;
        g[NOD] = 0.05f;
        if (mode == SEATED) {
            g[LEAN] = 0.1f;
            // left hand on the wheel; the right one holds the thing (waving it a little) or grips the wheel too
            g[L_ARM] = 1.25f;
            g[L_ARM_OUT] = -0.12f;
            g[L_ELBOW] = 0.75f;
            if (held) {
                g[R_ARM] = 0.55f + (float) Math.sin(t * 2.6f) * 0.12f;
                g[R_ARM_OUT] = 0.7f;
                g[R_ELBOW] = 1.7f + (float) Math.sin(t * 2.6f + 1f) * 0.2f;
            } else {
                g[R_ARM] = 1.25f;
                g[R_ARM_OUT] = -0.12f;
                g[R_ELBOW] = 0.75f;
            }
            // sitting: thighs forward, shins down
            g[L_LEG] = g[R_LEG] = 1.5f;
            g[L_LEG_OUT] = g[R_LEG_OUT] = 0.12f;
            g[L_KNEE] = g[R_KNEE] = 1.45f;
        } else {
            // standing: arms hanging loose (the right one shows off the thing it holds), shifting its weight
            float sway = (float) Math.sin(t * 1.1f);
            g[SIDE] = sway * 0.04f;
            g[SHIFT] = sway * 0.03f;
            g[L_ARM] = 0.08f;
            g[L_ARM_OUT] = 0.2f;
            g[L_ELBOW] = 0.25f;
            if (held) {
                g[R_ARM] = 0.6f;
                g[R_ARM_OUT] = 0.25f;
                g[R_ELBOW] = 1.4f + (float) Math.sin(t * 2.1f) * 0.12f;
            } else {
                g[R_ARM] = 0.08f;
                g[R_ARM_OUT] = 0.2f;
                g[R_ELBOW] = 0.25f;
            }
            g[L_LEG_OUT] = g[R_LEG_OUT] = 0.06f;
            g[L_KNEE] = g[R_KNEE] = 0.05f;
        }
        if (air) {
            // WHEEE: both arms up
            g[L_ARM] = g[R_ARM] = 2.5f;
            g[L_ARM_OUT] = g[R_ARM_OUT] = 0.45f + (float) Math.sin(t * 9f) * 0.12f;
            g[L_ELBOW] = g[R_ELBOW] = 0.3f;
            g[RISE] = 0.12f;
            g[NOD] = -0.3f;
        }
        if (flailT > 0f) {
            // arms (and legs) windmilling, head all over the place
            float f = t * 14f;
            g[L_ARM] = 1.6f + (float) Math.sin(f) * 1.1f;
            g[R_ARM] = 1.6f + (float) Math.sin(f + 2.2f) * 1.1f;
            g[L_ARM_OUT] = 0.9f + (float) Math.sin(f * 0.7f) * 0.5f;
            g[R_ARM_OUT] = 0.9f + (float) Math.cos(f * 0.8f) * 0.5f;
            g[L_ELBOW] = 0.6f + (float) Math.sin(f * 1.3f) * 0.5f;
            g[R_ELBOW] = 0.6f + (float) Math.cos(f * 1.2f) * 0.5f;
            g[L_LEG] = 0.6f + (float) Math.sin(f + 1f) * 0.8f;
            g[R_LEG] = 0.6f + (float) Math.sin(f + 3f) * 0.8f;
            g[L_LEG_OUT] = g[R_LEG_OUT] = 0.35f;
            g[L_KNEE] = 0.8f + (float) Math.sin(f * 1.1f) * 0.6f;
            g[R_KNEE] = 0.8f + (float) Math.cos(f * 1.1f) * 0.6f;
            g[NOD] = (float) Math.sin(f * 0.5f) * 0.35f - 0.2f;
            g[TILT] = (float) Math.sin(f * 0.37f) * 0.4f;
        }
        for (int j = 0; j < JOINTS; j++) {
            float w = weight[j];
            if (w > 0f) g[j] += (over[j] - g[j]) * w;
        }
    }

    /**
     * Moves every joint one step along its spring. The forces push the torso (forward, sideways), the head and
     * the arms: inertia from the car's motion.
     */
    private void springs(float dt, float torsoF, float torsoS, float headF, float headS, float armF, float armS) {
        float[] p = pos, v = vel, g = target;
        for (int j = 0; j < JOINTS; j++) {
            float f = 0f;
            switch (j) {
                case LEAN: f = torsoF; break;
                case SIDE: f = torsoS; break;
                case NOD: f = headF; break;
                case TILT: f = headS; break;
                case L_ARM: case R_ARM: f = armF; break;
                case L_ARM_OUT: f = armS; break;
                case R_ARM_OUT: f = -armS; break;
                default: break;
            }
            v[j] += (K[j] * (g[j] - p[j]) - C[j] * v[j] + f) * dt;
            p[j] += v[j] * dt;
            if (p[j] < MIN[j]) {
                p[j] = MIN[j];
                if (v[j] < 0) v[j] *= -0.3f;
            } else if (p[j] > MAX[j]) {
                p[j] = MAX[j];
                if (v[j] > 0) v[j] *= -0.3f;
            }
        }
    }

    private static float rnd(float r) {
        return (float) (Math.random() * 2 - 1) * r;
    }
}
