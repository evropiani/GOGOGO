package com.gogogo.game.game;

import com.gogogo.game.engine.Camera;
import com.gogogo.game.engine.Font;
import com.gogogo.game.engine.GL;
import com.gogogo.game.engine.Input;
import com.gogogo.game.engine.Platform;
import com.gogogo.game.engine.Renderer;
import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/** Root object: owns the engine services, persistent profile and the active screen. */
public final class Game {
    /** The shorter screen side is this many UI units (the game runs in landscape). */
    public static final float UI_SHORT = 720f;

    public final Platform platform;
    public final GL gl;
    public final Input input = new Input();
    public final Camera cam = new Camera();
    public Renderer r;
    public UIBatch b;
    public UI ui;
    public Art art;
    public Sfx sfx;
    public Save save;

    public int width = 1, height = 1;
    /** Safe area (notches, rounded corners) in UI units. */
    public float safeTop, safeBottom, safeLeft, safeRight;
    public float time;

    private Screen screen, pending;
    private float fadeT = 0f;   // 0..1 cover amount
    private int fadeDir = 0;    // 1 = covering, -1 = uncovering
    private boolean created;
    private long lastNanos;
    private float shakeT, shakeAmp;

    public Game(Platform platform) {
        this.platform = platform;
        this.gl = platform.gl();
    }

    /** GL context (re)created. */
    public void onSurfaceCreated() {
        if (!created) {
            r = new Renderer(gl);
            Font[] fonts = Font.parse(platform.loadText("font.txt"));
            b = new UIBatch(gl, fonts, platform.loadImage("font.png"));
            ui = new UI(b, input);
            save = new Save(platform);
            save.load();
            sfx = new Sfx(this);
            ui.onClick = new Runnable() {
                public void run() {
                    sfx.play(Sfx.CLICK, 0.7f, 1f + (float) Math.random() * 0.15f);
                }
            };
            art = new Art(r);
        }
        r.initGL();
        b.initGL();
        if (!created) {
            created = true;
            screen = new TitleScreen(this);
            screen.enter();
            sfx.music(Sfx.MUSIC_MENU);
        }
        lastNanos = System.nanoTime();
    }

    public void onSurfaceChanged(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        gl.glViewport(0, 0, width, height);
        cam.width = width;
        cam.height = height;
        b.resize(width, height, UI_SHORT);
        readInsets();
    }

    private void readInsets() {
        safeTop = platform.safeInsetTop() / b.scale;
        safeBottom = platform.safeInsetBottom() / b.scale;
        safeLeft = platform.safeInsetLeft() / b.scale;
        safeRight = platform.safeInsetRight() / b.scale;
    }

    public void onDrawFrame() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1e9f;
        lastNanos = now;
        if (dt > 0.05f) dt = 0.05f;
        if (dt < 0f) dt = 0f;
        step(dt);
    }

    public void step(float dt) {
        time += dt;
        readInsets(); // the notch moves sides when the phone is flipped
        input.poll(b.scale, dt);
        if (fadeDir <= 0) screen.preInput();
        ui.frame(dt);

        if (input.backPressed() && fadeDir == 0) {
            if (!screen.back()) platform.exit();
        }

        // screen transition
        if (fadeDir != 0) {
            fadeT += fadeDir * dt * 5.5f;
            if (fadeDir > 0 && fadeT >= 1f) {
                fadeT = 1f;
                screen.exit();
                screen = pending;
                pending = null;
                screen.enter();
                fadeDir = -1;
            } else if (fadeDir < 0 && fadeT <= 0f) {
                fadeT = 0f;
                fadeDir = 0;
            }
        }
        ui.enabled = fadeDir == 0;
        screen.update(dt);

        if (shakeT > 0) shakeT = Math.max(0f, shakeT - dt);

        gl.glViewport(0, 0, width, height);
        gl.glClearColor(0.4f, 0.3f, 0.7f, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        r.drawSky();
        worldBegun = false;
        if (screen.render3d()) {
            if (!worldBegun) cam.update();
            r.flush(cam);
        }
        b.begin();
        screen.ui(dt);
        if (fadeT > 0f) drawTransition();
        b.end();
        ui.endFrame();
    }

    private boolean worldBegun;

    /** Screens call this after placing the camera and before queueing geometry (applies shake, enables culling). */
    public void beginWorld() {
        if (shakeT > 0 && save.shake) {
            float a = shakeAmp * (shakeT / 0.4f);
            cam.ex += (float) Math.sin(time * 71f) * a;
            cam.ey += (float) Math.sin(time * 53f + 1f) * a;
            cam.tx += (float) Math.sin(time * 61f + 2f) * a * 0.5f;
        }
        cam.update();
        r.setFrustum(cam);
        worldBegun = true;
    }

    private void drawTransition() {
        // bouncy diagonal stripes wipe
        float t = fadeT;
        int n = 9;
        float bandH = b.height / n;
        for (int i = 0; i < n; i++) {
            float k = Math.max(0f, Math.min(1f, t * 1.6f - (fadeDir > 0 ? i : (n - 1 - i)) * 0.07f));
            float w = b.width * 1.3f * k;
            int col = (i % 2 == 0) ? 0xFFFF4FA3 : 0xFF7B5CFF;
            b.roundRect(b.width / 2 - w / 2, i * bandH - 2, w, bandH + 4, 0, col);
        }
        if (t > 0.7f) {
            b.alpha((t - 0.7f) / 0.3f);
            b.textShadow(b.title, "GO!", b.width / 2, b.height / 2, 120, 0xFFFFE14D, UIBatch.CENTER, 0xFF2A1840, 12, 10, 0x80200040);
            b.alpha(1f);
        }
    }

    public void setScreen(Screen s) {
        if (fadeDir > 0) return;
        pending = s;
        fadeDir = 1;
    }

    public Screen screen() {
        return screen;
    }

    public void shake(float amp, float time) {
        if (amp >= shakeAmp || shakeT <= 0) {
            shakeAmp = amp;
            shakeT = Math.max(shakeT, time);
        }
    }

    public void vibrate(int ms) {
        if (save.vibration) platform.vibrate(ms);
    }

    public void onPause() {
        input.reset();
        if (screen != null) screen.pause();
        if (save != null) save.flush();
        if (sfx != null) sfx.pauseMusic();
    }

    public void onResume() {
        lastNanos = System.nanoTime();
        if (sfx != null) sfx.resumeMusic();
    }
}
