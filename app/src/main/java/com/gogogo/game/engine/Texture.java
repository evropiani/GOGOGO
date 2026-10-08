package com.gogogo.game.engine;

public final class Texture {
    public int id;
    public final int width, height;
    private final Platform.Image image;

    public Texture(Platform.Image image) {
        this.image = image;
        this.width = image.width;
        this.height = image.height;
    }

    public void upload(GL gl) {
        id = gl.glGenTexture();
        gl.glBindTexture(GL.GL_TEXTURE_2D, id);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        image.rgba.position(0);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, width, height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, image.rgba);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    }
}
