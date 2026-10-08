package com.gogogo.desktop;

import com.gogogo.game.engine.GL;

import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;

final class DesktopGL implements GL {
    public int glCreateShader(int type) { return GLES20.glCreateShader(type); }
    public void glShaderSource(int s, String src) { GLES20.glShaderSource(s, src); }
    public void glCompileShader(int s) { GLES20.glCompileShader(s); }
    public int glGetShaderi(int s, int p) { return GLES20.glGetShaderi(s, p); }
    public String glGetShaderInfoLog(int s) { return GLES20.glGetShaderInfoLog(s); }
    public void glDeleteShader(int s) { GLES20.glDeleteShader(s); }
    public int glCreateProgram() { return GLES20.glCreateProgram(); }
    public void glAttachShader(int p, int s) { GLES20.glAttachShader(p, s); }
    public void glLinkProgram(int p) { GLES20.glLinkProgram(p); }
    public int glGetProgrami(int p, int n) { return GLES20.glGetProgrami(p, n); }
    public String glGetProgramInfoLog(int p) { return GLES20.glGetProgramInfoLog(p); }
    public void glUseProgram(int p) { GLES20.glUseProgram(p); }
    public int glGetUniformLocation(int p, String n) { return GLES20.glGetUniformLocation(p, n); }
    public void glDeleteProgram(int p) { GLES20.glDeleteProgram(p); }
    public void glUniform1i(int l, int v) { GLES20.glUniform1i(l, v); }
    public void glUniform1f(int l, float v) { GLES20.glUniform1f(l, v); }
    public void glUniform2f(int l, float x, float y) { GLES20.glUniform2f(l, x, y); }
    public void glUniform3f(int l, float x, float y, float z) { GLES20.glUniform3f(l, x, y, z); }
    public void glUniform4f(int l, float x, float y, float z, float w) { GLES20.glUniform4f(l, x, y, z, w); }
    public void glUniformMatrix4fv(int l, float[] m) { GLES20.glUniformMatrix4fv(l, false, m); }
    public int glGenBuffer() { return GLES20.glGenBuffers(); }
    public void glDeleteBuffer(int b) { GLES20.glDeleteBuffers(b); }
    public void glBindBuffer(int t, int b) { GLES20.glBindBuffer(t, b); }

    public void glBufferData(int t, int size, Buffer data, int usage) {
        GLES20.nglBufferData(t, size, data == null ? 0L : MemoryUtil.memAddress0(data) + (long) data.position() * elem(data), usage);
    }

    public void glBufferSubData(int t, int off, int size, Buffer data) {
        GLES20.nglBufferSubData(t, off, size, MemoryUtil.memAddress0(data) + (long) data.position() * elem(data));
    }

    private static int elem(Buffer b) {
        if (b instanceof ByteBuffer) return 1;
        if (b instanceof java.nio.ShortBuffer) return 2;
        return 4;
    }

    public int glGenVertexArray() { return GLES30.glGenVertexArrays(); }
    public void glDeleteVertexArray(int v) { GLES30.glDeleteVertexArrays(v); }
    public void glBindVertexArray(int v) { GLES30.glBindVertexArray(v); }
    public void glEnableVertexAttribArray(int i) { GLES20.glEnableVertexAttribArray(i); }
    public void glVertexAttribPointer(int i, int size, int type, boolean norm, int stride, int off) { GLES20.glVertexAttribPointer(i, size, type, norm, stride, off); }
    public void glVertexAttribDivisor(int i, int d) { GLES30.glVertexAttribDivisor(i, d); }
    public void glDrawElements(int mode, int count, int type, int off) { GLES20.glDrawElements(mode, count, type, off); }
    public void glDrawElementsInstanced(int mode, int count, int type, int off, int n) { GLES30.glDrawElementsInstanced(mode, count, type, off, n); }
    public int glGenTexture() { return GLES20.glGenTextures(); }
    public void glDeleteTexture(int t) { GLES20.glDeleteTextures(t); }
    public void glBindTexture(int t, int tex) { GLES20.glBindTexture(t, tex); }
    public void glActiveTexture(int u) { GLES20.glActiveTexture(u); }
    public void glTexParameteri(int t, int p, int v) { GLES20.glTexParameteri(t, p, v); }

    public void glTexImage2D(int target, int level, int ifmt, int w, int h, int border, int fmt, int type, Buffer px) {
        GLES20.glTexImage2D(target, level, ifmt, w, h, border, fmt, type, (ByteBuffer) px);
    }

    public void glGenerateMipmap(int t) { GLES20.glGenerateMipmap(t); }
    public void glPixelStorei(int p, int v) { GLES20.glPixelStorei(p, v); }
    public void glEnable(int c) { GLES20.glEnable(c); }
    public void glDisable(int c) { GLES20.glDisable(c); }
    public void glBlendFunc(int s, int d) { GLES20.glBlendFunc(s, d); }
    public void glDepthMask(boolean f) { GLES20.glDepthMask(f); }
    public void glDepthFunc(int f) { GLES20.glDepthFunc(f); }
    public void glCullFace(int m) { GLES20.glCullFace(m); }
    public void glClearColor(float r, float g, float b, float a) { GLES20.glClearColor(r, g, b, a); }
    public void glClear(int m) { GLES20.glClear(m); }
    public void glViewport(int x, int y, int w, int h) { GLES20.glViewport(x, y, w, h); }
    public void glScissor(int x, int y, int w, int h) { GLES20.glScissor(x, y, w, h); }
    public int glGetError() { return GLES20.glGetError(); }
}
