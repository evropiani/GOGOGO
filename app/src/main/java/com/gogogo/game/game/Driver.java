package com.gogogo.game.game;

/**
 * A car's cartoon driver: the look it wears (see Outfits) and a floppy ragdoll pose. The pose is a set of joints,
 * each pulled by a spring-damper towards a target: the base pose (seated for the car's seat style, standing,
 * flailing, arms up in the air) blended joint by joint with overrides (pose(), relax()), and shoved around by the
 * car: acceleration, turns, bumps (jolt()), landings, power-up hits. When its car is eliminated the driver is flung
 * out and tumbles on its own through world space (fling(), tumble()).
 *
 * Emotes drive it like this, once per step or frame:
 * <pre>
 *     d.pose(Driver.RISE, 0.9f, 1f);     // climb half out of the seat
 *     d.pose(Driver.R_ARM, 2.9f, 1f);    // right arm straight up...
 *     d.pose(Driver.R_ELBOW, 0.4f + 0.4f * (float) Math.sin(t * 12f), 1f); // ...waving
 *     d.face = Outfits.FC_SILLY;         // pull a face
 *     ...
 *     d.relax();                         // done: the springs ease back into the base pose
 *     d.face = -1;
 * </pre>
 * Emotes (see Emotes) are played with play(): the driver steps them itself in ride() and idle(), and a hard knock,
 * a fall or an ice block cuts them short.
 * Allocated once per car; nothing here allocates per step.
 */
public final class Driver {
    // ---- joints: angles in radians, offsets in driver units ("its left" = the driver's own left)
    /** Torso: bend forward (+), lean to its left (+), turn to its left (+). */
    public static final int LEAN = 0, SIDE = 1, TWIST = 2;
    /** Head (on the torso): nod down (+), tilt to its left (+), look to its left (+). */
    public static final int NOD = 3, TILT = 4, LOOK = 5;
    /** Arms: swing forward / up (+, PI = straight up), raise out sideways (+), bend the elbow (+). */
    public static final int L_ARM = 6, L_ARM_OUT = 7, L_ELBOW = 8, R_ARM = 9, R_ARM_OUT = 10, R_ELBOW = 11;
    /** Legs: swing forward (+), spread out (+), bend the knee (+). */
    public static final int L_LEG = 12, L_LEG_OUT = 13, L_KNEE = 14, R_LEG = 15, R_LEG_OUT = 16, R_KNEE = 17;
    /**
     * The whole body around the hips: rise up out of the seat (+), shift to its left (+), move forward (+), turn to
     * its left (+, PI = facing backwards), stretch (+) or squash (-).
     */
    public static final int RISE = 18, SHIFT = 19, REACH = 20, TURN = 21, STRETCH = 22;
    /** The whole body tipping over around its middle: forward roll (+), backflip (-); only emotes move it. */
    public static final int FLIP = 23;
    public static final int JOINTS = 24;
    /** Joint names (debugging, emote tools). */
    public static final String[] JOINT_NAME = {"LEAN", "SIDE", "TWIST", "NOD", "TILT", "LOOK", "L_ARM", "L_ARM_OUT",
            "L_ELBOW", "R_ARM", "R_ARM_OUT", "R_ELBOW", "L_LEG", "L_LEG_OUT", "L_KNEE", "R_LEG", "R_LEG_OUT", "R_KNEE",
            "RISE", "SHIFT", "REACH", "TURN", "STRETCH", "FLIP"};

    // ---- skeleton in driver units (one unit is one car unit at seat scale 1): the hips are the origin,
    // +y up, +z forward, +x to the driver's left
    public static final float NECK_Y = 0.5f, HEAD_R = 0.36f, HEAD_Y = 0.33f;
    public static final float SHOULDER_X = 0.29f, SHOULDER_Y = 0.42f, UPPER_ARM = 0.21f, FOREARM = 0.19f, HAND_R = 0.085f;
    public static final float HIP_X = 0.13f, HIP_Y = -0.06f, THIGH = 0.24f, SHIN = 0.22f, SOLE = 0.07f;
    /** Top of the head (without a hat) over the hips. */
    public static final float HEAD_TOP = NECK_Y + HEAD_Y + HEAD_R;
    /** Height of the hips over the floor when standing straight. */
    public static final float STAND_Y = -HIP_Y + THIGH + SHIN + SOLE;
    /** The steering wheel in front of a seated driver (center height, distance forward, tilt back from upright). */
    public static final float WHEEL_Y = 0.43f, WHEEL_Z = 0.36f, WHEEL_TILT = 1.0f, WHEEL_R = 0.17f;
    /** A HATCH seat: the hips sit this far below the rim of the hole, which has this radius. */
    public static final float HATCH_DEPTH = 0.27f, HATCH_R = 0.37f;
    /** RISE at which the legs of a driver sitting in a hole come out (and get drawn). */
    public static final float LEGS_OUT = 0.3f;
    /** FLIP turns the body around this point over the hips (about its middle). */
    public static final float FLIP_Y = 0.35f;

    /** Base poses: sitting in a car, standing on the floor (showrooms, the podium, flung out). */
    public static final int SEATED = 0, STANDING = 1;

    // spring stiffness and damping per joint: torso, head (bobbly), arms (loose), legs, body
    private static final float[] K = {
            140, 140, 120, 90, 90, 80, 60, 60, 70, 60, 60, 70, 80, 80, 90, 80, 80, 90, 130, 120, 120, 100, 230, 160};
    private static final float[] C = {
            10, 10, 10, 4.5f, 4.5f, 5, 5, 5, 6, 5, 5, 6, 7, 7, 8, 7, 7, 8, 12, 12, 12, 10, 9, 14};
    private static final float[] MIN = {
            -1.3f, -1.1f, -1.6f, -1.2f, -1.2f, -1.7f, -1.4f, -0.4f, -0.1f, -1.4f, -0.4f, -0.1f,
            -1.2f, -0.3f, -0.1f, -1.2f, -0.3f, -0.1f, -0.8f, -1.6f, -1.6f, -3.3f, -0.45f, -1.7f};
    private static final float[] MAX = {
            1.4f, 1.1f, 1.6f, 1.2f, 1.2f, 1.7f, 3.4f, 2.6f, 2.6f, 3.4f, 2.6f, 2.6f,
            2.2f, 1.6f, 2.6f, 2.2f, 1.6f, 2.6f, 2.4f, 1.6f, 1.6f, 3.3f, 0.6f, 1.7f};

    /** What it wears: one item per Outfits part. */
    public final int[] look = new int[Outfits.PARTS];
    public int mode = SEATED;
    /** CarDef seat style of the car it sits in (picks how the legs sit) and, astride, how far they spread. */
    public int style = CarDef.HATCH;
    public float spread = 0.8f;
    /** Not drawn at all. */
    public boolean hidden;
    /** Face shown instead of the look's own (an Outfits face, -1 = its own). */
    public int face = -1;
    /** Multiplies the spring stiffness (snappier moves for emotes; 1 = normal). */
    public float stiffness = 1f;
    /** Thing in hand shown instead of the look's own (an Outfits HD_* value, -1 = its own): emote props. */
    public int prop = -1;

    /** The emote playing (an Emotes move, -1 = none) and how long it has been playing. */
    public int emote = -1;
    public float emoteT;
    /** Emote clock at the last Emotes.fx call (particles that go off at a moment). */
    float fxT;
    /**
     * Where its audience is, as a turn from the way its body faces (radians, + = to its left): emotes play towards
     * it (the camera, the player). Set by whoever plays the emote.
     */
    public float aim;
    /** Lets the head, the body turn and FLIP go round full circles (spins and flips), unclamped. */
    boolean loose;

    /** Current joint values and their speeds. */
    public final float[] pos = new float[JOINTS], vel = new float[JOINTS];
    /** Pose overrides: the target of joint j is the base pose blended towards over[j] by weight[j] (0..1). */
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
        return heldShown() != Outfits.HD_NONE;
    }

    /** The thing in its hand right now (an emote prop or its own). */
    public int heldShown() {
        return prop >= 0 ? prop : look[Outfits.HELD];
    }

    /** The face drawn right now. */
    public int faceShown() {
        return face >= 0 ? face : look[Outfits.FACE];
    }

    // ------------------------------------------------------------------ poses (for emotes)

    /** Pulls a joint towards value, blended with the base pose by w (0..1); stays until changed or relaxed. */
    public void pose(int joint, float value, float w) {
        over[joint] = value;
        weight[joint] = w;
    }

    /** Lets go of one joint: back to the base pose. */
    public void release(int joint) {
        weight[joint] = 0f;
    }

    /** Lets go of every override: back to the base pose (the springs ease into it). */
    public void relax() {
        for (int j = 0; j < JOINTS; j++) weight[j] = 0f;
        stiffness = 1f;
    }

    /** Kicks a joint (adds speed): slaps, shakes, jolts. */
    public void kick(int joint, float speed) {
        vel[joint] += speed;
    }

    /** Starts an emote (an Emotes move) from its beginning, cutting off any other one. */
    public void play(int move) {
        if (emote >= 0) stopEmote();
        emote = move;
        emoteT = 0f;
        fxT = 0f;
    }

    public boolean emoting() {
        return emote >= 0;
    }

    /** Ends the emote now: the springs ease back into the base pose (spun-round joints come back the short way). */
    public void stopEmote() {
        if (emote < 0) return;
        emote = -1;
        relax();
        face = -1;
        prop = -1;
        if (loose) {
            loose = false;
            unwind(LOOK);
            unwind(TURN);
            unwind(FLIP);
        }
    }

    /** Brings a joint that went round in circles back into -PI..PI (same angle). */
    private void unwind(int j) {
        float a = pos[j];
        while (a > (float) Math.PI) a -= 2f * (float) Math.PI;
        while (a < -(float) Math.PI) a += 2f * (float) Math.PI;
        pos[j] = a;
    }

    /** Snaps every joint to the current targets (no easing in): a fresh driver sits still right away. */
    public void settle() {
        baseTargets(false, 0f);
        blend();
        for (int j = 0; j < JOINTS; j++) {
            pos[j] = target[j];
            vel[j] = 0f;
        }
    }

    /**
     * A knock from outside: a bump (strength about 3..30), a power-up hit. (dx, dz) is where the push came from
     * in the car's frame (+x its left, +z ahead); the driver is thrown the other way and flails for a moment.
     */
    public void jolt(float strength, float dx, float dz) {
        // a hard hit knocks it out of any emote: the ragdoll takes over
        if (strength > EMOTE_BREAK) stopEmote();
        float k = Math.min(1f, strength / 18f);
        kick(LEAN, -dz * 7f * k);
        kick(SIDE, -dx * 7f * k);
        kick(NOD, -dz * 9f * k + rnd(3f) * k);
        kick(TILT, -dx * 9f * k + rnd(3f) * k);
        kick(STRETCH, -2.5f * k);
        kick(RISE, 1.5f * k);
        if (strength > 4f) flailT = Math.max(flailT, 0.25f + 0.6f * k);
    }

    /** Bumps harder than this (see jolt) end an emote. */
    public static final float EMOTE_BREAK = 8f;

    /** Frozen solid in a moment: snaps into a surprised pose (both arms up) that the ice then holds. */
    public void startle() {
        stopEmote();
        float[] p = pos;
        p[L_ARM] = p[R_ARM] = 2.6f;
        p[L_ARM_OUT] = p[R_ARM_OUT] = 0.6f;
        p[L_ELBOW] = p[R_ELBOW] = 0.5f;
        p[NOD] = -0.25f;
        p[LEAN] = -0.2f;
        p[STRETCH] = 0.12f;
        for (int j = 0; j < JOINTS; j++) vel[j] = 0f;
    }

    // ------------------------------------------------------------------ stepping

    /** One fixed step riding in a car: aFwd and aLeft are the car's acceleration along it and to its left. */
    public void ride(Car c, float dt, float aFwd, float aLeft) {
        time += dt;
        if (flailT > 0) flailT -= dt;
        mode = SEATED;
        if (c.falling) {
            flailT = Math.max(flailT, 0.5f);
            stopEmote();
        }
        if (emote >= 0) Emotes.step(this, dt);
        baseTargets(c.airborne && emote < 0, dt);
        // steering: lean and look into the turn, the hands follow the wheel
        float steer = c.steerVis;
        target[TWIST] += steer * 0.5f;
        target[LOOK] += steer * 0.9f;
        target[SIDE] += steer * 0.25f;
        if (!holding()) target[R_ARM_OUT] -= steer * 0.3f;
        target[L_ARM_OUT] += steer * 0.3f;
        // speed: tuck in a little; a bump boost throws it back in the seat (the push below)
        float sp = Math.min(1f, c.speed() / 18f);
        target[LEAN] += sp * 0.12f;
        // hops lift the car: the driver sinks into the seat, landings squash it
        float aUp = (c.hopV - lastHopV) / dt;
        lastHopV = c.hopV;
        if (aUp > 0f) {
            vel[RISE] -= Math.min(3f, aUp * 0.004f);
            vel[STRETCH] -= Math.min(4f, aUp * 0.006f);
        }
        if (c.justLanded) {
            vel[STRETCH] -= Math.min(4f, c.landSpeed * 0.18f);
            vel[NOD] += Math.min(6f, c.landSpeed * 0.25f);
            vel[RISE] -= Math.min(3f, c.landSpeed * 0.1f);
            flailT = Math.max(flailT, 0.3f);
        }
        blend();
        // inertia: thrown back when speeding up, to the outside of a turn (capped: a bump boost is one big shove)
        float fl = clamp(-aFwd, 420f), fs = clamp(-aLeft, 420f);
        springs(dt, fl * 0.9f, fs * 0.8f, fl * 0.8f, fs * 0.7f, fl * 0.4f, fs * 0.4f);
    }

    /** One step with no car moving it (showrooms, the podium): breathing and fidgeting only. */
    public void idle(float dt) {
        time += dt;
        if (flailT > 0) flailT -= dt;
        if (emote >= 0) Emotes.step(this, dt);
        baseTargets(false, dt);
        blend();
        springs(dt, 0, 0, 0, 0, 0, 0);
    }

    /** Thrown out of an eliminated car: leaves its seat on a tumbling arc of its own, away from the arena. */
    public void fling(Car c) {
        if (flung) return;
        stopEmote();
        CarDef d = c.def;
        float fx = (float) Math.sin(c.yaw), fz = (float) Math.cos(c.yaw);
        // seat position in the world (the car's left is (fz, -fx))
        wx = c.x + fx * d.seatZ + fz * d.seatX;
        wz = c.z + fz * d.seatZ - fx * d.seatX;
        wy = c.y + c.hop + d.seatY;
        float out = (float) Math.sqrt(c.x * c.x + c.z * c.z);
        float ox = out > 0.1f ? c.x / out : fx, oz = out > 0.1f ? c.z / out : fz;
        wvx = c.vx * 0.35f + ox * 3f + rnd(1.5f);
        wvz = c.vz * 0.35f + oz * 3f + rnd(1.5f);
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
        face = -1;
        relax();
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
        blend();
        springs(dt, 0, 0, 0, 0, 0, 0);
    }

    /** Back in the seat (a new match, a showroom). */
    public void reset() {
        stopEmote();
        flung = false;
        flailT = 0f;
        face = -1;
        relax();
        settle();
    }

    /** Fills target[] with the base pose for the mode and its flailing and arms-up variations. */
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
            if (style == CarDef.SADDLE) {
                // astride: thighs forward and out round the car, shins hanging down
                g[L_LEG] = g[R_LEG] = 0.75f;
                g[L_LEG_OUT] = g[R_LEG_OUT] = spread;
                g[L_KNEE] = g[R_KNEE] = 0.9f;
            } else {
                // sitting: thighs forward, shins down; climbing out of a hole straightens them
                float up = style == CarDef.BENCH ? 0f : clamp01((pos[RISE] - 0.15f) / 0.7f);
                g[L_LEG] = g[R_LEG] = 1.5f * (1f - up);
                g[L_LEG_OUT] = g[R_LEG_OUT] = 0.12f;
                g[L_KNEE] = g[R_KNEE] = 1.45f * (1f - up) + 0.1f * up;
            }
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
    }

    /** Blends the overrides (pose()) into the targets: last, so a weight of 1 holds a joint exactly there. */
    private void blend() {
        float[] g = target;
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
        float ks = stiffness, cs = (float) Math.sqrt(stiffness);
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
            v[j] += (K[j] * ks * (g[j] - p[j]) - C[j] * cs * v[j] + f) * dt;
            p[j] += v[j] * dt;
            if (loose && (j == LOOK || j == TURN || j == FLIP)) continue;
            if (p[j] < MIN[j]) {
                p[j] = MIN[j];
                if (v[j] < 0) v[j] *= -0.3f;
            } else if (p[j] > MAX[j]) {
                p[j] = MAX[j];
                if (v[j] > 0) v[j] *= -0.3f;
            }
        }
    }

    private static float clamp(float v, float r) {
        return v < -r ? -r : (v > r ? r : v);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float rnd(float r) {
        return (float) (Math.random() * 2 - 1) * r;
    }
}
