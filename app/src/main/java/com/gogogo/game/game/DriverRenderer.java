package com.gogogo.game.game;

import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Mesh;
import com.gogogo.game.engine.Renderer;

/**
 * Draws drivers from their Driver state: the whole ragdoll (FULL), a one-piece stand-in for drivers far from the
 * camera (BUST), or nothing (NONE). Allocation free (scratch matrices).
 */
public final class DriverRenderer {
    private DriverRenderer() {}

    /** Level of detail. */
    public static final int FULL = 0, BUST = 1, NONE = 2;

    private static final float[] HIPS = new float[16], TORSO = new float[16], HEAD = new float[16], LIMB = new float[16],
            JOINT = new float[16], LOCAL = new float[16], M = new float[16];
    private static final float PI = (float) Math.PI;

    /**
     * A driver in its car. seat is the car's body transform (without its squash) moved to the seat; head = false
     * leaves out the head (the hood camera sits there). tint / glow: frozen or flashing cars pass it on.
     */
    static void seated(Renderer r, Art art, Car c, float[] seat, int lod, boolean head, float time, int tint, float tintK, float glow) {
        Driver d = c.driver;
        CarDef def = c.def;
        float s = def.seatScale;
        float[] p = d.pos;
        if (lod == BUST) {
            M4.trs(LOCAL, p[Driver.SHIFT] * s, p[Driver.RISE] * s, p[Driver.REACH] * s, p[Driver.TURN] + p[Driver.TWIST],
                    p[Driver.LEAN], -p[Driver.SIDE], s, s, s);
            M4.mul(M, seat, LOCAL);
            int[] l = d.look;
            r.draw(art.drvBust, M, ice(Outfits.COLOR[Outfits.SKIN][l[Outfits.SKIN]], tint, tintK),
                    ice(Outfits.COLOR[Outfits.SHIRT][l[Outfits.SHIRT]], tint, tintK), glow);
            return;
        }
        if (head) {
            // the steering wheel stays with the car (it turns with the front wheels)
            M4.copy(seat, M);
            M4.postTranslate(M, 0, Driver.WHEEL_Y * s, Driver.WHEEL_Z * s);
            M4.postRotX(M, -Driver.WHEEL_TILT);
            M4.postRotY(M, -c.steerVis * 2.5f);
            M4.postScale(M, s, s, s);
            r.draw(art.steeringWheel, M, 0xFFFFFF, 0xFFFFFF, glow);
        }
        boolean legs = def.seatStyle != CarDef.HATCH;
        body(r, art, d, seat, s, legs, head, tint, tintK, glow);
    }

    /** A driver standing on the floor at (x, y, z) facing yaw, scale s (showrooms, the results). */
    public static void standing(Renderer r, Art art, Driver d, float x, float y, float z, float yaw, float s, float glow) {
        if (d.hidden) return;
        M4.trs(M, x, y + Driver.STAND_Y * s, z, yaw, 0, 0, 1, 1, 1);
        body(r, art, d, M, s, true, true, 0, 0f, glow);
    }

    /** A driver flung out of its car, tumbling through the air. */
    static void flung(Renderer r, Art art, Driver d) {
        if (d.wy < -90f) return;
        M4.trs(M, d.wx, d.wy, d.wz, d.wYaw, d.wPitch, d.wRoll, 1, 1, 1);
        body(r, art, d, M, d.scale, true, true, 0, 0f, 0f);
    }

    /** The ragdoll from its hips frame (root, before the body offsets), scaled by s. */
    private static void body(Renderer r, Art art, Driver d, float[] root, float s, boolean legs, boolean head, int tint, float k, float glow) {
        float[] p = d.pos;
        int[] l = d.look;
        int skin = ice(Outfits.COLOR[Outfits.SKIN][l[Outfits.SKIN]], tint, k);
        int shirtI = l[Outfits.SHIRT], pantsI = l[Outfits.PANTS];
        int shirt = ice(Outfits.COLOR[Outfits.SHIRT][shirtI], tint, k);
        int pants = ice(Outfits.COLOR[Outfits.PANTS][pantsI], tint, k);
        int sleeve = Outfits.SLEEVE[shirtI] == 0 ? skin : ice(Outfits.SLEEVE[shirtI], tint, k);
        int len = Outfits.SLEEVE_LEN[shirtI];
        int upper = len == 2 ? skin : sleeve, lower = len >= 1 ? skin : sleeve;

        // hips: body offsets, turn and stretch
        float st = p[Driver.STRETCH];
        M4.trs(LOCAL, p[Driver.SHIFT] * s, p[Driver.RISE] * s, p[Driver.REACH] * s, p[Driver.TURN], 0, 0,
                s * (1f - st * 0.5f), s * (1f + st), s * (1f - st * 0.5f));
        M4.mul(HIPS, root, LOCAL);
        // torso
        M4.trs(LOCAL, 0, 0, 0, p[Driver.TWIST], p[Driver.LEAN], -p[Driver.SIDE], 1, 1, 1);
        M4.mul(TORSO, HIPS, LOCAL);
        r.draw(art.drvTorso[shirtI], TORSO, pants, shirt, glow);

        if (head) {
            M4.trs(LOCAL, 0, Driver.NECK_Y, 0, p[Driver.LOOK], p[Driver.NOD], -p[Driver.TILT], 1, 1, 1);
            M4.mul(HEAD, TORSO, LOCAL);
            r.draw(art.drvHead, HEAD, skin, skin, glow);
            int hat = l[Outfits.HAT];
            if (!Outfits.HAT_FACE[hat]) r.draw(art.drvFace[l[Outfits.FACE]], HEAD, skin, skin, glow);
            int cover = Outfits.HAT_COVER[hat];
            Mesh hair = cover == 0 ? art.drvHair[l[Outfits.HAIR]] : (cover == 1 ? art.drvHairCapped[l[Outfits.HAIR]] : null);
            if (hair != null) {
                int hc = ice(Outfits.COLOR[Outfits.HAIR][l[Outfits.HAIR]], tint, k);
                r.draw(hair, HEAD, hc, hc, glow);
            }
            if (art.drvHat[hat] != null) {
                int c = ice(Outfits.COLOR[Outfits.HAT][hat], tint, k);
                r.draw(art.drvHat[hat], HEAD, c, c, glow);
            }
        }

        // arms (+1 = its left, -1 = its right; the right hand holds the thing)
        for (int side = 1; side >= -1; side -= 2) {
            int a = side > 0 ? Driver.L_ARM : Driver.R_ARM;
            M4.trs(LOCAL, side * Driver.SHOULDER_X, Driver.SHOULDER_Y, 0, 0, -p[a], side * p[a + 1], 1, 1, 1);
            M4.mul(LIMB, TORSO, LOCAL);
            r.draw(art.drvUpperArm, LIMB, skin, upper, glow);
            M4.trs(LOCAL, 0, -Driver.UPPER_ARM, 0, 0, -p[a + 2], 0, 1, 1, 1);
            M4.mul(JOINT, LIMB, LOCAL);
            r.draw(art.drvForearm, JOINT, skin, lower, glow);
            if (side < 0 && art.drvHeld[l[Outfits.HELD]] != null && head) {
                M4.postTranslate(JOINT, 0, -Driver.FOREARM, 0);
                M4.postRotZ(JOINT, PI);
                r.draw(art.drvHeld[l[Outfits.HELD]], JOINT, 0xFFFFFF, 0xFFFFFF, glow);
            }
        }

        if (!legs) return;
        int style = Outfits.PANTS_STYLE[pantsI];
        int shin = Outfits.SHORTS[pantsI] ? skin : pants;
        int shoeI = l[Outfits.SHOES];
        int shoe = ice(Outfits.COLOR[Outfits.SHOES][shoeI], tint, k);
        for (int side = 1; side >= -1; side -= 2) {
            int g = side > 0 ? Driver.L_LEG : Driver.R_LEG;
            M4.trs(LOCAL, side * Driver.HIP_X, Driver.HIP_Y, 0, 0, -p[g], side * p[g + 1], 1, 1, 1);
            M4.mul(LIMB, HIPS, LOCAL);
            r.draw(art.drvThigh[style], LIMB, pants, pants, glow);
            M4.trs(LOCAL, 0, -Driver.THIGH, 0, 0, p[g + 2], 0, 1, 1, 1);
            M4.mul(JOINT, LIMB, LOCAL);
            r.draw(art.drvShin[style == 0 || Outfits.SHORTS[pantsI] ? 0 : style], JOINT, shin, shin, glow);
            M4.postTranslate(JOINT, 0, -Driver.SHIN, 0);
            r.draw(art.drvShoe[shoeI], JOINT, shoe, skin, glow);
        }
    }

    private static int ice(int c, int tint, float k) {
        return k > 0f ? Palette.mix(c, tint, k) : c;
    }
}
