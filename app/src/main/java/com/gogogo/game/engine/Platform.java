package com.gogogo.game.engine;

import java.nio.ByteBuffer;

/** Everything the game needs from the host OS. Implemented for Android and the desktop test harness. */
public interface Platform {
    GL gl();

    /** Decodes an image asset into tightly packed RGBA bytes (direct buffer). */
    Image loadImage(String assetPath);

    /** Reads a whole text asset. */
    String loadText(String assetPath);

    /** Persistent key/value storage. */
    String load(String key);

    void save(String key, String value);

    /** Returns a handle for {@link #playSound}. */
    int loadSound(String assetPath);

    void playSound(int handle, float volume, float pitch);

    void playMusic(String assetPath, float volume);

    void setMusicVolume(float volume);

    void stopMusic();

    void vibrate(int millis);

    /** Screen area hidden by notches / system bars, in physical pixels. */
    int safeInsetTop();

    int safeInsetBottom();

    int safeInsetLeft();

    int safeInsetRight();

    /** Leave the app (Android back on the title screen). */
    void exit();

    final class Image {
        public final int width, height;
        public final ByteBuffer rgba;

        public Image(int width, int height, ByteBuffer rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }
    }
}
