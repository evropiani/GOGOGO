package com.gogogo.game.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.gogogo.game.engine.GL;
import com.gogogo.game.engine.Input;
import com.gogogo.game.engine.Platform;
import com.gogogo.game.game.Game;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/** The single Android activity hosting the GL surface. Also implements the platform services. */
public final class GoActivity extends Activity implements Platform {
    private GLSurfaceView view;
    private Game game;
    private final GL gl = new AndroidGL();
    private SoundPool pool;
    private MediaPlayer music;
    private String musicFile;
    private float musicVolume = 0.5f;
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int insetTop, insetBottom, insetLeft, insetRight;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        allowCutout(w);

        prefs = getSharedPreferences("gogogo", Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(14).setAudioAttributes(attrs).build();
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);

        game = new Game(this);
        view = new GLSurfaceView(this) {
            @Override
            public boolean onTouchEvent(MotionEvent e) {
                handleTouch(e);
                return true;
            }
        };
        view.setEGLContextClientVersion(3);
        view.setEGLConfigChooser(new MsaaChooser());
        view.setPreserveEGLContextOnPause(true);
        view.setRenderer(new GLSurfaceView.Renderer() {
            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                game.onSurfaceCreated();
            }

            public void onSurfaceChanged(GL10 unused, int width, int height) {
                game.onSurfaceChanged(width, height);
            }

            public void onDrawFrame(GL10 unused) {
                game.onDrawFrame();
            }
        });
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                readCutout(insets);
                return insets;
            }
        });
        setContentView(view);
        immersive();
    }

    private void handleTouch(MotionEvent e) {
        Input in = game.input;
        int action = e.getActionMasked();
        int idx = e.getActionIndex();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                in.push(Input.DOWN, e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) in.push(Input.MOVE, e.getPointerId(i), e.getX(i), e.getY(i));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                in.push(Input.UP, e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_CANCEL:
                for (int i = 0; i < e.getPointerCount(); i++) in.push(Input.CANCEL, e.getPointerId(i), e.getX(i), e.getY(i));
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /** Draw into the notch area on API 28+ (we keep the HUD clear of it via safe insets). */
    private static void allowCutout(Window w) {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.getClass().getField("layoutInDisplayCutoutMode").setInt(lp, 1); // SHORT_EDGES
            w.setAttributes(lp);
        } catch (Throwable ignored) {
        }
    }

    private void readCutout(WindowInsets insets) {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            Method gc = insets.getClass().getMethod("getDisplayCutout");
            Object cut = gc.invoke(insets);
            if (cut != null) {
                insetTop = (Integer) cut.getClass().getMethod("getSafeInsetTop").invoke(cut);
                insetBottom = (Integer) cut.getClass().getMethod("getSafeInsetBottom").invoke(cut);
                insetLeft = (Integer) cut.getClass().getMethod("getSafeInsetLeft").invoke(cut);
                insetRight = (Integer) cut.getClass().getMethod("getSafeInsetRight").invoke(cut);
            } else {
                insetTop = insetBottom = insetLeft = insetRight = 0;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.queueEvent(new Runnable() {
            public void run() {
                game.onPause();
            }
        });
        view.onPause();
        pauseMusicNow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
        view.queueEvent(new Runnable() {
            public void run() {
                game.onResume();
            }
        });
        immersive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pool != null) pool.release();
        if (music != null) {
            music.release();
            music = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        game.input.pushBack();
    }

    // ------------------------------------------------------------------ Platform

    public GL gl() {
        return gl;
    }

    public Image loadImage(String path) {
        try {
            InputStream in = getAssets().open(path);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            o.inPremultiplied = false;
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, o);
            in.close();
            ByteBuffer buf = ByteBuffer.allocateDirect(bmp.getWidth() * bmp.getHeight() * 4).order(ByteOrder.nativeOrder());
            bmp.copyPixelsToBuffer(buf);
            buf.position(0);
            Image img = new Image(bmp.getWidth(), bmp.getHeight(), buf);
            bmp.recycle();
            return img;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load " + path, e);
        }
    }

    public String loadText(String path) {
        try {
            InputStream in = getAssets().open(path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Cannot load " + path, e);
        }
    }

    public String load(String key) {
        return prefs.getString(key, null);
    }

    public void save(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public int loadSound(String path) {
        try {
            AssetFileDescriptor fd = getAssets().openFd(path);
            int id = pool.load(fd, 1);
            fd.close();
            return id;
        } catch (Exception e) {
            return 0;
        }
    }

    public void playSound(int handle, float volume, float pitch) {
        if (handle != 0) pool.play(handle, volume, volume, 1, 0, pitch);
    }

    public void playMusic(final String path, final float volume) {
        main.post(new Runnable() {
            public void run() {
                musicVolume = volume;
                if (path.equals(musicFile) && music != null) {
                    music.setVolume(volume, volume);
                    if (!music.isPlaying()) music.start();
                    return;
                }
                stopMusicNow();
                try {
                    AssetFileDescriptor fd = getAssets().openFd(path);
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                    fd.close();
                    mp.setLooping(true);
                    mp.setVolume(volume, volume);
                    mp.prepare();
                    mp.start();
                    music = mp;
                    musicFile = path;
                } catch (Exception e) {
                    music = null;
                    musicFile = null;
                }
            }
        });
    }

    public void setMusicVolume(final float volume) {
        main.post(new Runnable() {
            public void run() {
                musicVolume = volume;
                if (music != null) music.setVolume(volume, volume);
            }
        });
    }

    public void stopMusic() {
        main.post(new Runnable() {
            public void run() {
                stopMusicNow();
            }
        });
    }

    private void stopMusicNow() {
        if (music != null) {
            try {
                music.stop();
            } catch (Exception ignored) {
            }
            music.release();
            music = null;
            musicFile = null;
        }
    }

    private void pauseMusicNow() {
        if (music != null && music.isPlaying()) music.pause();
    }

    @SuppressWarnings("deprecation")
    public void vibrate(int millis) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) vibrator.vibrate(millis);
        } catch (Exception ignored) {
        }
    }

    public int safeInsetTop() {
        return insetTop;
    }

    public int safeInsetBottom() {
        return insetBottom;
    }

    public int safeInsetLeft() {
        return insetLeft;
    }

    public int safeInsetRight() {
        return insetRight;
    }

    public void exit() {
        main.post(new Runnable() {
            public void run() {
                finish();
            }
        });
    }

    /** Prefers 4x MSAA, falls back to any RGB888 + depth config. */
    private static final class MsaaChooser implements GLSurfaceView.EGLConfigChooser {
        private static final int EGL_OPENGL_ES3_BIT = 0x40;

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[][] attempts = {
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16,
                            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, 4, EGL10.EGL_NONE},
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16,
                            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL10.EGL_NONE},
                    {EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE, 5, EGL10.EGL_DEPTH_SIZE, 16,
                            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL10.EGL_NONE},
                    {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16,
                            EGL10.EGL_RENDERABLE_TYPE, 4 /* ES2 bit: most drivers still give a 3.0 context */, EGL10.EGL_NONE},
            };
            for (int[] spec : attempts) {
                int[] num = new int[1];
                if (egl.eglChooseConfig(display, spec, null, 0, num) && num[0] > 0) {
                    EGLConfig[] configs = new EGLConfig[num[0]];
                    egl.eglChooseConfig(display, spec, configs, num[0], num);
                    return configs[0];
                }
            }
            throw new IllegalArgumentException("No usable EGL config");
        }
    }
}
