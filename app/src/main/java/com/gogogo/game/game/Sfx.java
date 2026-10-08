package com.gogogo.game.game;

/** Sound effects and music, respecting the player's settings. */
public final class Sfx {
    public static final int CLICK = 0, BUY = 1, NOPE = 2, BEEP = 3, GO = 4, TICK = 5, DROP = 6, LAND = 7,
            BUMP = 8, BONK = 9, BOOST = 10, FALL = 11, POP = 12, WIN = 13, LOSE = 14, COIN = 15,
            QUACK = 16, UNLOCK = 17, WHOOSH = 18, HONK = 19;
    private static final String[] FILES = {
            "click", "buy", "nope", "beep", "go", "tick", "drop", "land",
            "bump", "bonk", "boost", "fall", "pop", "win", "lose", "coin",
            "quack", "unlock", "whoosh", "honk"};

    public static final String MUSIC_MENU = "sfx/music_menu.ogg";
    public static final String MUSIC_GAME = "sfx/music_game.ogg";

    private final Game game;
    private final int[] handles = new int[FILES.length];
    private final float[] lastPlayed = new float[FILES.length];
    private String currentMusic;
    private boolean musicPaused;

    public Sfx(Game game) {
        this.game = game;
        for (int i = 0; i < FILES.length; i++) handles[i] = game.platform.loadSound("sfx/" + FILES[i] + ".ogg");
    }

    public void play(int id) {
        play(id, 1f, 1f);
    }

    public void play(int id, float volume, float pitch) {
        if (!game.save.sound) return;
        // avoid machine-gunning the same sample in one frame
        if (game.time - lastPlayed[id] < 0.03f) return;
        lastPlayed[id] = game.time;
        game.platform.playSound(handles[id], Math.min(1f, volume), Math.max(0.5f, Math.min(2f, pitch)));
    }

    public void music(String file) {
        currentMusic = file;
        if (game.save.music && !musicPaused) game.platform.playMusic(file, 0.55f);
        else game.platform.stopMusic();
    }

    public void refreshMusic() {
        if (currentMusic != null) music(currentMusic);
    }

    public void pauseMusic() {
        musicPaused = true;
        game.platform.stopMusic();
    }

    public void resumeMusic() {
        musicPaused = false;
        refreshMusic();
    }
}
