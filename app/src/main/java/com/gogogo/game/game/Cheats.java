package com.gogogo.game.game;

import java.security.MessageDigest;

/** Developer helpers and secret code handling. */
public final class Cheats {
    private Cheats() {}

    public static final int GOD = 0, FREEZE = 1, SPEED = 2, DUMB = 3, DUCK = 4, SLOW = 5, FEW = 6;
    public static final String[] FLAG_NAME = {"Never fall", "Freeze timer", "Super speed", "Dumb bots", "Duck every match", "Double timer", "Only 10 cars"};

    public static final int CODE_NONE = 0, CODE_CAR = 1, CODE_DEV = 2;

    // SHA-256 of "gogogo:" + code
    private static final String H_CAR = "c2c83497596f97d60d0c7e84a4f683edeec2471211259d12acbad0004f130ee0";
    private static final String H_DEV = "70c257bdc2b1a62a7052e92603764ae93fd1e8df6c65b5cdb03569660714865f";

    public static int check(String code) {
        String h = hash(code.trim().toUpperCase(java.util.Locale.US));
        if (h.equals(H_CAR)) return CODE_CAR;
        if (h.equals(H_DEV)) return CODE_DEV;
        return CODE_NONE;
    }

    public static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("gogogo:" + code).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x & 255));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Knocks every bot off the map except the player. */
    public static void instantWin(Match m) {
        if (m.player == null || !m.player.alive) return;
        for (Car c : m.cars) {
            if (c != m.player && c.alive && !c.falling) {
                c.x += 1000f; // off the grid: falls next step
            }
        }
    }

    /** Puts the special duck right next to the player. */
    public static void duckHere(Match m) {
        if (m.player == null || !m.player.alive) return;
        Arena.Tile t = m.arena.cellAt(m.player.x, m.player.z);
        if (t == null) return;
        m.duckTile = t.index;
        m.duckSpawned = true;
        m.duckCollected = false;
        m.duckLost = false;
        m.duckX = t.x + 1.2f;
        m.duckZ = t.z - 1.2f;
        m.duckY = 0;
        m.duckVy = 0;
    }
}
