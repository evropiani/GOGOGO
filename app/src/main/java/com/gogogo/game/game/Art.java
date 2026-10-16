package com.gogogo.game.game;

import com.gogogo.game.engine.Mesh;
import com.gogogo.game.engine.MeshBuilder;
import com.gogogo.game.engine.Renderer;

/** All procedural meshes, built once at startup. */
public final class Art {
    public static final float TILE = 4.0f;       // tile size
    public static final float TILE_H = 1.0f;     // tile thickness

    private static final float PI = (float) Math.PI;

    public final Mesh tile;
    /** One tile mesh per arena skin (see Skins). */
    public final Mesh[] tileSkins = new Mesh[Skins.COUNT];
    public final Mesh[] symbols = new Mesh[6];
    public final Mesh[] cars = new Mesh[Cars.ALL.length];
    public final Mesh[] wheels = new Mesh[Palette.WHEEL_NAME.length];
    public final Mesh[] toppers = new Mesh[Palette.TOPPER_NAME.length];
    public final Mesh propeller;
    public final Mesh cube, ball, star, cloud, duck, marker, pedestal, ring, bonk;

    public Art(Renderer r) {
        MeshBuilder m;

        for (int i = 0; i < Skins.COUNT; i++) {
            m = new MeshBuilder();
            Skins.build(m, i);
            tileSkins[i] = r.register(m.build());
        }
        tile = tileSkins[0];

        for (int i = 0; i < 6; i++) {
            m = new MeshBuilder();
            m.paint(1f);
            m.extrude(symbolPoly(i), -0.05f, 0.07f);
            symbols[i] = r.register(m.build());
        }

        for (int i = 0; i < cars.length; i++) {
            m = new MeshBuilder();
            Cars.ALL[i].build(m);
            Cars.ALL[i].buildHatch(m);
            cars[i] = r.register(m.build());
            placeHoodCamera(Cars.ALL[i], cars[i]);
            carTop[i] = highest(cars[i]);
            if (i == Cars.DISCO) carTop[i] = Math.max(carTop[i], DISCO_Y + DISCO_R);
        }
        buildCarExtras(r);

        for (int i = 0; i < wheels.length; i++) {
            m = new MeshBuilder();
            m.rotateZ(PI / 2); // lathe axis Y -> X
            buildWheel(m, i);
            wheels[i] = r.register(m.build());
        }

        for (int i = 0; i < toppers.length; i++) {
            m = new MeshBuilder();
            buildTopper(m, i);
            if (m.isEmpty()) m.fixed(0xFFFFFF).box(0, -10, 0, 0.01f, 0.01f, 0.01f, 0);
            toppers[i] = r.register(m.build());
        }

        m = new MeshBuilder();
        m.fixed(0xFF3B5C).box(0, 0, 0, 1.3f, 0.05f, 0.16f, 0.04f);
        m.fixed(0x3BB8FF).box(0, 0, 0, 0.16f, 0.05f, 1.3f, 0.04f);
        m.fixed(0xFFD23B).sphere(0, 0.02f, 0, 0.1f, 8);
        propeller = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).box(0, 0, 0, 1, 1, 1, 0.18f, 1);
        cube = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).sphere(0, 0, 0, 1, 12);
        ball = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2).extrude(starPoly(1f, 0.45f), -0.2f, 0.2f).pop();
        star = r.register(m.build());

        m = new MeshBuilder();
        m.fixed(0xFFFFFF);
        m.sphere(0, 0, 0, 3.2f, 14);
        m.sphere(3.4f, -0.6f, 0.5f, 2.4f, 12);
        m.sphere(-3.2f, -0.8f, -0.3f, 2.2f, 12);
        m.sphere(1.2f, 1.6f, -0.8f, 2.2f, 12);
        m.sphere(-1.5f, 1.0f, 1.2f, 1.8f, 10);
        cloud = r.register(m.build());

        m = new MeshBuilder();
        m.fixed(0xFFE03B).ellipsoid(0, 0.45f, 0, 0.5f, 0.38f, 0.62f, 14);
        m.fixed(0xFFE03B).sphere(0, 0.98f, 0.28f, 0.33f, 14);
        m.push().translate(0, 0.62f, -0.55f).rotateX(-0.6f);
        m.fixed(0xFFE03B).ellipsoid(0, 0, 0, 0.2f, 0.16f, 0.2f, 8);
        m.pop();
        m.fixed(0xFF8A1F).ellipsoid(0, 0.92f, 0.62f, 0.18f, 0.07f, 0.16f, 10);
        m.fixed(0x1A1028).sphere(-0.15f, 1.06f, 0.55f, 0.06f, 6);
        m.fixed(0x1A1028).sphere(0.15f, 1.06f, 0.55f, 0.06f, 6);
        duck = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f);
        m.lathe(new float[]{0, -0.75f, 0.62f, 0.05f, 0.62f, 0.05f, 0.5f, 0.2f, 0.5f, 0.2f, 0, 0.2f}, 4);
        m.paint(1.1f).box(0, 0.55f, 0, 0.36f, 0.62f, 0.36f, 0.12f);
        marker = r.register(m.build());

        m = new MeshBuilder();
        m.twoTone(0xFFFFFF, 0xD8D0F0).slot(MeshBuilder.PRIMARY);
        m.cylinder(0, -1.2f, 0, 3.4f, 0, 1.2f, 0.25f, 40);
        m.accent(1f).cylinder(0, -0.75f, 0, 3.46f, 0, 0.3f, 0.12f, 40);
        pedestal = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).torus(0, 0, 0, 1f, 0.12f, 32, 8);
        ring = r.register(m.build());

        // comic "BONK" burst: a spiky star disc
        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2).extrude(starPoly2(10, 1f, 0.6f), -0.1f, 0.1f).pop();
        bonk = r.register(m.build());

        // ---- arena looks (skins, skies, trails): see the block at the end of the class
        buildLooks(r);
        // ---- power-ups: see the block after it
        buildPowerUps(r);
        // ---- drivers and their wardrobe: the last block
        buildDrivers(r);
    }

    /** Puts the hood camera over the middle of the car, just above its roof, so the hood shows below. */
    private static void placeHoodCamera(CarDef d, Mesh body) {
        if (d.hoodFixed) return;
        float top = 0f;
        float[] v = body.vertices;
        for (int k = 0; k < v.length; k += Mesh.VERTEX_FLOATS) {
            float z = v[k + 2];
            if (z > -0.3f && z < 0.6f && Math.abs(v[k]) < 0.6f) top = Math.max(top, v[k + 1]);
        }
        d.hoodY = Math.max(1.3f, top) + 0.3f;
        d.hoodZ = 0f;
    }

    // ------------------------------------------------------------------ premium cars and toppers: moving parts, shape helpers
    // The bodies live in Cars; the parts that move (mirror balls, traffic lamps) are drawn by CarRenderer.

    /** DISCO DASHER mirror ball (center in body space, radius). */
    public static final float DISCO_Y = 1.8f, DISCO_Z = -0.92f, DISCO_R = 0.36f;
    /** TRAFFIC TITAN traffic light wing: lamp x for red, yellow, green (red on the left seen from behind), height, z, radius. */
    public static final float[] TITAN_LAMP_X = {0.48f, 0f, -0.48f};
    public static final float TITAN_LAMP_Y = 1.72f, TITAN_LAMP_Z = -1.0f, TITAN_LAMP_R = 0.19f;
    /** Traffic Light topper lamps (topper space): heights of red, yellow, green; radius. */
    public static final float[] MINI_LAMP_Y = {0.93f, 0.72f, 0.51f};
    public static final float MINI_LAMP_R = 0.085f;
    /** Disco Ball topper: ball center height and radius. */
    public static final float MINI_BALL_Y = 0.66f, MINI_BALL_R = 0.3f;

    /** Unit mirror ball (fixed colors). */
    public Mesh mirrorBall;
    /** Unit traffic lamp along z with a lens dome at both ends; the primary instance color lights it. */
    public Mesh lamp;

    private void buildCarExtras(Renderer r) {
        MeshBuilder m = new MeshBuilder();
        mirrorBall(m);
        mirrorBall = r.register(m.build());

        m = new MeshBuilder();
        m.push().rotateX(PI / 2);
        m.paint(1f).cylinder(0, 0, 0, 1f, -1f, 1f, 0f, 16);
        m.pop();
        for (int s = -1; s <= 1; s += 2) {
            m.paint(1f).ellipsoid(0, 0, s * 1f, 1f, 1f, 0.5f, 16);
            m.fixed(0xFFFFFF).ellipsoid(-0.36f, 0.4f, s * 1.37f, 0.26f, 0.17f, 0.1f, 8);
            m.fixed(0xFFFFFF).sphere(0.16f, 0.56f, s * 1.33f, 0.09f, 6);
        }
        lamp = r.register(m.build());
    }

    /** Mirror ball of unit radius: flat silver tiles (a few catch party colors) around a dark core. */
    static void mirrorBall(MeshBuilder m) {
        m.fixed(0x3A3550).sphere(0, 0, 0, 0.9f, 12);
        int rings = 8;
        float[] q = new float[12];
        for (int j = 0; j < rings; j++) {
            double t0 = -Math.PI / 2 + Math.PI * j / rings, t1 = -Math.PI / 2 + Math.PI * (j + 1) / rings;
            int segs = Math.max(3, (int) Math.round(16 * Math.cos((t0 + t1) / 2)));
            for (int i = 0; i < segs; i++) {
                double p0 = Math.PI * 2 * i / segs + j * 0.4, p1 = Math.PI * 2 * (i + 1) / segs + j * 0.4;
                sph(q, 0, t0, p0);
                sph(q, 3, t0, p1);
                sph(q, 6, t1, p1);
                sph(q, 9, t1, p0);
                int h = (i * 7 + j * 13) % 11;
                int col = h == 0 ? 0xFF8AD8 : h == 5 ? 0x8AE8FF : h == 8 ? 0xFFE58A : (h % 2 == 0 ? 0xF4F8FF : 0xB8C2D8);
                m.fixed(col);
                facetQuad(m, q, 0.88f, 1.02f, 0, 0, 0);
            }
        }
    }

    /**
     * Traffic light visor: a curved hood of inner radius r and thickness t over the top of a lamp,
     * from angle a0 to a1 (0 = +x, PI / 2 = up), reaching from z = 0 to z = d.
     */
    static void visor(MeshBuilder m, float r, float t, float d, float a0, float a1, int segs) {
        int prev = -1;
        for (int i = 0; i <= segs; i++) {
            float a = a0 + (a1 - a0) * i / segs;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float ro = r + t;
            int o0 = m.vertex(c * ro, s * ro, 0, c, s, 0);
            m.vertex(c * ro, s * ro, d, c, s, 0);
            m.vertex(c * r, s * r, 0, -c, -s, 0);
            m.vertex(c * r, s * r, d, -c, -s, 0);
            m.vertex(c * ro, s * ro, d, 0, 0, 1);
            m.vertex(c * r, s * r, d, 0, 0, 1);
            if (prev >= 0) {
                m.quad(prev, o0, o0 + 1, prev + 1);
                m.quad(prev + 2, prev + 3, o0 + 3, o0 + 2);
                m.quad(prev + 5, prev + 4, o0 + 4, o0 + 5);
            }
            prev = o0;
        }
    }

    private static void sph(float[] q, int o, double t, double p) {
        q[o] = (float) (Math.cos(t) * Math.cos(p));
        q[o + 1] = (float) Math.sin(t);
        q[o + 2] = (float) (Math.cos(t) * Math.sin(p));
    }

    /**
     * Flat-shaded quad from four corners (12 floats), shrunk toward its center by {@code shrink}
     * and pushed out by {@code grow}; faces away from the reference point (ox, oy, oz).
     */
    static void facetQuad(MeshBuilder m, float[] q, float shrink, float grow, float ox, float oy, float oz) {
        float cx = (q[0] + q[3] + q[6] + q[9]) / 4, cy = (q[1] + q[4] + q[7] + q[10]) / 4, cz = (q[2] + q[5] + q[8] + q[11]) / 4;
        float[] p = FACET;
        for (int k = 0; k < 4; k++) {
            p[k * 3] = (cx + (q[k * 3] - cx) * shrink - ox) * grow + ox;
            p[k * 3 + 1] = (cy + (q[k * 3 + 1] - cy) * shrink - oy) * grow + oy;
            p[k * 3 + 2] = (cz + (q[k * 3 + 2] - cz) * shrink - oz) * grow + oz;
        }
        // normal from the diagonals (robust when two corners meet at a pole)
        float ax = p[6] - p[0], ay = p[7] - p[1], az = p[8] - p[2];
        float bx = p[9] - p[3], by = p[10] - p[4], bz = p[11] - p[5];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-8f) return;
        nx /= l;
        ny /= l;
        nz /= l;
        boolean flip = nx * (cx - ox) + ny * (cy - oy) + nz * (cz - oz) < 0;
        if (flip) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }
        int a = m.vertex(p[0], p[1], p[2], nx, ny, nz);
        int b = m.vertex(p[3], p[4], p[5], nx, ny, nz);
        int c = m.vertex(p[6], p[7], p[8], nx, ny, nz);
        int d = m.vertex(p[9], p[10], p[11], nx, ny, nz);
        if (flip) {
            m.quad(a, d, c, b);
        } else {
            m.quad(a, b, c, d);
        }
    }

    private static final float[] FACET = new float[12];

    /** Flat-shaded triangle facing away from (ox, oy, oz). */
    static void facetTri(MeshBuilder m, float ax, float ay, float az, float bx, float by, float bz,
                         float cx, float cy, float cz, float ox, float oy, float oz) {
        float ux = bx - ax, uy = by - ay, uz = bz - az, vx = cx - ax, vy = cy - ay, vz = cz - az;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-8f) return;
        nx /= l;
        ny /= l;
        nz /= l;
        float mx = (ax + bx + cx) / 3 - ox, my = (ay + by + cy) / 3 - oy, mz = (az + bz + cz) / 3 - oz;
        boolean flip = nx * mx + ny * my + nz * mz < 0;
        if (flip) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }
        int a = m.vertex(ax, ay, az, nx, ny, nz);
        int b = m.vertex(bx, by, bz, nx, ny, nz);
        int c = m.vertex(cx, cy, cz, nx, ny, nz);
        if (flip) {
            m.tri(a, c, b);
        } else {
            m.tri(a, b, c);
        }
    }

    /** Convex polygon (x,z pairs) with corners rounded by quadratic curves, returned counter-clockwise for extrude(). */
    static float[] roundPoly(float[] c, float r, int segs) {
        int n = c.length / 2;
        float area = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += c[i * 2] * c[j * 2 + 1] - c[j * 2] * c[i * 2 + 1];
        }
        float[] out = new float[n * (segs + 1) * 2];
        int k = 0;
        for (int ii = 0; ii < n; ii++) {
            int i = area >= 0 ? ii : n - 1 - ii;
            int prev = area >= 0 ? (i + n - 1) % n : (i + 1) % n;
            int next = area >= 0 ? (i + 1) % n : (i + n - 1) % n;
            float px = c[i * 2], pz = c[i * 2 + 1];
            float ax = c[prev * 2] - px, az = c[prev * 2 + 1] - pz;
            float bx = c[next * 2] - px, bz = c[next * 2 + 1] - pz;
            float la = (float) Math.sqrt(ax * ax + az * az), lb = (float) Math.sqrt(bx * bx + bz * bz);
            float ra = Math.min(r, la * 0.45f), rb = Math.min(r, lb * 0.45f);
            float x0 = px + ax / la * ra, z0 = pz + az / la * ra;
            float x1 = px + bx / lb * rb, z1 = pz + bz / lb * rb;
            for (int s = 0; s <= segs; s++) {
                float t = s / (float) segs, u = 1 - t;
                out[k++] = u * u * x0 + 2 * u * t * px + t * t * x1;
                out[k++] = u * u * z0 + 2 * u * t * pz + t * t * z1;
            }
        }
        return out;
    }

    /**
     * Faceted gem along +Y: pavilion point at y = 0, girdle of radius r at 0.42 h, flat table at h.
     * Facets alternate between the two colors.
     */
    static void gem(MeshBuilder m, float r, float h, int n, int c1, int c2) {
        float gy = h * 0.42f, oy = h * 0.45f;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n, am = (a0 + a1) / 2, an = am + Math.PI * 2 / n;
            float gx0 = (float) Math.cos(a0) * r, gz0 = (float) Math.sin(a0) * r;
            float gx1 = (float) Math.cos(a1) * r, gz1 = (float) Math.sin(a1) * r;
            float ux0 = (float) Math.cos(am) * r * 0.56f, uz0 = (float) Math.sin(am) * r * 0.56f;
            float ux1 = (float) Math.cos(an) * r * 0.56f, uz1 = (float) Math.sin(an) * r * 0.56f;
            m.fixed(i % 2 == 0 ? c2 : c1);
            facetTri(m, 0, 0, 0, gx0, gy, gz0, gx1, gy, gz1, 0, oy, 0);
            m.fixed(i % 2 == 0 ? c1 : c2);
            facetTri(m, gx0, gy, gz0, gx1, gy, gz1, ux0, h, uz0, 0, oy, 0);
            m.fixed(i % 2 == 0 ? 0xFFFFFF : c1);
            facetTri(m, ux0, h, uz0, gx1, gy, gz1, ux1, h, uz1, 0, oy, 0);
            m.fixed(c1);
            facetTri(m, 0, h, 0, ux0, h, uz0, ux1, h, uz1, 0, oy, 0);
        }
    }

    // ------------------------------------------------------------------ shapes

    static float[] symbolPoly(int i) {
        switch (i) {
            case 0: return ngon(18, 0.8f, 0f);                       // circle
            case 1: return starPoly(0.95f, 0.42f);                   // star
            case 2: return ngon(4, 0.95f, 0f);                        // diamond
            case 3: return ngon(3, 0.95f, PI / 2);                    // triangle (points to +z)
            case 4: return plusPoly(0.9f, 0.3f);                      // plus
            default: return ngon(6, 0.85f, 0f);                       // hexagon
        }
    }

    static float[] ngon(int n, float r, float rot) {
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = rot + i * Math.PI * 2 / n;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    static float[] starPoly(float ro, float ri) {
        float[] p = new float[20];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            float r = (i % 2 == 0) ? ro : ri;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    static float[] starPoly2(int points, float ro, float ri) {
        float[] p = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = i * Math.PI / points;
            float r = (i % 2 == 0) ? ro : ri;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    static float[] plusPoly(float a, float b) {
        return new float[]{
                b, -b, a, -b, a, b, b, b, b, a, -b, a,
                -b, b, -a, b, -a, -b, -b, -b, -b, -a, b, -a};
    }

    // ------------------------------------------------------------------ wheels (built along Y, rotated to X)

    private static void buildWheel(MeshBuilder m, int style) {
        switch (style) {
            case 1: // candy
                m.fixed(0xFF6FB5).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.55f, -0.54f, 0.54f, 0.08f, 14);
                m.fixed(0xFF3B8A).box(0.25f, 0.55f, 0, 0.2f, 0.06f, 0.2f, 0.04f);
                m.fixed(0xFF3B8A).box(0.25f, -0.55f, 0, 0.2f, 0.06f, 0.2f, 0.04f);
                break;
            case 2: // neon
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0x3BF0FF).cylinder(0, 0, 0, 0.62f, -0.54f, 0.54f, 0.08f, 14);
                m.fixed(0xFF3BF0).cylinder(0, 0, 0, 0.3f, -0.58f, 0.58f, 0.06f, 10);
                m.fixed(0x2B2B38).box(0.38f, 0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                m.fixed(0x2B2B38).box(0.38f, -0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                break;
            case 3: // donut
                m.push().rotateX(0);
                m.fixed(0xE0A060).torus(0, 0, 0, 0.62f, 0.4f, 20, 10);
                m.fixed(0xFF7AC8).torus(0, 0.12f, 0, 0.62f, 0.33f, 20, 8);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    m.fixed(i % 2 == 0 ? 0xFFFFFF : 0x3BB8FF).box((float) Math.cos(a) * 0.62f, 0.45f, (float) Math.sin(a) * 0.62f, 0.18f, 0.06f, 0.06f, 0.02f);
                }
                m.pop();
                break;
            case 4: // gold
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0xFFC83D).cylinder(0, 0, 0, 0.72f, -0.54f, 0.54f, 0.1f, 14);
                m.fixed(0xFFF0A0).cylinder(0, 0, 0, 0.3f, -0.58f, 0.58f, 0.06f, 10);
                m.fixed(0xC08A10).box(0.45f, 0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                m.fixed(0xC08A10).box(0.45f, -0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                break;
            case 5: // pizza: crust tire, cheese face, pepperoni, slice cuts
                m.fixed(0xE3A253).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.32f, 16);
                m.fixed(0xFFD452).cylinder(0, 0, 0, 0.8f, -0.53f, 0.53f, 0.06f, 18);
                for (int k = 0; k < 6; k++) {
                    m.push().rotateY(k * PI / 3);
                    m.fixed(0xD9A040).box(0.4f, 0, 0, 0.78f, 1.09f, 0.035f, 0f);
                    m.pop();
                }
                for (int k = 0; k < 5; k++) {
                    double a = k * Math.PI * 2 / 5 + 0.4;
                    m.fixed(0xD8402E).cylinder((float) Math.cos(a) * 0.5f, 0, (float) Math.sin(a) * 0.5f, 0.15f, -0.57f, 0.57f, 0f, 12);
                }
                m.fixed(0xD8402E).cylinder(0, 0, 0, 0.13f, -0.57f, 0.57f, 0f, 12);
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + 1.0;
                    m.fixed(0x4FB848).box((float) Math.cos(a) * 0.3f, 0, (float) Math.sin(a) * 0.3f, 0.1f, 1.12f, 0.06f, 0.02f, 1);
                }
                break;
            case 6: // rainbow: stepped rings of color on a dark tire
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.3f, 16);
                for (int k = 0; k < 6; k++) {
                    float h = 0.52f + k * 0.025f;
                    m.fixed(RAINBOW_RINGS[k]).cylinder(0, 0, 0, 0.84f - k * 0.12f, -h, h, 0.02f, 18);
                }
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.12f, -0.69f, 0.69f, 0.04f, 10);
                break;
            case 7: // spiky: studded tire, steel hub with a chariot spike
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 0.94f, -0.5f, 0.5f, 0.28f, 14);
                m.fixed(0x9AA0B4).cylinder(0, 0, 0, 0.55f, -0.54f, 0.54f, 0.08f, 12);
                m.fixed(0xFF3B5C).cylinder(0, 0, 0, 0.32f, -0.58f, 0.58f, 0.05f, 12);
                for (int k = 0; k < 8; k++) {
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().rotateY(k * PI / 4 + (s > 0 ? 0f : PI / 8)).translate(0.84f, s * 0.24f, 0).rotateZ(-PI / 2);
                        m.fixed(0xE4E8F2).cone(0, 0, 0, 0.15f, 0.01f, 0.3f, 8);
                        m.pop();
                    }
                }
                for (int s = -1; s <= 1; s += 2) {
                    m.push().rotateX(s > 0 ? 0f : PI);
                    m.fixed(0xE4E8F2).cone(0, 0.56f, 0, 0.2f, 0.01f, 0.5f, 10);
                    m.pop();
                }
                break;
            case 8: // flames: a fireball swirling on a black tire
                m.fixed(0x22202C).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.3f, 14);
                m.fixed(0xFF3B1F).cylinder(0, 0, 0, 0.8f, -0.53f, 0.53f, 0.06f, 16);
                m.fixed(0xFF8A1F).extrude(swirlStar(8, 0.88f, 0.4f, 0.34f), -0.57f, 0.57f);
                m.fixed(0xFFD23B).extrude(swirlStar(8, 0.6f, 0.26f, 0.34f), -0.61f, 0.61f);
                m.fixed(0xFFF6C0).cylinder(0, 0, 0, 0.14f, -0.65f, 0.65f, 0.04f, 10);
                break;
            case 9: // diamond: platinum rim with a cut gem on each side
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.3f, 14);
                m.fixed(0xDDE6F2).cylinder(0, 0, 0, 0.8f, -0.53f, 0.53f, 0.06f, 16);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().rotateX(s > 0 ? 0f : PI).translate(0, 0.37f, 0);
                    gem(m, 0.58f, 0.4f, 8, 0xBFF4FF, 0x6FCBFF);
                    m.pop();
                }
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + Math.PI / 8;
                    m.fixed(0xF4FCFF).cylinder((float) Math.cos(a) * 0.7f, 0, (float) Math.sin(a) * 0.7f, 0.05f, -0.57f, 0.57f, 0.02f, 6);
                }
                break;
            default: // classic
                m.fixed(0x2B2B38).cylinder(0, 0, 0, 1f, -0.5f, 0.5f, 0.3f, 14);
                m.fixed(0xF0F0F8).cylinder(0, 0, 0, 0.52f, -0.54f, 0.54f, 0.08f, 14);
                m.fixed(0xFF4FA3).box(0.24f, 0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                m.fixed(0xFF4FA3).box(0.24f, -0.56f, 0, 0.16f, 0.06f, 0.16f, 0.04f);
                break;
        }
    }

    private static final int[] RAINBOW_RINGS = {0xFF4F5E, 0xFF9A3D, 0xFFE14D, 0x5EE65A, 0x3BB8FF, 0xA46BFF};

    /** Star whose points are swept sideways by {@code twist} radians (a pinwheel of flames); twist stays below PI / points. */
    static float[] swirlStar(int points, float ro, float ri, float twist) {
        float[] p = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = i * Math.PI / points + (i % 2 == 0 ? twist : 0f);
            float r = (i % 2 == 0) ? ro : ri;
            p[i * 2] = (float) (Math.cos(a) * r);
            p[i * 2 + 1] = (float) (Math.sin(a) * r);
        }
        return p;
    }

    /** Upright half-ellipse slab (hat brims): half width w, height h, from z0 to z1. */
    private static void panel(MeshBuilder m, int rgb, float w, float h, float z0, float z1) {
        float[] p = new float[26];
        for (int i = 0; i <= 12; i++) {
            double a = Math.PI * i / 12;
            p[i * 2] = (float) Math.cos(a) * w;
            p[i * 2 + 1] = (float) Math.sin(a) * h;
        }
        m.push().rotateX(-PI / 2);
        m.fixed(rgb).extrude(p, -z1, -z0);
        m.pop();
    }

    // ------------------------------------------------------------------ toppers (base at y = 0)

    private static void buildTopper(MeshBuilder m, int i) {
        switch (i) {
            case 1: // antenna ball
                m.fixed(0xB0B0C0).cylinder(0, 0, 0, 0.04f, 0, 1.0f, 0.01f, 6);
                m.fixed(0xFFE14D).sphere(0, 1.05f, 0, 0.2f, 12);
                m.fixed(0x1A1028).sphere(-0.07f, 1.09f, 0.15f, 0.05f, 6);
                m.fixed(0x1A1028).sphere(0.07f, 1.09f, 0.15f, 0.05f, 6);
                break;
            case 2: // traffic cone
                m.fixed(0xFF7A1A).box(0, 0.05f, 0, 0.8f, 0.1f, 0.8f, 0.05f);
                m.fixed(0xFF7A1A).cone(0, 0.08f, 0, 0.32f, 0.22f, 0.3f, 14);
                m.fixed(0xFFFFFF).cone(0, 0.38f, 0, 0.22f, 0.15f, 0.2f, 14);
                m.fixed(0xFF7A1A).cone(0, 0.58f, 0, 0.15f, 0.04f, 0.32f, 14);
                break;
            case 3: // party hat
                m.fixed(0x9B6BFF).cone(0, 0, 0, 0.34f, 0.02f, 0.9f, 16);
                m.fixed(0xFFE14D).cone(0, 0.25f, 0, 0.27f, 0.25f, 0.08f, 16);
                m.fixed(0x5EE65A).cone(0, 0.55f, 0, 0.17f, 0.15f, 0.08f, 16);
                m.fixed(0xFF4FA3).sphere(0, 0.95f, 0, 0.13f, 10);
                break;
            case 4: // propeller cap (blades drawn separately)
                m.fixed(0xFF3B5C).ellipsoid(0, 0, 0, 0.45f, 0.3f, 0.45f, 16);
                m.fixed(0x3BB8FF).box(0, 0.03f, 0.42f, 0.5f, 0.06f, 0.3f, 0.03f);
                m.fixed(0xFFD23B).cylinder(0, 0.25f, 0, 0.05f, 0, 0.18f, 0.01f, 6);
                break;
            case 5: // chef hat
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.3f, 0, 0.38f, 0.05f, 14);
                m.fixed(0xFFFFFF).sphere(0, 0.5f, 0, 0.3f, 12);
                m.fixed(0xFFFFFF).sphere(0.2f, 0.45f, 0.1f, 0.22f, 10);
                m.fixed(0xFFFFFF).sphere(-0.2f, 0.45f, 0.08f, 0.22f, 10);
                m.fixed(0xFFFFFF).sphere(0, 0.45f, -0.2f, 0.22f, 10);
                break;
            case 6: // banana
                for (int k = 0; k < 7; k++) {
                    float t = k / 6f;
                    float a = -1.1f + t * 2.2f;
                    float x = (float) Math.sin(a) * 0.6f, y = 0.15f + (float) Math.cos(a) * 0.1f + (1 - Math.abs(t - 0.5f) * 2) * 0.05f;
                    float rr = 0.2f * (1f - Math.abs(t - 0.5f) * 1.1f);
                    m.fixed(0xFFE14D).sphere(x, y + 0.4f - (float) Math.cos(a) * 0.4f, 0, rr + 0.04f, 10);
                }
                m.fixed(0x6B4A2A).sphere(-0.55f, 0.32f, 0, 0.06f, 6);
                m.fixed(0x6B4A2A).sphere(0.55f, 0.32f, 0, 0.06f, 6);
                break;
            case 7: // shark fin
                m.push().rotateX(-0.35f).scale(0.28f, 1f, 1f);
                m.fixed(0x6D7FA8).cone(0, 0, 0, 0.5f, 0.02f, 1.0f, 16);
                m.pop();
                break;
            case 8: // flower
                m.fixed(0x3FAF4A).cylinder(0, 0, 0, 0.05f, 0, 0.7f, 0.01f, 6);
                m.fixed(0x5EE65A).ellipsoid(0.15f, 0.3f, 0, 0.16f, 0.05f, 0.08f, 8);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    m.fixed(0xFF6FB5).ellipsoid((float) Math.cos(a) * 0.2f, 0.78f, (float) Math.sin(a) * 0.2f, 0.15f, 0.08f, 0.15f, 8);
                }
                m.fixed(0xFFD23B).sphere(0, 0.8f, 0, 0.12f, 8);
                break;
            case 9: // top hat
                m.fixed(0x252030).cylinder(0, 0, 0, 0.5f, 0, 0.07f, 0.03f, 18);
                m.fixed(0x252030).cylinder(0, 0.05f, 0, 0.32f, 0, 0.7f, 0.04f, 18);
                m.fixed(0xFF3B5C).cylinder(0, 0.1f, 0, 0.335f, 0, 0.12f, 0.02f, 18);
                break;
            case 10: // viking horns
                m.fixed(0x9AA0B0).ellipsoid(0, 0, 0, 0.45f, 0.38f, 0.45f, 16);
                m.fixed(0xC8A050).cylinder(0, 0.02f, 0, 0.47f, 0, 0.1f, 0.03f, 16);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.38f, 0.15f, 0).rotateZ(-s * 1.0f);
                    m.fixed(0xFFF6E0).cone(0, 0, 0, 0.13f, 0.02f, 0.55f, 10);
                    m.pop();
                }
                break;
            case 11: // crown
                m.fixed(0xFFC83D).lathe(new float[]{0, 0, 0.4f, 0, 0.4f, 0, 0.42f, 0.3f, 0.42f, 0.3f, 0.34f, 0.3f, 0.34f, 0.3f, 0.32f, 0.04f, 0.32f, 0.04f, 0, 0.04f}, 18);
                for (int k = 0; k < 5; k++) {
                    double a = k * Math.PI * 2 / 5;
                    m.fixed(0xFFC83D).cone((float) Math.cos(a) * 0.37f, 0.28f, (float) Math.sin(a) * 0.37f, 0.08f, 0.01f, 0.22f, 8);
                    m.fixed(k % 2 == 0 ? 0xFF3B5C : 0x3BB8FF).sphere((float) Math.cos(a) * 0.42f, 0.15f, (float) Math.sin(a) * 0.42f, 0.06f, 6);
                }
                break;
            case 12: // halo
                m.fixed(0xFFE58A).torus(0, 0.55f, 0, 0.42f, 0.07f, 24, 8);
                break;
            case 13: // bunny ears on a pink pad, one ear flopped over
                m.fixed(0xFF6FB5).ellipsoid(0, 0.02f, 0, 0.36f, 0.09f, 0.2f, 14);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.17f, 0.04f, 0).rotateZ(-s * 0.2f).rotateX(-0.12f);
                    if (s < 0) {
                        m.fixed(0xFFFFFF).ellipsoid(0, 0.44f, 0, 0.13f, 0.44f, 0.08f, 14);
                        m.fixed(0xFFA6CE).ellipsoid(0, 0.44f, 0.045f, 0.075f, 0.34f, 0.045f, 12);
                    } else {
                        m.fixed(0xFFFFFF).ellipsoid(0, 0.26f, 0, 0.13f, 0.28f, 0.08f, 12);
                        m.fixed(0xFFA6CE).ellipsoid(0, 0.26f, 0.045f, 0.075f, 0.2f, 0.045f, 10);
                        m.push().translate(0, 0.48f, 0).rotateZ(-1.0f).rotateX(0.3f);
                        m.fixed(0xFFFFFF).ellipsoid(0, 0.2f, 0, 0.125f, 0.24f, 0.08f, 12);
                        m.fixed(0xFFA6CE).ellipsoid(0, 0.2f, 0.045f, 0.07f, 0.17f, 0.045f, 10);
                        m.pop();
                    }
                    m.pop();
                }
                break;
            case 14: // wizard hat with a floppy tip and stars
                m.fixed(0x4632B4).cylinder(0, 0, 0, 0.52f, 0, 0.07f, 0.035f, 20);
                m.fixed(0x4632B4).cone(0, 0.05f, 0, 0.32f, 0.22f, 0.42f, 18);
                m.fixed(0xFFC83D).cone(0, 0.06f, 0, 0.335f, 0.31f, 0.1f, 18);
                m.push().translate(0, 0.46f, 0).rotateX(-0.3f);
                m.fixed(0x4632B4).cone(0, -0.02f, 0, 0.22f, 0.13f, 0.3f, 14);
                m.push().translate(0, 0.27f, 0).rotateX(-0.55f);
                m.fixed(0x4632B4).cone(0, -0.02f, 0, 0.13f, 0.015f, 0.32f, 12);
                m.fixed(0xFFE14D).sphere(0, 0.3f, 0, 0.05f, 8);
                m.pop();
                m.pop();
                for (int k = 0; k < 3; k++) {
                    float y = k == 0 ? 0.26f : 0.34f, r = 0.32f - (y - 0.05f) * 0.24f;
                    m.push().rotateY(k == 0 ? 0f : (k == 1 ? 1.4f : -1.5f)).translate(0, y, r).rotateX(PI / 2 - 0.24f);
                    m.fixed(0xFFE14D).extrude(starPoly(k == 0 ? 0.1f : 0.07f, k == 0 ? 0.045f : 0.032f), -0.02f, 0.025f);
                    m.pop();
                }
                break;
            case 15: // pirate hat: black bicorne with gold trim and a skull
                m.fixed(0x2A2430).cylinder(0, 0, 0, 0.26f, 0, 0.2f, 0.04f, 16);
                m.fixed(0x2A2430).ellipsoid(0, 0.2f, 0, 0.26f, 0.18f, 0.24f, 16);
                m.push().translate(0, 0, -0.22f).rotateX(0.22f).rotateY(PI);
                panel(m, 0xFFC83D, 0.54f, 0.46f, 0f, 0.05f);
                panel(m, 0x2A2430, 0.5f, 0.42f, 0.02f, 0.08f);
                m.pop();
                m.push().translate(0, 0, 0.22f).rotateX(-0.18f);
                panel(m, 0xFFC83D, 0.6f, 0.58f, 0f, 0.05f);
                panel(m, 0x2A2430, 0.56f, 0.54f, 0.02f, 0.08f);
                // skull and crossbones
                m.push().translate(0, 0.28f, 0.08f);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().rotateZ(s * 0.75f);
                    m.fixed(0xFFFFFF).box(0, 0, 0.015f, 0.34f, 0.045f, 0.03f, 0.02f, 1);
                    m.fixed(0xFFFFFF).sphere(0.17f, 0.025f, 0.015f, 0.03f, 6);
                    m.fixed(0xFFFFFF).sphere(0.17f, -0.025f, 0.015f, 0.03f, 6);
                    m.fixed(0xFFFFFF).sphere(-0.17f, 0.025f, 0.015f, 0.03f, 6);
                    m.fixed(0xFFFFFF).sphere(-0.17f, -0.025f, 0.015f, 0.03f, 6);
                    m.pop();
                }
                m.fixed(0xFFFFFF).ellipsoid(0, 0.02f, 0.015f, 0.09f, 0.085f, 0.045f, 10);
                m.fixed(0xFFFFFF).box(0, -0.06f, 0.015f, 0.085f, 0.05f, 0.05f, 0.015f, 1);
                m.fixed(0x2A2430).sphere(-0.033f, 0.03f, 0.053f, 0.024f, 6);
                m.fixed(0x2A2430).sphere(0.033f, 0.03f, 0.053f, 0.024f, 6);
                m.pop();
                m.pop();
                break;
            case 16: // unicorn horn: spiral pearl horn on a rainbow tuft
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + 0.3;
                    m.fixed(RAINBOW_RINGS[k]).sphere((float) Math.cos(a) * 0.17f, 0.06f, (float) Math.sin(a) * 0.17f, 0.12f, 10);
                }
                m.push().translate(0, 0.08f, 0.02f).rotateX(0.22f);
                m.fixed(0xFFF1CC).cone(0, 0, 0, 0.17f, 0.012f, 0.92f, 16);
                for (int k = 0; k < 14; k++) {
                    float t = k / 14f;
                    double a = t * Math.PI * 5;
                    float rr = 0.17f * (1 - t) + 0.005f;
                    m.fixed(0xFFC83D).sphere((float) Math.cos(a) * rr, t * 0.86f + 0.03f, (float) Math.sin(a) * rr, 0.045f * (1 - t * 0.6f), 6);
                }
                m.pop();
                break;
            case 17: // headphones: band over the roof, chunky cups at the sides
                m.push().translate(0, 0.2f, 0);
                m.fixed(0x3A3550);
                visor(m, 0.42f, 0.07f, 0.07f, 0f, PI, 16);
                m.rotateY(PI);
                visor(m, 0.42f, 0.07f, 0.07f, 0f, PI, 16);
                m.pop();
                m.fixed(0xFF8AC8).box(0, 0.68f, 0, 0.3f, 0.06f, 0.16f, 0.03f, 1);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.45f, 0.21f, 0).rotateZ(PI / 2);
                    m.fixed(0xFF4FA3).cylinder(0, 0, 0, 0.22f, -0.08f, 0.08f, 0.05f, 18);
                    m.fixed(0x3A3550).cylinder(0, s * 0.1f, 0, 0.18f, -0.04f, 0.04f, 0.03f, 16);
                    m.fixed(0xFFFFFF).cylinder(0, -s * 0.09f, 0, 0.13f, -0.03f, 0.03f, 0.02f, 14);
                    m.pop();
                }
                break;
            case 18: // disco ball stand (the ball is drawn spinning by CarRenderer)
                m.fixed(0xD8DEEA).cylinder(0, 0, 0, 0.24f, 0, 0.07f, 0.03f, 16);
                m.fixed(0xD8DEEA).cylinder(0, 0, 0, 0.045f, 0.05f, MINI_BALL_Y - MINI_BALL_R + 0.04f, 0.015f, 8);
                m.fixed(0xFFC83D).cylinder(0, MINI_BALL_Y - MINI_BALL_R - 0.02f, 0, 0.09f, 0, 0.05f, 0.02f, 10);
                m.fixed(0xFF4FA3).sphere(0.17f, 0.08f, 0.12f, 0.035f, 6);
                m.fixed(0x3BE0FF).sphere(-0.16f, 0.08f, 0.13f, 0.035f, 6);
                m.fixed(0xFFE14D).sphere(0.02f, 0.08f, -0.2f, 0.035f, 6);
                break;
            case 19: // mini traffic light (the lamps are drawn by CarRenderer)
                m.fixed(0x3A3F50).cylinder(0, 0, 0, 0.17f, 0, 0.07f, 0.03f, 14);
                m.fixed(0x9AA0B4).cylinder(0, 0.05f, 0, 0.04f, 0, 0.36f, 0.015f, 8);
                m.fixed(0xFFC21F).box(0, 0.72f, 0, 0.32f, 0.74f, 0.08f, 0.04f);
                m.fixed(0x262633).box(0, 0.72f, 0, 0.25f, 0.66f, 0.16f, 0.08f);
                for (int k = 0; k < 3; k++) {
                    for (int s = -1; s <= 1; s += 2) {
                        m.push().translate(0, MINI_LAMP_Y[k], s * 0.075f).rotateY(s > 0 ? 0f : PI).rotateX(-0.1f);
                        m.fixed(0x262633);
                        visor(m, MINI_LAMP_R + 0.012f, 0.02f, 0.08f, -0.2f, PI + 0.2f, 10);
                        m.pop();
                    }
                }
                break;
            case 20: // golden laurel wreath with a ruby at the back
                for (int s = -1; s <= 1; s += 2) {
                    for (int k = 0; k < 7; k++) {
                        float th = s * (0.42f + k * 0.37f);
                        for (int side = -1; side <= 1; side += 2) {
                            m.push().rotateY(th).translate(0, 0.14f + k * 0.01f, 0.4f + side * 0.045f).rotateY(-s * 0.35f * side).rotateX(side * -1.0f);
                            m.fixed((k + (side > 0 ? 0 : 1)) % 2 == 0 ? 0xFFC83D : 0xFFE07A).ellipsoid(0, 0, 0, 0.15f, 0.03f, 0.075f, 8);
                            m.pop();
                        }
                    }
                }
                m.fixed(0xE0A020).torus(0, 0.08f, 0, 0.4f, 0.025f, 24, 5);
                m.fixed(0xFFC83D).sphere(0, 0.12f, -0.42f, 0.08f, 10);
                m.fixed(0xFF2E4A).sphere(0, 0.13f, -0.48f, 0.06f, 8);
                break;
            default:
                break;
        }
    }

    // ================================================================== arena looks: skins, skies, trails

    /** Glow overlay per skin (null when the skin has none, see Skins.buildGlow). */
    public final Mesh[] tileGlow = new Mesh[Skins.COUNT];
    /** Trail particles. */
    public Mesh heart, bolt, coin, bubble, sparkle;
    /** Sky decorations (see Skies.DECOR). */
    public Mesh puff, moon, lollipop, candyCane, planet, ringedPlanet, rock, rockLava, grid, sun, sunDisc, aurora;

    /** Spacing of the synthwave grid lines (MatchView scrolls the grid by this much). */
    public static final float GRID_STEP = 12f;

    private void buildLooks(Renderer r) {
        MeshBuilder m;
        for (int i = 0; i < Skins.COUNT; i++) {
            m = new MeshBuilder();
            Skins.buildGlow(m, i);
            tileGlow[i] = m.isEmpty() ? null : r.register(m.build());
        }

        // ---- trail particles (all stand up facing +z; Particles turns them to the camera)
        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2).extrude(heartPoly(), -0.22f, 0.22f).pop();
        heart = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2);
        m.extrude(ccw(new float[]{0.05f, -1.1f, 0.6f, -1.1f, 0.15f, -0.1f, -0.4f, -0.1f}), -0.15f, 0.15f);
        m.extrude(ccw(new float[]{-0.4f, -0.1f, 0.15f, -0.1f, 0.62f, 0.1f, 0.0f, 0.1f}), -0.15f, 0.15f);
        m.extrude(ccw(new float[]{0.0f, 0.1f, 0.62f, 0.1f, -0.45f, 1.25f}), -0.15f, 0.15f);
        m.pop();
        bolt = r.register(m.build());

        m = new MeshBuilder();
        m.push().rotateX(PI / 2);
        m.fixed(0xF0A818).cylinder(0, 0, 0, 0.6f, -0.1f, 0.1f, 0.05f, 16);
        m.fixed(0xFFD84A).cylinder(0, 0, 0, 0.42f, -0.13f, 0.13f, 0.03f, 16);
        m.fixed(0xFFF3B0).box(0, 0, 0, 0.12f, 0.3f, 0.42f, 0.04f, 1);
        m.pop();
        coin = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).sphere(0, 0, 0, 1f, 12);
        m.fixed(0xFFFFFF).ellipsoid(-0.38f, 0.42f, 0.66f, 0.26f, 0.2f, 0.12f, 8);
        m.fixed(0xFFFFFF).sphere(-0.62f, 0.12f, 0.6f, 0.09f, 6);
        bubble = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).push().rotateX(PI / 2).extrude(starPoly2(4, 1f, 0.26f), -0.12f, 0.12f).pop();
        sparkle = r.register(m.build());

        // ---- sky decorations
        m = new MeshBuilder();
        m.paint(1f);
        m.sphere(0, 0, 0, 3.2f, 12);
        m.sphere(3.4f, -0.6f, 0.5f, 2.4f, 10);
        m.sphere(-3.2f, -0.8f, -0.3f, 2.2f, 10);
        m.sphere(1.2f, 1.6f, -0.8f, 2.2f, 10);
        m.sphere(-1.5f, 1.0f, 1.2f, 1.8f, 10);
        m.paint(0.92f).ellipsoid(0, -1.4f, 0, 4.6f, 1.2f, 2.6f, 12);
        puff = r.register(m.build());

        m = new MeshBuilder();
        m.fixed(0xFFF4C8).sphere(0, 0, 0, 1f, 20);
        m.fixed(0xE8D49A);
        m.ellipsoid(0.3f, 0.35f, 0.86f, 0.24f, 0.24f, 0.08f, 8);
        m.ellipsoid(-0.4f, -0.2f, 0.86f, 0.18f, 0.18f, 0.08f, 8);
        m.ellipsoid(0.1f, -0.55f, 0.8f, 0.14f, 0.14f, 0.08f, 8);
        m.ellipsoid(-0.86f, 0.3f, 0.3f, 0.08f, 0.2f, 0.2f, 8);
        moon = r.register(m.build());

        m = new MeshBuilder();
        buildLollipop(m);
        lollipop = r.register(m.build());

        m = new MeshBuilder();
        buildCandyCane(m);
        candyCane = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).sphere(0, 0, 0, 1f, 20);
        m.paint(0.82f).cylinder(0, 0, 0, 1.012f, 0.18f, 0.36f, 0f, 20);
        m.paint(0.88f).cylinder(0, 0, 0, 1.012f, -0.42f, -0.28f, 0f, 20);
        m.accent(1f).sphere(0.45f, 0.35f, 0.75f, 0.16f, 8);
        planet = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).sphere(0, 0, 0, 1f, 20);
        m.paint(0.85f).cylinder(0, 0, 0, 1.012f, -0.12f, 0.08f, 0f, 20);
        m.push().rotateX(0.38f).rotateZ(0.2f).scale(1f, 0.12f, 1f);
        m.accent(1f).torus(0, 0, 0, 1.75f, 0.32f, 32, 6);
        m.accent(0.8f).torus(0, 0, 0, 2.2f, 0.1f, 32, 4);
        m.pop();
        ringedPlanet = r.register(m.build());

        m = new MeshBuilder();
        m.twoTone(0xFFFFFF, 0xC8C0C8).slot(MeshBuilder.PRIMARY);
        m.box(0, 0, 0, 2.2f, 1.6f, 1.9f, 0.5f, 1);
        m.push().rotateY(0.6f).rotateZ(0.3f);
        m.box(0.8f, 0.4f, 0.3f, 1.4f, 1.3f, 1.3f, 0.4f, 1);
        m.pop();
        m.push().rotateX(0.5f);
        m.box(-0.7f, -0.5f, -0.2f, 1.3f, 1.1f, 1.5f, 0.4f, 1);
        m.pop();
        rock = r.register(m.build());

        // glowing lava cracks and hot spots on the rock (drawn over it with a flash)
        m = new MeshBuilder();
        m.fixed(0xFF7A1A);
        m.box(0.2f, 0.81f, 0.1f, 1.2f, 0.06f, 0.2f, 0f);
        m.box(0.2f, 0.81f, 0.1f, 0.2f, 0.06f, 0.9f, 0f);
        m.box(-1.11f, 0.1f, 0.2f, 0.06f, 0.18f, 1.0f, 0f);
        m.fixed(0xFFB03B);
        m.box(-0.3f, -0.1f, 0.96f, 0.18f, 1.0f, 0.06f, 0f);
        m.box(0.3f, 0.2f, 0.96f, 0.7f, 0.16f, 0.06f, 0f);
        m.box(1.11f, -0.2f, -0.3f, 0.06f, 0.2f, 0.9f, 0f);
        m.fixed(0xFFD84A).ellipsoid(0.1f, -0.79f, 0.1f, 0.6f, 0.06f, 0.5f, 6);
        rockLava = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f);
        int lines = 34;
        float ext = lines / 2 * GRID_STEP;
        for (int i = -lines / 2; i <= lines / 2; i++) {
            m.box(i * GRID_STEP, 0, 0, 0.5f, 0.12f, ext * 2, 0f);
            m.box(0, 0, i * GRID_STEP, ext * 2, 0.12f, 0.5f, 0f);
        }
        grid = r.register(m.build());

        m = new MeshBuilder();
        buildSun(m);
        sun = r.register(m.build());

        // setting sun: a glowing disc with a soft halo (faces +z)
        m = new MeshBuilder();
        m.fixed(0xFF8A5A);
        flat(m, ngon(36, 1.3f, 0f), 0f);
        m.fixed(0xFFB45A);
        flat(m, ngon(36, 1.12f, 0f), 0.04f);
        m.fixed(0xFFE27A);
        flat(m, ngon(36, 1f, 0f), 0.08f);
        sunDisc = r.register(m.build());

        m = new MeshBuilder();
        buildAurora(m);
        aurora = r.register(m.build());
    }

    private static float[] heartPoly() {
        int n = 28;
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double t = -i * Math.PI * 2 / n;
            double s = Math.sin(t);
            double x = 16 * s * s * s;
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            p[i * 2] = (float) (x / 16.0);
            p[i * 2 + 1] = (float) (-(y + 2.5) / 16.0);
        }
        return ccw(p);
    }

    /** Returns the polygon in the winding extrude() expects (the same as ngon). */
    private static float[] ccw(float[] p) {
        int n = p.length / 2;
        float area = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
        }
        if (area >= 0) return p;
        float[] q = new float[p.length];
        for (int i = 0; i < n; i++) {
            q[i * 2] = p[(n - 1 - i) * 2];
            q[i * 2 + 1] = p[(n - 1 - i) * 2 + 1];
        }
        return q;
    }

    /** Lollipop: white stick, PRIMARY disc with a white spiral on both faces (disc faces +z). */
    private static void buildLollipop(MeshBuilder m) {
        m.fixed(0xFFFFFF).cylinder(0, -4.2f, 0, 0.16f, 0, 3.4f, 0.05f, 8);
        m.push().rotateX(PI / 2);
        m.paint(1f).cylinder(0, 0, 0, 1.6f, -0.3f, 0.3f, 0.14f, 24);
        m.pop();
        m.fixed(0xFFFFFF);
        for (int side = -1; side <= 1; side += 2) {
            int steps = 60;
            int base = -1;
            for (int i = 0; i <= steps; i++) {
                double a = i / (double) steps * Math.PI * 2 * 2.6;
                float rr = 0.12f + (float) (a / (Math.PI * 2 * 2.6)) * 1.36f;
                float w = 0.08f + rr * 0.1f;
                float c = (float) Math.cos(a), s = (float) Math.sin(a) * side;
                float z = side * 0.31f;
                int v0 = m.vertex(c * (rr - w), s * (rr - w), z, 0, 0, side);
                m.vertex(c * (rr + w), s * (rr + w), z, 0, 0, side);
                if (base >= 0) m.quad(base, base + 1, v0 + 1, v0);
                base = v0;
            }
        }
    }

    /** Candy cane: a red and white banded tube with a hook at the top. */
    private static void buildCandyCane(MeshBuilder m) {
        // path: straight up, then a half circle hook
        int n = 30;
        float[] px = new float[n], py = new float[n];
        for (int i = 0; i < n; i++) {
            if (i < 14) {
                px[i] = 0;
                py[i] = -3.2f + i * (3.2f / 13f);
            } else {
                double a = (i - 13) / (double) (n - 14) * Math.PI;
                px[i] = 0.9f - (float) Math.cos(a) * 0.9f;
                py[i] = (float) Math.sin(a) * 0.9f;
            }
        }
        float rad = 0.3f;
        int ring = 10;
        for (int i = 0; i < n - 1; i++) {
            m.fixed((i / 2) % 2 == 0 ? 0xFF3050 : 0xFFFFFF);
            int base = -1;
            for (int e = 0; e < 2; e++) {
                int k = i + e;
                int a0 = Math.max(0, k - 1), a1 = Math.min(n - 1, k + 1);
                float tx = px[a1] - px[a0], ty = py[a1] - py[a0];
                float tl = (float) Math.sqrt(tx * tx + ty * ty);
                float nx = -ty / tl, ny = tx / tl;
                int first = -1;
                for (int j = 0; j <= ring; j++) {
                    double ang = j * Math.PI * 2 / ring;
                    float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
                    float ox = nx * c, oy = ny * c, oz = s;
                    int v = m.vertex(px[k] + ox * rad, py[k] + oy * rad, oz * rad, ox, oy, oz);
                    if (first < 0) first = v;
                }
                if (base >= 0) {
                    for (int j = 0; j < ring; j++) m.quad(base + j, base + j + 1, first + j + 1, first + j);
                }
                base = first;
            }
        }
        m.fixed(0xFFFFFF).sphere(0, -3.2f, 0, rad, 8);
        m.fixed(0xFF3050).sphere(1.8f, 0, 0, rad, 8);
    }

    /** Synthwave sun: a disc with stripes cut out of its lower half, yellow on top fading to pink (faces +z). */
    private static void buildSun(MeshBuilder m) {
        float[][] bands = {
                {0.998f, 0.12f}, {0.06f, -0.1f}, {-0.16f, -0.3f}, {-0.37f, -0.48f}, {-0.56f, -0.65f}, {-0.74f, -0.8f}, {-0.88f, -0.92f}};
        for (int b = 0; b < bands.length; b++) {
            float y0 = bands[b][0], y1 = bands[b][1];
            float t = (1f - (y0 + y1) / 2f) / 2f;
            m.fixed(Palette.mix(0xFFE45A, 0xFF2E8C, Math.min(1f, t * 1.15f)));
            int steps = b == 0 ? 14 : 3;
            float[] p = new float[(steps + 1) * 4];
            int k = 0;
            for (int i = 0; i <= steps; i++) {
                float y = y0 + (y1 - y0) * i / steps;
                p[k++] = (float) Math.sqrt(Math.max(0f, 1f - y * y));
                p[k++] = y;
            }
            for (int i = steps; i >= 0; i--) {
                float y = y0 + (y1 - y0) * i / steps;
                p[k++] = -(float) Math.sqrt(Math.max(0f, 1f - y * y));
                p[k++] = y;
            }
            flat(m, ccw(p), 0f);
        }
    }

    /**
     * A flat convex polygon (x, y pairs) facing +z for things hanging in the sky. Its normals lean up instead of
     * facing out, so it stays evenly lit whichever way the camera looks at it.
     */
    private static void flat(MeshBuilder m, float[] p, float z) {
        int n = p.length / 2;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            cx += p[i * 2];
            cy += p[i * 2 + 1];
        }
        int c = m.vertex(cx / n, cy / n, z, 0, 0.9f, 0.44f);
        for (int i = 0; i < n; i++) m.vertex(p[i * 2], p[i * 2 + 1], z, 0, 0.9f, 0.44f);
        for (int i = 0; i < n; i++) m.tri(c, c + 1 + i, c + 1 + (i + 1) % n);
    }

    /** Aurora: a wavy glowing curtain, green at the bottom fading to violet (double sided). */
    private static void buildAurora(MeshBuilder m) {
        int cols = 36;
        int[] rowColor = {0x3CFFA8, 0x38E8D8, 0x8A6BFF};
        float[] rowY = {0f, 0.45f, 1f};
        for (int side = -1; side <= 1; side += 2) {
            int base = -1;
            for (int i = 0; i <= cols; i++) {
                float u = i / (float) cols;
                float x = (u - 0.5f) * 2f;
                float z = (float) Math.sin(u * Math.PI * 3) * 0.12f;
                float dz = (float) Math.cos(u * Math.PI * 3) * 0.12f * (float) Math.PI * 3 / 2f;
                float l = (float) Math.sqrt(1 + dz * dz);
                float nx = -dz / l * side, nz = 1f / l * side;
                int first = -1;
                for (int k = 0; k < 3; k++) {
                    m.fixed(rowColor[k]);
                    int v = m.vertex(x, rowY[k] - (float) Math.sin(u * Math.PI * 2) * 0.08f, z, nx, 0.35f, nz);
                    if (first < 0) first = v;
                }
                if (base >= 0) {
                    for (int k = 0; k < 2; k++) {
                        if (side > 0) m.quad(base + k, first + k, first + k + 1, base + k + 1);
                        else m.quad(base + k, base + k + 1, first + k + 1, first + k);
                    }
                }
                base = first;
            }
        }
    }

    // ================================================================== power-ups

    /** Height of the highest point of each car body (some carry tall things on the roof). */
    public final float[] carTop = new float[Cars.ALL.length];

    private static float highest(Mesh body) {
        float top = 0f;
        float[] v = body.vertices;
        for (int k = 0; k < v.length; k += Mesh.VERTEX_FLOATS) top = Math.max(top, v[k + 1]);
        return top;
    }

    /**
     * Power-up icons by PowerUps type, about one unit across around the origin, in fixed colors: they float in the
     * pickups, above the cars holding one, and fly as the snowball and the ice block.
     */
    public final Mesh[] powerIcon = new Mesh[PowerUps.COUNT];
    /** Pickup: a ring standing up around the icon (PRIMARY tints). */
    public Mesh pickupFrame;
    /** Ice crystal along +y, unit height (PRIMARY tints): the shell of a frozen car. */
    public Mesh shard;
    /** Lumpy goo around a tire in wheel space (like the wheels: axle along x; PRIMARY tints): sticky wheels. */
    public Mesh goo;
    /** Thin flat ring of radius 1 (PRIMARY tints): shockwaves. */
    public Mesh wave;

    private void buildPowerUps(Renderer r) {
        MeshBuilder m;
        for (int i = 0; i < PowerUps.COUNT; i++) {
            m = new MeshBuilder();
            buildPowerIcon(m, i);
            powerIcon[i] = r.register(m.build());
        }

        m = new MeshBuilder();
        m.push().rotateX(PI / 2);
        m.paint(1f).torus(0, 0, 0, 0.95f, 0.12f, 32, 8);
        m.pop();
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            m.paint(1f).sphere((float) Math.cos(a) * 0.95f, (float) Math.sin(a) * 0.95f, 0, 0.17f, 8);
        }
        pickupFrame = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).lathe(new float[]{0, 0, 0.19f, 0, 0.19f, 0, 0.24f, 0.55f, 0.24f, 0.55f, 0, 1f}, 6);
        shard = r.register(m.build());

        m = new MeshBuilder();
        m.rotateZ(PI / 2); // lathe axis y -> x, like the wheels
        m.paint(1f).torus(0, 0, 0, 1.0f, 0.15f, 20, 6);
        for (int k = 0; k < 9; k++) {
            double a = k * Math.PI * 2 / 9;
            float side = k % 2 == 0 ? 0.42f : -0.42f;
            m.paint(k % 3 == 0 ? 1.15f : 1f).ellipsoid((float) Math.cos(a) * 1.02f, side, (float) Math.sin(a) * 1.02f, 0.2f, 0.16f, 0.2f, 8);
        }
        goo = r.register(m.build());

        m = new MeshBuilder();
        m.paint(1f).torus(0, 0, 0, 1f, 0.045f, 48, 6);
        wave = r.register(m.build());
    }

    private static void buildPowerIcon(MeshBuilder m, int type) {
        switch (type) {
            case PowerUps.SNOWBALL: // a lumpy snowball
                m.fixed(0xF4FAFF).sphere(0, 0, 0, 0.48f, 14);
                m.fixed(0xFFFFFF).sphere(0.2f, 0.22f, 0.18f, 0.2f, 8);
                m.fixed(0xE2F0FF).sphere(-0.25f, -0.1f, 0.22f, 0.18f, 8);
                m.fixed(0xFFFFFF).sphere(0.06f, -0.27f, -0.24f, 0.17f, 8);
                m.fixed(0xD6EAFF).sphere(-0.16f, 0.28f, -0.2f, 0.16f, 8);
                m.fixed(0xEAF4FF).sphere(0.3f, -0.12f, -0.1f, 0.15f, 8);
                break;
            case PowerUps.ICE: // an ice cube with glints
                m.push().rotateY(0.6f).rotateX(0.35f);
                m.slot(MeshBuilder.FIXED).twoTone(0xDDF8FF, 0x74D4FF).box(0, 0, 0, 0.78f, 0.78f, 0.78f, 0.14f, 2);
                m.fixed(0xFFFFFF).box(-0.18f, 0.1f, 0.395f, 0.09f, 0.38f, 0.02f, 0.02f, 1);
                m.fixed(0xFFFFFF).box(0.0f, 0.18f, 0.395f, 0.06f, 0.2f, 0.02f, 0.02f, 1);
                m.fixed(0xFFFFFF).box(0.395f, 0.1f, 0.15f, 0.02f, 0.38f, 0.09f, 0.02f, 1);
                m.pop();
                break;
            case PowerUps.JUMP: // a spring under a big up arrow
                m.fixed(0x3F8A1E).cylinder(0, -0.6f, 0, 0.34f, 0, 0.09f, 0.03f, 16);
                for (int i = 0; i < 3; i++) m.fixed(0xD0D6E4).torus(0, -0.44f + i * 0.13f, 0, 0.24f, 0.05f, 16, 6);
                m.fixed(0x5CC82A).cylinder(0, -0.14f, 0, 0.12f, 0, 0.32f, 0.03f, 12);
                m.fixed(0x7BEA3C).cone(0, 0.16f, 0, 0.34f, 0f, 0.44f, 16);
                m.fixed(0xC8FF9A).cone(0, 0.16f, 0, 0.345f, 0.3f, 0.05f, 16);
                break;
            case PowerUps.STICKY: // a drop of goo on a little puddle
                m.fixed(0xB43CE6).ellipsoid(0, -0.46f, 0, 0.44f, 0.08f, 0.44f, 14);
                m.fixed(0xD05CFF).sphere(0, -0.12f, 0, 0.38f, 14);
                m.fixed(0xD05CFF).cone(0, 0.1f, 0, 0.31f, 0f, 0.5f, 14);
                m.fixed(0xF6D2FF).ellipsoid(-0.15f, 0.02f, 0.29f, 0.08f, 0.14f, 0.06f, 8);
                m.fixed(0xF6D2FF).sphere(-0.2f, -0.2f, 0.27f, 0.05f, 6);
                break;
            default: // super bump: a spiky burst
                m.push().rotateX(PI / 2);
                m.fixed(0xFF7A1F).extrude(starPoly2(8, 0.62f, 0.36f), -0.12f, 0.12f);
                m.fixed(0xFFD23B).extrude(starPoly2(8, 0.4f, 0.24f), -0.17f, 0.17f);
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.12f, -0.2f, 0.2f, 0.04f, 12);
                m.pop();
                break;
        }
    }

    // ================================================================== drivers and their wardrobe
    // Built in driver space (see Driver for the skeleton): limbs hang down (-y) from their joint, the head and all
    // that goes on it are built around the neck. PRIMARY tints the skin (head, hands), an item's own color (hats,
    // hair, shoes) or the pants (the hips of a torso); SECONDARY the shirt, sleeves, pants legs or bare feet.
    // Empty items (no hat, bald, empty hands) have no mesh (null).

    /** Head (skin), upper arm (sleeve), forearm with the hand, a far-away driver in one piece, the steering wheel. */
    public Mesh drvHead, drvUpperArm, drvForearm, drvBust, steeringWheel;
    /** Pants legs by Outfits.PANTS_STYLE: thighs and shins. */
    public final Mesh[] drvThigh = new Mesh[3], drvShin = new Mesh[3];
    public final Mesh[] drvTorso = new Mesh[Outfits.count(Outfits.SHIRT)];
    public final Mesh[] drvFace = new Mesh[Outfits.count(Outfits.FACE)];
    /** Hair, and what shows of it under a hat. */
    public final Mesh[] drvHair = new Mesh[Outfits.count(Outfits.HAIR)], drvHairCapped = new Mesh[Outfits.count(Outfits.HAIR)];
    public final Mesh[] drvHat = new Mesh[Outfits.count(Outfits.HAT)];
    /** Shoes around the ankle, toes to +z. */
    public final Mesh[] drvShoe = new Mesh[Outfits.count(Outfits.SHOES)];
    /** Things to hold, sticking up out of the fist (+y) with their front to +z. */
    public final Mesh[] drvHeld = new Mesh[Outfits.count(Outfits.HELD)];

    private static final float HY = Driver.HEAD_Y;
    private static final int INK = 0x1A1028;

    private void buildDrivers(Renderer r) {
        MeshBuilder m = new MeshBuilder();
        // a big round head with ears and a neck
        m.paint(1f).ellipsoid(0, HY, 0, 0.37f, 0.35f, 0.35f, 20);
        for (int s = -1; s <= 1; s += 2) m.paint(0.93f).ellipsoid(s * 0.355f, HY - 0.02f, -0.01f, 0.06f, 0.1f, 0.075f, 8);
        m.paint(0.9f).cylinder(0, -0.06f, 0, 0.11f, 0, 0.12f, 0.03f, 10);
        drvHead = r.register(m.build());

        m = new MeshBuilder();
        m.accent(1f).sphere(0, 0, 0, 0.095f, 10);
        m.accent(1f).cylinder(0, 0, 0, 0.078f, -Driver.UPPER_ARM, 0, 0.03f, 10);
        drvUpperArm = r.register(m.build());

        m = new MeshBuilder();
        m.accent(1f).sphere(0, 0, 0, 0.075f, 8);
        m.accent(1f).cylinder(0, 0, 0, 0.071f, -Driver.FOREARM + 0.05f, 0, 0.02f, 10);
        m.paint(1f).ellipsoid(0, -Driver.FOREARM, 0.005f, Driver.HAND_R, Driver.HAND_R * 1.05f, Driver.HAND_R * 0.9f, 10);
        m.paint(0.95f).ellipsoid(0.055f, -Driver.FOREARM + 0.02f, 0.045f, 0.032f, 0.045f, 0.032f, 6);
        drvForearm = r.register(m.build());

        for (int k = 0; k < 3; k++) {
            m = new MeshBuilder();
            m.accent(1f).sphere(0, 0, 0, 0.1f, 10);
            m.accent(1f).cylinder(0, 0, 0, 0.092f, -Driver.THIGH, 0, 0.03f, 10);
            legPattern(m, k, -Driver.THIGH, 0.094f);
            drvThigh[k] = r.register(m.build());
            m = new MeshBuilder();
            m.accent(1f).sphere(0, 0, 0, 0.088f, 10);
            m.accent(1f).cylinder(0, 0, 0, 0.082f, -Driver.SHIN, 0, 0.02f, 10);
            legPattern(m, k, -Driver.SHIN, 0.084f);
            drvShin[k] = r.register(m.build());
        }

        for (int i = 0; i < drvTorso.length; i++) {
            m = new MeshBuilder();
            buildTorso(m, i);
            drvTorso[i] = r.register(m.build());
        }
        for (int i = 0; i < drvFace.length; i++) {
            m = new MeshBuilder();
            buildFace(m, i);
            drvFace[i] = r.register(m.build());
        }
        for (int i = 0; i < drvHair.length; i++) {
            m = new MeshBuilder();
            buildHair(m, i, false);
            drvHair[i] = m.isEmpty() ? null : r.register(m.build());
            m = new MeshBuilder();
            buildHair(m, i, true);
            drvHairCapped[i] = m.isEmpty() ? null : r.register(m.build());
        }
        for (int i = 0; i < drvHat.length; i++) {
            m = new MeshBuilder();
            buildHat(m, i);
            drvHat[i] = m.isEmpty() ? null : r.register(m.build());
        }
        for (int i = 0; i < drvShoe.length; i++) {
            m = new MeshBuilder();
            buildShoe(m, i);
            drvShoe[i] = r.register(m.build());
        }
        for (int i = 0; i < drvHeld.length; i++) {
            m = new MeshBuilder();
            buildHeld(m, i);
            drvHeld[i] = m.isEmpty() ? null : r.register(m.build());
        }

        // the stand-in for drivers far away: head, body and arms reaching for the wheel in one piece
        m = new MeshBuilder();
        m.paint(1f).ellipsoid(0, Driver.NECK_Y + HY, 0, 0.37f, 0.35f, 0.35f, 10);
        m.accent(1f).box(0, 0.22f, 0, 0.56f, 0.6f, 0.36f, 0.16f, 1);
        for (int s = -1; s <= 1; s += 2) {
            m.accent(1f).box(s * 0.22f, 0.42f, 0.17f, 0.14f, 0.14f, 0.36f, 0.06f, 1);
            m.paint(1f).sphere(s * 0.19f, 0.43f, 0.37f, 0.085f, 6);
        }
        drvBust = r.register(m.build());

        // steering wheel lying in its xz plane with the column going down
        m = new MeshBuilder();
        m.fixed(0x2A2433).torus(0, 0, 0, Driver.WHEEL_R, 0.032f, 18, 6);
        for (int k = 0; k < 3; k++) {
            m.push().rotateY(PI / 2 + k * PI * 2 / 3);
            m.fixed(0x6A6478).box(Driver.WHEEL_R / 2, 0, 0, Driver.WHEEL_R, 0.025f, 0.04f, 0.012f, 1);
            m.pop();
        }
        m.fixed(0xFF3B4F).cylinder(0, 0, 0, 0.055f, -0.02f, 0.03f, 0.015f, 12);
        m.fixed(0x3A3550).cylinder(0, 0, 0, 0.03f, -0.5f, 0f, 0f, 8);
        steeringWheel = r.register(m.build());
    }

    /** Side stripes (style 1) or checkers (style 2) on a pants leg running down from y = 0 to y1 (radius r). */
    private static void legPattern(MeshBuilder m, int style, float y1, float r) {
        if (style == 1) {
            for (int s = -1; s <= 1; s += 2) m.fixed(0xFFFFFF).box(s * r, y1 / 2, 0, 0.02f, -y1 - 0.02f, 0.035f, 0.008f, 1);
        } else if (style == 2) {
            int rows = 3, cols = 8;
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    if ((i + j) % 2 != 0) continue;
                    m.push().rotateY(i * PI * 2 / cols).translate(0, y1 * (j + 0.5f) / rows, r);
                    m.fixed(INK).box(0, 0, 0, r * 0.75f, -y1 / rows, 0.012f, 0.004f, 1);
                    m.pop();
                }
            }
        }
    }

    /**
     * Band of an ellipsoid between latitudes a0 and a1 (PI / 2 = the top; a1 = PI / 2 closes it), tipped back by
     * tilt so its front edge rises and its back edge drops: scalps, hair and hats.
     */
    private static void dome(MeshBuilder m, float cy, float cz, float rx, float ry, float rz, float a0, float a1, float tilt, int segs) {
        int n = 8;
        float[] p = new float[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            p[i * 2] = (float) Math.cos(a);
            p[i * 2 + 1] = (float) Math.sin(a);
        }
        if (a1 >= PI / 2 - 1e-3f) p[n * 2] = 0f;
        m.push().translate(0, cy, cz).rotateX(-tilt).scale(rx, ry, rz);
        m.lathe(p, segs);
        m.pop();
    }

    /** A cone of length len from (x, y, z) pointing along (yaw, pitch above the horizon). */
    private static void spike(MeshBuilder m, float x, float y, float z, float yaw, float pitch, float r0, float len, int segs) {
        m.push().translate(x, y, z).rotateY(yaw).rotateX(PI / 2 - pitch);
        m.cone(0, 0, 0, r0, 0.004f, len, segs);
        m.pop();
    }

    /** Point on a sphere of radius rr around (0, cy, cz) at (yaw, pitch): written into P. */
    private static void onBall(float cy, float cz, float rr, float yaw, float pitch) {
        float c = (float) Math.cos(pitch);
        P[0] = c * (float) Math.sin(yaw) * rr;
        P[1] = cy + (float) Math.sin(pitch) * rr;
        P[2] = cz + c * (float) Math.cos(yaw) * rr;
    }

    private static final float[] P = new float[3];

    /** z of the front of the face at (x, y) (head space). */
    private static float faceZ(float x, float y) {
        float dx = x / 0.37f, dy = (y - HY) / 0.35f;
        return 0.35f * (float) Math.sqrt(Math.max(0f, 1f - dx * dx - dy * dy));
    }

    // ------------------------------------------------------------------ torsos (hips at the origin)

    /** Torso outline: radius factor at height y (the body is an oval barrel 0.29 wide and 0.19 deep). */
    private static float torsoR(float y) {
        float[] t = TORSO_PROFILE;
        if (y <= t[1]) return t[0];
        for (int i = 2; i < t.length; i += 2) {
            if (y <= t[i + 1]) {
                float k = (y - t[i - 1]) / (t[i + 1] - t[i - 1]);
                return t[i - 2] + (t[i] - t[i - 2]) * k;
            }
        }
        return 0f;
    }

    private static final float[] TORSO_PROFILE = {0.8f, -0.17f, 0.93f, -0.12f, 1f, 0.02f, 1f, 0.3f, 0.93f, 0.42f, 0.72f, 0.5f, 0.38f, 0.545f, 0f, 0.56f};

    /** The torso surface between heights y0 and y1, grown by g (bands of color over it), closed at the very ends. */
    private static void torsoBand(MeshBuilder m, float y0, float y1, float g) {
        int n = Math.max(2, (int) ((y1 - y0) / 0.04f));
        float[] p = new float[(n + 1) * 2 + 4];
        int k = 0;
        if (y0 <= -0.17f) {
            p[k++] = 0f;
            p[k++] = -0.17f;
        }
        for (int i = 0; i <= n; i++) {
            float y = y0 + (y1 - y0) * i / n;
            p[k++] = torsoR(y);
            p[k++] = y;
        }
        if (y1 >= 0.56f) {
            p[k - 2] = 0f;
        }
        float[] q = new float[k];
        System.arraycopy(p, 0, q, 0, k);
        m.push().scale(0.29f + g, 1f, 0.19f + g);
        m.lathe(q, 18);
        m.pop();
    }

    /** A point on the torso surface at height y, angle a around it (0 = front, PI / 2 = its left): written into P. */
    private static void onTorso(float y, float a, float g) {
        float rr = torsoR(y);
        P[0] = (float) Math.sin(a) * rr * (0.29f + g);
        P[1] = y;
        P[2] = (float) Math.cos(a) * rr * (0.19f + g);
    }

    /** Something flat stuck onto the torso at (y, a), facing out (built facing +z in the push). */
    private static void onTorsoPush(MeshBuilder m, float y, float a, float g) {
        onTorso(y, a, g);
        m.push().translate(P[0], P[1], P[2]).rotateY(a);
    }

    private static void buildTorso(MeshBuilder m, int shirt) {
        // pants on the hips (PRIMARY), the shirt above (SECONDARY)
        m.paint(1f);
        torsoBand(m, -0.17f, 0.0f, 0f);
        m.accent(1f);
        torsoBand(m, 0.0f, 0.56f, 0f);
        switch (shirt) {
            case Outfits.SH_TEE:
                m.accent(0.82f);
                torsoBand(m, 0.505f, 0.53f, 0.012f);
                break;
            case Outfits.SH_STRIPES:
                m.fixed(0xFFFFFF);
                for (int i = 0; i < 4; i++) torsoBand(m, 0.05f + i * 0.11f, 0.1f + i * 0.11f, 0.006f);
                break;
            case Outfits.SH_TANK:
                m.accent(0.8f);
                torsoBand(m, 0.46f, 0.49f, 0.01f);
                break;
            case Outfits.SH_HOODIE:
                // hood lying on the back, a pouch pocket and drawstrings
                m.accent(0.88f).ellipsoid(0, 0.5f, -0.15f, 0.21f, 0.11f, 0.11f, 12);
                m.accent(0.82f).box(0, 0.12f, 0.175f, 0.3f, 0.13f, 0.04f, 0.03f, 1);
                for (int s = -1; s <= 1; s += 2) m.fixed(0xFFFFFF).cylinder(s * 0.06f, 0.33f, 0.19f, 0.012f, 0f, 0.15f, 0.005f, 5);
                break;
            case Outfits.SH_SWEATER:
                // a zigzag band, front and back
                for (int side = 0; side < 2; side++) {
                    for (int i = 0; i < 6; i++) {
                        float a = (side == 0 ? 0f : PI) + (i - 2.5f) * 0.32f;
                        onTorsoPush(m, 0.26f, a, 0.004f);
                        m.rotateZ(i % 2 == 0 ? 0.75f : -0.75f);
                        m.fixed(i % 2 == 0 ? 0xFFFFFF : 0xFFE14D).box(0, 0, 0, 0.11f, 0.035f, 0.02f, 0.008f, 1);
                        m.pop();
                    }
                }
                m.accent(0.8f);
                torsoBand(m, 0.5f, 0.535f, 0.012f);
                break;
            case Outfits.SH_HAWAII:
                for (int i = 0; i < 10; i++) {
                    float a = i * 2.39f, y = 0.06f + (i * 0.137f) % 0.38f;
                    onTorsoPush(m, y, a, 0f);
                    int col = i % 3 == 0 ? 0xFFE14D : (i % 3 == 1 ? 0xFF6FB5 : 0xFFFFFF);
                    for (int p = 0; p < 4; p++) m.fixed(col).ellipsoid((float) Math.cos(p * PI / 2) * 0.025f, (float) Math.sin(p * PI / 2) * 0.025f, 0, 0.024f, 0.024f, 0.012f, 6);
                    m.fixed(0xFF9A2B).sphere(0, 0, 0.005f, 0.015f, 5);
                    m.pop();
                }
                // open collar
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.07f, 0.49f, 0.11f).rotateZ(s * 0.5f).rotateX(-0.6f);
                    m.fixed(0xFFFFFF).box(0, 0, 0, 0.1f, 0.03f, 0.08f, 0.012f, 1);
                    m.pop();
                }
                break;
            case Outfits.SH_OVERALLS:
                // a white shirt above the bib, straps over the shoulders, yellow buttons
                m.fixed(0xF4F4FF);
                torsoBand(m, 0.3f, 0.56f, 0.004f);
                m.accent(1f).box(0, 0.33f, 0.168f, 0.28f, 0.18f, 0.03f, 0.012f, 1);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.12f, 0.46f, 0f);
                    m.accent(1f).box(0, 0, 0, 0.055f, 0.03f, 0.41f, 0.012f, 1);
                    m.pop();
                    m.fixed(0xFFD23B).sphere(s * 0.1f, 0.4f, 0.19f, 0.022f, 6);
                }
                break;
            case Outfits.SH_JERSEY:
                // a big number 1 on the front and the back, trim at the neck
                for (int side = -1; side <= 1; side += 2) {
                    float z = side * 0.19f;
                    m.fixed(INK).box(0.01f, 0.24f, z, 0.055f, 0.24f, 0.02f, 0.01f, 1);
                    m.push().translate(-0.025f * side, 0.33f, z).rotateZ(-0.7f * side);
                    m.fixed(INK).box(0, 0, 0, 0.05f, 0.09f, 0.02f, 0.01f, 1);
                    m.pop();
                }
                m.fixed(0xFFFFFF);
                torsoBand(m, 0.5f, 0.535f, 0.012f);
                m.fixed(INK);
                torsoBand(m, 0.0f, 0.03f, 0.008f);
                break;
            case Outfits.SH_RACING:
                // red racing stripes down the sides, sponsor patches, a zipper
                for (int s = -1; s <= 1; s += 2) {
                    onTorsoPush(m, 0.2f, s * PI / 2, 0.003f);
                    m.fixed(0xFF3B4F).box(0, 0, 0, 0.07f, 0.42f, 0.015f, 0.006f, 1);
                    m.pop();
                }
                m.fixed(INK).box(0, 0.25f, 0.188f, 0.02f, 0.46f, 0.015f, 0.005f, 1);
                m.fixed(0x3BB8FF).box(0.11f, 0.33f, 0.18f, 0.08f, 0.05f, 0.02f, 0.01f, 1);
                m.fixed(0xFFD23B).box(-0.11f, 0.3f, 0.18f, 0.08f, 0.07f, 0.02f, 0.01f, 1);
                m.fixed(0xFF3B4F).box(0, 0.25f, -0.188f, 0.24f, 0.08f, 0.02f, 0.01f, 1);
                break;
            case Outfits.SH_HERO: {
                // a red cape down the back, a gold star on the chest, a gold belt
                m.push().translate(0, 0.47f, -0.17f).rotateX(-0.22f);
                m.fixed(0xFF2E4A).box(0, -0.32f, 0, 0.5f, 0.66f, 0.035f, 0.015f, 1);
                m.pop();
                m.fixed(0xFF2E4A);
                torsoBand(m, 0.5f, 0.54f, 0.014f);
                m.push().translate(0, 0.28f, 0.185f).rotateX(PI / 2);
                m.fixed(0xFFD23B).extrude(starPoly(0.11f, 0.05f), -0.01f, 0.015f);
                m.pop();
                m.fixed(0xFFD23B);
                torsoBand(m, -0.01f, 0.04f, 0.008f);
                break;
            }
            case Outfits.SH_TUX:
                // white shirt front, red bow tie, buttons, lapels
                m.fixed(0xFFFFFF).box(0, 0.36f, 0.168f, 0.15f, 0.3f, 0.03f, 0.012f, 1);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.075f, 0.37f, 0.18f).rotateZ(s * 0.35f);
                    m.accent(0.6f).box(0, 0, 0, 0.05f, 0.24f, 0.02f, 0.01f, 1);
                    m.pop();
                    m.push().translate(s * 0.05f, 0.47f, 0.2f).rotateZ(s * 0.25f);
                    m.fixed(0xFF2E4A).ellipsoid(0, 0, 0, 0.055f, 0.035f, 0.025f, 8);
                    m.pop();
                }
                m.fixed(0xFF2E4A).sphere(0, 0.47f, 0.215f, 0.022f, 6);
                for (int i = 0; i < 2; i++) m.fixed(0x3A3550).sphere(0, 0.29f - i * 0.08f, 0.188f, 0.014f, 5);
                break;
            case Outfits.SH_DISCO:
                // sequins all over and a huge collar
                for (int i = 0; i < 22; i++) {
                    float a = i * 2.39996f, y = 0.04f + (i * 0.0613f) % 0.42f;
                    onTorso(y, a, 0.004f);
                    m.fixed(i % 3 == 0 ? 0xFFFFFF : (i % 3 == 1 ? 0xFF6FD8 : 0x7FF0FF)).sphere(P[0], P[1], P[2], 0.02f, 5);
                }
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.09f, 0.5f, 0.12f).rotateZ(s * 0.9f).rotateX(-0.5f);
                    m.fixed(0xFFFFFF).box(0, 0, 0, 0.16f, 0.025f, 0.12f, 0.012f, 1);
                    m.pop();
                }
                break;
            default: // space suit: grey collar ring, chest panel with buttons, life support pack on the back
                m.fixed(0xA8B0C0);
                torsoBand(m, 0.49f, 0.545f, 0.02f);
                m.fixed(0xC8CED8).box(0, 0.3f, 0.175f, 0.2f, 0.13f, 0.04f, 0.015f, 1);
                m.fixed(0xFF3B4F).sphere(-0.05f, 0.31f, 0.198f, 0.02f, 5);
                m.fixed(0x5EE65A).sphere(0f, 0.31f, 0.198f, 0.02f, 5);
                m.fixed(0x3BB8FF).sphere(0.05f, 0.31f, 0.198f, 0.02f, 5);
                m.fixed(0xD8DEE8).box(0, 0.24f, -0.24f, 0.34f, 0.36f, 0.13f, 0.05f, 1);
                m.fixed(0x9AA2B4).cylinder(-0.08f, 0.42f, -0.25f, 0.035f, 0f, 0.06f, 0.01f, 8);
                m.fixed(0x9AA2B4).cylinder(0.08f, 0.42f, -0.25f, 0.035f, 0f, 0.06f, 0.01f, 8);
                break;
        }
    }

    // ------------------------------------------------------------------ faces (head space: neck at the origin)

    /** Round eye at (x, y): white, a pupil looking (px, py), a glint. */
    private static void eye(MeshBuilder m, float x, float y, float r, float px, float py) {
        float z = faceZ(x, y);
        m.fixed(0xFFFFFF).ellipsoid(x, y, z - 0.02f, r, r * 1.2f, r * 0.7f, 10);
        m.fixed(INK).sphere(x + px, y + py, z + r * 0.42f, r * 0.55f, 8);
        m.fixed(0xFFFFFF).sphere(x + px + r * 0.18f, y + py + r * 0.22f, z + r * 0.75f, r * 0.17f, 5);
    }

    /** A curve of beads on the face: a smile (curve > 0: the ends rise) or frown, from -w to w. */
    private static void arc(MeshBuilder m, float y, float w, float curve, float thick, int color, int n) {
        m.fixed(color);
        for (int i = 0; i <= n; i++) {
            float t = -1f + 2f * i / n;
            float x = t * w, yy = y + curve * t * t;
            m.sphere(x, yy, faceZ(x, yy) + 0.005f, thick, 6);
        }
    }

    /** A short bar on the face (brows, closed eyes), turned by angle. */
    private static void bar(MeshBuilder m, float x, float y, float w, float h, float angle, int color) {
        m.push().translate(x, y, faceZ(x, y) + 0.01f).rotateZ(angle);
        m.fixed(color).box(0, 0, 0, w, h, 0.03f, Math.min(w, h) * 0.45f, 1);
        m.pop();
    }

    private static void nose(MeshBuilder m) {
        float y = HY - 0.03f;
        m.paint(0.88f).ellipsoid(0, y, faceZ(0, y) - 0.005f, 0.05f, 0.045f, 0.05f, 8);
    }

    private static void blush(MeshBuilder m) {
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 0.2f, y = HY - 0.07f;
            m.fixed(0xFF9EB8).ellipsoid(x, y, faceZ(x, y) - 0.012f, 0.06f, 0.035f, 0.02f, 8);
        }
    }

    private static void buildFace(MeshBuilder m, int face) {
        float ey = HY + 0.07f, ex = 0.13f, er = 0.075f, my = HY - 0.13f;
        switch (face) {
            case Outfits.FC_SMILE:
                eye(m, -ex, ey, er, 0, 0);
                eye(m, ex, ey, er, 0, 0);
                arc(m, my, 0.11f, 0.05f, 0.022f, INK, 8);
                nose(m);
                blush(m);
                break;
            case Outfits.FC_GRIN: {
                // happy squints and a wide toothy grin
                for (int s = -1; s <= 1; s += 2) {
                    bar(m, s * ex - 0.035f, ey, 0.08f, 0.026f, 0.5f, INK);
                    bar(m, s * ex + 0.035f, ey, 0.08f, 0.026f, -0.5f, INK);
                }
                float z = faceZ(0, my);
                m.push().translate(0, my + 0.02f, z - 0.03f).rotateX(-0.25f);
                m.fixed(0x5A1A2A).ellipsoid(0, 0, 0, 0.15f, 0.08f, 0.045f, 12);
                m.fixed(0xFFFFFF).box(0, 0.035f, 0.025f, 0.2f, 0.035f, 0.03f, 0.012f, 1);
                m.fixed(0xFF6F8F).ellipsoid(0, -0.04f, 0.022f, 0.07f, 0.03f, 0.025f, 8);
                m.pop();
                nose(m);
                blush(m);
                break;
            }
            case Outfits.FC_SHADES: {
                // black shades with a shine, and a smirk
                float z = faceZ(0, ey);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.13f, ey, z + 0.01f).rotateY(s * 0.25f);
                    m.fixed(0x14101E).box(0, 0, 0, 0.17f, 0.11f, 0.04f, 0.035f, 1);
                    m.fixed(0x8A9AD0).box(-0.04f, 0.025f, 0.02f, 0.05f, 0.015f, 0.01f, 0.006f, 1);
                    m.pop();
                }
                m.fixed(0x14101E).box(0, ey + 0.02f, z + 0.025f, 0.08f, 0.025f, 0.03f, 0.01f, 1);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.27f, ey + 0.02f, faceZ(0.27f, ey) - 0.02f).rotateY(s * 1.1f);
                    m.fixed(0x14101E).box(0, 0, 0, 0.12f, 0.025f, 0.02f, 0.008f, 1);
                    m.pop();
                }
                arc(m, my, 0.08f, 0.03f, 0.02f, INK, 6);
                bar(m, 0.1f, my + 0.035f, 0.05f, 0.022f, 0.6f, INK);
                nose(m);
                break;
            }
            case Outfits.FC_SILLY: {
                // cross-eyed, tongue out
                eye(m, -ex, ey, er * 1.1f, 0.025f, 0);
                eye(m, ex, ey + 0.015f, er, -0.025f, 0.01f);
                float z = faceZ(0, my);
                m.fixed(0x5A1A2A).ellipsoid(0, my + 0.01f, z - 0.02f, 0.08f, 0.045f, 0.035f, 10);
                m.push().translate(0.02f, my - 0.02f, z + 0.005f).rotateX(0.7f);
                m.fixed(0xFF6F8F).ellipsoid(0, 0, 0.04f, 0.055f, 0.022f, 0.07f, 10);
                m.fixed(0xE04F70).box(0, 0.012f, 0.05f, 0.008f, 0.01f, 0.06f, 0.003f, 1);
                m.pop();
                nose(m);
                blush(m);
                break;
            }
            case Outfits.FC_GRUMPY:
                eye(m, -ex, ey - 0.01f, er, 0, -0.01f);
                eye(m, ex, ey - 0.01f, er, 0, -0.01f);
                bar(m, -ex, ey + 0.085f, 0.13f, 0.04f, -0.45f, INK);
                bar(m, ex, ey + 0.085f, 0.13f, 0.04f, 0.45f, INK);
                arc(m, my - 0.02f, 0.09f, -0.04f, 0.022f, INK, 7);
                nose(m);
                break;
            case Outfits.FC_MUSTACHE: {
                eye(m, -ex, ey, er, 0, 0);
                eye(m, ex, ey, er, 0, 0);
                bar(m, -ex, ey + 0.08f, 0.11f, 0.03f, 0.15f, 0x4A2A1A);
                bar(m, ex, ey + 0.08f, 0.11f, 0.03f, -0.15f, 0x4A2A1A);
                // a big curly handlebar mustache
                float y = HY - 0.09f;
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 0; i < 4; i++) {
                        float x = s * (0.03f + i * 0.045f), yy = y - 0.01f + (i == 3 ? 0.03f : 0f) - (i == 1 ? 0.012f : 0f);
                        m.fixed(0x4A2A1A).ellipsoid(x, yy, faceZ(x, yy) + 0.01f, 0.045f - i * 0.004f, 0.035f, 0.035f, 8);
                    }
                }
                nose(m);
                break;
            }
            case Outfits.FC_SHOCKED: {
                eye(m, -ex, ey + 0.01f, er * 1.25f, 0, 0);
                eye(m, ex, ey + 0.01f, er * 1.25f, 0, 0);
                bar(m, -ex, ey + 0.12f, 0.1f, 0.03f, 0.3f, INK);
                bar(m, ex, ey + 0.12f, 0.1f, 0.03f, -0.3f, INK);
                float z = faceZ(0, my - 0.01f);
                m.fixed(0x5A1A2A).ellipsoid(0, my - 0.01f, z - 0.015f, 0.055f, 0.07f, 0.035f, 10);
                nose(m);
                break;
            }
            case Outfits.FC_WINK:
                eye(m, ex, ey, er, 0, 0);
                arc(m, ey - 0.01f, 0.055f, -0.025f, 0.018f, INK, 5);
                arc(m, my, 0.11f, 0.06f, 0.022f, INK, 8);
                m.fixed(0xFF6F8F).ellipsoid(0.05f, my + 0.005f, faceZ(0.05f, my) + 0.008f, 0.035f, 0.025f, 0.02f, 8);
                nose(m);
                blush(m);
                break;
            case Outfits.FC_NERD: {
                eye(m, -ex, ey, er * 0.9f, 0, 0);
                eye(m, ex, ey, er * 0.9f, 0, 0);
                // round glasses
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * ex, ey, faceZ(s * ex, ey) + 0.03f).rotateX(PI / 2).rotateZ(s * -0.25f);
                    m.fixed(0x3A2A20).torus(0, 0, 0, 0.1f, 0.015f, 18, 5);
                    m.pop();
                }
                m.fixed(0x3A2A20).box(0, ey + 0.01f, faceZ(0, ey) + 0.03f, 0.07f, 0.018f, 0.02f, 0.008f, 1);
                arc(m, my + 0.01f, 0.08f, 0.03f, 0.02f, INK, 6);
                float z = faceZ(0, my - 0.03f);
                m.fixed(0xFFFFFF).box(-0.022f, my - 0.03f, z + 0.01f, 0.04f, 0.05f, 0.02f, 0.008f, 1);
                m.fixed(0xFFFFFF).box(0.022f, my - 0.03f, z + 0.01f, 0.04f, 0.05f, 0.02f, 0.008f, 1);
                nose(m);
                break;
            }
            case Outfits.FC_PATCH: {
                // one eye, a black patch on a strap round the head, a big grin
                eye(m, ex, ey, er, 0, 0);
                float z = faceZ(-ex, ey);
                m.fixed(0x14101E).ellipsoid(-ex, ey, z, 0.09f, 0.09f, 0.03f, 10);
                m.push().translate(0, ey + 0.01f, 0).rotateZ(-0.3f);
                m.fixed(0x14101E).torus(0, 0, 0, 0.365f, 0.015f, 28, 4);
                m.pop();
                bar(m, ex, ey + 0.09f, 0.12f, 0.03f, 0.2f, INK);
                arc(m, my, 0.12f, 0.05f, 0.023f, INK, 8);
                nose(m);
                break;
            }
            case Outfits.FC_CLOWN: {
                eye(m, -ex, ey, er, 0, 0);
                eye(m, ex, ey, er, 0, 0);
                for (int s = -1; s <= 1; s += 2) {
                    float x = s * ex, y = ey - 0.12f;
                    m.fixed(0x3BB8FF).box(x, y, faceZ(x, y), 0.025f, 0.06f, 0.02f, 0.01f, 1);
                }
                arc(m, my - 0.01f, 0.15f, 0.08f, 0.03f, 0xFF3B4F, 10);
                float y = HY - 0.03f;
                m.fixed(0xFF2E3A).sphere(0, y, faceZ(0, y) + 0.04f, 0.085f, 12);
                m.fixed(0xFFFFFF).sphere(-0.025f, y + 0.03f, faceZ(0, y) + 0.11f, 0.02f, 5);
                blush(m);
                break;
            }
            case Outfits.FC_STARS:
                for (int s = -1; s <= 1; s += 2) {
                    float x = s * ex, z = faceZ(x, ey);
                    m.push().translate(x, ey, z).rotateY(s * 0.3f).rotateX(PI / 2);
                    m.fixed(0xFFD23B).extrude(starPoly(0.1f, 0.045f), -0.02f, 0.025f);
                    m.pop();
                }
                arc(m, my, 0.12f, 0.06f, 0.024f, INK, 8);
                nose(m);
                blush(m);
                break;
            default: { // laser eyes: glowing red eyes with a halo, a smug grin
                for (int s = -1; s <= 1; s += 2) {
                    float x = s * ex, z = faceZ(x, ey);
                    m.fixed(0xFF6A6A).ellipsoid(x, ey, z - 0.02f, er * 1.25f, er * 1.1f, er * 0.6f, 10);
                    m.fixed(0xFF1F2E).ellipsoid(x, ey, z + 0.005f, er * 0.95f, er * 0.85f, er * 0.6f, 10);
                    m.fixed(0xFFF0F0).sphere(x, ey, z + 0.04f, er * 0.4f, 6);
                }
                bar(m, -ex, ey + 0.09f, 0.13f, 0.035f, -0.3f, INK);
                bar(m, ex, ey + 0.09f, 0.13f, 0.035f, 0.3f, INK);
                arc(m, my, 0.1f, 0.025f, 0.022f, INK, 7);
                bar(m, 0.1f, my + 0.03f, 0.05f, 0.022f, 0.6f, INK);
                nose(m);
                break;
            }
        }
    }

    // ------------------------------------------------------------------ hair (capped: only what shows under a hat)

    private static void buildHair(MeshBuilder m, int hair, boolean capped) {
        float cy = HY + 0.015f;
        // most styles start from a scalp tipped back off the forehead; under a hat only its lower band shows
        float top = PI / 2;
        switch (hair) {
            case Outfits.HR_BALD:
                break;
            case Outfits.HR_BUZZ:
                m.paint(1f);
                dome(m, cy, -0.01f, 0.383f, 0.37f, 0.37f, -0.05f, capped ? 0.3f : top, 0.5f, 18);
                break;
            case Outfits.HR_MOP:
                m.paint(1f);
                dome(m, cy + 0.01f, -0.02f, 0.41f, 0.4f, 0.41f, -0.4f, capped ? 0.1f : top, 0.32f, 18);
                // bangs over the forehead
                m.paint(0.95f);
                for (int i = 0; i < 5; i++) {
                    float x = -0.2f + i * 0.1f, y = HY + 0.19f - Math.abs(i - 2) * 0.02f;
                    m.ellipsoid(x, y, faceZ(x, y) + 0.02f, 0.085f, 0.07f, 0.06f, 8);
                }
                break;
            case Outfits.HR_SPIKY:
                m.paint(1f);
                dome(m, cy, -0.01f, 0.383f, 0.37f, 0.37f, -0.05f, capped ? 0.3f : top, 0.5f, 18);
                for (int i = 0; i < 11; i++) {
                    float yaw = -2.6f + i * 0.52f, pitch = 0.55f + (i % 3) * 0.3f;
                    if (Math.abs(yaw) < 0.6f) pitch = 1.0f + (i % 2) * 0.2f;
                    if (capped && pitch > 0.6f) continue;
                    onBall(cy, -0.02f, 0.33f, yaw, pitch);
                    spike(m, P[0], P[1], P[2], yaw, pitch + 0.2f, 0.095f, 0.25f, 8);
                }
                if (capped) {
                    for (int s = -1; s <= 1; s += 2) {
                        onBall(cy, -0.02f, 0.33f, PI + s * 0.5f, 0.2f);
                        spike(m, P[0], P[1], P[2], PI + s * 0.5f, 0.1f, 0.08f, 0.2f, 8);
                    }
                }
                break;
            case Outfits.HR_PONY:
                m.paint(1f);
                dome(m, cy, -0.01f, 0.385f, 0.375f, 0.375f, -0.05f, capped ? 0.3f : top, 0.45f, 18);
                m.push().translate(0, HY + 0.12f, -0.36f).rotateX(-1.2f);
                m.fixed(0xFF6FB5).torus(0, 0, 0, 0.055f, 0.022f, 12, 5);
                m.pop();
                m.paint(1f);
                for (int i = 0; i < 4; i++) {
                    m.ellipsoid(0, HY + 0.08f - i * 0.09f, -0.43f - i * 0.03f, 0.085f - i * 0.008f, 0.12f, 0.085f - i * 0.008f, 10);
                }
                break;
            case Outfits.HR_CURLS: {
                m.paint(0.9f);
                dome(m, cy, -0.01f, 0.38f, 0.37f, 0.37f, -0.2f, capped ? 0.2f : top, 0.45f, 16);
                m.paint(1f);
                for (int j = 0; j < 5; j++) {
                    float pitch = -0.25f + j * 0.36f;
                    int n = j == 4 ? 3 : 9 - j;
                    for (int i = 0; i < n; i++) {
                        float yaw = PI + (i - (n - 1) / 2f) * (j == 4 ? 1.2f : 0.62f);
                        onBall(cy, -0.01f, 0.37f, yaw, pitch);
                        if (P[2] > 0.12f && P[1] < HY + 0.24f) continue; // keep the face clear
                        if (capped && P[1] > HY + 0.12f) continue;
                        m.sphere(P[0], P[1], P[2], 0.1f, 7);
                    }
                }
                break;
            }
            case Outfits.HR_AFRO:
            case Outfits.HR_RAINBOW: {
                boolean rainbow = hair == Outfits.HR_RAINBOW;
                if (rainbow) m.fixed(RAINBOW_RINGS[0]);
                else m.paint(1f);
                dome(m, HY + 0.11f, -0.07f, 0.56f, 0.52f, 0.55f, -0.3f, capped ? 0.25f : top, 0.75f, 20);
                // bumpy puffs on top
                for (int j = 0; j < 4; j++) {
                    float pitch = -0.05f + j * 0.42f;
                    int n = j == 3 ? 1 : 7 - j;
                    for (int i = 0; i < n; i++) {
                        float yaw = PI + (i - (n - 1) / 2f) * 0.85f;
                        onBall(HY + 0.11f, -0.07f, 0.5f, yaw, pitch);
                        if (P[2] > 0.25f && P[1] < HY + 0.35f) continue;
                        if (capped && P[1] > HY + 0.2f) continue;
                        if (rainbow) m.fixed(RAINBOW_RINGS[(j + i) % 6]);
                        else m.paint(0.94f);
                        m.sphere(P[0], P[1], P[2], 0.15f, 8);
                    }
                }
                break;
            }
            case Outfits.HR_PIGTAILS:
                m.paint(1f);
                dome(m, cy, -0.01f, 0.385f, 0.375f, 0.375f, -0.1f, capped ? 0.3f : top, 0.4f, 18);
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 0; i < 3; i++) {
                        m.paint(1f).ellipsoid(s * (0.42f + i * 0.05f), HY + 0.12f - i * 0.1f, -0.12f, 0.08f - i * 0.006f, 0.1f, 0.08f, 9);
                    }
                    m.fixed(0xFF4FA3).ellipsoid(s * 0.39f, HY + 0.17f, -0.12f, 0.035f, 0.07f, 0.07f, 8);
                }
                break;
            case Outfits.HR_MOHAWK:
                m.paint(0.55f);
                dome(m, cy, -0.01f, 0.375f, 0.362f, 0.362f, -0.05f, capped ? 0.3f : top, 0.5f, 16);
                m.paint(1f);
                for (int i = 0; i < 7; i++) {
                    float a = 0.65f + i * 0.33f; // over the top from the forehead to the back
                    if (capped && i < 6) continue;
                    float y = cy + (float) Math.sin(a) * 0.33f, z = -0.01f + (float) Math.cos(a) * 0.33f;
                    m.push().translate(0, y, z).rotateX(PI / 2 - a).scale(0.32f, 1f, 1f);
                    m.cone(0, 0, 0, 0.16f, 0.01f, 0.28f - Math.abs(i - 3) * 0.03f, 10);
                    m.pop();
                }
                break;
            case Outfits.HR_LONG:
                m.paint(1f);
                dome(m, cy + 0.005f, -0.015f, 0.395f, 0.38f, 0.385f, -0.3f, capped ? 0.2f : top, 0.45f, 18);
                m.paint(0.95f).box(0, HY - 0.12f, -0.24f, 0.62f, 0.5f, 0.22f, 0.1f, 1);
                for (int s = -1; s <= 1; s += 2) m.paint(0.95f).ellipsoid(s * 0.32f, HY - 0.12f, 0.04f, 0.08f, 0.24f, 0.1f, 9);
                break;
            case Outfits.HR_POMP:
                m.paint(1f);
                dome(m, cy, -0.01f, 0.385f, 0.375f, 0.375f, -0.1f, capped ? 0.3f : top, 0.45f, 18);
                if (!capped) {
                    m.push().translate(0, HY + 0.33f, 0.16f).rotateX(-0.35f);
                    m.paint(1f).ellipsoid(0, 0, 0, 0.27f, 0.14f, 0.24f, 14);
                    m.fixed(0x8A8AA8).ellipsoid(-0.08f, 0.1f, 0.06f, 0.08f, 0.03f, 0.1f, 6);
                    m.pop();
                }
                for (int s = -1; s <= 1; s += 2) m.paint(0.9f).ellipsoid(s * 0.34f, HY + 0.02f, 0.06f, 0.045f, 0.12f, 0.06f, 8);
                break;
            default: // flame hair: an orange scalp with flames licking up and back
                m.paint(1f);
                dome(m, cy, -0.01f, 0.383f, 0.37f, 0.37f, -0.05f, capped ? 0.3f : top, 0.5f, 18);
                for (int i = 0; i < 9; i++) {
                    float yaw = -2.4f + i * 0.6f, pitch = 0.75f + (i % 2) * 0.35f;
                    if (capped) {
                        if (Math.abs(yaw) < 1.6f) continue;
                        pitch = 0.15f;
                    }
                    onBall(cy, -0.02f, 0.3f, yaw, pitch);
                    float lean = pitch + 0.35f;
                    m.fixed(0xFF3B1F);
                    spike(m, P[0], P[1], P[2], yaw + PI * 0.15f * (i % 2 == 0 ? 1 : -1), lean, 0.11f, 0.32f, 8);
                    m.fixed(0xFFD23B);
                    spike(m, P[0], P[1] + 0.01f, P[2], yaw, lean, 0.06f, 0.22f, 7);
                }
                break;
        }
    }

    // ------------------------------------------------------------------ hats (head space)

    private static void buildHat(MeshBuilder m, int hat) {
        switch (hat) {
            case Outfits.H_NONE:
                break;
            case Outfits.H_CAP:
                m.paint(1f);
                dome(m, HY + 0.02f, -0.01f, 0.39f, 0.38f, 0.385f, 0.32f, PI / 2, 0.22f, 18);
                m.push().translate(0, HY + 0.2f, 0.3f).rotateX(0.18f);
                m.paint(0.88f).ellipsoid(0, 0, 0.06f, 0.25f, 0.03f, 0.21f, 14);
                m.pop();
                m.fixed(0xFFFFFF).sphere(0, HY + 0.39f, -0.04f, 0.035f, 6);
                m.fixed(0xFFFFFF).box(0, HY + 0.24f, 0.33f, 0.12f, 0.08f, 0.03f, 0.02f, 1);
                break;
            case Outfits.H_BEANIE:
                m.paint(1f);
                dome(m, HY + 0.03f, -0.01f, 0.39f, 0.4f, 0.39f, 0.25f, PI / 2, 0.15f, 18);
                m.paint(0.8f);
                dome(m, HY + 0.03f, -0.01f, 0.41f, 0.42f, 0.41f, 0.2f, 0.52f, 0.15f, 18);
                m.fixed(0xFFFFFF).sphere(0, HY + 0.44f, -0.06f, 0.09f, 10);
                break;
            case Outfits.H_BUCKET:
                m.push().translate(0, 0, -0.02f).rotateX(-0.12f);
                m.paint(1f).cone(0, HY + 0.17f, 0, 0.37f, 0.31f, 0.2f, 18);
                m.paint(1f).cylinder(0, HY + 0.36f, 0, 0.31f, 0f, 0.02f, 0.01f, 18);
                m.paint(0.9f).cone(0, HY + 0.1f, 0, 0.56f, 0.37f, 0.09f, 20);
                m.fixed(0x6A4A2A).cone(0, HY + 0.18f, 0, 0.375f, 0.36f, 0.05f, 18);
                m.pop();
                break;
            case Outfits.H_HARD:
                m.paint(1f);
                dome(m, HY + 0.04f, -0.01f, 0.4f, 0.4f, 0.41f, 0.3f, PI / 2, 0.12f, 18);
                m.push().translate(0, HY + 0.16f, 0f).rotateX(0.1f);
                m.paint(0.92f).cone(0, 0, 0, 0.47f, 0.41f, 0.04f, 20);
                m.pop();
                m.paint(0.95f).box(0, HY + 0.43f, -0.02f, 0.07f, 0.06f, 0.48f, 0.03f, 1);
                m.fixed(0xFFFFFF).box(0, HY + 0.3f, 0.36f, 0.14f, 0.08f, 0.03f, 0.015f, 1);
                break;
            case Outfits.H_COWBOY:
                m.push().translate(0, HY + 0.12f, -0.02f).rotateX(-0.12f);
                m.paint(1f).lathe(new float[]{0.31f, 0f, 0.29f, 0.2f, 0.26f, 0.3f, 0.12f, 0.28f, 0f, 0.25f}, 18);
                m.fixed(0x3A2418).cylinder(0, 0.0f, 0, 0.315f, 0f, 0.07f, 0.01f, 18);
                m.paint(0.92f).ellipsoid(0, 0.0f, 0, 0.6f, 0.035f, 0.55f, 20);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.52f, 0.05f, 0).rotateZ(s * 0.7f);
                    m.paint(0.92f).ellipsoid(0, 0, 0, 0.12f, 0.035f, 0.42f, 12);
                    m.pop();
                }
                m.pop();
                break;
            case Outfits.H_PROPELLER: {
                int[] cols = {0xFF4FA3, 0xFFD23B, 0x3BB8FF, 0x5EE65A};
                m.paint(1f);
                dome(m, HY + 0.03f, -0.01f, 0.39f, 0.39f, 0.39f, 0.3f, PI / 2, 0f, 16);
                for (int k = 0; k < 4; k++) {
                    m.push().rotateY(k * PI / 4);
                    m.fixed(cols[k]);
                    dome(m, HY + 0.03f, -0.01f, 0.05f, 0.395f, 0.395f, 0.3f, PI / 2, 0f, 8);
                    m.pop();
                }
                m.paint(0.85f).ellipsoid(0, HY + 0.15f, 0.36f, 0.2f, 0.025f, 0.12f, 10);
                m.fixed(0xC8CCD8).cylinder(0, HY + 0.4f, -0.01f, 0.02f, 0f, 0.1f, 0.005f, 6);
                m.push().translate(0, HY + 0.51f, -0.01f).rotateY(0.4f);
                m.fixed(0xFF3B4F).box(0.14f, 0, 0, 0.26f, 0.02f, 0.07f, 0.01f, 1);
                m.fixed(0x3BB8FF).box(-0.14f, 0, 0, 0.26f, 0.02f, 0.07f, 0.01f, 1);
                m.fixed(0xFFD23B).sphere(0, 0.01f, 0, 0.03f, 6);
                m.pop();
                break;
            }
            case Outfits.H_HELMET:
                // full racing helmet, open at the face, with a red stripe and a visor peak
                m.paint(1f);
                dome(m, HY + 0.01f, -0.02f, 0.43f, 0.43f, 0.43f, -0.62f, PI / 2, 0.88f, 20);
                m.fixed(0xFF3B4F);
                dome(m, HY + 0.01f, -0.02f, 0.08f, 0.435f, 0.435f, -0.62f, PI / 2, 0.88f, 8);
                m.push().translate(0, HY + 0.2f, 0.3f).rotateX(0.35f);
                m.fixed(0x2A2433).box(0, 0, 0.04f, 0.42f, 0.03f, 0.14f, 0.015f, 1);
                m.pop();
                break;
            case Outfits.H_CONE:
                m.fixed(0xFF7A1A).cylinder(0, HY + 0.17f, -0.01f, 0.35f, 0f, 0.05f, 0.02f, 16);
                m.fixed(0xFF7A1A).cone(0, HY + 0.2f, -0.01f, 0.3f, 0.2f, 0.25f, 16);
                m.fixed(0xFFFFFF).cone(0, HY + 0.45f, -0.01f, 0.2f, 0.14f, 0.15f, 16);
                m.fixed(0xFF7A1A).cone(0, HY + 0.6f, -0.01f, 0.14f, 0.03f, 0.25f, 16);
                break;
            case Outfits.H_VIKING:
                m.fixed(0x9AA0B4);
                dome(m, HY + 0.03f, -0.01f, 0.395f, 0.39f, 0.395f, 0.2f, PI / 2, 0.1f, 18);
                m.fixed(0xD8A030);
                dome(m, HY + 0.03f, -0.01f, 0.41f, 0.405f, 0.41f, 0.18f, 0.4f, 0.1f, 18);
                m.fixed(0xD8A030).box(0, HY + 0.3f, 0.02f, 0.06f, 0.09f, 0.78f, 0.03f, 1);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.34f, HY + 0.2f, -0.02f).rotateZ(-s * 0.9f);
                    m.fixed(0xFFF2D8).cone(0, 0, 0, 0.1f, 0.06f, 0.22f, 10);
                    m.push().translate(0, 0.2f, 0).rotateZ(s * 0.7f);
                    m.fixed(0xFFF2D8).cone(0, 0, 0, 0.065f, 0.01f, 0.22f, 10);
                    m.pop();
                    m.pop();
                }
                break;
            case Outfits.H_SHARK: {
                // a plush shark swallowing the head: the face peeks out of its mouth
                m.paint(1f);
                dome(m, HY + 0.02f, -0.06f, 0.45f, 0.45f, 0.47f, -0.6f, PI / 2, 0.85f, 20);
                m.fixed(0xF4F4FF);
                dome(m, HY + 0.02f, -0.06f, 0.455f, 0.455f, 0.475f, -0.6f, -0.35f, 0.85f, 20);
                for (int i = 0; i < 9; i++) {
                    float t = -1f + i * 0.25f, x = t * 0.3f, y = HY + 0.26f - 0.07f * t * t;
                    m.push().translate(x, y, faceZ(x, y) + 0.04f).rotateX(PI + 0.4f);
                    m.fixed(0xFFFFFF).cone(0, 0, 0, 0.035f, 0.003f, 0.07f, 6);
                    m.pop();
                }
                for (int s = -1; s <= 1; s += 2) {
                    m.fixed(0xFFFFFF).sphere(s * 0.3f, HY + 0.3f, 0.2f, 0.06f, 8);
                    m.fixed(INK).sphere(s * 0.32f, HY + 0.31f, 0.24f, 0.035f, 6);
                }
                m.push().translate(0, HY + 0.4f, -0.08f).rotateX(-0.45f).scale(0.28f, 1f, 1f);
                m.paint(1f).cone(0, 0, 0, 0.24f, 0.01f, 0.36f, 12);
                m.pop();
                m.push().translate(0, HY - 0.05f, -0.5f).rotateX(-1.3f).scale(0.3f, 1f, 1f);
                m.paint(1f).cone(0, 0, 0, 0.18f, 0.01f, 0.3f, 10);
                m.pop();
                break;
            }
            case Outfits.H_TOPHAT:
                m.push().translate(0.07f, HY + 0.31f, -0.02f).rotateZ(-0.28f).rotateX(-0.1f);
                m.paint(1f).cylinder(0, 0, 0, 0.22f, 0f, 0.03f, 0.012f, 16);
                m.paint(1f).cylinder(0, 0.02f, 0, 0.14f, 0f, 0.28f, 0.02f, 16);
                m.fixed(0xFF2E4A).cylinder(0, 0.04f, 0, 0.145f, 0f, 0.06f, 0.01f, 16);
                m.pop();
                break;
            case Outfits.H_CROWN: {
                m.fixed(0xC8203C);
                dome(m, HY + 0.05f, -0.01f, 0.32f, 0.36f, 0.32f, 0.45f, PI / 2, 0.05f, 16);
                m.push().translate(0, HY + 0.18f, -0.01f).rotateX(-0.05f);
                m.fixed(0xFFC83D).lathe(new float[]{0.36f, 0f, 0.36f, 0f, 0.38f, 0.16f, 0.38f, 0.16f, 0.33f, 0.16f, 0.33f, 0.16f, 0.31f, 0.02f, 0.31f, 0.02f, 0f, 0.02f}, 20);
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + Math.PI / 6;
                    float x = (float) Math.cos(a) * 0.355f, z = (float) Math.sin(a) * 0.355f;
                    m.fixed(0xFFC83D).cone(x, 0.14f, z, 0.075f, 0.01f, 0.19f, 8);
                    m.fixed(0xFFF0A0).sphere(x, 0.34f, z, 0.035f, 6);
                    int jewel = k % 3 == 0 ? 0xFF2E4A : (k % 3 == 1 ? 0x3BB8FF : 0x34D058);
                    m.fixed(jewel).sphere((float) Math.cos(a) * 0.385f, 0.08f, (float) Math.sin(a) * 0.385f, 0.04f, 6);
                }
                m.pop();
                break;
            }
            default: // space helmet: a white dome all round the head, a gold visor, an antenna
                m.paint(1f).ellipsoid(0, HY + 0.03f, 0, 0.47f, 0.47f, 0.47f, 20);
                m.push().translate(0, HY + 0.05f, 0.12f);
                m.fixed(0xFFC83D).ellipsoid(0, 0, 0, 0.36f, 0.27f, 0.38f, 16);
                m.fixed(0xFFF6D0).ellipsoid(-0.13f, 0.1f, 0.33f, 0.07f, 0.04f, 0.03f, 6);
                m.pop();
                m.fixed(0xA8B0C0).torus(0, -0.01f, 0, 0.3f, 0.06f, 18, 6);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.46f, HY + 0.03f, 0).rotateZ(PI / 2);
                    m.fixed(0x9AA2B4).cylinder(0, 0, 0, 0.08f, -0.04f, 0.04f, 0.015f, 10);
                    m.pop();
                }
                m.fixed(0x9AA2B4).cylinder(0.18f, HY + 0.4f, -0.12f, 0.015f, 0f, 0.25f, 0.005f, 6);
                m.fixed(0xFF3B4F).sphere(0.18f, HY + 0.66f, -0.12f, 0.04f, 6);
                break;
        }
    }

    // ------------------------------------------------------------------ shoes (ankle at the origin, toes to +z)

    /** A basic shoe: body (current color) from the heel to the toe, sole in the given color. */
    private static void shoe(MeshBuilder m, float len, float w, float h, int sole) {
        float zc = len / 2 - 0.07f;
        m.box(0, -Driver.SOLE + 0.02f + h / 2, zc, w, h, len, Math.min(w, h) * 0.45f, 2);
        m.fixed(sole).box(0, -Driver.SOLE + 0.012f, zc, w + 0.012f, 0.024f, len + 0.012f, 0.012f, 1);
    }

    private static void buildShoe(MeshBuilder m, int s) {
        switch (s) {
            case Outfits.S_BOOTS:
                m.paint(1f).cylinder(0, -0.03f, -0.005f, 0.095f, 0f, 0.14f, 0.025f, 10);
                m.paint(1f);
                shoe(m, 0.25f, 0.15f, 0.09f, 0x3A2418);
                m.paint(0.75f).cylinder(0, 0.1f, -0.005f, 0.1f, 0f, 0.025f, 0.01f, 10);
                break;
            case Outfits.S_FLIPFLOPS:
                m.paint(1f).box(0, -Driver.SOLE + 0.01f, 0.05f, 0.14f, 0.02f, 0.27f, 0.01f, 1);
                m.accent(1f).box(0, -0.03f, 0.04f, 0.11f, 0.06f, 0.22f, 0.03f, 1);
                for (int i = 0; i < 4; i++) m.accent(1f).sphere(-0.04f + i * 0.026f, -0.035f, 0.16f, 0.02f, 5);
                m.fixed(0xFFFFFF).box(0, -0.02f, 0.09f, 0.115f, 0.015f, 0.025f, 0.007f, 1);
                break;
            case Outfits.S_HIGHTOPS:
                m.paint(1f).cylinder(0, -0.03f, -0.01f, 0.095f, 0f, 0.12f, 0.03f, 10);
                m.paint(1f);
                shoe(m, 0.25f, 0.15f, 0.09f, 0xFFFFFF);
                m.fixed(0xFFFFFF).ellipsoid(0, -0.03f, 0.15f, 0.07f, 0.04f, 0.05f, 8);
                for (int k = -1; k <= 1; k += 2) {
                    m.push().translate(k * 0.078f, 0.0f, 0.0f).rotateZ(k * PI / 2);
                    m.fixed(0xFFFFFF).extrude(starPoly(0.04f, 0.018f), -0.004f, 0.004f);
                    m.pop();
                }
                break;
            case Outfits.S_RAIN:
                m.paint(1f).cylinder(0, -0.03f, -0.005f, 0.1f, 0f, 0.19f, 0.03f, 10);
                m.paint(1f);
                shoe(m, 0.25f, 0.15f, 0.09f, 0x3A3550);
                m.paint(0.8f).cylinder(0, 0.15f, -0.005f, 0.105f, 0f, 0.03f, 0.01f, 10);
                break;
            case Outfits.S_DRESS:
                m.paint(1f).ellipsoid(0, -0.03f, 0.06f, 0.075f, 0.045f, 0.16f, 12);
                m.paint(1f).box(0, -0.055f, -0.04f, 0.1f, 0.04f, 0.08f, 0.015f, 1);
                m.fixed(0x6A6A80).ellipsoid(-0.03f, 0.0f, 0.1f, 0.025f, 0.01f, 0.05f, 6);
                break;
            case Outfits.S_CLOWN:
                m.paint(1f).ellipsoid(0, -0.025f, 0.14f, 0.13f, 0.07f, 0.28f, 14);
                m.fixed(0xFFFFFF).box(0, -0.065f, 0.12f, 0.2f, 0.02f, 0.5f, 0.01f, 1);
                m.fixed(0xFFD23B).ellipsoid(-0.035f, 0.035f, 0.04f, 0.05f, 0.02f, 0.03f, 6);
                m.fixed(0xFFD23B).ellipsoid(0.035f, 0.035f, 0.04f, 0.05f, 0.02f, 0.03f, 6);
                break;
            case Outfits.S_BUNNY:
                m.paint(1f).ellipsoid(0, -0.025f, 0.06f, 0.1f, 0.065f, 0.18f, 12);
                for (int k = -1; k <= 1; k += 2) {
                    m.push().translate(k * 0.04f, 0.02f, 0.13f).rotateZ(-k * 0.25f).rotateX(-0.35f);
                    m.paint(1f).ellipsoid(0, 0.07f, 0, 0.03f, 0.08f, 0.02f, 8);
                    m.fixed(0xFFA6CE).ellipsoid(0, 0.07f, 0.012f, 0.018f, 0.06f, 0.012f, 6);
                    m.pop();
                    m.fixed(INK).sphere(k * 0.03f, 0.005f, 0.18f, 0.014f, 5);
                }
                m.fixed(0xFF6F9F).sphere(0, -0.02f, 0.235f, 0.018f, 5);
                break;
            case Outfits.S_SKATES:
                m.paint(1f).cylinder(0, -0.03f, -0.005f, 0.095f, 0f, 0.12f, 0.03f, 10);
                m.paint(1f).box(0, -0.035f, 0.05f, 0.14f, 0.09f, 0.25f, 0.04f, 2);
                m.fixed(0xB8BECC).box(0, -0.085f, 0.05f, 0.1f, 0.02f, 0.25f, 0.008f, 1);
                for (int i = 0; i < 2; i++) {
                    for (int k = -1; k <= 1; k += 2) {
                        m.push().translate(k * 0.055f, -0.115f, -0.04f + i * 0.18f).rotateZ(PI / 2);
                        m.fixed(0xFFD23B).cylinder(0, 0, 0, 0.035f, -0.022f, 0.022f, 0.008f, 10);
                        m.pop();
                    }
                }
                m.fixed(0xFF3B4F).sphere(0, -0.07f, 0.19f, 0.03f, 6);
                break;
            case Outfits.S_ROCKET:
                m.paint(1f).cylinder(0, -0.03f, -0.005f, 0.098f, 0f, 0.13f, 0.03f, 10);
                m.paint(1f);
                shoe(m, 0.25f, 0.15f, 0.09f, 0x3A3550);
                m.push().translate(0, -0.01f, -0.1f).rotateX(-PI / 2 - 0.5f);
                m.fixed(0x3A3550).cone(0, 0, 0, 0.04f, 0.055f, 0.06f, 10);
                m.fixed(0xFF8A1F).cone(0, 0.06f, 0, 0.045f, 0.005f, 0.12f, 8);
                m.fixed(0xFFE14D).cone(0, 0.06f, 0, 0.025f, 0.003f, 0.08f, 6);
                m.pop();
                m.fixed(0xFF3B4F).box(0, 0.05f, 0.09f, 0.16f, 0.03f, 0.03f, 0.01f, 1);
                break;
            default: // sneakers and golden kicks (with little white wings)
                m.paint(1f);
                shoe(m, 0.25f, 0.15f, 0.09f, s == Outfits.S_GOLD ? 0xFFFFFF : 0xFF3B4F);
                m.fixed(s == Outfits.S_GOLD ? 0xFFF4C0 : 0xC8CCD8).box(0, 0.0f, 0.06f, 0.07f, 0.02f, 0.1f, 0.008f, 1);
                if (s == Outfits.S_GOLD) {
                    for (int k = -1; k <= 1; k += 2) {
                        m.push().translate(k * 0.08f, 0.0f, -0.05f).rotateZ(-k * 0.6f).rotateX(0.5f);
                        m.fixed(0xFFFFFF).ellipsoid(0, 0.05f, 0, 0.012f, 0.06f, 0.035f, 6);
                        m.pop();
                    }
                    m.fixed(0x9AF0FF).sphere(0, 0.0f, 0.17f, 0.022f, 6);
                }
                break;
        }
    }

    // ------------------------------------------------------------------ things to hold (fist at the origin)

    private static void buildHeld(MeshBuilder m, int i) {
        switch (i) {
            case Outfits.HD_NONE:
                break;
            case Outfits.HD_FINGER:
                m.fixed(0x3BB8FF).box(0, 0.13f, 0, 0.2f, 0.2f, 0.09f, 0.045f, 2);
                m.fixed(0x3BB8FF).cylinder(0.035f, 0.2f, 0, 0.042f, 0f, 0.28f, 0.04f, 10);
                m.fixed(0x3BB8FF).ellipsoid(-0.11f, 0.15f, 0.01f, 0.035f, 0.07f, 0.035f, 8);
                for (int k = 0; k < 3; k++) m.fixed(0x2A9FD8).sphere(-0.05f + k * 0.05f, 0.23f, 0.03f, 0.03f, 6);
                m.fixed(0xFFE14D).box(-0.03f, 0.1f, 0.047f, 0.035f, 0.1f, 0.01f, 0.005f, 1);
                m.fixed(0xFFE14D).box(0.04f, 0.1f, 0.047f, 0.025f, 0.1f, 0.01f, 0.005f, 1);
                break;
            case Outfits.HD_ICECREAM:
                m.fixed(0xE8A65A).cone(0, -0.06f, 0, 0.015f, 0.085f, 0.27f, 12);
                m.fixed(0xFF8AC0).sphere(0, 0.25f, 0, 0.1f, 12);
                m.fixed(0xFFF6E0).sphere(0, 0.36f, 0, 0.085f, 12);
                m.fixed(0xFF8AC0).ellipsoid(0.06f, 0.2f, 0.06f, 0.025f, 0.05f, 0.025f, 6);
                m.fixed(0xFF2244).sphere(0, 0.45f, 0, 0.03f, 6);
                break;
            case Outfits.HD_BANANA:
                for (int k = 0; k < 7; k++) {
                    float t = k / 6f, a = -0.9f + t * 1.8f;
                    float z = (1f - (float) Math.cos(a)) * 0.16f, y = 0.05f + (float) Math.sin(a + 0.9f) * 0.22f;
                    float rr = 0.06f * (1f - Math.abs(t - 0.5f) * 1.1f) + 0.02f;
                    m.fixed(0xFFE14D).sphere(0, y, z, rr, 8);
                }
                m.fixed(0x6B4A2A).sphere(0, 0.42f, 0.13f, 0.02f, 5);
                break;
            case Outfits.HD_CHICKEN:
                // a rubber chicken by its feet, screaming
                for (int k = -1; k <= 1; k += 2) m.fixed(0xFF8A1F).cylinder(k * 0.02f, -0.02f, 0, 0.012f, 0f, 0.1f, 0.004f, 5);
                m.fixed(0xFFE14D).ellipsoid(0, 0.2f, 0, 0.075f, 0.13f, 0.07f, 10);
                m.fixed(0xFFE14D).cylinder(0, 0.3f, 0.01f, 0.03f, 0f, 0.14f, 0.01f, 8);
                m.fixed(0xFFE14D).sphere(0, 0.46f, 0.02f, 0.055f, 10);
                m.fixed(0xFF2E3A).box(0, 0.52f, 0.0f, 0.02f, 0.05f, 0.08f, 0.01f, 1);
                m.push().translate(0, 0.45f, 0.07f).rotateX(PI / 2);
                m.fixed(0xFF8A1F).cone(0, 0, 0, 0.025f, 0.004f, 0.06f, 6);
                m.pop();
                m.fixed(INK).sphere(0.035f, 0.48f, 0.05f, 0.012f, 4);
                m.fixed(INK).sphere(-0.035f, 0.48f, 0.05f, 0.012f, 4);
                for (int k = -1; k <= 1; k += 2) m.fixed(0xFFD23B).ellipsoid(k * 0.075f, 0.2f, 0, 0.02f, 0.08f, 0.05f, 6);
                break;
            case Outfits.HD_BALLOON:
                m.fixed(0xFFFFFF).cylinder(0, 0, 0, 0.007f, -0.05f, 0.62f, 0f, 4);
                m.fixed(0xFF2E4A).cone(0, 0.6f, 0, 0.012f, 0.03f, 0.04f, 6);
                m.fixed(0xFF2E4A).ellipsoid(0, 0.84f, 0, 0.2f, 0.24f, 0.2f, 14);
                m.fixed(0xFFB8C4).ellipsoid(-0.08f, 0.92f, 0.13f, 0.04f, 0.06f, 0.02f, 6);
                break;
            case Outfits.HD_PHONE:
                m.fixed(0x2A2433).box(0, 0.11f, 0, 0.13f, 0.25f, 0.025f, 0.02f, 1);
                m.fixed(0x6FE8FF).box(0, 0.115f, 0.012f, 0.105f, 0.2f, 0.006f, 0.004f, 1);
                m.fixed(0xFF6FB5).box(0, 0.115f, -0.012f, 0.12f, 0.24f, 0.006f, 0.01f, 1);
                break;
            case Outfits.HD_TACO:
                m.fixed(0xFFC94D).ellipsoid(0, 0.13f, 0, 0.05f, 0.13f, 0.17f, 12);
                for (int k = 0; k < 5; k++) {
                    float z = -0.1f + k * 0.05f;
                    m.fixed(k % 2 == 0 ? 0x5ED64A : 0xFF4040).sphere(k % 2 == 0 ? 0.02f : -0.02f, 0.235f, z, 0.04f, 6);
                }
                m.fixed(0x8A4B2A).sphere(0, 0.22f, 0.01f, 0.045f, 6);
                break;
            case Outfits.HD_LOLLIPOP:
                m.fixed(0xFFFFFF).cylinder(0, -0.04f, 0, 0.014f, 0f, 0.32f, 0.005f, 6);
                m.push().translate(0, 0.42f, 0).rotateX(PI / 2);
                m.fixed(0xFF4FA3).cylinder(0, 0, 0, 0.14f, -0.025f, 0.025f, 0.012f, 16);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(0, s * 0.026f, 0).scale(1f, 0.25f, 1f);
                    m.fixed(0xFFFFFF).torus(0, 0, 0, 0.095f, 0.016f, 16, 4);
                    m.fixed(0xFFFFFF).torus(0, 0, 0, 0.045f, 0.016f, 12, 4);
                    m.pop();
                }
                m.pop();
                break;
            case Outfits.HD_FLAG:
                m.fixed(0xD8DEEA).cylinder(0, -0.08f, 0, 0.014f, 0f, 0.78f, 0.005f, 6);
                m.fixed(0xFFD23B).sphere(0, 0.71f, 0, 0.025f, 6);
                for (int col = 0; col < 4; col++) {
                    for (int row = 0; row < 3; row++) {
                        float x = 0.05f + col * 0.07f, y = 0.65f - row * 0.07f;
                        float z = (float) Math.sin(col * 1.4f) * 0.02f;
                        m.fixed((col + row) % 2 == 0 ? INK : 0xFFFFFF).box(x, y, z, 0.07f, 0.07f, 0.012f, 0f, 1);
                    }
                }
                break;
            case Outfits.HD_TROPHY:
                m.fixed(0x3A3550).box(0, 0.03f, 0, 0.14f, 0.06f, 0.14f, 0.015f, 1);
                m.fixed(0xFFC83D).lathe(new float[]{0.05f, 0.06f, 0.025f, 0.1f, 0.025f, 0.16f, 0.03f, 0.18f, 0.03f, 0.18f, 0.11f, 0.24f,
                        0.12f, 0.36f, 0.12f, 0.36f, 0.1f, 0.36f, 0.1f, 0.36f, 0f, 0.3f}, 16);
                for (int s = -1; s <= 1; s += 2) {
                    m.push().translate(s * 0.12f, 0.3f, 0).rotateX(PI / 2);
                    m.fixed(0xFFC83D).torus(0, 0, 0, 0.045f, 0.012f, 12, 4);
                    m.pop();
                }
                m.push().translate(0, 0.28f, 0.11f).rotateX(PI / 2);
                m.fixed(0xFFF0A0).extrude(starPoly(0.04f, 0.018f), -0.005f, 0.008f);
                m.pop();
                break;
            case Outfits.HD_MEGAPHONE:
                m.fixed(0x3A3550).box(0, 0.05f, 0, 0.045f, 0.12f, 0.05f, 0.015f, 1);
                m.push().translate(0, 0.14f, -0.06f).rotateX(PI / 2);
                m.fixed(0xFFFFFF).cone(0, 0, 0, 0.05f, 0.14f, 0.3f, 14);
                m.fixed(0xFF3B4F).cone(0, 0.2f, 0, 0.1f, 0.123f, 0.05f, 14);
                m.fixed(0xFF3B4F).cylinder(0, -0.03f, 0, 0.05f, 0f, 0.04f, 0.01f, 12);
                m.fixed(0x3A3550).cylinder(0, 0.29f, 0, 0.12f, 0f, 0.015f, 0f, 14);
                m.pop();
                break;
            case Outfits.HD_WAND:
                m.fixed(0x2A2433).cylinder(0, -0.06f, 0, 0.016f, 0f, 0.44f, 0.006f, 6);
                m.fixed(0xFFFFFF).cylinder(0, 0.3f, 0, 0.018f, 0f, 0.08f, 0.006f, 6);
                m.push().translate(0, 0.46f, 0).rotateX(PI / 2);
                m.fixed(0xFFD23B).extrude(starPoly(0.1f, 0.045f), -0.02f, 0.02f);
                m.pop();
                m.fixed(0xFFFFFF).sphere(0.12f, 0.52f, 0.02f, 0.018f, 4);
                m.fixed(0xFFFFFF).sphere(-0.1f, 0.58f, -0.01f, 0.014f, 4);
                break;
            default: // a huge diamond held up high
                m.push().translate(0, -0.02f, 0);
                gem(m, 0.17f, 0.3f, 8, 0xBFF4FF, 0x6FCBFF);
                m.pop();
                break;
        }
    }
}
