package com.gogogo.game.engine;

public final class Ease {
    private Ease() {}

    public static float clamp01(float t) {
        return t < 0f ? 0f : (t > 1f ? 1f : t);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float outBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float outElastic(float t) {
        t = clamp01(t);
        if (t == 0f || t == 1f) return t;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1);
    }

    public static float outBounce(float t) {
        t = clamp01(t);
        float n1 = 7.5625f, d1 = 2.75f;
        if (t < 1f / d1) return n1 * t * t;
        if (t < 2f / d1) { t -= 1.5f / d1; return n1 * t * t + 0.75f; }
        if (t < 2.5f / d1) { t -= 2.25f / d1; return n1 * t * t + 0.9375f; }
        t -= 2.625f / d1;
        return n1 * t * t + 0.984375f;
    }

    public static float outCubic(float t) {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float inCubic(float t) {
        t = clamp01(t);
        return t * t * t;
    }

    public static float inOutSine(float t) {
        t = clamp01(t);
        return (float) (-(Math.cos(Math.PI * t) - 1) / 2);
    }

    /** Damped approach of current toward target; rate = 1/seconds. */
    public static float approach(float current, float target, float rate, float dt) {
        return target + (current - target) * (float) Math.exp(-rate * dt);
    }

    public static float wrapAngle(float a) {
        while (a > Math.PI) a -= (float) (Math.PI * 2);
        while (a < -Math.PI) a += (float) (Math.PI * 2);
        return a;
    }
}
