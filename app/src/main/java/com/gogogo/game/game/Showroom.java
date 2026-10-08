package com.gogogo.game.game;

import com.gogogo.game.engine.Ease;
import com.gogogo.game.engine.M4;

/** A single car spinning on a pedestal (garage, results, reveal). */
public final class Showroom {
    private final Game game;
    public Car car;
    public float spin = 0.6f;
    public float spinSpeed = 0.7f;
    public float time;
    public int pedestalColor = 0xFF8AC8, pedestalAccent = 0xFFE14D;
    public final Particles fx = new Particles();
    private final float[] base = new float[16], m = new float[16];
    private float drop; // drop-in animation 1 -> 0

    /** Where the car sits on screen, as a fraction of the screen height from the top. */
    public float screenY = 0.3f;
    public float distance = 17f;
    public float height = 7f;

    public Showroom(Game game) {
        this.game = game;
    }

    public void show(int carId, int paint, int accent, int topper, int wheel, boolean animate) {
        int[] lv = new int[4];
        boolean same = car != null && car.def.id == carId;
        car = new Car(0, Cars.ALL[carId], "", false, lv);
        car.setLook(paint, accent, topper, wheel);
        if (animate && !same) drop = 1f;
        else if (animate) car.squashV = -6f;
    }

    public void showSaved(int carId) {
        Save s = game.save;
        show(carId, s.carPaint[carId], s.carAccent[carId], s.carTopper[carId], s.carWheel[carId], true);
    }

    public void celebrate() {
        if (car == null) return;
        car.squashV = -9f;
        fx.confetti(0, 2.5f, 0, 70, 1.1f);
    }

    public void update(float dt) {
        time += dt;
        spin += dt * spinSpeed;
        fx.update(dt);
        if (car == null) return;
        if (drop > 0) {
            drop = Math.max(0f, drop - dt * 2.2f);
            if (drop == 0) car.squashV -= 8f;
        }
        car.squashV += (-car.squash * 160f - car.squashV * 9f) * dt;
        car.squash += car.squashV * dt;
        car.topVX += (-car.topX * 90f - car.topVX * 5f) * dt;
        car.topVZ += (-car.topZ * 90f - car.topVZ * 5f) * dt;
        car.topX += car.topVX * dt;
        car.topZ += car.topVZ * dt;
        car.wheelSpin += dt * 2f;
    }

    public void render() {
        float fov = 38f;
        // aim so that the car's center (y = 1) lands at screenY
        double below = Math.atan((height - 1f) / distance);
        double off = Math.atan((1f - 2f * screenY) * Math.tan(Math.toRadians(fov / 2)));
        float ty = height - distance * (float) Math.tan(below + off);
        game.cam.fov = fov;
        game.cam.set(0, height, distance, 0, ty, 0);
        game.beginWorld();
        M4.trs(m, 0, 0, 0, spin * 0.3f, 0, 0, 1, 1, 1);
        game.r.draw(game.art.pedestal, m, pedestalColor, pedestalAccent, 0f);
        if (car != null) {
            car.yaw = spin;
            float y = drop > 0 ? Ease.inCubic(drop) * 12f : 0f;
            car.y = y;
            CarRenderer.draw(game.r, game.art, car, time, base, m);
            game.r.blob(0, 0.03f, 0, 1.6f, 1.3f, spin, 0.45f * (1f - Math.min(1f, y / 8f)));
        }
        fx.draw(game.r, game.art);
    }
}
