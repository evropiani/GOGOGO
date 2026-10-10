package com.gogogo.game.game;

import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Renderer;

/** Draws one car (body, wheels, topper) from its simulation state. */
public final class CarRenderer {
    private CarRenderer() {}

    /** base and m are scratch matrices; base receives the body transform. */
    public static void draw(Renderer r, Art art, Car c, float time, float[] base, float[] m) {
        draw(r, art, c, time, base, m, true);
    }

    /**
     * topper = false for the car the hood camera sits on: drawn without its topper, and level in a super jump
     * (the camera doesn't pitch with the body, so the hood would swing through the view).
     */
    public static void draw(Renderer r, Art art, Car c, float time, float[] base, float[] m, boolean topper) {
        CarDef d = c.def;
        int paint = Palette.paintColor(c.paint, time);
        int accent = Palette.paintColor(c.accent, time + 1.3f);
        float glow = c.flash * 0.6f;
        if (c.frozenT > 0) {
            // frozen solid: icy tint and a cold shine
            float k = Math.min(1f, c.frozenT * 4f) * 0.6f;
            paint = Palette.mix(paint, ICE_TINT, k);
            accent = Palette.mix(accent, ICE_TINT, k);
            glow = Math.max(glow, 0.14f * k / 0.6f);
        }
        float s = Math.max(-0.35f, Math.min(0.35f, c.squash));
        float sy = 1f + s, sxz = 1f - s * 0.5f;
        float bob = c.falling ? 0f : (float) Math.abs(Math.sin(time * 9f + c.index)) * Math.min(1f, c.speed() / 10f) * 0.06f;
        float pitch = (topper || !c.airborne ? c.rotX : 0f) - c.topZ * 0.15f, roll = c.rotZ + c.tilt;
        M4.trs(base, c.x, c.y + c.hop + bob, c.z, c.yaw, pitch, roll, sxz, sy, sxz);
        r.draw(art.cars[d.id], base, paint, accent, glow);
        if (d.id == Cars.DISCO || d.id == Cars.TITAN) drawBodyExtras(r, art, c, d, time, base, m);

        for (int w = 0; w < 4; w++) {
            float wx = (w % 2 == 0) ? -d.wheelX : d.wheelX;
            boolean front = w < 2;
            float wz = front ? d.wheelZf : d.wheelZr;
            M4.copy(base, m);
            M4.postTranslate(m, wx, d.wheelR, wz);
            if (front) M4.postRotY(m, c.steerVis);
            M4.postRotX(m, c.wheelSpin);
            M4.postScale(m, d.wheelW, d.wheelR, d.wheelR);
            r.draw(art.wheels[c.wheel], m, paint, accent, c.flash * 0.4f);
            if (c.stickyT > 0) {
                // sticky wheels: goo around the tire (wobbling, thinning out in the last second)
                float g = Math.min(1f, c.stickyT) * (1f + 0.06f * (float) Math.sin(time * 9f + w));
                M4.postScale(m, 1.05f, g, g);
                r.draw(art.goo, m, STICKY_GOO, STICKY_GOO, 0.12f);
            }
        }

        if (topper && c.topper > 0) {
            M4.copy(base, m);
            M4.postTranslate(m, 0, d.topY, d.topZ);
            M4.postRotX(m, c.topZ * 0.9f);
            M4.postRotZ(m, -c.topX * 0.9f);
            r.draw(art.toppers[c.topper], m, paint, accent, 0f);
            if (c.topper == 4) {
                M4.postTranslate(m, 0, 0.45f, 0);
                M4.postRotY(m, time * 18f + c.index);
                r.draw(art.propeller, m, 0xFFFFFF);
            } else if (c.topper == 18) {
                // disco ball topper: the ball turns under its hanger
                M4.postTranslate(m, 0, Art.MINI_BALL_Y, 0);
                M4.postRotY(m, time * 2.4f + c.index);
                M4.postScale(m, Art.MINI_BALL_R, Art.MINI_BALL_R, Art.MINI_BALL_R);
                r.draw(art.mirrorBall, m, 0xFFFFFF, 0xFFFFFF, sparkle(time, c.index));
            } else if (c.topper == 19) {
                // working mini traffic light
                float[] lm = LAMP_M;
                for (int i = 0; i < 3; i++) {
                    M4.copy(m, lm);
                    M4.postTranslate(lm, 0, Art.MINI_LAMP_Y[i], 0);
                    M4.postScale(lm, Art.MINI_LAMP_R, Art.MINI_LAMP_R, Art.MINI_LAMP_R);
                    float on = lampOn(i, time * 0.9f + c.index * 0.53f, false);
                    r.draw(art.lamp, lm, lampColor(i, on), 0, on * 0.24f);
                }
            }
        }
    }

    private static final float[] LAMP_M = new float[16];
    /** Frozen cars are tinted towards this; sticky wheels get goo of this color. */
    static final int ICE_TINT = 0xBDEBFF, STICKY_GOO = 0xC04CF0;
    /** Lamp colors: red, yellow, green, and their switched-off tints. */
    private static final int[] LAMP_ON = {0xFF2A1A, 0xFFCC00, 0x1EE048};
    private static final int[] LAMP_OFF = {0x5A1E28, 0x5A4A20, 0x1E4A2E};

    /** Animated parts of the premium bodies (built in Art, placed in body space). */
    private static void drawBodyExtras(Renderer r, Art art, Car c, CarDef d, float time, float[] base, float[] m) {
        if (d.id == Cars.DISCO) {
            // DISCO DASHER: the mirror ball spins on its stand
            M4.copy(base, m);
            M4.postTranslate(m, 0, Art.DISCO_Y, Art.DISCO_Z);
            M4.postRotY(m, time * 2.2f + c.index);
            M4.postRotX(m, 0.25f);
            M4.postScale(m, Art.DISCO_R, Art.DISCO_R, Art.DISCO_R);
            r.draw(art.mirrorBall, m, 0xFFFFFF, 0xFFFFFF, Math.max(c.flash * 0.6f, sparkle(time, c.index)));
            return;
        }
        // TRAFFIC TITAN: red, yellow, green in turn, then all three blink
        for (int i = 0; i < 3; i++) {
            M4.copy(base, m);
            M4.postTranslate(m, Art.TITAN_LAMP_X[i], Art.TITAN_LAMP_Y, Art.TITAN_LAMP_Z);
            M4.postScale(m, Art.TITAN_LAMP_R, Art.TITAN_LAMP_R, Art.TITAN_LAMP_R);
            float on = lampOn(i, time * 1.1f + c.index * 0.53f, true);
            r.draw(art.lamp, m, lampColor(i, on), 0, Math.max(c.flash * 0.6f, on * 0.24f));
        }
    }

    /** How lit lamp i (0 red, 1 yellow, 2 green) is; t counts steps. The party cycle ends with all three blinking. */
    private static float lampOn(int i, float t, boolean party) {
        int steps = party ? 4 : 3;
        float k = t % steps;
        int step = (int) k;
        float f = k - step;
        if (step == 3) return (f % 0.5f) < 0.25f ? 1f : 0f;
        if (step != i) return 0f;
        // quick fade in and out at the ends of each step
        return Math.min(1f, Math.min(f, 1f - f) * 8f);
    }

    private static int lampColor(int i, float on) {
        return Palette.mix(LAMP_OFF[i], LAMP_ON[i], on);
    }

    /** Glints on a mirror ball. */
    private static float sparkle(float time, int index) {
        float s = (float) Math.sin(time * 7.3f + index * 1.7f) * (float) Math.sin(time * 3.1f + index);
        return 0.08f + Math.max(0f, s) * 0.22f;
    }
}
