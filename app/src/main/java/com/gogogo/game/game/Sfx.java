package com.gogogo.game.game;

/** Sound effects and music, respecting the player's settings. */
public final class Sfx {
    public static final int CLICK = 0, BUY = 1, NOPE = 2, BEEP = 3, GO = 4, TICK = 5, DROP = 6, LAND = 7,
            BUMP = 8, BONK = 9, BOOST = 10, FALL = 11, POP = 12, WIN = 13, LOSE = 14, COIN = 15,
            QUACK = 16, UNLOCK = 17, WHOOSH = 18, HONK = 19,
            START_RED = 20, START_YELLOW = 21, START_GO = 22, // start lights
            // power-ups
            PICKUP = 23, THROW = 24, SPLAT = 25, FREEZE = 26, SPRING = 27, THUD = 28, GOO = 29, BOOM = 30,
            NITRO = 31, NITRO_ROAR = 32,
            // emotes
            RASPBERRY = 33, TAUNT = 34;
    private static final String[] FILES = {
            "click", "buy", "nope", "beep", "go", "tick", "drop", "land",
            "bump", "bonk", "boost", "fall", "pop", "win", "lose", "coin",
            "quack", "unlock", "whoosh", "honk",
            "start_red", "start_yellow", "start_go",
            "pickup", "throw", "splat", "freeze", "spring", "thud", "goo", "boom",
            "nitro", "nitro_roar",
            "raspberry", "taunt"};

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
        playStream(id, volume, pitch, false);
    }

    /**
     * Like play, for a long sound: returns the playing stream so it can be cut short with stop (0 if nothing plays),
     * and a burst of short sounds doesn't cut it off.
     */
    public int start(int id, float volume, float pitch) {
        return playStream(id, volume, pitch, true);
    }

    private int playStream(int id, float volume, float pitch, boolean keep) {
        if (!game.save.sound) return 0;
        // avoid machine-gunning the same sample in one frame
        if (game.time - lastPlayed[id] < 0.03f) return 0;
        lastPlayed[id] = game.time;
        return game.platform.playSound(handles[id], Math.min(1f, volume), Math.max(0.5f, Math.min(2f, pitch)), keep);
    }

    public void stop(int stream) {
        if (stream != 0) game.platform.stopSound(stream);
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
