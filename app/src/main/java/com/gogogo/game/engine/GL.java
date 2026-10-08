package com.gogogo.game.engine;

import java.nio.Buffer;

/** The subset of OpenGL ES 3.0 the game uses, so the engine runs on Android and desktop alike. */
public interface GL {
    int GL_FLOAT = 0x1406;
    int GL_UNSIGNED_SHORT = 0x1403;
    int GL_UNSIGNED_BYTE = 0x1401;
    int GL_TRIANGLES = 0x0004;
    int GL_ARRAY_BUFFER = 0x8892;
    int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    int GL_STATIC_DRAW = 0x88E4;
    int GL_DYNAMIC_DRAW = 0x88E8;
    int GL_STREAM_DRAW = 0x88E0;
    int GL_VERTEX_SHADER = 0x8B31;
    int GL_FRAGMENT_SHADER = 0x8B30;
    int GL_COMPILE_STATUS = 0x8B81;
    int GL_LINK_STATUS = 0x8B82;
    int GL_DEPTH_TEST = 0x0B71;
    int GL_CULL_FACE = 0x0B44;
    int GL_BLEND = 0x0BE2;
    int GL_SCISSOR_TEST = 0x0C11;
    int GL_SRC_ALPHA = 0x0302;
    int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    int GL_ONE = 1;
    int GL_COLOR_BUFFER_BIT = 0x4000;
    int GL_DEPTH_BUFFER_BIT = 0x0100;
    int GL_TEXTURE_2D = 0x0DE1;
    int GL_TEXTURE0 = 0x84C0;
    int GL_TEXTURE_MIN_FILTER = 0x2801;
    int GL_TEXTURE_MAG_FILTER = 0x2800;
    int GL_TEXTURE_WRAP_S = 0x2802;
    int GL_TEXTURE_WRAP_T = 0x2803;
    int GL_LINEAR = 0x2601;
    int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    int GL_CLAMP_TO_EDGE = 0x812F;
    int GL_RGBA = 0x1908;
    int GL_BACK = 0x0405;
    int GL_LEQUAL = 0x0203;
    int GL_LESS = 0x0201;
    int GL_UNPACK_ALIGNMENT = 0x0CF5;

    int glCreateShader(int type);
    void glShaderSource(int shader, String source);
    void glCompileShader(int shader);
    int glGetShaderi(int shader, int pname);
    String glGetShaderInfoLog(int shader);
    void glDeleteShader(int shader);

    int glCreateProgram();
    void glAttachShader(int program, int shader);
    void glLinkProgram(int program);
    int glGetProgrami(int program, int pname);
    String glGetProgramInfoLog(int program);
    void glUseProgram(int program);
    int glGetUniformLocation(int program, String name);
    void glDeleteProgram(int program);

    void glUniform1i(int loc, int v);
    void glUniform1f(int loc, float v);
    void glUniform2f(int loc, float x, float y);
    void glUniform3f(int loc, float x, float y, float z);
    void glUniform4f(int loc, float x, float y, float z, float w);
    void glUniformMatrix4fv(int loc, float[] m);

    int glGenBuffer();
    void glDeleteBuffer(int buffer);
    void glBindBuffer(int target, int buffer);
    void glBufferData(int target, int sizeBytes, Buffer data, int usage);
    void glBufferSubData(int target, int offsetBytes, int sizeBytes, Buffer data);

    int glGenVertexArray();
    void glDeleteVertexArray(int vao);
    void glBindVertexArray(int vao);
    void glEnableVertexAttribArray(int index);
    void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offsetBytes);
    void glVertexAttribDivisor(int index, int divisor);

    void glDrawElements(int mode, int count, int type, int offsetBytes);
    void glDrawElementsInstanced(int mode, int count, int type, int offsetBytes, int instances);

    int glGenTexture();
    void glDeleteTexture(int texture);
    void glBindTexture(int target, int texture);
    void glActiveTexture(int unit);
    void glTexParameteri(int target, int pname, int value);
    void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer pixels);
    void glGenerateMipmap(int target);
    void glPixelStorei(int pname, int value);

    void glEnable(int cap);
    void glDisable(int cap);
    void glBlendFunc(int src, int dst);
    void glDepthMask(boolean flag);
    void glDepthFunc(int func);
    void glCullFace(int mode);
    void glClearColor(float r, float g, float b, float a);
    void glClear(int mask);
    void glViewport(int x, int y, int w, int h);
    void glScissor(int x, int y, int w, int h);
    int glGetError();
}
