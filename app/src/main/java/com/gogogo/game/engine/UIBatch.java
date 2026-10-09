package com.gogogo.game.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Batched 2D renderer for the HUD and menus. Shapes are analytic rounded rectangles
 * (borders, gloss, soft shadows) and text uses the SDF font atlas (outlines, shadows).
 * Coordinates are UI units with the origin at the top-left, y down.
 */
public final class UIBatch {
    private static final String VS =
            "#version 300 es\n" +
            "layout(location=0) in vec2 a_pos;\n" +
            "layout(location=1) in vec2 a_uv;\n" +
            "layout(location=2) in vec4 a_col;\n" +
            "layout(location=3) in vec4 a_col2;\n" +
            "layout(location=4) in vec4 a_p;\n" +
            "layout(location=5) in vec4 a_q;\n" +
            "uniform mat4 u_proj;\n" +
            "out vec2 v_uv; out vec4 v_col; out vec4 v_col2; out vec4 v_p; out vec4 v_q;\n" +
            "void main(){\n" +
            "  v_uv = a_uv; v_col = a_col; v_col2 = a_col2; v_p = a_p; v_q = a_q;\n" +
            "  gl_Position = u_proj * vec4(a_pos, 0.0, 1.0);\n" +
            "}\n";

    private static final String FS =
            "#version 300 es\n" +
            "precision highp float;\n" +
            "in vec2 v_uv; in vec4 v_col; in vec4 v_col2; in vec4 v_p; in vec4 v_q;\n" +
            "uniform sampler2D u_tex;\n" +
            "out vec4 o;\n" +
            "float sdRR(vec2 p, vec2 b, float r){ vec2 q = abs(p) - b + vec2(r); return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r; }\n" +
            "void main(){\n" +
            "  if (v_q.z < 0.5) {\n" +
            "    float d = sdRR(v_uv, v_p.xy, v_p.z);\n" +
            "    float fw = max(fwidth(d), 0.0001);\n" +
            "    float soft = max(v_q.x, fw);\n" +
            "    float a = 1.0 - smoothstep(-soft * 0.5, soft * 0.5, d);\n" +
            "    float g = clamp(-v_uv.y / max(v_p.y, 1.0), -1.0, 1.0);\n" +
            "    vec4 c = vec4(v_col.rgb * (1.0 + v_q.y * g * 0.45), v_col.a);\n" +
            "    if (v_p.w > 0.0) {\n" +
            "      float ib = 1.0 - smoothstep(-fw * 0.5, fw * 0.5, d + v_p.w);\n" +
            "      c = mix(v_col2, c, ib);\n" +
            "    }\n" +
            "    o = vec4(c.rgb, c.a * a);\n" +
            "  } else if (v_q.z < 1.5) {\n" +
            "    float dist = texture(u_tex, v_uv).r;\n" +
            "    float fw = max(fwidth(dist) * 0.7, 0.0001) + v_p.y;\n" +
            "    float fillA = smoothstep(0.5 - fw, 0.5 + fw, dist);\n" +
            "    float ow = v_p.x;\n" +
            "    if (ow > 0.0) {\n" +
            "      float outA = smoothstep(0.5 - ow - fw, 0.5 - ow + fw, dist);\n" +
            "      vec3 rgb = mix(v_col2.rgb, v_col.rgb, fillA);\n" +
            "      o = vec4(rgb, mix(v_col2.a * outA, v_col.a, fillA));\n" +
            "    } else {\n" +
            "      o = vec4(v_col.rgb, v_col.a * fillA);\n" +
            "    }\n" +
            "  } else {\n" +
            "    o = texture(u_tex, v_uv) * v_col;\n" +
            "  }\n" +
            "}\n";

    private static final int FLOATS = 20;
    private static final int MAX_QUADS = 3000;

    public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

    private final GL gl;
    private Shader shader;
    private int vao, vbo, ibo;
    private final float[] data = new float[MAX_QUADS * 4 * FLOATS];
    private final FloatBuffer buf = ByteBuffer.allocateDirect(MAX_QUADS * 4 * FLOATS * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int quads;
    private final float[] proj = new float[16];

    public Font title, body;
    private Texture fontTex;
    private int boundTex = -1;

    public float width, height; // UI units
    public float scale = 1f;   // pixels per UI unit
    private int pixelW, pixelH;
    private float alphaMul = 1f;

    // clip rect in UI units (scissor); width < 0 = disabled
    private float clipX, clipY, clipW = -1, clipH;

    public UIBatch(GL gl, Font[] fonts, Platform.Image atlas) {
        this.gl = gl;
        for (Font f : fonts) {
            if (f.name.equals("title")) title = f;
            else if (f.name.equals("body")) body = f;
        }
        fontTex = new Texture(atlas);
    }

    public void initGL() {
        shader = new Shader(gl, VS, FS);
        fontTex.upload(gl);
        vao = gl.glGenVertexArray();
        gl.glBindVertexArray(vao);
        vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, data.length * 4, null, GL.GL_STREAM_DRAW);
        int stride = FLOATS * 4;
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 2, GL.GL_FLOAT, false, stride, 8);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 4, GL.GL_FLOAT, false, stride, 16);
        gl.glEnableVertexAttribArray(3);
        gl.glVertexAttribPointer(3, 4, GL.GL_FLOAT, false, stride, 32);
        gl.glEnableVertexAttribArray(4);
        gl.glVertexAttribPointer(4, 4, GL.GL_FLOAT, false, stride, 48);
        gl.glEnableVertexAttribArray(5);
        gl.glVertexAttribPointer(5, 4, GL.GL_FLOAT, false, stride, 64);
        short[] idx = new short[MAX_QUADS * 6];
        for (int i = 0; i < MAX_QUADS; i++) {
            idx[i * 6] = (short) (i * 4);
            idx[i * 6 + 1] = (short) (i * 4 + 1);
            idx[i * 6 + 2] = (short) (i * 4 + 2);
            idx[i * 6 + 3] = (short) (i * 4);
            idx[i * 6 + 4] = (short) (i * 4 + 2);
            idx[i * 6 + 5] = (short) (i * 4 + 3);
        }
        ShortBuffer ib = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(idx).position(0);
        ibo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, idx.length * 2, ib, GL.GL_STATIC_DRAW);
        gl.glBindVertexArray(0);
        boundTex = -1;
    }

    /** Sets up the UI space: the shorter screen side spans virtualShort UI units. */
    public void resize(int pixelW, int pixelH, float virtualShort) {
        this.pixelW = pixelW;
        this.pixelH = pixelH;
        scale = Math.min(pixelW, pixelH) / virtualShort;
        width = pixelW / scale;
        height = pixelH / scale;
    }

    public void begin() {
        M4.ortho(proj, 0, width, height, 0, -1, 1);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDepthMask(false);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        shader.use();
        shader.setMat("u_proj", proj);
        shader.seti("u_tex", 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, fontTex.id);
        quads = 0;
        alphaMul = 1f;
    }

    public void end() {
        flush();
        gl.glDisable(GL.GL_SCISSOR_TEST);
        clipW = -1;
        gl.glDepthMask(true);
    }

    public void flush() {
        if (quads == 0) return;
        int floats = quads * 4 * FLOATS;
        buf.position(0);
        buf.put(data, 0, floats).position(0);
        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, data.length * 4, null, GL.GL_STREAM_DRAW);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, floats * 4, buf);
        gl.glDrawElements(GL.GL_TRIANGLES, quads * 6, GL.GL_UNSIGNED_SHORT, 0);
        gl.glBindVertexArray(0);
        quads = 0;
    }

    /** Global alpha multiplier for subsequent draws (fades). */
    public void alpha(float a) {
        alphaMul = Math.max(0f, Math.min(1f, a));
    }

    public float alpha() {
        return alphaMul;
    }

    public void clip(float x, float y, float w, float h) {
        flush();
        clipX = x; clipY = y; clipW = w; clipH = h;
        gl.glEnable(GL.GL_SCISSOR_TEST);
        int sx = (int) Math.floor(x * scale);
        int sy = (int) Math.floor(pixelH - (y + h) * scale);
        gl.glScissor(sx, sy, (int) Math.ceil(w * scale), (int) Math.ceil(h * scale));
    }

    public void unclip() {
        flush();
        clipW = -1;
        gl.glDisable(GL.GL_SCISSOR_TEST);
    }

    // ------------------------------------------------------------------ primitives

    private void vtx(int o, float x, float y, float u, float v, int c1, int c2, float p0, float p1, float p2, float p3, float q0, float q1, float q2) {
        data[o] = x;
        data[o + 1] = y;
        data[o + 2] = u;
        data[o + 3] = v;
        data[o + 4] = ((c1 >> 16) & 255) / 255f;
        data[o + 5] = ((c1 >> 8) & 255) / 255f;
        data[o + 6] = (c1 & 255) / 255f;
        data[o + 7] = ((c1 >>> 24) / 255f) * alphaMul;
        data[o + 8] = ((c2 >> 16) & 255) / 255f;
        data[o + 9] = ((c2 >> 8) & 255) / 255f;
        data[o + 10] = (c2 & 255) / 255f;
        data[o + 11] = ((c2 >>> 24) / 255f) * alphaMul;
        data[o + 12] = p0;
        data[o + 13] = p1;
        data[o + 14] = p2;
        data[o + 15] = p3;
        data[o + 16] = q0;
        data[o + 17] = q1;
        data[o + 18] = q2;
        data[o + 19] = 0f;
    }

    /**
     * Rounded rectangle centered at (cx, cy). Colors are ARGB.
     * @param border     border width (0 = none), drawn in borderColor
     * @param gloss      0..1 vertical light-to-dark gradient
     * @param softness   edge blur in UI units (for shadows/glows)
     * @param rot        rotation in radians around the center
     */
    public void shape(float cx, float cy, float w, float h, float radius, int fill, int borderColor, float border, float gloss, float softness, float rot) {
        if (quads >= MAX_QUADS) flush();
        float hw = w / 2f, hh = h / 2f;
        radius = Math.min(radius, Math.min(hw, hh));
        float ex = hw + softness + 1.5f, ey = hh + softness + 1.5f;
        float c = (float) Math.cos(rot), s = (float) Math.sin(rot);
        int o = quads * 4 * FLOATS;
        float[] lx = {-ex, ex, ex, -ex};
        float[] ly = {-ey, -ey, ey, ey};
        for (int i = 0; i < 4; i++) {
            float x = cx + lx[i] * c - ly[i] * s;
            float y = cy + lx[i] * s + ly[i] * c;
            vtx(o + i * FLOATS, x, y, lx[i], ly[i], fill, borderColor, hw, hh, radius, border, softness, gloss, 0f);
        }
        quads++;
    }

    public void rect(float x, float y, float w, float h, int fill) {
        shape(x + w / 2, y + h / 2, w, h, 0, fill, 0, 0, 0, 0, 0);
    }

    public void roundRect(float x, float y, float w, float h, float radius, int fill) {
        shape(x + w / 2, y + h / 2, w, h, radius, fill, 0, 0, 0, 0, 0);
    }

    public void roundRect(float x, float y, float w, float h, float radius, int fill, int border, float borderW) {
        shape(x + w / 2, y + h / 2, w, h, radius, fill, border, borderW, 0, 0, 0);
    }

    public void circle(float cx, float cy, float r, int fill) {
        shape(cx, cy, r * 2, r * 2, r, fill, 0, 0, 0, 0, 0);
    }

    public void circle(float cx, float cy, float r, int fill, int border, float borderW) {
        shape(cx, cy, r * 2, r * 2, r, fill, border, borderW, 0, 0, 0);
    }

    public void shadow(float x, float y, float w, float h, float radius, int color, float softness) {
        shape(x + w / 2, y + h / 2, w, h, radius, color, 0, 0, 0, softness, 0);
    }

    /** A line segment with round caps. */
    public void line(float x0, float y0, float x1, float y1, float thick, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        shape((x0 + x1) / 2, (y0 + y1) / 2, len + thick, thick, thick / 2, color, 0, 0, 0, 0, (float) Math.atan2(dy, dx));
    }

    // ------------------------------------------------------------------ text

    /**
     * Draws text whose capital letters are vertically centered on y.
     * @param outline outline width in UI units (0 = none)
     */
    public float text(Font f, CharSequence s, float x, float y, float size, int color, int align, int outlineColor, float outline) {
        float k = size / Font.em;
        float w = f.width(s, size);
        float px = align == CENTER ? x - w / 2 : (align == RIGHT ? x - w : x);
        float top = y - (f.capTop + f.capBottom) / 2f * k;
        float od = outline <= 0 ? 0 : Math.min(0.48f, outline / k / (2f * Font.spread));
        glyphs(f, s, px, top, k, color, outlineColor, od, 0f);
        return w;
    }

    /** Text with a soft drop shadow below it. */
    public float textShadow(Font f, CharSequence s, float x, float y, float size, int color, int align, int outlineColor, float outline, float shadowDy, int shadowColor) {
        float k = size / Font.em;
        float w = f.width(s, size);
        float px = align == CENTER ? x - w / 2 : (align == RIGHT ? x - w : x);
        float top = y - (f.capTop + f.capBottom) / 2f * k;
        float od = outline <= 0 ? 0 : Math.min(0.48f, outline / k / (2f * Font.spread));
        glyphs(f, s, px, top + shadowDy, k, shadowColor, shadowColor, od, 0.04f);
        glyphs(f, s, px, top, k, color, outlineColor, od, 0f);
        return w;
    }

    private void glyphs(Font f, CharSequence s, float px, float top, float k, int color, int outlineColor, float od, float blur) {
        float iw = 1f / Font.atlasW, ih = 1f / Font.atlasH;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 128 || !f.has[ch]) ch = '?';
            if (f.gw[ch] > 0) {
                if (quads >= MAX_QUADS) flush();
                float x0 = px + f.xoff[ch] * k, y0 = top + f.yoff[ch] * k;
                float x1 = x0 + f.gw[ch] * k, y1 = y0 + f.gh[ch] * k;
                float u0 = f.gx[ch] * iw, v0 = f.gy[ch] * ih;
                float u1 = (f.gx[ch] + f.gw[ch]) * iw, v1 = (f.gy[ch] + f.gh[ch]) * ih;
                int o = quads * 4 * FLOATS;
                vtx(o, x0, y0, u0, v0, color, outlineColor, od, blur, 0, 0, 0, 0, 1f);
                vtx(o + FLOATS, x1, y0, u1, v0, color, outlineColor, od, blur, 0, 0, 0, 0, 1f);
                vtx(o + FLOATS * 2, x1, y1, u1, v1, color, outlineColor, od, blur, 0, 0, 0, 0, 1f);
                vtx(o + FLOATS * 3, x0, y1, u0, v1, color, outlineColor, od, blur, 0, 0, 0, 0, 1f);
                quads++;
            }
            px += f.adv[ch] * k;
        }
    }

    /** Draws text scaled down to fit maxWidth if needed. Returns the used size. */
    public float textFit(Font f, CharSequence s, float x, float y, float size, float maxWidth, int color, int align, int outlineColor, float outline) {
        float w = f.width(s, size);
        if (w > maxWidth && w > 0) size *= maxWidth / w;
        text(f, s, x, y, size, color, align, outlineColor, outline);
        return size;
    }

    public static int argb(int a, int rgb) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int withAlpha(int argb, float a) {
        int al = (int) ((argb >>> 24) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }
}
