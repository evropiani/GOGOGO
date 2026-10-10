package com.gogogo.game.game;

/** A game screen. update() runs logic, render3d() queues world geometry, ui() draws the overlay. */
public abstract class Screen {
    protected final Game game;

    protected Screen(Game game) {
        this.game = game;
    }

    public void enter() {}

    public void exit() {}

    /** Runs right after input polling, before UI widgets see the touches (claim gameplay touches here). */
    public void preInput() {}

    /** Background (see Skies) drawn behind this screen. */
    public int skyId() {
        return 0;
    }

    /** False keeps level-up / achievement pop-ups waiting (they would cover the match HUD). */
    public boolean allowNotes() {
        return true;
    }

    public abstract void update(float dt);

    /** Queue 3D geometry and set up the camera. Return false to skip the 3D pass. */
    public boolean render3d() {
        return false;
    }

    public abstract void ui(float dt);

    /** Android back button. Return true if handled. */
    public boolean back() {
        return false;
    }

    /** App went to background. */
    public void pause() {}
}
