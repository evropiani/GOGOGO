package com.gogogo.game.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/** Instanced 3D renderer: toy-plastic lit meshes, soft blob shadows and a gradient sky. */
public final class Renderer {
    private static final String LIT_VS =
            "#version 300 es\n" +
            "layout(location=0) in vec3 a_pos;\n" +
            "layout(location=1) in vec3 a_nrm;\n" +
            "layout(location=2) in vec4 a_col;\n" +
            "layout(location=3) in vec4 i_m0;\n" +
            "layout(location=4) in vec4 i_m1;\n" +
            "layout(location=5) in vec4 i_m2;\n" +
            "layout(location=6) in vec4 i_m3;\n" +
            "layout(location=7) in vec4 i_c1;\n" +
            "layout(location=8) in vec4 i_c2;\n" +
            "uniform mat4 u_vp;\n" +
            "out vec3 v_nrm;\n" +
            "out vec3 v_col;\n" +
            "out vec3 v_wpos;\n" +
            "out float v_flash;\n" +
            "void main(){\n" +
            "  mat4 m = mat4(i_m0, i_m1, i_m2, i_m3);\n" +
            "  vec4 wp = m * vec4(a_pos, 1.0);\n" +
            "  vec3 s2 = vec3(dot(i_m0.xyz, i_m0.xyz), dot(i_m1.xyz, i_m1.xyz), dot(i_m2.xyz, i_m2.xyz));\n" +
            "  v_nrm = mat3(m) * (a_nrm / s2);\n" +
            "  vec3 c = a_col.rgb;\n" +
            "  if (a_col.a > 1.5) c *= i_c2.rgb; else if (a_col.a > 0.5) c *= i_c1.rgb;\n" +
            "  v_col = c;\n" +
            "  v_flash = i_c1.a;\n" +
            "  v_wpos = wp.xyz;\n" +
            "  gl_Position = u_vp * wp;\n" +
            "}\n";

    private static final String LIT_FS =
            "#version 300 es\n" +
            "precision highp float;\n" +
            "in vec3 v_nrm;\n" +
            "in vec3 v_col;\n" +
            "in vec3 v_wpos;\n" +
            "in float v_flash;\n" +
            "uniform vec3 u_light;\n" +
            "uniform vec3 u_cam;\n" +
            "uniform vec3 u_sky;\n" +
            "uniform vec3 u_ground;\n" +
            "uniform vec3 u_fog;\n" +
            "uniform vec3 u_abyss;\n" +
            "uniform vec2 u_fogRange;\n" +
            "out vec4 o;\n" +
            "void main(){\n" +
            "  vec3 n = normalize(v_nrm);\n" +
            "  vec3 v = normalize(u_cam - v_wpos);\n" +
            "  float ndl = dot(n, u_light);\n" +
            "  float diff = clamp(ndl * 0.5 + 0.5, 0.0, 1.0);\n" +
            "  diff = diff * diff;\n" +
            "  vec3 amb = mix(u_ground, u_sky, n.y * 0.5 + 0.5);\n" +
            "  vec3 col = v_col * (amb * 0.62 + vec3(1.0, 0.97, 0.92) * diff * 0.62);\n" +
            "  vec3 h = normalize(u_light + v);\n" +
            "  float spec = pow(max(dot(n, h), 0.0), 40.0) * 0.32;\n" +
            "  float rim = pow(1.0 - max(dot(n, v), 0.0), 3.0) * 0.30;\n" +
            "  col += vec3(spec) + rim * u_sky;\n" +
            "  col = mix(col, vec3(1.0), v_flash);\n" +
            "  float d = length(u_cam - v_wpos);\n" +
            "  float f = clamp((d - u_fogRange.x) / (u_fogRange.y - u_fogRange.x), 0.0, 1.0);\n" +
            "  col = mix(col, u_fog, f * f);\n" +
            "  float fall = clamp(-(v_wpos.y + 2.0) / 45.0, 0.0, 1.0);\n" +
            "  col = mix(col, u_abyss, fall);\n" +
            "  o = vec4(col, 1.0);\n" +
            "}\n";

    private static final String BLOB_VS =
            "#version 300 es\n" +
            "layout(location=0) in vec2 a_pos;\n" +
            "layout(location=3) in vec4 i_a;\n" +
            "layout(location=4) in vec4 i_b;\n" +
            "uniform mat4 u_vp;\n" +
            "out vec2 v_uv;\n" +
            "out float v_alpha;\n" +
            "void main(){\n" +
            "  float c = cos(i_b.z), s = sin(i_b.z);\n" +
            "  vec2 p = vec2(a_pos.x * i_a.w, a_pos.y * i_a.w * i_b.y);\n" +
            "  vec2 r = vec2(p.x * c + p.y * s, -p.x * s + p.y * c);\n" +
            "  v_uv = a_pos;\n" +
            "  v_alpha = i_b.x;\n" +
            "  gl_Position = u_vp * vec4(i_a.x + r.x, i_a.y, i_a.z + r.y, 1.0);\n" +
            "}\n";

    private static final String BLOB_FS =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_uv;\n" +
            "in float v_alpha;\n" +
            "uniform vec3 u_shadow;\n" +
            "out vec4 o;\n" +
            "void main(){\n" +
            "  float d = length(v_uv);\n" +
            "  float a = (1.0 - smoothstep(0.55, 1.0, d)) * v_alpha;\n" +
            "  o = vec4(u_shadow, a);\n" +
            "}\n";

    private static final String SKY_VS =
            "#version 300 es\n" +
            "layout(location=0) in vec2 a_pos;\n" +
            "out vec2 v_uv;\n" +
            "void main(){ v_uv = a_pos * 0.5 + 0.5; gl_Position = vec4(a_pos, 0.999, 1.0); }\n";

    private static final String SKY_FS =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_uv;\n" +
            "uniform vec3 u_top;\n" +
            "uniform vec3 u_mid;\n" +
            "uniform vec3 u_bottom;\n" +
            "uniform float u_horizon;\n" +
            "out vec4 o;\n" +
            "void main(){\n" +
            "  float y = v_uv.y;\n" +
            "  vec3 c = y > u_horizon ? mix(u_mid, u_top, smoothstep(u_horizon, 1.0, y)) : mix(u_bottom, u_mid, smoothstep(0.0, u_horizon, y));\n" +
            "  float vig = 1.0 - 0.18 * pow(length(v_uv - 0.5) * 1.3, 2.0);\n" +
            "  o = vec4(c * vig, 1.0);\n" +
            "}\n";

    private final GL gl;
    private Shader lit, blob, sky;
    private final ArrayList<Mesh> meshes = new ArrayList<Mesh>();
    private final ArrayList<Mesh> queue = new ArrayList<Mesh>();

    private int quadVao, quadVbo, blobInstVbo, quadIbo, skyVao;
    private float[] blobs = new float[8 * 256];
    private int blobCount;
    private FloatBuffer blobBuf;

    // lighting / atmosphere (linear-ish RGB floats)
    public final float[] light = {-0.45f, 0.80f, 0.40f};
    public final float[] skyAmb = {0.80f, 0.85f, 1.0f};
    public final float[] groundAmb = {0.70f, 0.55f, 0.75f};
    public final float[] fog = {1.0f, 0.78f, 0.88f};
    public final float[] abyss = {0.36f, 0.27f, 0.62f};
    public final float[] bgTop = {1.0f, 0.80f, 0.86f};
    public final float[] bgMid = {0.78f, 0.62f, 0.96f};
    public final float[] bgBottom = {0.36f, 0.30f, 0.70f};
    public final float[] shadowColor = {0.18f, 0.08f, 0.30f};
    public float horizon = 0.62f;
    public float fogNear = 60f, fogFar = 170f;

    public Renderer(GL gl) {
        this.gl = gl;
        float l = (float) Math.sqrt(light[0] * light[0] + light[1] * light[1] + light[2] * light[2]);
        for (int i = 0; i < 3; i++) light[i] /= l;
    }

    /** (Re)creates all GL objects. Call on every new GL context. */
    public void initGL() {
        lit = new Shader(gl, LIT_VS, LIT_FS);
        blob = new Shader(gl, BLOB_VS, BLOB_FS);
        sky = new Shader(gl, SKY_VS, SKY_FS);
        for (int i = 0; i < meshes.size(); i++) meshes.get(i).upload(gl);

        float[] quad = {-1, -1, 1, -1, 1, 1, -1, 1};
        FloatBuffer qb = ByteBuffer.allocateDirect(quad.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        qb.put(quad).position(0);
        java.nio.ShortBuffer ib = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(new short[]{0, 1, 2, 0, 2, 3}).position(0);
        quadVao = gl.glGenVertexArray();
        gl.glBindVertexArray(quadVao);
        quadVbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, quad.length * 4, qb, GL.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 8, 0);
        quadIbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, quadIbo);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, 12, ib, GL.GL_STATIC_DRAW);
        blobInstVbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, blobInstVbo);
        gl.glEnableVertexAttribArray(3);
        gl.glVertexAttribPointer(3, 4, GL.GL_FLOAT, false, 32, 0);
        gl.glVertexAttribDivisor(3, 1);
        gl.glEnableVertexAttribArray(4);
        gl.glVertexAttribPointer(4, 4, GL.GL_FLOAT, false, 32, 16);
        gl.glVertexAttribDivisor(4, 1);
        gl.glBindVertexArray(0);

        skyVao = gl.glGenVertexArray();
        gl.glBindVertexArray(skyVao);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVbo);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 8, 0);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, quadIbo);
        gl.glBindVertexArray(0);
    }

    /** Meshes must be registered once; they are uploaded now if GL is ready and again on context loss. */
    public Mesh register(Mesh m) {
        meshes.add(m);
        if (lit != null) m.upload(gl);
        return m;
    }

    public void draw(Mesh m, float[] model, int c1, int c2, float flash) {
        if (!m.queued) {
            m.queued = true;
            queue.add(m);
        }
        m.addInstance(model,
                ((c1 >> 16) & 255) / 255f, ((c1 >> 8) & 255) / 255f, (c1 & 255) / 255f, flash,
                ((c2 >> 16) & 255) / 255f, ((c2 >> 8) & 255) / 255f, (c2 & 255) / 255f, 1f);
    }

    public void draw(Mesh m, float[] model, int c1) {
        draw(m, model, c1, c1, 0f);
    }

    /** Soft oval shadow lying flat at height y. stretch scales the local z axis, yaw rotates it. */
    public void blob(float x, float y, float z, float radius, float stretch, float yaw, float alpha) {
        if (alpha <= 0.01f) return;
        if ((blobCount + 1) * 8 > blobs.length) {
            float[] n = new float[blobs.length * 2];
            System.arraycopy(blobs, 0, n, 0, blobs.length);
            blobs = n;
        }
        int o = blobCount * 8;
        blobs[o] = x;
        blobs[o + 1] = y;
        blobs[o + 2] = z;
        blobs[o + 3] = radius;
        blobs[o + 4] = alpha;
        blobs[o + 5] = stretch;
        blobs[o + 6] = yaw;
        blobs[o + 7] = 0f;
        blobCount++;
    }

    public void drawSky() {
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDisable(GL.GL_BLEND);
        sky.use();
        sky.set("u_top", bgTop[0], bgTop[1], bgTop[2]);
        sky.set("u_mid", bgMid[0], bgMid[1], bgMid[2]);
        sky.set("u_bottom", bgBottom[0], bgBottom[1], bgBottom[2]);
        sky.set("u_horizon", horizon);
        gl.glBindVertexArray(skyVao);
        gl.glDrawElements(GL.GL_TRIANGLES, 6, GL.GL_UNSIGNED_SHORT, 0);
        gl.glBindVertexArray(0);
    }

    /** Draws everything queued since the last call. */
    public void flush(Camera cam) {
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glDepthMask(true);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);
        gl.glDisable(GL.GL_BLEND);
        lit.use();
        lit.setMat("u_vp", cam.viewProj);
        lit.set("u_light", light[0], light[1], light[2]);
        lit.set("u_cam", cam.ex, cam.ey, cam.ez);
        lit.set("u_sky", skyAmb[0], skyAmb[1], skyAmb[2]);
        lit.set("u_ground", groundAmb[0], groundAmb[1], groundAmb[2]);
        lit.set("u_fog", fog[0], fog[1], fog[2]);
        lit.set("u_abyss", abyss[0], abyss[1], abyss[2]);
        lit.set("u_fogRange", fogNear, fogFar);
        for (int i = 0; i < queue.size(); i++) {
            Mesh m = queue.get(i);
            m.drawInstances(gl);
            m.queued = false;
        }
        queue.clear();

        if (blobCount > 0) {
            gl.glDepthMask(false);
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL.GL_CULL_FACE);
            blob.use();
            blob.setMat("u_vp", cam.viewProj);
            blob.set("u_shadow", shadowColor[0], shadowColor[1], shadowColor[2]);
            int floats = blobCount * 8;
            if (blobBuf == null || blobBuf.capacity() < floats) {
                blobBuf = ByteBuffer.allocateDirect(blobs.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            blobBuf.position(0);
            blobBuf.put(blobs, 0, floats).position(0);
            gl.glBindVertexArray(quadVao);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, blobInstVbo);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, floats * 4, blobBuf, GL.GL_STREAM_DRAW);
            gl.glDrawElementsInstanced(GL.GL_TRIANGLES, 6, GL.GL_UNSIGNED_SHORT, 0, blobCount);
            gl.glBindVertexArray(0);
            gl.glDepthMask(true);
            blobCount = 0;
        }
        gl.glBindVertexArray(0);
    }
}
