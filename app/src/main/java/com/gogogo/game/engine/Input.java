package com.gogogo.game.engine;

/** Thread-safe pointer input: the platform pushes raw pixel events, the game polls once per frame. */
public final class Input {
    public static final int DOWN = 0, MOVE = 1, UP = 2, CANCEL = 3;
    public static final int MAX = 10;

    public static final class Pointer {
        public int id = -1;
        public boolean down, justDown, justUp;
        public float x, y, startX, startY;
        public float downTime;
        /** Set by whoever claims this touch (joystick, button) so others ignore it. */
        public int owner;
    }

    public final Pointer[] pointers = new Pointer[MAX];
    private final int[] qType = new int[256], qId = new int[256];
    private final float[] qX = new float[256], qY = new float[256];
    private int qn;
    private boolean backPressed, backQueued;

    public Input() {
        for (int i = 0; i < MAX; i++) pointers[i] = new Pointer();
    }

    public synchronized void push(int type, int id, float px, float py) {
        if (qn >= qType.length) return;
        qType[qn] = type;
        qId[qn] = id;
        qX[qn] = px;
        qY[qn] = py;
        qn++;
    }

    public synchronized void pushBack() {
        backQueued = true;
    }

    /** Applies queued events. Coordinates are converted to UI units with the given scale. */
    public synchronized void poll(float uiScale, float dt) {
        for (Pointer p : pointers) {
            if (p.justUp) {
                p.id = -1;
                p.owner = 0;
            }
            p.justDown = false;
            p.justUp = false;
            if (p.down) p.downTime += dt;
        }
        for (int i = 0; i < qn; i++) {
            float x = qX[i] / uiScale, y = qY[i] / uiScale;
            int type = qType[i];
            Pointer p = find(qId[i]);
            if (type == DOWN) {
                if (p == null) p = free();
                if (p == null) continue;
                p.id = qId[i];
                p.down = true;
                p.justDown = true;
                p.justUp = false;
                p.x = p.startX = x;
                p.y = p.startY = y;
                p.downTime = 0f;
                p.owner = 0;
            } else if (p != null && p.down) {
                p.x = x;
                p.y = y;
                if (type == UP || type == CANCEL) {
                    p.down = false;
                    p.justUp = true;
                }
            }
        }
        qn = 0;
        backPressed = backQueued;
        backQueued = false;
    }

    private Pointer find(int id) {
        for (Pointer p : pointers) if (p.id == id && (p.down || p.justUp)) return p;
        return null;
    }

    private Pointer free() {
        for (Pointer p : pointers) if (!p.down && !p.justUp) return p;
        return null;
    }

    public boolean backPressed() {
        boolean b = backPressed;
        backPressed = false;
        return b;
    }

    /** Releases all touches (e.g. when the app pauses). */
    public synchronized void reset() {
        for (Pointer p : pointers) {
            p.down = false;
            p.justDown = false;
            p.justUp = false;
            p.id = -1;
            p.owner = 0;
        }
        qn = 0;
    }
}
