package com.gogogo.game.engine;

/** Column-major 4x4 matrix helpers operating on float[16] (OpenGL layout). Allocation free. */
public final class M4 {
    private M4() {}

    private static final float[] T1 = new float[16];
    private static final float[] T2 = new float[16];

    public static void identity(float[] m) {
        for (int i = 0; i < 16; i++) m[i] = 0f;
        m[0] = m[5] = m[10] = m[15] = 1f;
    }

    public static void copy(float[] src, float[] dst) {
        System.arraycopy(src, 0, dst, 0, 16);
    }

    /** out = a * b. out may alias a or b. */
    public static void mul(float[] out, float[] a, float[] b) {
        float[] r = (out == a || out == b) ? T1 : out;
        for (int c = 0; c < 4; c++) {
            float b0 = b[c * 4], b1 = b[c * 4 + 1], b2 = b[c * 4 + 2], b3 = b[c * 4 + 3];
            r[c * 4] = a[0] * b0 + a[4] * b1 + a[8] * b2 + a[12] * b3;
            r[c * 4 + 1] = a[1] * b0 + a[5] * b1 + a[9] * b2 + a[13] * b3;
            r[c * 4 + 2] = a[2] * b0 + a[6] * b1 + a[10] * b2 + a[14] * b3;
            r[c * 4 + 3] = a[3] * b0 + a[7] * b1 + a[11] * b2 + a[15] * b3;
        }
        if (r != out) System.arraycopy(r, 0, out, 0, 16);
    }

    public static void perspective(float[] m, float fovYDeg, float aspect, float near, float far) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fovYDeg) / 2.0));
        for (int i = 0; i < 16; i++) m[i] = 0f;
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1f;
        m[14] = (2f * far * near) / (near - far);
    }

    public static void ortho(float[] m, float l, float r, float b, float t, float n, float f) {
        for (int i = 0; i < 16; i++) m[i] = 0f;
        m[0] = 2f / (r - l);
        m[5] = 2f / (t - b);
        m[10] = -2f / (f - n);
        m[12] = -(r + l) / (r - l);
        m[13] = -(t + b) / (t - b);
        m[14] = -(f + n) / (f - n);
        m[15] = 1f;
    }

    public static void lookAt(float[] m, float ex, float ey, float ez, float cx, float cy, float cz, float ux, float uy, float uz) {
        float fx = cx - ex, fy = cy - ey, fz = cz - ez;
        float rl = 1f / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx *= rl; fy *= rl; fz *= rl;
        float sx = fy * uz - fz * uy, sy = fz * ux - fx * uz, sz = fx * uy - fy * ux;
        rl = 1f / (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx *= rl; sy *= rl; sz *= rl;
        float vx = sy * fz - sz * fy, vy = sz * fx - sx * fz, vz = sx * fy - sy * fx;
        m[0] = sx; m[1] = vx; m[2] = -fx; m[3] = 0f;
        m[4] = sy; m[5] = vy; m[6] = -fy; m[7] = 0f;
        m[8] = sz; m[9] = vz; m[10] = -fz; m[11] = 0f;
        m[12] = -(sx * ex + sy * ey + sz * ez);
        m[13] = -(vx * ex + vy * ey + vz * ez);
        m[14] = (fx * ex + fy * ey + fz * ez);
        m[15] = 1f;
    }

    /** m = translation * rotY(yaw) * rotX(pitch) * rotZ(roll) * scale. Angles in radians. */
    public static void trs(float[] m, float tx, float ty, float tz, float yaw, float pitch, float roll, float sx, float sy, float sz) {
        float cy = (float) Math.cos(yaw), sy0 = (float) Math.sin(yaw);
        float cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        // R = Ry * Rx * Rz
        float r00 = cy * cr + sy0 * sp * sr, r01 = -cy * sr + sy0 * sp * cr, r02 = sy0 * cp;
        float r10 = cp * sr, r11 = cp * cr, r12 = -sp;
        float r20 = -sy0 * cr + cy * sp * sr, r21 = sy0 * sr + cy * sp * cr, r22 = cy * cp;
        m[0] = r00 * sx; m[1] = r10 * sx; m[2] = r20 * sx; m[3] = 0f;
        m[4] = r01 * sy; m[5] = r11 * sy; m[6] = r21 * sy; m[7] = 0f;
        m[8] = r02 * sz; m[9] = r12 * sz; m[10] = r22 * sz; m[11] = 0f;
        m[12] = tx; m[13] = ty; m[14] = tz; m[15] = 1f;
    }

    public static void translate(float[] m, float x, float y, float z) {
        identity(m);
        m[12] = x; m[13] = y; m[14] = z;
    }

    public static void scale(float[] m, float x, float y, float z) {
        identity(m);
        m[0] = x; m[5] = y; m[10] = z;
    }

    public static void rotX(float[] m, float a) {
        identity(m);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        m[5] = c; m[6] = s; m[9] = -s; m[10] = c;
    }

    public static void rotY(float[] m, float a) {
        identity(m);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        m[0] = c; m[2] = -s; m[8] = s; m[10] = c;
    }

    public static void rotZ(float[] m, float a) {
        identity(m);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        m[0] = c; m[1] = s; m[4] = -s; m[5] = c;
    }

    /** m = m * T(x,y,z) */
    public static void postTranslate(float[] m, float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
    }

    /** m = m * Ry(a) */
    public static void postRotY(float[] m, float a) {
        rotY(T2, a);
        mul(m, m, T2);
    }

    public static void postRotX(float[] m, float a) {
        rotX(T2, a);
        mul(m, m, T2);
    }

    public static void postRotZ(float[] m, float a) {
        rotZ(T2, a);
        mul(m, m, T2);
    }

    public static void postScale(float[] m, float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
    }

    /** Transforms point (x,y,z,1); writes x,y,z,w into out[0..3]. */
    public static void transform(float[] m, float x, float y, float z, float[] out) {
        out[0] = m[0] * x + m[4] * y + m[8] * z + m[12];
        out[1] = m[1] * x + m[5] * y + m[9] * z + m[13];
        out[2] = m[2] * x + m[6] * y + m[10] * z + m[14];
        out[3] = m[3] * x + m[7] * y + m[11] * z + m[15];
    }

    public static boolean invert(float[] out, float[] m) {
        float[] inv = T1;
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (det == 0f) return false;
        det = 1f / det;
        for (int i = 0; i < 16; i++) out[i] = inv[i] * det;
        return true;
    }
}
