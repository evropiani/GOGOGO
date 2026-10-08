package com.gogogo.game.engine;

import java.util.HashMap;

public final class Shader {
    private final GL gl;
    public final int program;
    private final HashMap<String, Integer> uniforms = new HashMap<String, Integer>();

    public Shader(GL gl, String vertexSrc, String fragmentSrc) {
        this.gl = gl;
        int vs = compile(GL.GL_VERTEX_SHADER, vertexSrc);
        int fs = compile(GL.GL_FRAGMENT_SHADER, fragmentSrc);
        program = gl.glCreateProgram();
        gl.glAttachShader(program, vs);
        gl.glAttachShader(program, fs);
        gl.glLinkProgram(program);
        if (gl.glGetProgrami(program, GL.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Shader link failed: " + gl.glGetProgramInfoLog(program));
        }
        gl.glDeleteShader(vs);
        gl.glDeleteShader(fs);
    }

    private int compile(int type, String src) {
        int s = gl.glCreateShader(type);
        gl.glShaderSource(s, src);
        gl.glCompileShader(s);
        if (gl.glGetShaderi(s, GL.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile failed: " + gl.glGetShaderInfoLog(s) + "\n" + src);
        }
        return s;
    }

    public void use() {
        gl.glUseProgram(program);
    }

    public int loc(String name) {
        Integer l = uniforms.get(name);
        if (l == null) {
            l = gl.glGetUniformLocation(program, name);
            uniforms.put(name, l);
        }
        return l;
    }

    public void set(String name, float v) {
        gl.glUniform1f(loc(name), v);
    }

    public void set(String name, float x, float y) {
        gl.glUniform2f(loc(name), x, y);
    }

    public void set(String name, float x, float y, float z) {
        gl.glUniform3f(loc(name), x, y, z);
    }

    public void set(String name, float x, float y, float z, float w) {
        gl.glUniform4f(loc(name), x, y, z, w);
    }

    public void seti(String name, int v) {
        gl.glUniform1i(loc(name), v);
    }

    public void setMat(String name, float[] m) {
        gl.glUniformMatrix4fv(loc(name), m);
    }
}
