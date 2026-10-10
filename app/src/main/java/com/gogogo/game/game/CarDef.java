package com.gogogo.game.game;

import com.gogogo.game.engine.MeshBuilder;

/** Static description of a car model: stats, wheel layout, look. */
public abstract class CarDef {
    public static final int SPEED = 0, GRIP = 1, BOOST = 2, WEIGHT = 3;
    public static final String[] STAT_NAME = {"SPEED", "GRIP", "BUMP", "WEIGHT"};
    public static final int MAX_LEVEL = 5;

    public final int id;
    public final String name, blurb;
    public final int price;
    public final float[] stats; // base 1..5
    public final int defPaint, defAccent;
    public boolean secret;
    /** Earned by reaching this player level (0 = not a level reward). */
    public int levelReq;
    /** Premium price in Rims (0 = not a premium car). */
    public int rimsPrice;
    /** Bots never drive this car. */
    public boolean exclusive;

    // layout
    public float wheelR = 0.4f, wheelW = 0.34f, wheelX = 0.8f, wheelZf = 0.8f, wheelZr = -0.8f;
    public float topY = 1.6f, topZ = 0f;
    public float radius = 1.15f;
    /** Hood camera position (computed from the body mesh in Art unless set with hood()). */
    public float hoodY = 1.8f, hoodZ = 0.6f;
    public boolean hoodFixed;

    protected CarDef(int id, String name, String blurb, int price, float speed, float grip, float boost, float weight, int defPaint, int defAccent) {
        this.id = id;
        this.name = name;
        this.blurb = blurb;
        this.price = price;
        this.stats = new float[]{speed, grip, boost, weight};
        this.defPaint = defPaint;
        this.defAccent = defAccent;
    }

    /** Places the hood camera by hand, for cars whose tallest part is not the cabin. */
    public CarDef hood(float y, float z) {
        hoodY = y;
        hoodZ = z;
        hoodFixed = true;
        return this;
    }

    public CarDef wheels(float r, float w, float x, float zf, float zr) {
        wheelR = r;
        wheelW = w;
        wheelX = x;
        wheelZf = zf;
        wheelZr = zr;
        return this;
    }

    /** Makes this car a level reward. */
    public CarDef level(int lv) {
        levelReq = lv;
        exclusive = true;
        return this;
    }

    /** Makes this car a premium (Rims) car. */
    public CarDef rims(int price) {
        rimsPrice = price;
        return this;
    }

    public CarDef topper(float y, float z) {
        topY = y;
        topZ = z;
        return this;
    }

    /** Builds the body (everything but wheels and topper). */
    public abstract void build(MeshBuilder m);

    /** Effective stat including upgrade levels. */
    public float stat(int which, int level) {
        return stats[which] + level * 0.6f;
    }

    // ---- shared detail helpers
    protected static final int GLASS = 0x2E3A78;
    protected static final int DARK = 0x3A3550;
    protected static final int LIGHT = 0xFFF6C0;

    /** Cartoon googly eyes looking forward (+Z). */
    protected static void eyes(MeshBuilder m, float x, float y, float z, float r) {
        for (int s = -1; s <= 1; s += 2) {
            m.fixed(0xFFFFFF).sphere(s * x, y, z, r, 14);
            m.fixed(0x1A1028).sphere(s * x + s * r * 0.12f, y - r * 0.08f, z + r * 0.62f, r * 0.48f, 10);
            m.fixed(0xFFFFFF).sphere(s * x + s * r * 0.02f, y + r * 0.12f, z + r * 0.98f, r * 0.12f, 6);
        }
    }
}
