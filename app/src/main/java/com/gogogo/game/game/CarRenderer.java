package com.gogogo.game.game;

import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Renderer;

/** Draws one car (body, wheels, topper) from its simulation state. */
public final class CarRenderer {
    private CarRenderer() {}

    /** base and m are scratch matrices; base receives the body transform. */
    public static void draw(Renderer r, Art art, Car c, float time, float[] base, float[] m) {
        CarDef d = c.def;
        int paint = Palette.paintColor(c.paint, time);
        int accent = Palette.paintColor(c.accent, time + 1.3f);
        float s = Math.max(-0.35f, Math.min(0.35f, c.squash));
        float sy = 1f + s, sxz = 1f - s * 0.5f;
        float bob = c.falling ? 0f : (float) Math.abs(Math.sin(time * 9f + c.index)) * Math.min(1f, c.speed() / 10f) * 0.06f;
        float pitch = c.rotX - c.topZ * 0.15f, roll = c.rotZ + c.tilt;
        M4.trs(base, c.x, c.y + c.hop + bob, c.z, c.yaw, pitch, roll, sxz, sy, sxz);
        r.draw(art.cars[d.id], base, paint, accent, c.flash * 0.6f);

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
        }

        if (c.topper > 0) {
            M4.copy(base, m);
            M4.postTranslate(m, 0, d.topY, d.topZ);
            M4.postRotX(m, c.topZ * 0.9f);
            M4.postRotZ(m, -c.topX * 0.9f);
            r.draw(art.toppers[c.topper], m, paint, accent, 0f);
            if (c.topper == 4) {
                M4.postTranslate(m, 0, 0.45f, 0);
                M4.postRotY(m, time * 18f + c.index);
                r.draw(art.propeller, m, 0xFFFFFF);
            }
        }
    }
}
