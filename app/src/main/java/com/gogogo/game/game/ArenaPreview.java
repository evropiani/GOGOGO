package com.gogogo.game.game;

/** A tiny live bot match for previewing arena skins, skies, themes and trails. */
public final class ArenaPreview {
    private final Game game;
    private Match match;
    private MatchView view;
    private float angle;
    private long seed = 1;
    public int skin, sky, theme, trail;

    /** Where the middle of the mini arena lands on screen (fractions of width / height). */
    public float screenX = 0.27f, screenY = 0.55f;
    public float distance = 34f;

    public ArenaPreview(Game game) {
        this.game = game;
        rebuild();
    }

    private void rebuild() {
        Match.Options o = new Match.Options();
        o.attract = true;
        o.bots = 7;
        o.size = 6;
        o.slowTimer = true;
        o.noPowerUps = true;
        match = new Match(seed++, o, null);
        // show all six colors of the theme: start at the rounds that use six
        match.round = 11;
        match.numColors = 6;
        match.arena.assignColors(match.rng, 6, match.target, 1f / 6f, 3);
        if (view == null) view = new MatchView(game, match);
        else view.setMatch(match);
        view.quiet = true;
        // no chase camera here: bring the sky decorations in close so they frame the mini arena
        view.decorReach = 0.45f;
        view.decorSize = 0.8f;
        apply();
    }

    /** Shows this look. */
    public void set(int skin, int sky, int theme, int trail) {
        this.skin = skin;
        this.sky = sky;
        this.theme = theme;
        this.trail = trail;
        apply();
    }

    private void apply() {
        view.skin = skin;
        view.sky = sky;
        Themes.apply(theme);
        for (Car c : match.cars) c.trail = trail;
    }

    public void update(float dt) {
        match.update(dt);
        view.update(dt);
        angle += dt * 0.25f;
        // show off the trail
        for (Car c : match.cars) {
            if (c.alive && c.boostCd <= 0 && Math.random() < dt * 0.8f) c.wantBoost = true;
        }
        if (match.phase == Match.OVER && match.phaseT > 2f) rebuild();
    }

    public void render() {
        float fov = 40f;
        float h = distance * 0.62f;
        float cx = (float) Math.sin(angle) * distance, cz = (float) Math.cos(angle) * distance;
        // slide the view so the arena's middle lands at (screenX, screenY)
        float aspect = game.cam.width / (float) Math.max(1, game.cam.height);
        float t = (float) Math.tan(Math.toRadians(fov / 2));
        float reach = (float) Math.sqrt(distance * distance + h * h);
        float side = -(2f * screenX - 1f) * reach * t * aspect;
        float up = (2f * screenY - 1f) * reach * t;
        float rx = (float) Math.cos(angle), rz = -(float) Math.sin(angle);
        game.cam.fov = fov;
        game.cam.set(cx + rx * side, h + up, cz + rz * side, rx * side, up - 2f, rz * side);
        game.beginWorld();
        view.draw();
    }
}
