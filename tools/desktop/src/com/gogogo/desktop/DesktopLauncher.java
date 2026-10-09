package com.gogogo.desktop;

import com.gogogo.game.engine.GL;
import com.gogogo.game.engine.Input;
import com.gogogo.game.engine.Platform;
import com.gogogo.game.game.Game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * Development harness: runs the real game on desktop OpenGL ES (Mesa) and executes a
 * scripted session, saving screenshots. Not part of the Android app.
 *
 * Usage: DesktopLauncher <assetsDir> <width> <height> "<script>"
 * Script commands (separated by ';'): wait N | tap X Y | down X Y | move X Y | up |
 * back | shot FILE | flag N | quit. Coordinates are UI units (the short side is 720).
 */
public final class DesktopLauncher implements Platform {
    private final File assets;
    private final GL gl = new DesktopGL();
    private final Properties store = new Properties();
    private final File storeFile;

    DesktopLauncher(File assets, File storeFile) {
        this.assets = assets;
        this.storeFile = storeFile;
        if (storeFile.exists()) {
            try (FileInputStream in = new FileInputStream(storeFile)) {
                store.load(in);
            } catch (Exception ignored) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        File assets = new File(args[0]);
        int w = Integer.parseInt(args[1]), h = Integer.parseInt(args[2]);
        String script = args.length > 3 ? args[3] : "wait 120; shot shot.png";
        File storeFile = new File(System.getProperty("store", "desktop-save.properties"));
        DesktopLauncher p = new DesktopLauncher(assets, storeFile);

        if (!GLFW.glfwInit()) throw new RuntimeException("glfwInit failed");
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_ES_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_EGL_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 24);
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
        long win = GLFW.glfwCreateWindow(w, h, "GO! GO! GO!", 0, 0);
        if (win == 0) throw new RuntimeException("window failed");
        GLFW.glfwMakeContextCurrent(win);
        GLES.createCapabilities();

        Game game = new Game(p);
        game.onSurfaceCreated();
        game.onSurfaceChanged(w, h);

        float dt = 1f / 60f;
        float scale = Math.min(Math.min(w, h) / Game.UI_SHORT, w / Game.UI_MIN_W);
        for (String raw : script.split(";")) {
            String[] t = raw.trim().split("\\s+");
            if (t.length == 0 || t[0].isEmpty()) continue;
            switch (t[0]) {
                case "wait": {
                    int n = Integer.parseInt(t[1]);
                    for (int i = 0; i < n; i++) game.step(dt);
                    break;
                }
                case "tap": {
                    float x = Float.parseFloat(t[1]) * scale, y = Float.parseFloat(t[2]) * scale;
                    game.input.push(Input.DOWN, 0, x, y);
                    game.step(dt);
                    game.step(dt);
                    game.input.push(Input.UP, 0, x, y);
                    game.step(dt);
                    break;
                }
                case "down":
                    game.input.push(Input.DOWN, Integer.parseInt(t.length > 3 ? t[3] : "0"), Float.parseFloat(t[1]) * scale, Float.parseFloat(t[2]) * scale);
                    game.step(dt);
                    break;
                case "move":
                    game.input.push(Input.MOVE, Integer.parseInt(t.length > 3 ? t[3] : "0"), Float.parseFloat(t[1]) * scale, Float.parseFloat(t[2]) * scale);
                    game.step(dt);
                    break;
                case "up":
                    game.input.push(Input.UP, Integer.parseInt(t.length > 1 ? t[1] : "0"), 0, 0);
                    game.step(dt);
                    break;
                case "back":
                    game.input.pushBack();
                    game.step(dt);
                    break;
                case "shot":
                    game.step(dt);
                    save(t[1], w, h);
                    System.out.println("saved " + t[1]);
                    break;
                case "flag":
                    // developer toggles for scripted tests
                    game.save.dev = true;
                    game.save.devFlags[Integer.parseInt(t[1])] = true;
                    break;
                case "quit":
                    break;
                default:
                    System.out.println("unknown command " + t[0]);
            }
        }
        int err = GLES20.glGetError();
        if (err != 0) System.out.println("GL error: 0x" + Integer.toHexString(err));
        p.flush();
        GLFW.glfwTerminate();
    }

    private static void save(String path, int w, int h) throws Exception {
        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = ((h - 1 - y) * w + x) * 4;
                int r = buf.get(i) & 255, g = buf.get(i + 1) & 255, b = buf.get(i + 2) & 255;
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        MemoryUtil.memFree(buf);
        ImageIO.write(img, "png", new File(path));
    }

    void flush() {
        try (FileOutputStream out = new FileOutputStream(storeFile)) {
            store.store(out, "desktop save");
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ Platform

    public GL gl() {
        return gl;
    }

    public Image loadImage(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(assets, path));
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
            boolean gray = img.getType() == BufferedImage.TYPE_BYTE_GRAY;
            int[] px = new int[4];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (gray) {
                        // raw values: getRGB would apply a gamma curve to linear gray
                        int v = img.getRaster().getPixel(x, y, px)[0];
                        buf.put((byte) v).put((byte) v).put((byte) v).put((byte) 255);
                    } else {
                        int argb = img.getRGB(x, y);
                        buf.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
                    }
                }
            }
            buf.position(0);
            return new Image(w, h, buf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String loadText(String path) {
        try (InputStream in = new FileInputStream(new File(assets, path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String load(String key) {
        return store.getProperty(key);
    }

    public void save(String key, String value) {
        store.setProperty(key, value);
        flush();
    }

    public int loadSound(String path) {
        return 0;
    }

    public void playSound(int handle, float volume, float pitch) {
    }

    public void playMusic(String path, float volume) {
    }

    public void setMusicVolume(float volume) {
    }

    public void stopMusic() {
    }

    public void vibrate(int millis) {
    }

    public int safeInsetTop() {
        return Integer.getInteger("insetTop", 0);
    }

    public int safeInsetBottom() {
        return 0;
    }

    public int safeInsetLeft() {
        return Integer.getInteger("insetLeft", 0);
    }

    public int safeInsetRight() {
        return 0;
    }

    public void exit() {
        System.out.println("exit requested");
    }
}
