package com.gogogo.game.game;

/**
 * Emotes: short, silly animations of a driver (and now and then its car). Every car has a signature emote of its own
 * (the EMOTE button in a match, bots taunting each other and the player), and there is a catalog of victory emotes
 * for the results screen that is unlocked like any other item (Items.EMOTE, see VICTORY).
 *
 * A move is one animation. Driver.play(move) starts it; the driver steps it itself (step(), from ride() and idle()):
 * every step the move sets pose overrides from its clock, eased in and out, and when it is over the springs take the
 * driver back. Moves work seated in a car (climbing half out of it where they need room) and standing on the floor.
 * Particles that go with a move come from fx(), the car's hops from car(). Nothing here allocates.
 */
public final class Emotes {
    private Emotes() {}

    // ---- moves: the signature emotes of the cars (in car order), the victory emotes, and the sulk of a loser
    public static final int RASPBERRY = 0, BEEP = 1, SPICY = 2, WIGGLE = 3, SCREAM = 4, FLEX = 5, POP_UP = 6, MOON = 7,
            COUCH = 8, FLAP = 9, DISCO = 10, SELFIE = 11, KISSES = 12, TRAFFIC_COP = 13,
            WAVE = 14, JUMP = 15, APPLAUSE = 16, LAUGH = 17, DAB = 18, FACEPALM = 19, CRY = 20, CHICKEN = 21,
            AIR_GUITAR = 22, SIT_UPS = 23, HEAD_SPIN = 24, BACKFLIP = 25, WORM = 26, CONFETTI = 27, SULK = 28;
    public static final int MOVES = 29;

    public static final String[] NAME = {
            "RASPBERRY", "BEEP BEEP", "TOO SPICY", "WIENER WIGGLE", "THE SCREAM", "FLEX", "POP UP", "MOON",
            "COUCH POTATO", "FLAP FLAP", "DISCO FEVER", "SELFIE", "BLOW KISSES", "TRAFFIC COP",
            "WAVE", "VICTORY JUMP", "APPLAUSE", "LAUGH", "DAB", "FACEPALM", "FAKE CRY", "CHICKEN DANCE",
            "AIR GUITAR", "SIT-UPS", "HEAD SPIN", "BACKFLIP", "THE WORM", "CONFETTI", "SULK"};

    /** The word that pops up over the driver (readable from far away) and its color. */
    public static final String[] WORD = {
            "PBBBT!", "BEEP BEEP!", "SPICY!", "WIGGLE WIGGLE!", "AAAAAH!", "FLEX!", "DING!", "NYAH NYAH!",
            "TOO EASY!", "FLAP FLAP!", "GROOVY!", "SAY CHEESE!", "MWAH!", "GO GO GO!",
            "HEY!", "YEAH!", "BRAVO!", "LOL!", "DAB!", "UGH!", "WAAAH!", "BAWK BAWK!",
            "SHRED!", "FEEL THE BURN!", "WHAT?!", "BACKFLIP!", "WORM IT!", "PARTY!", "HMPF!"};
    public static final int[] WORD_COLOR = {
            0xFFFF6F9A, 0xFFFFE14D, 0xFFFF6A3A, 0xFFFFC21F, 0xFF8FE4FF, 0xFFFF9A2B, 0xFFFFFFFF, 0xFFFF4FA3,
            0xFFB08CFF, 0xFFFFD23B, 0xFFFF6FD8, 0xFFFFE14D, 0xFFFF7EB6, 0xFF5EE65A,
            0xFFFFFFFF, 0xFFFFE14D, 0xFF5EE65A, 0xFFFFC21F, 0xFF3BE0FF, 0xFFB8B0C8, 0xFF6FC8FF, 0xFFFFD23B,
            0xFFFF6A3A, 0xFFFF9A2B, 0xFFB08CFF, 0xFF3BE0FF, 0xFFFF7EB6, 0xFFFF4FA3, 0xFFB8B0C8};

    /** The sound a move starts with (Sfx), its volume and pitch. */
    public static final int[] SOUND = {
            Sfx.RASPBERRY, Sfx.HONK, Sfx.WHOOSH, Sfx.TAUNT, Sfx.TAUNT, Sfx.TAUNT, Sfx.SPRING, Sfx.TAUNT,
            Sfx.TAUNT, Sfx.QUACK, Sfx.TAUNT, Sfx.CLICK, Sfx.POP, Sfx.BEEP,
            Sfx.POP, Sfx.SPRING, Sfx.POP, Sfx.TAUNT, Sfx.WHOOSH, Sfx.BUMP, Sfx.TAUNT, Sfx.TAUNT,
            Sfx.TAUNT, Sfx.POP, Sfx.WHOOSH, Sfx.WHOOSH, Sfx.POP, Sfx.POP, Sfx.NOPE};
    public static final float[] VOLUME = {
            1f, 0.9f, 0.8f, 0.8f, 0.8f, 0.8f, 0.8f, 0.9f, 0.7f, 1f, 0.8f, 1f, 0.9f, 0.7f,
            0.8f, 0.7f, 0.8f, 0.8f, 0.8f, 0.6f, 0.8f, 0.8f, 0.8f, 0.7f, 0.8f, 0.8f, 0.7f, 0.9f, 0.6f};
    public static final float[] PITCH = {
            1f, 1.1f, 0.7f, 1.25f, 1.45f, 0.85f, 1.2f, 1f, 0.8f, 1.1f, 1.15f, 0.7f, 1.4f, 1.8f,
            1.2f, 1.1f, 1.3f, 1.35f, 1.4f, 0.8f, 0.75f, 1.2f, 0.9f, 0.9f, 0.9f, 0.8f, 0.9f, 1.5f, 0.8f};

    /** How long each move plays (seconds). */
    public static final float[] DUR = {
            1.9f, 2.0f, 2.1f, 2.3f, 2.0f, 2.4f, 2.0f, 2.5f, 2.6f, 2.1f, 2.6f, 2.3f, 2.4f, 2.5f,
            2.0f, 2.0f, 2.1f, 2.3f, 1.9f, 2.1f, 2.5f, 2.6f, 2.6f, 3.0f, 2.0f, 2.0f, 3.0f, 2.4f, 2.4f};

    /** Every car's signature emote, by CarDef id. */
    private static final int[] SIGNATURE = {
            RASPBERRY, BEEP, SPICY, WIGGLE, SCREAM, FLEX, POP_UP, MOON, COUCH, FLAP, DISCO, SELFIE, KISSES, TRAFFIC_COP};

    public static int signature(int carId) {
        return SIGNATURE[carId % SIGNATURE.length];
    }

    // ---- the victory emotes: unlockable, shown on the results screen (index = Items.EMOTE item; never reorder,
    // saves keep what is owned by position)
    private static final int F = Items.FREE, L = Items.LEVEL, C = Items.COINS, R = Items.RIMS;
    public static final int[] VICTORY = {
            WAVE, JUMP, APPLAUSE, LAUGH, DAB, FACEPALM, CRY, CHICKEN, AIR_GUITAR, SIT_UPS, HEAD_SPIN, BACKFLIP, WORM, CONFETTI};
    public static final int COUNT = VICTORY.length;
    public static final int[] RULE = {F, F, F, L, C, C, C, L, L, C, R, R, R, R};
    /** The level (LEVEL) or the price (COINS / RIMS) that goes with RULE. */
    public static final int[] VALUE = {0, 0, 0, 8, 350, 300, 450, 14, 26, 600, 40, 60, 80, 100};

    public static String victoryName(int i) {
        return NAME[VICTORY[i]];
    }

    // ------------------------------------------------------------------ stepping

    private static final float PI = (float) Math.PI, TAU = 2f * PI;

    /** One step of the driver's emote (from Driver.ride / idle, before the pose targets are blended). */
    static void step(Driver d, float dt) {
        int m = d.emote;
        d.emoteT += dt;
        float t = d.emoteT, dur = DUR[m];
        if (t >= dur) {
            d.stopEmote();
            return;
        }
        // eased in quickly, out a little slower
        float w = Math.max(0f, Math.min(1f, Math.min(t / 0.16f, (dur - t) / 0.3f)));
        d.stiffness = 1.7f;
        pose(d, m, t, dur, w);
    }

    /** Sets the pose overrides of move m at time t (w = blend weight of the move as a whole). */
    private static void pose(Driver d, int m, float t, float dur, float w) {
        boolean seat = d.mode == Driver.SEATED;
        switch (m) {
            case RASPBERRY: {
                // head out towards them, thumbs in the ears, tongue out
                stand(d, 0.45f, w);
                face(d, 0f, w);
                float wig = (float) Math.sin(t * 17f) * 0.18f;
                arms(d, 2.25f, 1.05f + wig, 2.3f, w);
                d.pose(Driver.LEAN, 0.25f, w);
                d.pose(Driver.REACH, 0.2f, w);
                d.pose(Driver.NOD, -0.5f, w);
                d.pose(Driver.TILT, (float) Math.sin(t * 9f) * 0.3f, w);
                d.face = Outfits.FC_SILLY;
                break;
            }
            case BEEP: {
                // honk honk: waves its thing (or a flag) round over its head, fist pumping
                stand(d, 0.35f, w);
                face(d, 0f, w * 0.7f);
                if (d.look[Outfits.HELD] == Outfits.HD_NONE) d.prop = Outfits.HD_FLAG;
                float s = (float) Math.sin(t * 11f);
                arm(d, false, 2.8f, 0.4f + 0.5f * s, 0.35f + 0.3f * (float) Math.sin(t * 11f + 1f), w);
                arm(d, true, 1.1f + 1.3f * Math.max(0f, (float) Math.sin(t * 7f)), 0.3f, 1.3f, w);
                d.pose(Driver.NOD, -0.2f, w);
                d.pose(Driver.SIDE, s * 0.15f, w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case SPICY: {
                // fanning its burning mouth, hopping on the spot
                stand(d, 0.3f, w);
                face(d, 0f, w);
                arm(d, false, 1.35f, -0.15f + 0.45f * (float) Math.sin(t * 19f), 2.1f, w);
                arm(d, true, 0.7f, -0.1f, 1.6f, w);
                hop(d, 0.3f, 0.15f * Math.abs((float) Math.sin(t * 9f)), w);
                d.pose(Driver.NOD, -0.35f, w);
                d.pose(Driver.STRETCH, 0.1f * (float) Math.sin(t * 18f), w);
                d.face = Outfits.FC_SHOCKED;
                break;
            }
            case WIGGLE: {
                // a wobbly noodle from head to toe
                stand(d, 0.4f, w);
                face(d, 0f, w);
                float s = (float) Math.sin(t * 7f), c = (float) Math.sin(t * 7f + 1.5f);
                d.pose(Driver.SIDE, 0.6f * s, w);
                d.pose(Driver.SHIFT, -0.3f * s, w);
                d.pose(Driver.TILT, -0.5f * s, w);
                d.pose(Driver.TWIST, 0.3f * (float) Math.sin(t * 3.5f), w);
                arm(d, true, 1.5f + 0.6f * s, 0.9f + 0.4f * c, 1f + 0.8f * c, w);
                arm(d, false, 1.5f - 0.6f * s, 0.9f - 0.4f * c, 1f - 0.8f * c, w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case SCREAM: {
                // hands on the cheeks, mouth wide open, shaking
                stand(d, 0.5f, w);
                face(d, 0f, w);
                arms(d, 1.15f, 0.05f, 2.55f, w);
                d.pose(Driver.LOOK, 0.22f * (float) Math.sin(t * 31f), w);
                d.pose(Driver.TILT, 0.15f * (float) Math.sin(t * 23f), w);
                d.pose(Driver.STRETCH, 0.25f, w);
                d.pose(Driver.LEAN, -0.2f, w);
                d.face = Outfits.FC_SHOCKED;
                break;
            }
            case FLEX: {
                // up on its feet: double biceps, then the crab
                stand(d, 0.85f, w);
                face(d, 0f, w);
                float pump = (float) Math.sin(t * 8f);
                if (t < 1.25f) {
                    arms(d, 1.57f, 1.45f, 1.8f + 0.4f * pump, w);
                    d.pose(Driver.LEAN, -0.1f, w);
                    d.pose(Driver.NOD, -0.15f, w);
                } else {
                    arms(d, 0.9f, 0.45f, 1.3f + 0.2f * pump, w);
                    d.pose(Driver.LEAN, 0.35f, w);
                    d.pose(Driver.NOD, 0.1f, w);
                }
                d.pose(Driver.STRETCH, 0.1f + 0.05f * pump, w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case POP_UP: {
                // like toast: crouch, shoot up out of the car with a flip, drop back in
                d.loose = true;
                d.stiffness = 3f;
                float h = seat ? 1.7f : 1.3f;
                if (t < 0.25f) {
                    d.pose(Driver.RISE, seat ? -0.15f : -0.12f, w);
                    knees(d, 0.8f, w);
                    arms(d, 0.4f, 0.2f, 0.8f, w);
                    d.pose(Driver.FLIP, 0f, 1f);
                } else if (t < 1.45f) {
                    float u = (t - 0.25f) / 1.2f;
                    d.pose(Driver.RISE, h * 4f * u * (1f - u), 1f);
                    d.pose(Driver.FLIP, -TAU * smooth(u), 1f);
                    arms(d, 2.8f, 0.5f, 0.3f, 1f);
                    knees(d, 1.2f * (float) Math.sin(u * PI), 1f);
                } else {
                    d.pose(Driver.RISE, 0f, w);
                    d.pose(Driver.FLIP, -TAU, 1f);
                    arms(d, 2.6f, 0.9f, 0.4f, w);
                }
                d.face = Outfits.FC_STARS;
                break;
            }
            case MOON: {
                // up, back to them, bent over, wiggling (cartoon pants stay on), grinning over its shoulder
                stand(d, 0.8f, w);
                face(d, PI, w);
                float s = (float) Math.sin(t * 10f);
                d.pose(Driver.LEAN, 1.35f, w);
                // the hips pushed out towards them, wiggling
                float ax = (float) Math.sin(d.aim), az = (float) Math.cos(d.aim);
                d.pose(Driver.SHIFT, 0.3f * ax + 0.12f * s * az, w);
                d.pose(Driver.REACH, 0.3f * az - 0.12f * s * ax, w);
                d.pose(Driver.SIDE, 0.3f * s, w);
                d.pose(Driver.STRETCH, -0.1f, w);
                d.pose(Driver.LOOK, 1.5f * side(d), w);
                d.pose(Driver.NOD, -0.6f, w);
                arm(d, false, -0.8f + 0.5f * s, 0.35f, 0.5f, w);
                arm(d, true, 0.7f, 0.9f + 0.3f * s, 0.4f, w);
                legs(d, 0.05f, 0.15f, 0.1f, w);
                d.face = Outfits.FC_SILLY;
                break;
            }
            case COUCH: {
                // lounging, legs crossed, hands behind the head, a smug look at them (in a car: up on the backrest,
                // turned round to them)
                d.pose(Driver.LEAN, -0.35f, w);
                arms(d, 2.7f, 1f, 2.5f, w);
                if (seat) {
                    // (up and back: sat on top of the backrest)
                    d.pose(Driver.RISE, 0.8f, w);
                    d.pose(Driver.REACH, -0.5f, w);
                    face(d, 0f, w);
                    d.pose(Driver.L_LEG, 1.45f, w);
                    d.pose(Driver.R_LEG, 1.7f, w);
                    d.pose(Driver.L_LEG_OUT, 0.15f, w);
                    d.pose(Driver.R_LEG_OUT, -0.25f, w);
                    knees(d, 0.9f, w);
                } else {
                    d.pose(Driver.R_LEG, 0.5f, w);
                    d.pose(Driver.R_KNEE, 0.9f, w);
                    d.pose(Driver.SHIFT, 0.08f, w);
                    d.pose(Driver.LOOK, clamp(d.aim, 1.4f) * 0.8f, w);
                }
                d.pose(Driver.NOD, -0.1f + 0.05f * (float) Math.sin(t * 2f), w);
                d.face = t < 1.2f ? Outfits.FC_GRIN : Outfits.FC_WINK;
                break;
            }
            case FLAP: {
                // flapping its elbows, head bobbing back and forth
                stand(d, 0.4f, w);
                face(d, 0f, w * 0.6f);
                float s = (float) Math.sin(t * 12f);
                arms(d, 0.2f, 0.35f + 0.9f * Math.abs(s), 1.4f, w);
                d.pose(Driver.REACH, 0.16f * s, w);
                d.pose(Driver.NOD, 0.25f * (float) Math.sin(t * 12f + 1f), w);
                d.pose(Driver.SIDE, 0.15f * (float) Math.sin(t * 6f), w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case DISCO: {
                // point up, point down, hips swinging, shades on
                stand(d, 0.6f, w);
                face(d, 0f, w);
                float u = 0.5f + 0.5f * (float) Math.sin(t * 5f);
                arm(d, false, lerp(0.35f, 2.8f, u), lerp(-0.5f, 0.7f, u), 0.05f, w);
                arm(d, true, 0.3f, 0.7f, 1.8f, w);
                float s = (float) Math.sin(t * 5f);
                d.pose(Driver.SHIFT, 0.2f * s, w);
                d.pose(Driver.SIDE, -0.25f * s, w);
                d.pose(Driver.TILT, 0.2f * s, w);
                d.pose(Driver.NOD, -0.35f * u + 0.15f, w);
                d.face = Outfits.FC_SHADES;
                break;
            }
            case SELFIE: {
                // phone up high, head tilted, peace sign... flash!
                stand(d, 0.5f, w);
                face(d, 0f, w);
                d.prop = Outfits.HD_PHONE;
                arm(d, false, 2.1f, 0.95f, 0.15f, w);
                arm(d, true, 2.5f, 0.3f, 1.5f, w);
                d.pose(Driver.TILT, -0.4f, w);
                d.pose(Driver.LOOK, -0.6f, w);
                d.pose(Driver.NOD, -0.2f, w);
                d.face = t > 1.25f && t < 1.7f ? Outfits.FC_WINK : Outfits.FC_GRIN;
                break;
            }
            case KISSES: {
                // hand to the lips, then thrown out at them: right, then left
                stand(d, 0.45f, w);
                face(d, 0f, w);
                float k = t % 1.1f;
                boolean right = t < 1.1f;
                float out = smooth(Math.max(0f, Math.min(1f, (k - 0.35f) / 0.25f)));
                arm(d, !right, lerp(1.3f, 2.1f, out), lerp(-0.25f, 0.8f, out), lerp(2.4f, 0.2f, out), w);
                arm(d, right, 0.3f, 0.3f, 0.5f, w);
                d.pose(Driver.LEAN, 0.25f * out, w);
                d.pose(Driver.TILT, right ? 0.2f : -0.2f, w);
                d.face = Outfits.FC_WINK;
                break;
            }
            case TRAFFIC_COP: {
                // STOP! ...then waves everyone on: GO GO GO!
                stand(d, 0.85f, w);
                face(d, 0f, w);
                if (t < 1f) {
                    arm(d, false, 1.57f, 0f, 0f, w);
                    arm(d, true, 0.3f, 0.75f, 1.8f, w);
                    d.pose(Driver.LEAN, 0.1f, w);
                    d.face = Outfits.FC_GRUMPY;
                } else {
                    float s = (float) Math.sin(t * 11f);
                    arms(d, 1.3f + 0.9f * s, 0.9f, 0.5f, w);
                    hop(d, 0.85f, 0.12f * Math.abs(s), w);
                    d.face = Outfits.FC_GRIN;
                }
                break;
            }
            case WAVE: {
                face(d, 0f, w);
                float s = (float) Math.sin(t * 10f);
                arm(d, false, 2.7f, 0.55f + 0.35f * s, 0.5f + 0.35f * (float) Math.sin(t * 10f + 0.5f), w);
                d.pose(Driver.TILT, 0.15f * (float) Math.sin(t * 5f), w);
                d.pose(Driver.SIDE, -0.1f, w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case JUMP: {
                // two big jumps with a fist pump each
                face(d, 0f, w);
                float u = t % 1f;
                if (u < 0.22f) {
                    hop(d, 0f, -0.15f, w);
                    knees(d, 0.9f, w);
                    arms(d, 0.4f, 0.3f, 1.2f, w);
                } else if (u < 0.82f) {
                    float a = (u - 0.22f) / 0.6f;
                    hop(d, 0f, (seat ? 0.5f : 0.95f) * (float) Math.sin(a * PI), 1f);
                    knees(d, 0.7f * (float) Math.sin(a * PI), w);
                    arms(d, 2.9f, 0.45f, 0.3f, w);
                } else {
                    hop(d, 0f, -0.1f, w);
                    knees(d, 0.6f, w);
                    arms(d, 2.4f, 0.6f, 0.5f, w);
                }
                d.face = Outfits.FC_GRIN;
                break;
            }
            case APPLAUSE: {
                face(d, 0f, w);
                float s = (float) Math.sin(t * 19f);
                arms(d, 1.65f, -0.05f + 0.3f * s, 0.75f, w);
                d.pose(Driver.LEAN, 0.1f, w);
                d.pose(Driver.NOD, 0.1f * (float) Math.sin(t * 9.5f), w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case LAUGH: {
                face(d, 0f, w);
                float s = (float) Math.sin(t * 14f);
                if (t < 1.2f) {
                    // holding its belly, rocking back
                    d.pose(Driver.LEAN, -0.35f + 0.15f * s, w);
                    arms(d, 0.6f, -0.15f, 1.7f, w);
                    d.pose(Driver.NOD, -0.4f, w);
                } else {
                    // doubled over, slapping its knee
                    d.pose(Driver.LEAN, 0.6f + 0.1f * s, w);
                    arm(d, false, 0.5f + 0.45f * (float) Math.sin(t * 12f), 0.2f, 0.4f, w);
                    arm(d, true, 0.6f, -0.15f, 1.7f, w);
                    d.pose(Driver.NOD, -0.1f, w);
                }
                hop(d, 0f, 0.06f * Math.abs(s), w);
                d.pose(Driver.STRETCH, 0.06f * s, w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case DAB: {
                // dab to one side, then the other
                face(d, 0f, w);
                float k = t < 0.95f ? smooth(Math.min(1f, t / 0.18f)) : 1f;
                boolean flip = t >= 0.95f;
                float sd = flip ? -1f : 1f;
                arm(d, flip, 2.35f, 1f, 0.05f, w * k);
                arm(d, !flip, 1.75f, -0.3f, 2.3f, w * k);
                d.pose(Driver.NOD, 0.55f, w * k);
                d.pose(Driver.LOOK, 0.5f * sd, w * k);
                d.pose(Driver.SIDE, -0.15f * sd, w * k);
                d.face = Outfits.FC_SHADES;
                break;
            }
            case FACEPALM: {
                face(d, 0f, w);
                arm(d, false, 1.35f, -0.25f, 2.6f, w);
                d.pose(Driver.NOD, 0.45f, w);
                d.pose(Driver.LEAN, 0.15f, w);
                if (t > 0.6f) d.pose(Driver.LOOK, 0.25f * (float) Math.sin(t * 7f), w);
                d.face = Outfits.FC_GRUMPY;
                break;
            }
            case CRY: {
                // hands on the eyes, sobbing (the tears come from fx)
                face(d, 0f, w);
                float s = (float) Math.sin(t * 12f);
                arms(d, 1.2f, -0.1f, 2.6f, w);
                d.pose(Driver.STRETCH, 0.06f * s, w);
                d.pose(Driver.NOD, 0.2f + 0.1f * s, w);
                d.pose(Driver.LEAN, 0.2f, w);
                if (!seat) knees(d, 0.3f, w);
                d.face = Outfits.FC_SHOCKED;
                break;
            }
            case CHICKEN: {
                face(d, 0f, w);
                float s = (float) Math.sin(t * 13f);
                arms(d, 0.15f, 0.4f + 0.8f * Math.abs(s), 2.6f, w);
                d.pose(Driver.REACH, 0.18f * s, w);
                d.pose(Driver.NOD, 0.3f * s, w);
                d.pose(Driver.SIDE, 0.12f * (float) Math.sin(t * 6.5f), w);
                if (!seat) {
                    float k = (float) Math.sin(t * 6.5f);
                    hop(d, 0f, -0.08f * Math.abs(k), w);
                    d.pose(Driver.L_KNEE, 0.5f * Math.max(0f, k), w);
                    d.pose(Driver.R_KNEE, 0.5f * Math.max(0f, -k), w);
                }
                d.face = Outfits.FC_SILLY;
                break;
            }
            case AIR_GUITAR: {
                face(d, 0f, w);
                arm(d, true, 0.9f, 0.75f, 0.7f, w);
                arm(d, false, 0.55f, -0.05f, 1.5f + 0.5f * (float) Math.sin(t * 22f), w);
                d.pose(Driver.LEAN, -0.3f, w);
                d.pose(Driver.NOD, 0.45f * (float) Math.sin(t * 9f), w);
                if (!seat) {
                    legs(d, 0f, 0.35f, 0.35f, w);
                    hop(d, 0f, -0.08f, w);
                }
                d.face = Outfits.FC_SILLY;
                break;
            }
            case SIT_UPS: {
                // down on its back (side on to them), three sit-ups, back up
                float down = smooth(Math.min(1f, t / 0.4f)) * smooth(Math.min(1f, (dur - t) / 0.4f));
                face(d, PI / 2f, w);
                d.pose(Driver.FLIP, -1.45f * down, 1f);
                hop(d, 0f, -0.45f * down, 1f);
                d.pose(Driver.L_LEG, 1f * down, 1f);
                d.pose(Driver.R_LEG, 1f * down, 1f);
                knees(d, 1.6f * down, 1f);
                arms(d, 2.6f, 0.9f, 2.4f, w);
                float rep = t > 0.4f && t < dur - 0.4f ? Math.abs((float) Math.sin((t - 0.4f) * PI * 3f / (dur - 0.8f))) : 0f;
                d.pose(Driver.LEAN, 1.3f * rep * down, 1f);
                d.face = rep > 0.7f ? Outfits.FC_GRIN : Outfits.FC_GRUMPY;
                break;
            }
            case HEAD_SPIN: {
                // the head goes round, twice
                d.loose = true;
                d.stiffness = 3f;
                face(d, 0f, w);
                d.pose(Driver.LOOK, 2f * TAU * smooth(Math.min(1f, t / (dur - 0.25f))), 1f);
                arms(d, 0.6f, 1.2f, 0.3f, w);
                d.pose(Driver.STRETCH, 0.1f, w);
                d.face = t < dur - 0.6f ? Outfits.FC_SHOCKED : Outfits.FC_STARS;
                break;
            }
            case BACKFLIP: {
                d.loose = true;
                d.stiffness = 3f;
                face(d, 0f, w);
                if (t < 0.3f) {
                    hop(d, 0f, -0.2f, w);
                    knees(d, 1f, w);
                    arms(d, -0.6f, 0.2f, 0.4f, w);
                    d.pose(Driver.FLIP, 0f, 1f);
                } else if (t < 1.2f) {
                    float u = (t - 0.3f) / 0.9f;
                    hop(d, 0f, (seat ? 1.4f : 1.5f) * (float) Math.sin(u * PI), 1f);
                    d.pose(Driver.FLIP, -TAU * smooth(u), 1f);
                    float tuck = (float) Math.sin(u * PI);
                    knees(d, 1.7f * tuck, 1f);
                    d.pose(Driver.L_LEG, 1.3f * tuck, 1f);
                    d.pose(Driver.R_LEG, 1.3f * tuck, 1f);
                    arms(d, 2.8f, 0.4f, 0.4f, 1f);
                } else {
                    // stuck the landing: ta-da!
                    d.pose(Driver.FLIP, -TAU, 1f);
                    hop(d, 0f, t < 1.45f ? -0.15f : 0f, w);
                    knees(d, t < 1.45f ? 0.8f : 0f, w);
                    arms(d, 2.6f, 0.9f, 0.3f, w);
                }
                d.face = Outfits.FC_GRIN;
                break;
            }
            case WORM: {
                // dives down on its belly (side on to them), wriggles forward like a worm, pops back up
                float down = smooth(Math.min(1f, t / 0.4f)) * smooth(Math.min(1f, (dur - t) / 0.45f));
                face(d, PI / 2f, w);
                d.pose(Driver.FLIP, 1.45f * down, 1f);
                float ph = t * 9f;
                float lift = Math.max(0f, (float) Math.sin(ph));
                hop(d, 0f, -0.42f * down + 0.12f * Math.max(0f, (float) Math.sin(ph - 0.9f)) * down, 1f);
                d.pose(Driver.LEAN, -0.6f * lift * down, 1f);
                d.pose(Driver.NOD, -0.4f * down, 1f);
                d.pose(Driver.L_LEG, -0.6f * Math.max(0f, (float) Math.sin(ph - 1.8f)) * down, 1f);
                d.pose(Driver.R_LEG, -0.6f * Math.max(0f, (float) Math.sin(ph - 1.8f)) * down, 1f);
                float go = 0.5f * smooth(Math.min(1f, t / dur)) * down, turn = d.pos[Driver.TURN];
                d.pose(Driver.SHIFT, go * (float) Math.sin(turn), 1f);
                d.pose(Driver.REACH, go * (float) Math.cos(turn), 1f);
                arms(d, down > 0.5f ? 0.3f : 2.6f, 0.35f, 0.4f, w);
                d.face = Outfits.FC_GRIN;
                break;
            }
            case CONFETTI: {
                // gathers, then throws it all up in the air (twice)
                face(d, 0f, w);
                float u = t % 1.2f;
                if (u < 0.45f) {
                    arms(d, 0.4f, -0.2f, 0.8f, w);
                    d.pose(Driver.LEAN, 0.3f, w);
                    knees(d, seat ? 0f : 0.5f, w);
                    hop(d, 0f, -0.08f, w);
                } else {
                    arms(d, 3f, 0.7f, 0.1f, w);
                    d.pose(Driver.LEAN, -0.2f, w);
                    hop(d, 0f, 0.25f * (float) Math.sin(Math.min(1f, (u - 0.45f) / 0.4f) * PI), w);
                    knees(d, 0f, w);
                }
                d.face = Outfits.FC_STARS;
                break;
            }
            default: {
                // SULK: arms crossed, looking away, kicking at the floor
                arms(d, 1.05f, -0.38f, 2.2f, w);
                d.pose(Driver.TURN, clamp(d.aim, 3f) * 0.3f + 0.5f, w);
                d.pose(Driver.LOOK, 0.9f, w);
                d.pose(Driver.NOD, 0.3f, w);
                if (!seat) {
                    d.pose(Driver.R_LEG, 0.5f * Math.max(0f, (float) Math.sin(t * 5f)), w);
                    d.pose(Driver.R_KNEE, 0.3f, w);
                }
                d.face = Outfits.FC_GRUMPY;
                break;
            }
        }
    }

    // ------------------------------------------------------------------ the car's part

    /** The car joins in (hops and squashes on the beats of its driver's emote), once per step after Driver.ride. */
    static void car(Car c, float dt) {
        Driver d = c.driver;
        if (c.airborne || c.falling || c.frozenT > 0) return;
        float t = d.emoteT, t0 = t - dt;
        switch (d.emote) {
            case BEEP:
                if (at(t0, t, 0.05f) || at(t0, t, 0.5f)) bounce(c, 5f);
                break;
            case SPICY:
                if (at(t0, t, 0.6f) || at(t0, t, 1.3f)) bounce(c, 3.5f);
                break;
            case FLEX:
                if (at(t0, t, 0.4f) || at(t0, t, 0.8f) || at(t0, t, 1.6f) || at(t0, t, 2f)) c.squashV -= 5f;
                break;
            case POP_UP:
                if (at(t0, t, 0.25f)) c.squashV += 8f;
                if (at(t0, t, 1.45f)) c.squashV -= 9f;
                break;
            case DISCO:
                if ((int) (t * 1.6f) != (int) (t0 * 1.6f)) c.squashV -= 3.5f;
                break;
            case TRAFFIC_COP:
                if (at(t0, t, 1f)) bounce(c, 4f);
                break;
            case LAUGH:
                if ((int) (t * 3f) != (int) (t0 * 3f)) bounce(c, 2.5f);
                break;
            default:
                break;
        }
    }

    private static void bounce(Car c, float v) {
        if (c.hop > 0f) return;
        c.hopV = v;
        c.hop = 0.001f;
        c.squashV -= v * 0.5f;
    }

    /** True if x lies in (t0, t]. */
    private static boolean at(float t0, float t, float x) {
        return t0 < x && t >= x;
    }

    // ------------------------------------------------------------------ particles

    private static final int SPIT = 0xE8F8FF, TEAR = 0x6FC8FF, BUBBLE = 0xE8FCFF, FEATHER = 0xFFF4C8, ICE = 0xBDEBFF,
            HEART = 0xFF5A9A, GLITTER = 0xFFD6F0, SMOKE = 0x8A8098;
    private static final int[] DISCO_COLORS = {0xFF4FA3, 0xFFD23B, 0x3BE0FF, 0xA46BFF};

    /**
     * Particles that go with a driver's emote, once per frame: (hx, hy, hz) is the middle of its head in the world,
     * yaw the way its body faces (world yaw) and s its size.
     */
    public static void fx(Particles fx, Driver d, float hx, float hy, float hz, float yaw, float s) {
        int m = d.emote;
        if (m < 0) return;
        float t = d.emoteT, t0 = d.fxT;
        d.fxT = t;
        if (t0 > t) t0 = t;
        float dt = Math.min(0.1f, t - t0);
        t0 = t - dt;
        float fx0 = (float) Math.sin(yaw), fz0 = (float) Math.cos(yaw); // ahead of it
        float lx = fz0, lz = -fx0;                                       // its left
        float mx = hx + fx0 * 0.36f * s, my = hy - 0.08f * s, mz = hz + fz0 * 0.36f * s; // its mouth
        switch (m) {
            case RASPBERRY:
                for (int n = count(30f, dt); n > 0; n--) {
                    int i = fx.spawn(Particles.BALL, mx, my, mz, fx0 * 4f + rnd(1.2f), 1.5f + rnd(1f), fz0 * 4f + rnd(1.2f),
                            (0.05f + 0.04f * (float) Math.random()) * s, 0.4f, SPIT, 9f);
                    fx.drag(i, 1f);
                }
                break;
            case SPICY:
                for (int n = count(35f, dt); n > 0; n--) {
                    int i = fx.spawn(Particles.BALL, mx, my, mz, fx0 * 5f + rnd(1f), 1f + rnd(0.6f), fz0 * 5f + rnd(1f),
                            (0.14f + 0.14f * (float) Math.random()) * s, 0.35f, MatchView.NITRO_CORE, -1f);
                    fx.fade(i, MatchView.NITRO_EMBER);
                    fx.glow(i, 0.15f);
                    fx.drag(i, 3f);
                }
                break;
            case SCREAM:
                if (at(t0, t, 0.02f)) for (int k = 0; k < 10; k++) fx.sparkle(hx + rnd(0.9f * s), hy + rnd(0.7f * s), hz + rnd(0.9f * s), 0.4f * s, ICE);
                if (count(6f, dt) > 0) fx.sparkle(hx + rnd(0.8f * s), hy + rnd(0.6f * s), hz + rnd(0.8f * s), 0.3f * s, ICE);
                break;
            case FLEX:
            case AIR_GUITAR:
                if (count(5f, dt) > 0) {
                    float sd = Math.random() < 0.5 ? 1f : -1f;
                    fx.sparkle(hx + lx * sd * 0.6f * s, hy - 0.1f * s, hz + lz * sd * 0.6f * s, 0.35f * s, 0xFFF0A0);
                }
                break;
            case POP_UP:
                if (at(t0, t, 0.25f)) {
                    dust(fx, hx, hy - 0.9f * s, hz, 8, s, 0xD8D0E0);
                    for (int k = -1; k <= 1; k += 2) {
                        int i = fx.spawn(Particles.CUBE, hx + lx * k * 0.3f * s, hy - 0.5f * s, hz + lz * k * 0.3f * s, lx * k * 2f,
                                9f, lz * k * 2f, 0.32f * s, 1.2f, 0xE8A65A, 18f);
                        fx.drag(i, 0.5f);
                    }
                }
                break;
            case MOON:
                // soap bubbles floating up round the tub (not in front of the joke)
                for (int n = count(5f, dt); n > 0; n--) {
                    float a = (float) Math.random() * TAU;
                    int i = fx.spawn(Particles.BUBBLE, hx + (float) Math.cos(a) * 1.1f * s, hy - 1.2f * s, hz + (float) Math.sin(a) * 1.1f * s,
                            rnd(0.3f), 0.8f + 0.6f * (float) Math.random(), rnd(0.3f), (0.09f + 0.09f * (float) Math.random()) * s, 1.2f,
                            BUBBLE, -0.4f);
                    fx.drag(i, 1.5f);
                }
                break;
            case COUCH:
                for (int n = count(2.5f, dt); n > 0; n--) {
                    int i = fx.spawn(Particles.BUBBLE, hx + rnd(0.2f * s), hy + 0.5f * s, hz + rnd(0.2f * s), rnd(0.4f),
                            1.2f + (float) Math.random(), rnd(0.4f), (0.14f + 0.14f * (float) Math.random()) * s, 1.1f, BUBBLE, -0.5f);
                    fx.drag(i, 1.5f);
                }
                break;
            case FLAP:
            case CHICKEN:
                for (int n = count(7f, dt); n > 0; n--) {
                    float sd = Math.random() < 0.5 ? 1f : -1f;
                    int i = fx.spawn(Particles.CUBE, hx + lx * sd * 0.5f * s, hy - 0.4f * s, hz + lz * sd * 0.5f * s,
                            lx * sd * 2f + rnd(1f), 1.5f + rnd(1f), lz * sd * 2f + rnd(1f), 0.08f * s, 0.9f,
                            Math.random() < 0.5 ? FEATHER : 0xFFFFFF, 2f);
                    fx.drag(i, 3f);
                }
                break;
            case DISCO:
                for (int n = count(12f, dt); n > 0; n--) {
                    fx.sparkle(hx + rnd(1.3f * s), hy + rnd(0.9f * s), hz + rnd(1.3f * s), 0.3f * s,
                            DISCO_COLORS[(int) (Math.random() * DISCO_COLORS.length)]);
                }
                break;
            case SELFIE:
                if (at(t0, t, 1.25f)) {
                    // the flash, up at the phone
                    float px = hx - lx * 0.45f * s, py = hy + 0.5f * s, pz = hz - lz * 0.45f * s;
                    for (int k = 0; k < 6; k++) fx.sparkle(px + rnd(0.3f * s), py + rnd(0.3f * s), pz + rnd(0.3f * s), 0.5f * s, 0xFFFFFF);
                    int i = fx.spawn(Particles.BALL, px, py, pz, 0f, 0f, 0f, 0.3f * s, 0.18f, 0xFFFFFF, 0f);
                    fx.glow(i, 1f);
                }
                break;
            case KISSES:
                if (at(t0, t, 0.6f) || at(t0, t, 1.7f)) {
                    for (int k = 0; k < 3; k++) {
                        int i = fx.spawn(Particles.HEART, mx, my, mz, fx0 * 3f + rnd(1f), 2f + rnd(0.8f), fz0 * 3f + rnd(1f),
                                0.28f * s, 1.2f, HEART, -0.5f);
                        fx.drag(i, 1.5f);
                        fx.glow(i, 0.15f);
                    }
                }
                if (count(4f, dt) > 0) fx.sparkle(hx + rnd(0.8f * s), hy + rnd(0.6f * s), hz + rnd(0.8f * s), 0.3f * s, GLITTER);
                break;
            case TRAFFIC_COP:
                if (at(t0, t, 1f)) fx.stars(hx, hy + 0.3f * s, hz, 4);
                break;
            case JUMP:
                if (at(t0, t, 0.22f) || at(t0, t, 1.22f)) dust(fx, hx, feet(d, hy, s), hz, 5, s, 0xFFFFFF);
                break;
            case APPLAUSE:
                if (count(6f, dt) > 0) fx.sparkle(mx + fx0 * 0.2f * s + rnd(0.2f * s), my - 0.25f * s, mz + fz0 * 0.2f * s + rnd(0.2f * s), 0.3f * s, 0xFFF0A0);
                break;
            case DAB:
                if (at(t0, t, 0.18f) || at(t0, t, 1.05f)) {
                    float sd = t < 0.9f ? -1f : 1f;
                    for (int k = 0; k < 5; k++) fx.sparkle(hx + lx * sd * 0.8f * s + rnd(0.3f), hy + 0.5f * s + rnd(0.3f), hz + lz * sd * 0.8f * s + rnd(0.3f), 0.4f * s, 0x8FE4FF);
                }
                break;
            case FACEPALM:
                if (at(t0, t, 0.25f)) fx.bonk(mx, hy + 0.2f * s, mz, 0.6f * s);
                break;
            case CRY:
                for (int n = count(40f, dt); n > 0; n--) {
                    float sd = Math.random() < 0.5 ? 1f : -1f;
                    int i = fx.spawn(Particles.BALL, mx + lx * sd * 0.15f * s, hy, mz + lz * sd * 0.15f * s,
                            lx * sd * 3f + fx0 * 0.6f, 2f + rnd(0.6f), lz * sd * 3f + fz0 * 0.6f, (0.06f + 0.04f * (float) Math.random()) * s,
                            0.6f, TEAR, 12f);
                    fx.drag(i, 0.8f);
                }
                break;
            case SIT_UPS:
                if (count(3f, dt) > 0) fx.spawn(Particles.BALL, hx + rnd(0.3f * s), hy + 0.2f * s, hz + rnd(0.3f * s), rnd(1f), 2f, rnd(1f), 0.07f * s, 0.6f, TEAR, 9f);
                break;
            case HEAD_SPIN:
                if (count(10f, dt) > 0) {
                    float a = t * 14f;
                    fx.sparkle(hx + (float) Math.cos(a) * 0.6f * s, hy + 0.3f * s, hz + (float) Math.sin(a) * 0.6f * s, 0.3f * s, 0xC8B0FF);
                }
                break;
            case BACKFLIP:
                if (at(t0, t, 0.3f)) dust(fx, hx, feet(d, hy, s), hz, 4, s, 0xFFFFFF);
                if (at(t0, t, 1.2f)) {
                    dust(fx, hx, feet(d, hy, s), hz, 7, s, 0xFFFFFF);
                    fx.stars(hx, hy, hz, 4);
                }
                break;
            case WORM:
                if (count(5f, dt) > 0 && t > 0.4f && t < DUR[WORM] - 0.4f) dust(fx, hx, hy - 0.35f * s, hz, 1, s, 0xF0E8F4);
                break;
            case CONFETTI:
                if (at(t0, t, 0.5f) || at(t0, t, 1.7f)) {
                    // thrown up out of both hands: a shower of little bits
                    for (int k = 0; k < 40; k++) {
                        float a = (float) Math.random() * TAU, v = (1f + 1.5f * (float) Math.random()) * s;
                        int i = fx.spawn(Particles.CUBE, hx + rnd(0.3f * s), hy + 0.9f * s, hz + rnd(0.3f * s), (float) Math.cos(a) * v,
                                (3.5f + 2.5f * (float) Math.random()) * s, (float) Math.sin(a) * v, (0.09f + 0.07f * (float) Math.random()) * s,
                                1.6f + 0.6f * (float) Math.random(), Palette.TILE[(int) (Math.random() * Palette.TILE.length)], 5f * s);
                        fx.drag(i, 1.8f);
                    }
                }
                break;
            case SULK:
                if (count(2.5f, dt) > 0) {
                    int i = fx.spawn(Particles.BALL, hx + rnd(0.25f * s), hy + 0.75f * s, hz + rnd(0.25f * s), rnd(0.2f), 0.3f, rnd(0.2f),
                            (0.18f + 0.1f * (float) Math.random()) * s, 0.9f, SMOKE, -0.2f);
                    fx.drag(i, 2f);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Where the head of a car's seated driver is in the world (out[0..2]); returns the way its body faces (world
     * yaw). Close enough for particles and words over it.
     */
    static float seatHead(Car c, float[] out) {
        Driver d = c.driver;
        CarDef def = c.def;
        float s = def.seatScale;
        float fx0 = (float) Math.sin(c.yaw), fz0 = (float) Math.cos(c.yaw);
        float along = def.seatZ + d.pos[Driver.REACH] * s, across = def.seatX + d.pos[Driver.SHIFT] * s;
        out[0] = c.x + fx0 * along + fz0 * across;
        out[1] = c.y + c.hop + def.seatY + (d.pos[Driver.RISE] + Driver.NECK_Y + Driver.HEAD_Y) * s;
        out[2] = c.z + fz0 * along - fx0 * across;
        return c.yaw + d.pos[Driver.TURN];
    }

    /** A little cloud of dust (n puffs) sized to the driver (s). */
    private static void dust(Particles fx, float x, float y, float z, int n, float s, int col) {
        for (int k = 0; k < n; k++) {
            float a = (float) Math.random() * TAU, v = (0.8f + 1.4f * (float) Math.random()) * s;
            int i = fx.spawn(Particles.BALL, x, y, z, (float) Math.cos(a) * v, (0.3f + 0.6f * (float) Math.random()) * s,
                    (float) Math.sin(a) * v, (0.12f + 0.1f * (float) Math.random()) * s, 0.4f + 0.2f * (float) Math.random(), col, -0.5f);
            fx.drag(i, 3.5f);
        }
    }

    /** Where the driver's feet rest (the floor, or the seat in a car), from the middle of its head (hy). */
    private static float feet(Driver d, float hy, float s) {
        float up = d.pos[Driver.RISE] + Driver.NECK_Y + Driver.HEAD_Y + (d.mode == Driver.STANDING ? Driver.STAND_Y : 0f);
        return hy - up * s;
    }

    /** How many particles to spawn this frame for a rate per second (whole on average). */
    private static int count(float rate, float dt) {
        return (int) (rate * dt + Math.random());
    }

    private static float rnd(float r) {
        return (float) (Math.random() * 2 - 1) * r;
    }

    // ------------------------------------------------------------------ pose helpers

    /** One arm (left = its own left): swing, raise out, elbow. */
    private static void arm(Driver d, boolean left, float swing, float out, float elbow, float w) {
        int a = left ? Driver.L_ARM : Driver.R_ARM;
        d.pose(a, swing, w);
        d.pose(a + 1, out, w);
        d.pose(a + 2, elbow, w);
    }

    /** Both arms alike (mirrored). */
    private static void arms(Driver d, float swing, float out, float elbow, float w) {
        arm(d, true, swing, out, elbow, w);
        arm(d, false, swing, out, elbow, w);
    }

    private static void legs(Driver d, float swing, float out, float knee, float w) {
        d.pose(Driver.L_LEG, swing, w);
        d.pose(Driver.R_LEG, swing, w);
        d.pose(Driver.L_LEG_OUT, out, w);
        d.pose(Driver.R_LEG_OUT, out, w);
        d.pose(Driver.L_KNEE, knee, w);
        d.pose(Driver.R_KNEE, knee, w);
    }

    private static void knees(Driver d, float knee, float w) {
        d.pose(Driver.L_KNEE, knee, w);
        d.pose(Driver.R_KNEE, knee, w);
    }

    /** Bobs the whole body up (a jump) or down (a crouch) from base, where stand() put a seated driver. */
    private static void hop(Driver d, float base, float rise, float w) {
        d.pose(Driver.RISE, (d.mode == Driver.SEATED ? base : 0f) + rise, w);
    }

    /**
     * Climbs up out of the seat (rise in driver units; about 0.85 stands on the roof of a hatch) with straight legs;
     * on its own feet it stays standing as it is.
     */
    private static void stand(Driver d, float rise, float w) {
        if (d.mode != Driver.SEATED) {
            d.pose(Driver.RISE, 0f, w);
            return;
        }
        d.pose(Driver.RISE, rise, w);
        if (d.style == CarDef.BENCH || d.style == CarDef.SADDLE) {
            // stood up on the seat (or the saddle): the legs straighten out
            float k = Math.min(1f, rise / 0.5f);
            legs(d, 0.1f, d.style == CarDef.SADDLE ? 0.35f : 0.12f, 0.1f, w * k);
        }
    }

    /**
     * Turns towards the audience (d.aim), plus extra (PI = the back to it): the whole body, or for a driver sitting
     * on a bench (its legs in the way) the torso and head.
     */
    private static void face(Driver d, float extra, float w) {
        float cur = d.pos[Driver.TURN];
        float target = cur + wrap(d.aim + extra - cur);
        target = Math.max(-3.2f, Math.min(3.2f, target));
        boolean sitting = d.mode == Driver.SEATED && d.style == CarDef.BENCH
                && (d.weight[Driver.RISE] <= 0f || d.over[Driver.RISE] < 0.3f);
        if (sitting) {
            float tw = clamp(target * 0.5f, 1.3f);
            d.pose(Driver.TWIST, tw, w);
            d.pose(Driver.LOOK, clamp(target - tw, 1.5f), w);
        } else {
            d.pose(Driver.TURN, target, w);
        }
    }

    /** +1 or -1: the side its audience is on (where to look back over the shoulder). */
    private static float side(Driver d) {
        return d.aim >= 0f ? 1f : -1f;
    }

    private static float wrap(float a) {
        while (a > PI) a -= TAU;
        while (a < -PI) a += TAU;
        return a;
    }

    private static float clamp(float v, float r) {
        return v < -r ? -r : (v > r ? r : v);
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }

    private static float smooth(float u) {
        u = u < 0f ? 0f : (u > 1f ? 1f : u);
        return u * u * (3f - 2f * u);
    }
}
