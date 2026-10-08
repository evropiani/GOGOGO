package com.gogogo.game.android;

import android.opengl.GLES20;
import android.opengl.GLES30;

import com.gogogo.game.engine.GL;

import java.nio.Buffer;

/** {@link GL} backed by android.opengl.GLES30. */
final class AndroidGL implements GL {
    private final int[] one = new int[1];

    public int glCreateShader(int type) { return GLES20.glCreateShader(type); }
    public void glShaderSource(int s, String src) { GLES20.glShaderSource(s, src); }
    public void glCompileShader(int s) { GLES20.glCompileShader(s); }

    public int glGetShaderi(int s, int p) {
        GLES20.glGetShaderiv(s, p, one, 0);
        return one[0];
    }

    public String glGetShaderInfoLog(int s) { return GLES20.glGetShaderInfoLog(s); }
    public void glDeleteShader(int s) { GLES20.glDeleteShader(s); }
    public int glCreateProgram() { return GLES20.glCreateProgram(); }
    public void glAttachShader(int p, int s) { GLES20.glAttachShader(p, s); }
    public void glLinkProgram(int p) { GLES20.glLinkProgram(p); }

    public int glGetProgrami(int p, int n) {
        GLES20.glGetProgramiv(p, n, one, 0);
        return one[0];
    }

    public String glGetProgramInfoLog(int p) { return GLES20.glGetProgramInfoLog(p); }
    public void glUseProgram(int p) { GLES20.glUseProgram(p); }
    public int glGetUniformLocation(int p, String n) { return GLES20.glGetUniformLocation(p, n); }
    public void glDeleteProgram(int p) { GLES20.glDeleteProgram(p); }
    public void glUniform1i(int l, int v) { GLES20.glUniform1i(l, v); }
    public void glUniform1f(int l, float v) { GLES20.glUniform1f(l, v); }
    public void glUniform2f(int l, float x, float y) { GLES20.glUniform2f(l, x, y); }
    public void glUniform3f(int l, float x, float y, float z) { GLES20.glUniform3f(l, x, y, z); }
    public void glUniform4f(int l, float x, float y, float z, float w) { GLES20.glUniform4f(l, x, y, z, w); }
    public void glUniformMatrix4fv(int l, float[] m) { GLES20.glUniformMatrix4fv(l, 1, false, m, 0); }

    public int glGenBuffer() {
        GLES20.glGenBuffers(1, one, 0);
        return one[0];
    }

    public void glDeleteBuffer(int b) {
        one[0] = b;
        GLES20.glDeleteBuffers(1, one, 0);
    }

    public void glBindBuffer(int t, int b) { GLES20.glBindBuffer(t, b); }
    public void glBufferData(int t, int size, Buffer data, int usage) { GLES20.glBufferData(t, size, data, usage); }
    public void glBufferSubData(int t, int off, int size, Buffer data) { GLES20.glBufferSubData(t, off, size, data); }

    public int glGenVertexArray() {
        GLES30.glGenVertexArrays(1, one, 0);
        return one[0];
    }

    public void glDeleteVertexArray(int v) {
        one[0] = v;
        GLES30.glDeleteVertexArrays(1, one, 0);
    }

    public void glBindVertexArray(int v) { GLES30.glBindVertexArray(v); }
    public void glEnableVertexAttribArray(int i) { GLES20.glEnableVertexAttribArray(i); }
    public void glVertexAttribPointer(int i, int size, int type, boolean norm, int stride, int off) { GLES20.glVertexAttribPointer(i, size, type, norm, stride, off); }
    public void glVertexAttribDivisor(int i, int d) { GLES30.glVertexAttribDivisor(i, d); }
    public void glDrawElements(int mode, int count, int type, int off) { GLES20.glDrawElements(mode, count, type, off); }
    public void glDrawElementsInstanced(int mode, int count, int type, int off, int n) { GLES30.glDrawElementsInstanced(mode, count, type, off, n); }

    public int glGenTexture() {
        GLES20.glGenTextures(1, one, 0);
        return one[0];
    }

    public void glDeleteTexture(int t) {
        one[0] = t;
        GLES20.glDeleteTextures(1, one, 0);
    }

    public void glBindTexture(int t, int tex) { GLES20.glBindTexture(t, tex); }
    public void glActiveTexture(int u) { GLES20.glActiveTexture(u); }
    public void glTexParameteri(int t, int p, int v) { GLES20.glTexParameteri(t, p, v); }

    public void glTexImage2D(int target, int level, int ifmt, int w, int h, int border, int fmt, int type, Buffer px) {
        GLES20.glTexImage2D(target, level, ifmt, w, h, border, fmt, type, px);
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
