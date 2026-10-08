package com.gogogo.game.engine;

/** Small fast xorshift random generator (deterministic when seeded). */
public final class Rng {
    private long s;

    public Rng(long seed) {
        s = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
    }

    public long nextLong() {
        s ^= s << 13;
        s ^= s >>> 7;
        s ^= s << 17;
        return s;
    }

    public float f() {
        return (nextLong() >>> 40) / (float) (1L << 24);
    }

    public float range(float a, float b) {
        return a + (b - a) * f();
    }

    public int i(int n) {
        return (int) ((nextLong() >>> 33) % n);
    }

    public boolean chance(float p) {
        return f() < p;
    }

    public <T> T pick(T[] arr) {
        return arr[i(arr.length)];
    }
}
