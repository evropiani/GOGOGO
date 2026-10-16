package com.gogogo.game.game;

import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Mesh;
import com.gogogo.game.engine.Renderer;

/**
 * Draws drivers from their Driver state: the whole ragdoll (FULL), a cheap stand-in for drivers far from the
 * camera (BUST: one merged body mesh plus a cap for the hat or hair), or nothing (NONE). Allocation free (scratch
 * matrices).
 */
public final class DriverRenderer {
    private DriverRenderer() {}

    /** Level of detail. */
    public static final int FULL = 0, BUST = 1, NONE = 2;
    /** Debug counters: drivers drawn whole and as a stand-in since the last reset (performance checks). */
    public static int countFull, countBust;

    private static final float[] HIPS = new float[16], TORSO = new float[16], HEAD = new float[16], LIMB = new float[16],
            JOINT = new float[16], LOCAL = new float[16], M = new float[16], SPIN = new float[16];
    private static final float PI = (float) Math.PI;
    /** Things in hand are drawn a bit bigger than life: readable from the chase camera. */
    private static final float HELD_SCALE = 1.3f;

    /**
     * A driver in its car. seat is the car's body transform (without its squash) moved to the seat. tint / tintK:
     * frozen cars pass their icy tint on; glow: flashing cars.
     */
    static void seated(Renderer r, Art art, Car c, float[] seat, int lod, int tint, float tintK, float glow) {
        Driver d = c.driver;
        CarDef def = c.def;
        float s = def.seatScale;
        float[] p = d.pos;
        int[] l = d.look;
        if (lod == BUST) {
            bust(r, art, d, seat, s, tint, tintK, glow);
            return;
        }
        // the steering wheel stays with the car (it turns with the front wheels)
        M4.copy(seat, M);
        M4.postTranslate(M, 0, Driver.WHEEL_Y * s, Driver.WHEEL_Z * s);
        M4.postRotX(M, -Driver.WHEEL_TILT);
        M4.postRotY(M, -c.steerVis * 2.5f);
        M4.postScale(M, s, s, s);
        r.draw(art.steeringWheel, M, 0xFFFFFF, 0xFFFFFF, glow);
        countFull++;
        boolean legs = !def.legsHidden() || p[Driver.RISE] > Driver.LEGS_OUT;
        int face = c.frozenT > 0 ? Outfits.FC_SHOCKED : d.faceShown();
        body(r, art, d, seat, s, legs, face, tint, tintK, glow);
    }

    /** A driver standing on the floor at (x, y, z) facing yaw, scale s (showrooms, the results). */
    public static void standing(Renderer r, Art art, Driver d, float x, float y, float z, float yaw, float s, float glow) {
        if (d.hidden) return;
        M4.trs(M, x, y + Driver.STAND_Y * s, z, yaw, 0, 0, 1, 1, 1);
        body(r, art, d, M, s, true, d.faceShown(), 0, 0f, glow);
    }

    /** A driver flung out of its car, tumbling through the air. */
    static void flung(Renderer r, Art art, Driver d, int lod) {
        if (d.wy < -90f || lod == NONE) return;
        M4.trs(SPIN, d.wx, d.wy, d.wz, d.wYaw, d.wPitch, d.wRoll, 1, 1, 1);
        if (lod == BUST) bust(r, art, d, SPIN, d.scale, 0, 0f, 0f);
        else body(r, art, d, SPIN, d.scale, true, Outfits.FC_SHOCKED, 0, 0f, 0f);
    }

    /**
     * The stand-in: body, head and arms in one piece, and a cap in the color of its hat or hair - two meshes for all
     * of them whatever they wear (every hat and hairdo drawn for real would be a draw call of its own).
     */
    private static void bust(Renderer r, Art art, Driver d, float[] root, float s, int tint, float k, float glow) {
        countBust++;
        float[] p = d.pos;
        int[] l = d.look;
        M4.trs(LOCAL, p[Driver.SHIFT] * s, p[Driver.RISE] * s, p[Driver.REACH] * s, p[Driver.TURN] + p[Driver.TWIST],
                p[Driver.LEAN] + p[Driver.FLIP], -p[Driver.SIDE], s, s, s);
        M4.mul(M, root, LOCAL);
        int skin = ice(Outfits.COLOR[Outfits.SKIN][l[Outfits.SKIN]], tint, k);
        r.draw(art.drvBust, M, skin, ice(Outfits.COLOR[Outfits.SHIRT][l[Outfits.SHIRT]], tint, k), glow);
        int top = Outfits.topColor(l);
        if (top < 0) return;
        M4.postTranslate(M, 0, Driver.NECK_Y, 0);
        top = ice(top, tint, k);
        r.draw(art.drvBustCap, M, top, top, glow);
    }

    /** Hat, the hair it leaves showing, and (unless the hat covers it) nothing else: head is the neck frame. */
    private static void topOfHead(Renderer r, Art art, int[] l, float[] head, int tint, float k, float glow) {
        int hat = l[Outfits.HAT];
        int cover = Outfits.HAT_COVER[hat];
        Mesh hair = cover == 0 ? art.drvHair[l[Outfits.HAIR]] : (cover == 1 ? art.drvHairCapped[l[Outfits.HAIR]] : null);
        if (hair != null) {
            int hc = ice(Outfits.COLOR[Outfits.HAIR][l[Outfits.HAIR]], tint, k);
            r.draw(hair, head, hc, hc, glow);
        }
        if (art.drvHat[hat] != null) {
            int c = ice(Outfits.COLOR[Outfits.HAT][hat], tint, k);
            r.draw(art.drvHat[hat], head, c, c, glow);
        }
    }

    /** The ragdoll from its hips frame (root, before the body offsets), scaled by s. */
    private static void body(Renderer r, Art art, Driver d, float[] root, float s, boolean legs, int face,
                             int tint, float k, float glow) {
        float[] p = d.pos;
        int[] l = d.look;
        int skin = ice(Outfits.COLOR[Outfits.SKIN][l[Outfits.SKIN]], tint, k);
        int shirtI = l[Outfits.SHIRT], pantsI = l[Outfits.PANTS];
        int shirt = ice(Outfits.COLOR[Outfits.SHIRT][shirtI], tint, k);
        int pants = ice(Outfits.COLOR[Outfits.PANTS][pantsI], tint, k);
        int sleeve = Outfits.SLEEVE[shirtI] == 0 ? skin : ice(Outfits.SLEEVE[shirtI], tint, k);
        int len = Outfits.SLEEVE_LEN[shirtI];
        int upper = len == 2 ? skin : sleeve, lower = len >= 1 ? skin : sleeve;

        // hips: body offsets, turn, flip and stretch
        float st = p[Driver.STRETCH], flip = p[Driver.FLIP];
        M4.trs(LOCAL, p[Driver.SHIFT] * s, p[Driver.RISE] * s, p[Driver.REACH] * s, p[Driver.TURN], flip, 0,
                s * (1f - st * 0.5f), s * (1f + st), s * (1f - st * 0.5f));
        if (flip != 0f) {
            // tip over around the middle of the body, not the hips
            float pv = Driver.FLIP_Y * s, sf = (float) Math.sin(flip);
            LOCAL[12] -= pv * (float) Math.sin(p[Driver.TURN]) * sf;
            LOCAL[13] += pv * (1f - (float) Math.cos(flip));
            LOCAL[14] -= pv * (float) Math.cos(p[Driver.TURN]) * sf;
        }
        M4.mul(HIPS, root, LOCAL);
        // torso
        M4.trs(LOCAL, 0, 0, 0, p[Driver.TWIST], p[Driver.LEAN], -p[Driver.SIDE], 1, 1, 1);
        M4.mul(TORSO, HIPS, LOCAL);
        r.draw(art.drvTorso[shirtI], TORSO, pants, shirt, glow);
        M4.trs(LOCAL, 0, Driver.NECK_Y, 0, p[Driver.LOOK], p[Driver.NOD], -p[Driver.TILT], 1, 1, 1);
        M4.mul(HEAD, TORSO, LOCAL);
        r.draw(art.drvHead, HEAD, skin, skin, glow);
        if (!Outfits.HAT_FACE[l[Outfits.HAT]]) r.draw(art.drvFace[face], HEAD, skin, skin, glow);
        topOfHead(r, art, l, HEAD, tint, k, glow);

        // arms (+1 = its left, -1 = its right; the right hand holds the thing)
        int held = d.heldShown();
        for (int side = 1; side >= -1; side -= 2) {
            int a = side > 0 ? Driver.L_ARM : Driver.R_ARM;
            M4.trs(LOCAL, side * Driver.SHOULDER_X, Driver.SHOULDER_Y, 0, 0, -p[a], side * p[a + 1], 1, 1, 1);
            M4.mul(LIMB, TORSO, LOCAL);
            r.draw(art.drvUpperArm, LIMB, skin, upper, glow);
            M4.trs(LOCAL, 0, -Driver.UPPER_ARM, 0, 0, -p[a + 2], 0, 1, 1, 1);
            M4.mul(JOINT, LIMB, LOCAL);
            r.draw(art.drvForearm, JOINT, skin, lower, glow);
            if (side < 0 && art.drvHeld[held] != null) {
                // gripped across the fist: it stands up when the forearm points forward
                M4.postTranslate(JOINT, 0, -Driver.FOREARM, 0);
                M4.postRotX(JOINT, PI / 2);
                M4.postScale(JOINT, HELD_SCALE, HELD_SCALE, HELD_SCALE);
                r.draw(art.drvHeld[held], JOINT, 0xFFFFFF, 0xFFFFFF, glow);
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
