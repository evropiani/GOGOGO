package com.gogogo.game.game;

import com.gogogo.game.engine.UI;
import com.gogogo.game.engine.UIBatch;

/**
 * Small flat pictures of the emotes: a little stick figure striking the pose, with a prop or a squiggle where it
 * helps (tears, confetti, a guitar...). A car's signature emote shows the car itself with a star.
 */
final class EmoteIcons {
    private EmoteIcons() {}

    private static final int INK = 0xFF2A1840;

    // limb angles in degrees from hanging straight down, + = out to its side (180 = straight up, past it = over the
    // head, - = across the body): left upper arm, left forearm, right upper arm, right forearm, left thigh, left shin,
    // right thigh, right shin; then the body's lean (degrees) and how far the figure is lifted (fraction of its size)
    private static final float[][] POSE = new float[Emotes.MOVES][];

    static {
        float[] stand = {12, 8, 12, 8, 10, 6, 10, 6, 0, 0};
        for (int m = 0; m < Emotes.MOVES; m++) POSE[m] = stand;
        POSE[Emotes.WAVE] = new float[]{12, 8, 145, 195, 10, 6, 10, 6, 0, 0};
        POSE[Emotes.JUMP] = new float[]{150, 175, 150, 175, 35, -25, 35, -25, 0, 0.1f};
        POSE[Emotes.APPLAUSE] = new float[]{40, -60, 40, -60, 10, 6, 10, 6, 0, 0};
        POSE[Emotes.LAUGH] = new float[]{35, -85, 35, -85, 12, 6, 12, 6, -14, 0};
        POSE[Emotes.DAB] = new float[]{125, 125, 95, 235, 12, 6, 12, 6, 6, 0};
        POSE[Emotes.FACEPALM] = new float[]{12, 8, 35, 165, 10, 6, 10, 6, 8, 0};
        POSE[Emotes.CRY] = new float[]{30, 172, 30, 172, 10, 6, 10, 6, 6, 0};
        POSE[Emotes.CHICKEN] = new float[]{75, -120, 75, -120, 25, -15, 25, -15, 0, 0};
        POSE[Emotes.AIR_GUITAR] = new float[]{65, 100, 30, -45, 25, 10, 25, 10, -10, 0};
        POSE[Emotes.HEAD_SPIN] = new float[]{90, 95, 90, 95, 10, 6, 10, 6, 0, 0};
        POSE[Emotes.CONFETTI] = new float[]{155, 170, 155, 170, 14, 8, 14, 8, 0, 0.04f};
        POSE[Emotes.SULK] = new float[]{35, -115, 35, -125, 10, 6, 10, 6, 0, 0};
    }

    /** The picture of a move, about s units across, centered at (cx, cy); color = the figure's color. */
    static void move(Game game, int m, float cx, float cy, float s, int color, float t) {
        UIBatch b = game.b;
        UI ui = game.ui;
        switch (m) {
            case Emotes.SIT_UPS: {
                // a dumbbell and a drop of sweat
                b.line(cx - s * 0.3f, cy + s * 0.08f, cx + s * 0.3f, cy - s * 0.08f, s * 0.08f, INK);
                for (int sd = -1; sd <= 1; sd += 2) {
                    float x = cx + sd * s * 0.3f, y = cy - sd * s * 0.08f;
                    b.shape(x, y, s * 0.16f, s * 0.42f, s * 0.06f, color, INK, s * 0.03f, 0.3f, 0, -0.26f);
                }
                b.shape(cx + s * 0.2f, cy - s * 0.32f, s * 0.12f, s * 0.12f, s * 0.02f, 0xFF6FC8FF, 0, 0, 0, 0, 0.785f);
                b.circle(cx + s * 0.2f, cy - s * 0.28f, s * 0.06f, 0xFF6FC8FF);
                return;
            }
            case Emotes.BACKFLIP: {
                // a curled-up figure in a looping arrow
                MatchScreen.ring(b, cx, cy, s * 0.36f, 0.8f, s * 0.07f, INK);
                float a = -(float) Math.PI / 2f + 0.8f * 6.2832f;
                float ax = cx + (float) Math.cos(a) * s * 0.36f, ay = cy + (float) Math.sin(a) * s * 0.36f;
                b.line(ax, ay, ax + s * 0.13f, ay - s * 0.02f, s * 0.07f, INK);
                b.line(ax, ay, ax + s * 0.02f, ay + s * 0.13f, s * 0.07f, INK);
                b.circle(cx - s * 0.06f, cy + s * 0.08f, s * 0.12f, color, INK, s * 0.03f);
                b.shape(cx + s * 0.06f, cy - s * 0.04f, s * 0.26f, s * 0.18f, s * 0.09f, color, INK, s * 0.03f, 0, 0, 0.6f);
                return;
            }
            case Emotes.WORM: {
                // a wiggly line along the floor with a head on it
                float px = cx - s * 0.32f, py = cy + s * 0.08f;
                for (int i = 1; i <= 8; i++) {
                    float x = cx - s * 0.32f + i * s * 0.07f, y = cy + s * 0.08f - (float) Math.abs(Math.sin(i * 0.9f + t * 4f)) * s * 0.14f;
                    b.line(px, py, x, y, s * 0.11f, INK);
                    px = x;
                    py = y;
                }
                px = cx - s * 0.32f;
                py = cy + s * 0.08f;
                for (int i = 1; i <= 8; i++) {
                    float x = cx - s * 0.32f + i * s * 0.07f, y = cy + s * 0.08f - (float) Math.abs(Math.sin(i * 0.9f + t * 4f)) * s * 0.14f;
                    b.line(px, py, x, y, s * 0.06f, color);
                    px = x;
                    py = y;
                }
                b.circle(cx + s * 0.32f, py - s * 0.04f, s * 0.11f, color, INK, s * 0.03f);
                b.line(cx - s * 0.4f, cy + s * 0.2f, cx + s * 0.44f, cy + s * 0.2f, s * 0.03f, 0x602A1840);
                return;
            }
            default:
                break;
        }
        figure(b, POSE[m], cx, cy, s, color, m == Emotes.FACEPALM || m == Emotes.SULK || m == Emotes.DAB ? 0.04f : 0f);
        float hy = cy - s * 0.25f - POSE[m][9] * s;
        switch (m) {
            case Emotes.WAVE:
                for (int i = 0; i < 2; i++) {
                    float x = cx + s * (0.36f + i * 0.08f);
                    b.line(x, hy - s * 0.2f, x + s * 0.03f, hy - s * 0.08f, s * 0.035f, INK);
                }
                break;
            case Emotes.JUMP:
                b.line(cx - s * 0.3f, cy + s * 0.4f, cx + s * 0.3f, cy + s * 0.4f, s * 0.03f, 0x602A1840);
                break;
            case Emotes.APPLAUSE:
                for (int i = -1; i <= 1; i++) {
                    float a = -1.5708f + i * 0.6f;
                    float x = cx + (float) Math.cos(a) * s * 0.14f, y = cy - s * 0.02f + (float) Math.sin(a) * s * 0.14f;
                    b.line(x, y, x + (float) Math.cos(a) * s * 0.08f, y + (float) Math.sin(a) * s * 0.08f, s * 0.03f, 0xFFFFB321);
                }
                break;
            case Emotes.LAUGH:
                b.text(b.title, "HA", cx + s * 0.3f, hy - s * 0.12f, s * 0.2f, INK, UIBatch.CENTER, 0, 0);
                break;
            case Emotes.CRY:
                for (int sd = -1; sd <= 1; sd += 2) {
                    float d = (t * 0.8f + (sd > 0 ? 0.5f : 0f)) % 1f;
                    b.circle(cx + sd * s * (0.18f + d * 0.08f), hy + s * (0.02f + d * 0.22f), s * 0.045f, 0xFF6FC8FF);
                }
                break;
            case Emotes.CHICKEN:
                b.shape(cx + s * 0.13f, hy + s * 0.02f, s * 0.1f, s * 0.05f, s * 0.02f, 0xFFFF9A2B, 0, 0, 0, 0, 0);
                b.circle(cx, hy - s * 0.15f, s * 0.04f, 0xFFFF3B4F);
                break;
            case Emotes.AIR_GUITAR:
                b.line(cx - s * 0.12f, cy + s * 0.08f, cx - s * 0.38f, cy - s * 0.1f, s * 0.04f, 0xFF8A5A30);
                b.shape(cx + s * 0.02f, cy + s * 0.12f, s * 0.24f, s * 0.17f, s * 0.08f, 0xFFFF3B4F, INK, s * 0.025f, 0.3f, 0, -0.5f);
                break;
            case Emotes.HEAD_SPIN:
                MatchScreen.ring(b, cx, hy, s * 0.2f, 0.75f, s * 0.035f, 0xFFB08CFF);
                break;
            case Emotes.CONFETTI:
                for (int i = 0; i < 7; i++) {
                    float a = i * 0.9f + t;
                    b.shape(cx + (float) Math.cos(a) * s * 0.35f, cy - s * 0.32f + (float) Math.sin(a * 1.3f) * s * 0.12f,
                            s * 0.06f, s * 0.06f, s * 0.01f, 0xFF000000 | Palette.TILE[i % Palette.TILE.length], 0, 0, 0, 0, a);
                }
                break;
            case Emotes.SULK:
                b.circle(cx - s * 0.06f, hy - s * 0.3f, s * 0.07f, 0xFF8A8098);
                b.circle(cx + s * 0.04f, hy - s * 0.32f, s * 0.09f, 0xFF8A8098);
                b.circle(cx + s * 0.13f, hy - s * 0.29f, s * 0.06f, 0xFF8A8098);
                break;
            case Emotes.DAB:
                ui.star(cx - s * 0.36f, hy - s * 0.18f, s * 0.07f, 0xFFFFE14D);
                break;
            default:
                break;
        }
    }

    /** The figure's color for a move (its word's color; white ones in blue so they show on light cards). */
    static int color(int m) {
        int c = Emotes.WORD_COLOR[m];
        return c == 0xFFFFFFFF ? 0xFF3BB8FF : c;
    }

    /** A car's signature emote: the car in its own colors with a star on it. */
    static void signature(Game game, int carId, int paint, int accent, float cx, float cy, float s, float t) {
        LockerScreen.carIcon(game.b, cx, cy + s * 0.04f, s * 0.95f, 0xFF000000 | Palette.paintColor(paint, t),
                0xFF000000 | Palette.paintColor(accent, t));
        game.ui.star(cx + s * 0.3f, cy - s * 0.3f, s * 0.15f, 0xFFFFE14D);
    }

    /** The stick figure: an outlined limb drawing (ink under, color on top). */
    private static void figure(UIBatch b, float[] p, float cx, float cy, float s, int color, float nod) {
        float lift = p[9] * s;
        float lean = p[8] * 0.01745f;
        float hipX = cx, hipY = cy + s * 0.1f - lift;
        float nx = hipX + (float) Math.sin(lean) * s * 0.24f, ny = hipY - (float) Math.cos(lean) * s * 0.24f;
        for (int pass = 0; pass < 2; pass++) {
            float w = pass == 0 ? s * 0.11f : s * 0.06f;
            int c = pass == 0 ? INK : color;
            b.line(hipX, hipY, nx, ny, w, c);
            float sx = nx, sy = ny + s * 0.03f;
            limb(b, sx, sy, -1, p[0], p[1], s * 0.17f, s * 0.15f, w, c);
            limb(b, sx, sy, 1, p[2], p[3], s * 0.17f, s * 0.15f, w, c);
            limb(b, hipX, hipY, -1, p[4], p[5], s * 0.17f, s * 0.16f, w, c);
            limb(b, hipX, hipY, 1, p[6], p[7], s * 0.17f, s * 0.16f, w, c);
        }
        float hx = nx + (float) Math.sin(lean) * s * 0.1f + nod * s, hy = ny - s * 0.1f + nod * s * 0.5f;
        b.circle(hx, hy, s * 0.11f, color, INK, s * 0.03f);
    }

    /** Upper and lower part of a limb from (x, y) on one side (-1 = screen left). */
    private static void limb(UIBatch b, float x, float y, int side, float a1, float a2, float l1, float l2, float w, int c) {
        float r1 = a1 * 0.01745f, r2 = a2 * 0.01745f;
        float ex = x + side * (float) Math.sin(r1) * l1, ey = y + (float) Math.cos(r1) * l1;
        float hx = ex + side * (float) Math.sin(r2) * l2, hy = ey + (float) Math.cos(r2) * l2;
        b.line(x, y, ex, ey, w, c);
        b.line(ex, ey, hx, hy, w, c);
    }
}
