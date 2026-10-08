package com.gogogo.game.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Static indexed mesh (position, normal, color+material slot) drawn with per-instance
 * transforms and tint colors. Keeps its CPU data so it can be re-uploaded after GL context loss.
 */
public final class Mesh {
    public static final int VERTEX_FLOATS = 10;
    public static final int INSTANCE_FLOATS = 24;

    public final float[] vertices;
    public final short[] indices;
    public final float radius; // bounding radius around origin

    int vao, vbo, ibo, instVbo;
    boolean uploaded;

    float[] inst = new float[INSTANCE_FLOATS * 16];
    int instCount;
    FloatBuffer instBuf;
    boolean queued;

    public Mesh(float[] vertices, short[] indices) {
        this.vertices = vertices;
        this.indices = indices;
        float r2 = 0f;
        for (int i = 0; i < vertices.length; i += VERTEX_FLOATS) {
            float x = vertices[i], y = vertices[i + 1], z = vertices[i + 2];
            r2 = Math.max(r2, x * x + y * y + z * z);
        }
        radius = (float) Math.sqrt(r2);
    }

    public int vertexCount() {
        return vertices.length / VERTEX_FLOATS;
    }

    void upload(GL gl) {
        FloatBuffer vb = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(vertices).position(0);
        ShortBuffer ib = ByteBuffer.allocateDirect(indices.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(indices).position(0);

        vao = gl.glGenVertexArray();
        gl.glBindVertexArray(vao);
        vbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, vb, GL.GL_STATIC_DRAW);
        int stride = VERTEX_FLOATS * 4;
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, stride, 12);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 4, GL.GL_FLOAT, false, stride, 24);

        ibo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, ib, GL.GL_STATIC_DRAW);

        instVbo = gl.glGenBuffer();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, instVbo);
        int istride = INSTANCE_FLOATS * 4;
        for (int i = 0; i < 6; i++) {
            gl.glEnableVertexAttribArray(3 + i);
            gl.glVertexAttribPointer(3 + i, 4, GL.GL_FLOAT, false, istride, i * 16);
            gl.glVertexAttribDivisor(3 + i, 1);
        }
        gl.glBindVertexArray(0);
        uploaded = true;
    }

    void addInstance(float[] m, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        if ((instCount + 1) * INSTANCE_FLOATS > inst.length) {
            float[] n = new float[inst.length * 2];
            System.arraycopy(inst, 0, n, 0, inst.length);
            inst = n;
        }
        int o = instCount * INSTANCE_FLOATS;
        System.arraycopy(m, 0, inst, o, 16);
        inst[o + 16] = r1;
        inst[o + 17] = g1;
        inst[o + 18] = b1;
        inst[o + 19] = a1;
        inst[o + 20] = r2;
        inst[o + 21] = g2;
        inst[o + 22] = b2;
        inst[o + 23] = a2;
        instCount++;
    }

    void drawInstances(GL gl) {
        if (instCount == 0) return;
        int floats = instCount * INSTANCE_FLOATS;
        if (instBuf == null || instBuf.capacity() < floats) {
            instBuf = ByteBuffer.allocateDirect(inst.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        }
        instBuf.position(0);
        instBuf.put(inst, 0, floats);
        instBuf.position(0);
        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, instVbo);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, floats * 4, instBuf, GL.GL_STREAM_DRAW);
        gl.glDrawElementsInstanced(GL.GL_TRIANGLES, indices.length, GL.GL_UNSIGNED_SHORT, 0, instCount);
        instCount = 0;
    }
}
