package com.gogogo.game.game;

import com.gogogo.game.engine.M4;
import com.gogogo.game.engine.Mesh;
import com.gogogo.game.engine.Renderer;

/** Pooled chunky 3D particles: confetti cubes, puffs, stars, and the boost trail shapes. */
public final class Particles {
    public static final int CUBE = 0, BALL = 1, STAR = 2, BONK = 3, HEART = 4, BOLT = 5, COIN = 6, BUBBLE = 7, SPARKLE = 8;
    private static final int MAX = 900;

    private final float[] x = new float[MAX], y = new float[MAX], z = new float[MAX];
    private final float[] vx = new float[MAX], vy = new float[MAX], vz = new float[MAX];
    private final float[] rx = new float[MAX], ry = new float[MAX], rvx = new float[MAX], rvy = new float[MAX];
    private final float[] life = new float[MAX], maxLife = new float[MAX], size = new float[MAX], grav = new float[MAX], drag = new float[MAX];
    private final float[] glow = new float[MAX];
    private final int[] type = new int[MAX], color = new int[MAX], endColor = new int[MAX];
    private final boolean[] shrink = new boolean[MAX];
    private int cursor;
    private final float[] mtx = new float[16];
    /** Yaw that turns +z towards the camera (set before draw; hearts, bolts, bubbles and sparkles face it). */
    public float faceYaw;

    public int spawn(int t, float px, float py, float pz, float pvx, float pvy, float pvz, float sz, float lifeS, int col, float gravity) {
        int i = cursor;
        cursor = (cursor + 1) % MAX;
        type[i] = t;
        x[i] = px;
        y[i] = py;
        z[i] = pz;
        vx[i] = pvx;
        vy[i] = pvy;
        vz[i] = pvz;
        size[i] = sz;
        life[i] = maxLife[i] = lifeS;
        color[i] = col;
        endColor[i] = col;
        grav[i] = gravity;
        drag[i] = 1.2f;
        glow[i] = 0f;
        rx[i] = (float) (Math.random() * 6.28);
        ry[i] = (float) (Math.random() * 6.28);
        rvx[i] = (float) (Math.random() * 10 - 5);
        rvy[i] = (float) (Math.random() * 10 - 5);
        shrink[i] = true;
        return i;
    }

    // tweaks for a particle just spawned
    public void glow(int i, float flash) {
        glow[i] = flash;
    }

    /** The color drifts to this one over the particle's life. */
    public void fade(int i, int toColor) {
        endColor[i] = toColor;
    }

    public void drag(int i, float d) {
        drag[i] = d;
    }

    public void spin(int i, float pitchRate, float yawRate) {
        rvx[i] = pitchRate;
        rvy[i] = yawRate;
    }

    public void orient(int i, float pitch, float yaw) {
        rx[i] = pitch;
        ry[i] = yaw;
    }

    public void confetti(float px, float py, float pz, int count, float power) {
        for (int k = 0; k < count; k++) {
            float a = (float) (Math.random() * Math.PI * 2);
            float s = (float) (0.3 + Math.random()) * power;
            int col = Palette.TILE[(int) (Math.random() * Palette.TILE.length)];
            int i = spawn(CUBE, px, py, pz, (float) Math.cos(a) * s * 0.6f, (float) (6 + Math.random() * 8) * power * 0.5f + 4f,
                    (float) Math.sin(a) * s * 0.6f, 0.28f + (float) Math.random() * 0.2f, 2.2f + (float) Math.random(), col, 14f);
            drag[i] = 0.8f;
        }
    }

    public void puff(float px, float py, float pz, int count, int col, float spread) {
        for (int k = 0; k < count; k++) {
            float a = (float) (Math.random() * Math.PI * 2);
            float s = (float) Math.random() * spread;
            int i = spawn(BALL, px, py, pz, (float) Math.cos(a) * s, (float) Math.random() * 1.5f + 0.5f, (float) Math.sin(a) * s,
                    0.35f + (float) Math.random() * 0.35f, 0.45f + (float) Math.random() * 0.3f, col, -1f);
            drag[i] = 4f;
        }
    }

    public void stars(float px, float py, float pz, int count) {
        for (int k = 0; k < count; k++) {
            float a = (float) (Math.random() * Math.PI * 2);
            float s = 4f + (float) Math.random() * 5f;
            spawn(STAR, px, py, pz, (float) Math.cos(a) * s, 5f + (float) Math.random() * 4f, (float) Math.sin(a) * s,
                    0.35f + (float) Math.random() * 0.2f, 0.7f + (float) Math.random() * 0.4f, 0xFFE14D, 18f);
        }
    }

    public void bonk(float px, float py, float pz, float sz) {
        int i = spawn(BONK, px, py, pz, 0, 2f, 0, sz, 0.35f, 0xFFFFFF, 0f);
        rvx[i] = 0;
        rvy[i] = 8f;
        rx[i] = 0;
        shrink[i] = false;
    }

    /** A twinkle that pops up, spins and vanishes. */
    public void sparkle(float px, float py, float pz, float sz, int col) {
        int i = spawn(SPARKLE, px, py, pz, 0, 0.6f, 0, sz, 0.55f + (float) Math.random() * 0.3f, col, 0f);
        rvx[i] = 4f;
        glow[i] = 0.45f;
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            life[i] -= dt;
            vy[i] -= grav[i] * dt;
            float d = (float) Math.exp(-drag[i] * dt);
            vx[i] *= d;
            vz[i] *= d;
            if (grav[i] < 0) vy[i] *= d;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            z[i] += vz[i] * dt;
            rx[i] += rvx[i] * dt;
            ry[i] += rvy[i] * dt;
        }
    }

    public void draw(Renderer r, Art art) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            float t = life[i] / maxLife[i];
            float s = size[i];
            int tp = type[i];
            if (tp == SPARKLE) s *= (float) Math.sin(Math.min(1f, t) * Math.PI);
            else if (shrink[i]) s *= Math.min(1f, t * 3f);
            else s *= 0.6f + (1f - t) * 0.8f;
            Mesh m;
            switch (tp) {
                case BALL: m = art.ball; break;
                case STAR: m = art.star; break;
                case BONK: m = art.bonk; break;
                case HEART: m = art.heart; break;
                case BOLT: m = art.bolt; break;
                case COIN: m = art.coin; break;
                case BUBBLE: m = art.bubble; break;
                case SPARKLE: m = art.sparkle; break;
                default: m = art.cube; break;
            }
            switch (tp) {
                case BONK:
                    M4.trs(mtx, x[i], y[i], z[i], 0, 0.9f, ry[i], s, s, s);
                    break;
                case HEART:
                    M4.trs(mtx, x[i], y[i], z[i], faceYaw, 0, (float) Math.sin(rx[i]) * 0.35f, s, s, s);
                    break;
                case BOLT:
                case SPARKLE:
                    M4.trs(mtx, x[i], y[i], z[i], faceYaw, 0, rx[i], s, s, s);
                    break;
                case BUBBLE:
                    M4.trs(mtx, x[i], y[i], z[i], faceYaw, 0, 0, s, s, s);
                    break;
                case COIN:
                    M4.trs(mtx, x[i], y[i], z[i], ry[i], 0, 0, s, s, s);
                    break;
                default:
                    M4.trs(mtx, x[i], y[i], z[i], ry[i], rx[i], 0, s, s, s);
                    break;
            }
            int c = color[i];
            if (endColor[i] != c) c = Palette.mix(endColor[i], c, t);
            r.draw(m, mtx, c, c, tp == BONK ? 0.15f : glow[i]);
        }
    }

    public void clear() {
        for (int i = 0; i < MAX; i++) life[i] = 0;
    }
}
