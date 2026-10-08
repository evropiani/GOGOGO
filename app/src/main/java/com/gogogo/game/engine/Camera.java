package com.gogogo.game.engine;

public final class Camera {
    public float ex, ey, ez; // eye
    public float tx, ty, tz; // target
    public float fov = 50f, near = 0.5f, far = 400f;
    public int width = 1, height = 1;

    public final float[] view = new float[16];
    public final float[] proj = new float[16];
    public final float[] viewProj = new float[16];
    private final float[] tmp = new float[4];

    public void set(float ex, float ey, float ez, float tx, float ty, float tz) {
        this.ex = ex; this.ey = ey; this.ez = ez;
        this.tx = tx; this.ty = ty; this.tz = tz;
    }

    public void update() {
        M4.lookAt(view, ex, ey, ez, tx, ty, tz, 0, 1, 0);
        M4.perspective(proj, fov, width / (float) Math.max(1, height), near, far);
        M4.mul(viewProj, proj, view);
    }

    /**
     * Projects a world point to screen pixels (origin top-left). Returns false when behind the camera.
     * Writes x,y into out[0], out[1].
     */
    public boolean project(float x, float y, float z, float[] out) {
        M4.transform(viewProj, x, y, z, tmp);
        if (tmp[3] <= 0.0001f) return false;
        float nx = tmp[0] / tmp[3], ny = tmp[1] / tmp[3];
        out[0] = (nx * 0.5f + 0.5f) * width;
        out[1] = (1f - (ny * 0.5f + 0.5f)) * height;
        return true;
    }
}
