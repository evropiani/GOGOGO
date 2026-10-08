package com.gogogo.game.engine;

/**
 * Procedural builder for chunky toy-like geometry: rounded boxes, lathed shapes, spheres,
 * extrusions. Every primitive is placed through a transform stack and carries a color and
 * a material slot (0 = fixed color, 1 = tinted by primary instance color, 2 = by secondary).
 */
public final class MeshBuilder {
    public static final int FIXED = 0, PRIMARY = 1, SECONDARY = 2;

    private float[] v = new float[4096];
    private int vn; // floats used
    private short[] ix = new short[4096];
    private int in;

    private final float[][] stack = new float[16][16];
    private int sp;
    private final float[] tmp = new float[16];

    private float cr = 1f, cg = 1f, cb = 1f, slot = FIXED;
    private float sr = 1f, sg = 1f, sb = 1f; // side color for two-tone boxes
    private boolean twoTone;

    public MeshBuilder() {
        M4.identity(stack[0]);
    }

    // ---------------------------------------------------------------- state

    public MeshBuilder color(int rgb) {
        cr = ((rgb >> 16) & 255) / 255f;
        cg = ((rgb >> 8) & 255) / 255f;
        cb = (rgb & 255) / 255f;
        twoTone = false;
        return this;
    }

    /** Boxes get {@code top} on upward faces and {@code side} elsewhere. */
    public MeshBuilder twoTone(int top, int side) {
        color(top);
        sr = ((side >> 16) & 255) / 255f;
        sg = ((side >> 8) & 255) / 255f;
        sb = (side & 255) / 255f;
        twoTone = true;
        return this;
    }

    public MeshBuilder slot(int s) {
        slot = s;
        return this;
    }

    /** Shorthand: tinted by primary color with a brightness multiplier. */
    public MeshBuilder paint(float brightness) {
        int b = Math.max(0, Math.min(255, (int) (brightness * 255)));
        color((b << 16) | (b << 8) | b);
        slot = PRIMARY;
        return this;
    }

    public MeshBuilder accent(float brightness) {
        paint(brightness);
        slot = SECONDARY;
        return this;
    }

    public MeshBuilder fixed(int rgb) {
        color(rgb);
        slot = FIXED;
        return this;
    }

    public MeshBuilder push() {
        System.arraycopy(stack[sp], 0, stack[sp + 1], 0, 16);
        sp++;
        return this;
    }

    public MeshBuilder pop() {
        sp--;
        return this;
    }

    public MeshBuilder translate(float x, float y, float z) {
        M4.postTranslate(stack[sp], x, y, z);
        return this;
    }

    public MeshBuilder rotateX(float a) {
        M4.postRotX(stack[sp], a);
        return this;
    }

    public MeshBuilder rotateY(float a) {
        M4.postRotY(stack[sp], a);
        return this;
    }

    public MeshBuilder rotateZ(float a) {
        M4.postRotZ(stack[sp], a);
        return this;
    }

    public MeshBuilder scale(float s) {
        M4.postScale(stack[sp], s, s, s);
        return this;
    }

    /** Non-uniform scale; normals are corrected per vertex. */
    public MeshBuilder scale(float x, float y, float z) {
        M4.postScale(stack[sp], x, y, z);
        return this;
    }

    // ---------------------------------------------------------------- raw

    public int vertex(float x, float y, float z, float nx, float ny, float nz) {
        float[] m = stack[sp];
        if (vn + Mesh.VERTEX_FLOATS > v.length) {
            float[] n = new float[v.length * 2];
            System.arraycopy(v, 0, n, 0, vn);
            v = n;
        }
        float px = m[0] * x + m[4] * y + m[8] * z + m[12];
        float py = m[1] * x + m[5] * y + m[9] * z + m[13];
        float pz = m[2] * x + m[6] * y + m[10] * z + m[14];
        // normal: use inverse-transpose for scale (columns squared length)
        float l0 = m[0] * m[0] + m[1] * m[1] + m[2] * m[2];
        float l1 = m[4] * m[4] + m[5] * m[5] + m[6] * m[6];
        float l2 = m[8] * m[8] + m[9] * m[9] + m[10] * m[10];
        float ax = nx / l0, ay = ny / l1, az = nz / l2;
        float qx = m[0] * ax + m[4] * ay + m[8] * az;
        float qy = m[1] * ax + m[5] * ay + m[9] * az;
        float qz = m[2] * ax + m[6] * ay + m[10] * az;
        float ql = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
        if (ql > 1e-8f) {
            qx /= ql;
            qy /= ql;
            qz /= ql;
        }
        boolean side = twoTone && qy < 0.6f;
        v[vn++] = px;
        v[vn++] = py;
        v[vn++] = pz;
        v[vn++] = qx;
        v[vn++] = qy;
        v[vn++] = qz;
        v[vn++] = side ? sr : cr;
        v[vn++] = side ? sg : cg;
        v[vn++] = side ? sb : cb;
        v[vn++] = slot;
        return vn / Mesh.VERTEX_FLOATS - 1;
    }

    public void tri(int a, int b, int c) {
        if (in + 3 > ix.length) {
            short[] n = new short[ix.length * 2];
            System.arraycopy(ix, 0, n, 0, in);
            ix = n;
        }
        ix[in++] = (short) a;
        ix[in++] = (short) b;
        ix[in++] = (short) c;
    }

    public void quad(int a, int b, int c, int d) {
        tri(a, b, c);
        tri(a, c, d);
    }

    // ---------------------------------------------------------------- primitives

    /** Box centered at (cx,cy,cz) with full sizes; radius rounds all edges. */
    public MeshBuilder box(float cx, float cy, float cz, float sx, float sy, float sz, float radius) {
        return box(cx, cy, cz, sx, sy, sz, radius, 2);
    }

    public MeshBuilder box(float cx, float cy, float cz, float sx, float sy, float sz, float radius, int segs) {
        float hx = sx / 2, hy = sy / 2, hz = sz / 2;
        radius = Math.min(radius, Math.min(hx, Math.min(hy, hz)));
        if (radius <= 0.0001f) segs = 0;
        float[] h = {hx, hy, hz};
        float[][] bp = {breakpoints(hx, radius, segs), breakpoints(hy, radius, segs), breakpoints(hz, radius, segs)};
        for (int a = 0; a < 3; a++) {
            for (int s = -1; s <= 1; s += 2) {
                int ua, va;
                if (a == 0) { ua = 1; va = 2; } else if (a == 1) { ua = 2; va = 0; } else { ua = 0; va = 1; }
                if (s < 0) { int t = ua; ua = va; va = t; }
                float[] us = bp[ua], vs = bp[va];
                int base = vn / Mesh.VERTEX_FLOATS;
                float[] p = new float[3];
                for (int j = 0; j < vs.length; j++) {
                    for (int i = 0; i < us.length; i++) {
                        p[a] = s * h[a];
                        p[ua] = us[i];
                        p[va] = vs[j];
                        float ix0 = clamp(p[0], -(hx - radius), hx - radius);
                        float iy0 = clamp(p[1], -(hy - radius), hy - radius);
                        float iz0 = clamp(p[2], -(hz - radius), hz - radius);
                        float dx = p[0] - ix0, dy = p[1] - iy0, dz = p[2] - iz0;
                        float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                        float nx, ny, nz, px, py, pz;
                        if (dl < 1e-6f || radius <= 0.0001f) {
                            nx = a == 0 ? s : 0;
                            ny = a == 1 ? s : 0;
                            nz = a == 2 ? s : 0;
                            px = p[0]; py = p[1]; pz = p[2];
                        } else {
                            nx = dx / dl; ny = dy / dl; nz = dz / dl;
                            px = ix0 + nx * radius;
                            py = iy0 + ny * radius;
                            pz = iz0 + nz * radius;
                        }
                        vertex(cx + px, cy + py, cz + pz, nx, ny, nz);
                    }
                }
                int w = us.length;
                for (int j = 0; j < vs.length - 1; j++) {
                    for (int i = 0; i < w - 1; i++) {
                        int q = base + j * w + i;
                        quad(q, q + 1, q + 1 + w, q + w);
                    }
                }
            }
        }
        return this;
    }

    private static float[] breakpoints(float h, float r, int n) {
        if (n == 0) return new float[]{-h, h};
        float inner = h - r;
        boolean collapsed = inner < 1e-4f;
        int count = 2 * n + (collapsed ? 1 : 2);
        float[] out = new float[count];
        int k = 0;
        for (int i = n; i >= 1; i--) out[k++] = -inner - r * (float) Math.tan(i / (float) n * Math.PI / 4);
        if (collapsed) {
            out[k++] = 0f;
        } else {
            out[k++] = -inner;
            out[k++] = inner;
        }
        for (int i = 1; i <= n; i++) out[k++] = inner + r * (float) Math.tan(i / (float) n * Math.PI / 4);
        return out;
    }

    private static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }

    /**
     * Revolves a profile of (radius, y) pairs around the local Y axis. Repeating a point
     * creates a hard edge there. The profile should run bottom to top.
     */
    public MeshBuilder lathe(float[] profile, int segs) {
        int n = profile.length / 2;
        float[] nr = new float[n], ny = new float[n];
        for (int i = 0; i < n; i++) {
            float r = profile[i * 2], y = profile[i * 2 + 1];
            boolean dupPrev = i > 0 && profile[i * 2 - 2] == r && profile[i * 2 - 1] == y;
            boolean dupNext = i < n - 1 && profile[i * 2 + 2] == r && profile[i * 2 + 3] == y;
            int a = (i == 0 || dupPrev) ? i : i - 1;
            int b = (i == n - 1 || dupNext) ? i : i + 1;
            if (a == b) { // isolated point
                a = Math.max(0, i - 1);
                b = Math.min(n - 1, i + 1);
            }
            float tr = profile[b * 2] - profile[a * 2];
            float ty = profile[b * 2 + 1] - profile[a * 2 + 1];
            float l = (float) Math.sqrt(tr * tr + ty * ty);
            if (l < 1e-6f) { tr = 0; ty = 1; l = 1; }
            nr[i] = ty / l;
            ny[i] = -tr / l;
        }
        int base = vn / Mesh.VERTEX_FLOATS;
        for (int s = 0; s <= segs; s++) {
            double ang = s / (double) segs * Math.PI * 2;
            float c = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            for (int i = 0; i < n; i++) {
                float r = profile[i * 2], y = profile[i * 2 + 1];
                vertex(r * c, y, r * sn, nr[i] * c, ny[i], nr[i] * sn);
            }
        }
        for (int s = 0; s < segs; s++) {
            for (int i = 0; i < n - 1; i++) {
                int a = base + s * n + i;
                int b = base + (s + 1) * n + i;
                if (profile[i * 2] == profile[i * 2 + 2] && profile[i * 2 + 1] == profile[i * 2 + 3]) continue;
                quad(a, a + 1, b + 1, b);
            }
        }
        return this;
    }

    public MeshBuilder sphere(float cx, float cy, float cz, float r, int segs) {
        return ellipsoid(cx, cy, cz, r, r, r, segs);
    }

    public MeshBuilder ellipsoid(float cx, float cy, float cz, float rx, float ry, float rz, int segs) {
        int rings = Math.max(4, segs / 2);
        float[] prof = new float[(rings + 1) * 2];
        for (int i = 0; i <= rings; i++) {
            double a = -Math.PI / 2 + Math.PI * i / rings;
            prof[i * 2] = (float) Math.cos(a);
            prof[i * 2 + 1] = (float) Math.sin(a);
        }
        prof[0] = 0f;
        prof[rings * 2] = 0f;
        push();
        translate(cx, cy, cz);
        scale(rx, ry, rz);
        lathe(prof, segs);
        pop();
        return this;
    }

    /** Capped cylinder along local Y from y0 to y1 with rounded rims of radius bevel. */
    public MeshBuilder cylinder(float cx, float cy, float cz, float r, float y0, float y1, float bevel, int segs) {
        float[] prof;
        if (bevel <= 0.0001f) {
            prof = new float[]{0, y0, r, y0, r, y0, r, y1, r, y1, 0, y1};
        } else {
            int bs = 3;
            prof = new float[(4 + bs * 2) * 2];
            int k = 0;
            prof[k++] = 0; prof[k++] = y0;
            for (int i = 0; i <= bs; i++) {
                double a = -Math.PI / 2 + (Math.PI / 2) * i / bs;
                prof[k++] = r - bevel + bevel * (float) Math.cos(a);
                prof[k++] = y0 + bevel + bevel * (float) Math.sin(a);
            }
            for (int i = 0; i <= bs; i++) {
                double a = (Math.PI / 2) * i / bs;
                prof[k++] = r - bevel + bevel * (float) Math.cos(a);
                prof[k++] = y1 - bevel + bevel * (float) Math.sin(a);
            }
            prof[k++] = 0; prof[k++] = y1;
        }
        push();
        translate(cx, cy, cz);
        lathe(prof, segs);
        pop();
        return this;
    }

    /** Cone / frustum along Y. */
    public MeshBuilder cone(float cx, float cy, float cz, float r0, float r1, float h, int segs) {
        float[] prof = {0, 0, r0, 0, r0, 0, r1, h, r1, h, 0, h};
        push();
        translate(cx, cy, cz);
        lathe(prof, segs);
        pop();
        return this;
    }

    /** Torus around local Y. */
    public MeshBuilder torus(float cx, float cy, float cz, float R, float r, int segs, int tube) {
        float[] prof = new float[(tube + 1) * 2];
        for (int i = 0; i <= tube; i++) {
            double a = -Math.PI / 2 + Math.PI * 2 * i / tube;
            prof[i * 2] = R + r * (float) Math.cos(a);
            prof[i * 2 + 1] = r * (float) Math.sin(a);
        }
        push();
        translate(cx, cy, cz);
        lathe(prof, segs);
        pop();
        return this;
    }

    /** Extrudes a convex or star-shaped polygon (x,z pairs, counter-clockwise seen from above) between y0 and y1. */
    public MeshBuilder extrude(float[] poly, float y0, float y1) {
        int n = poly.length / 2;
        float cx = 0, cz = 0;
        for (int i = 0; i < n; i++) {
            cx += poly[i * 2];
            cz += poly[i * 2 + 1];
        }
        cx /= n;
        cz /= n;
        int c = vertex(cx, y1, cz, 0, 1, 0);
        int first = vn / Mesh.VERTEX_FLOATS;
        for (int i = 0; i < n; i++) vertex(poly[i * 2], y1, poly[i * 2 + 1], 0, 1, 0);
        for (int i = 0; i < n; i++) tri(c, first + (i + 1) % n, first + i);
        int cb = vertex(cx, y0, cz, 0, -1, 0);
        int firstB = vn / Mesh.VERTEX_FLOATS;
        for (int i = 0; i < n; i++) vertex(poly[i * 2], y0, poly[i * 2 + 1], 0, -1, 0);
        for (int i = 0; i < n; i++) tri(cb, firstB + i, firstB + (i + 1) % n);
        for (int i = 0; i < n; i++) {
            float x0 = poly[i * 2], z0 = poly[i * 2 + 1];
            float x1 = poly[((i + 1) % n) * 2], z1 = poly[((i + 1) % n) * 2 + 1];
            float ex = x1 - x0, ez = z1 - z0;
            float l = (float) Math.sqrt(ex * ex + ez * ez);
            float nx = ez / l, nz = -ex / l;
            int a = vertex(x0, y0, z0, nx, 0, nz);
            int b = vertex(x1, y0, z1, nx, 0, nz);
            int d = vertex(x1, y1, z1, nx, 0, nz);
            int e = vertex(x0, y1, z0, nx, 0, nz);
            quad(a, e, d, b);
        }
        return this;
    }

    public boolean isEmpty() {
        return in == 0;
    }

    public Mesh build() {
        float[] vv = new float[vn];
        System.arraycopy(v, 0, vv, 0, vn);
        short[] ii = new short[in];
        System.arraycopy(ix, 0, ii, 0, in);
        if (vn / Mesh.VERTEX_FLOATS > 65535) throw new IllegalStateException("Mesh too large");
        return new Mesh(vv, ii);
    }
}
