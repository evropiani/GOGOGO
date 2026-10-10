package com.gogogo.game.engine;

import java.util.HashMap;

/**
 * Tiny immediate-mode UI on top of {@link UIBatch}: chunky bouncy buttons, panels,
 * toggles and drag-scrolling lists. One "UI pointer" at a time drives widgets.
 */
public final class UI {
    public static final int OWNER_UI = 1;

    public final UIBatch b;
    public final Input input;
    public float time;

    private Input.Pointer ptr;     // pointer currently driving the UI
    private String pressed;        // widget id under press
    private boolean released;      // ptr released this frame
    private float relX, relY;
    private boolean dragging;
    private String scrollId;
    private float lastY;
    private final HashMap<String, float[]> scroll = new HashMap<String, float[]>(); // offset, velocity, max
    private final HashMap<String, Float> bounce = new HashMap<String, Float>();
    private String clicked;        // id clicked last frame (for bounce anim)
    public boolean enabled = true;

    /** Called when any button is clicked (for click sounds). */
    public Runnable onClick;

    public UI(UIBatch b, Input input) {
        this.b = b;
        this.input = input;
    }

    public void frame(float dt) {
        time += dt;
        released = false;
        if (ptr != null && ptr.justUp) {
            released = true;
            relX = ptr.x;
            relY = ptr.y;
        }
        if (ptr != null && ptr.owner != 0 && ptr.owner != OWNER_UI) {
            // a gameplay control took this touch: let other fingers drive the UI
            ptr = null;
            pressed = null;
            dragging = false;
            scrollId = null;
        }
        if (ptr != null && !ptr.down && !ptr.justUp) {
            ptr = null;
            pressed = null;
            dragging = false;
            scrollId = null;
        }
        if (ptr == null) {
            for (Input.Pointer p : input.pointers) {
                if (p.justDown && p.owner == 0) {
                    ptr = p;
                    pressed = null;
                    dragging = false;
                    scrollId = null;
                    lastY = p.y;
                    if (p.justUp) {
                        released = true;
                        relX = p.x;
                        relY = p.y;
                    }
                    break;
                }
            }
        }
        // decay bounce animations
        for (java.util.Map.Entry<String, Float> e : bounce.entrySet()) {
            e.setValue(Math.max(0f, e.getValue() - dt * 3.5f));
        }
        // scroll physics
        for (java.util.Map.Entry<String, float[]> e : scroll.entrySet()) {
            float[] s = e.getValue();
            boolean held = dragging && e.getKey().equals(scrollId);
            if (!held) {
                s[0] += s[1] * dt;
                s[1] *= (float) Math.pow(0.04, dt);
                if (s[0] < 0) {
                    s[0] += (0 - s[0]) * Math.min(1f, dt * 12f);
                    s[1] = 0;
                } else if (s[0] > s[2]) {
                    s[0] += (s[2] - s[0]) * Math.min(1f, dt * 12f);
                    s[1] = 0;
                }
            }
        }
    }

    /** Called after the screen drew its widgets: a release always ends the press. */
    public void endFrame() {
        if (released) {
            ptr = null;
            pressed = null;
            dragging = false;
            scrollId = null;
        }
    }

    public boolean pointerDown() {
        return ptr != null && ptr.down;
    }

    /** True if the UI pointer is currently pressing inside the rect (and not scrolling). */
    public boolean holding(float x, float y, float w, float h) {
        return enabled && ptr != null && ptr.down && !dragging && inside(ptr.x, ptr.y, x, y, w, h);
    }

    private static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && py >= y && px <= x + w && py <= y + h;
    }

    /** Core hit logic. Returns true on click. */
    public boolean hit(String id, float x, float y, float w, float h) {
        if (!enabled || ptr == null) return false;
        if (ptr.justDown && inside(ptr.startX, ptr.startY, x, y, w, h) && inClip(ptr.startX, ptr.startY)) pressed = id;
        if (released && id.equals(pressed) && !dragging && inside(relX, relY, x - 12, y - 12, w + 24, h + 24)) {
            pressed = null;
            clicked = id;
            bounce.put(id, 1f);
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }

    public boolean isPressed(String id) {
        return id.equals(pressed) && ptr != null && ptr.down && !dragging;
    }

    /** 0..1 bounce animation value after a click. */
    public float bounce(String id) {
        Float f = bounce.get(id);
        return f == null ? 0f : f;
    }

    /** Swallows any press so that nothing underneath reacts (modal overlays). */
    public void block(float x, float y, float w, float h) {
        if (ptr != null && ptr.justDown && inside(ptr.startX, ptr.startY, x, y, w, h) && inClip(ptr.startX, ptr.startY) && pressed == null) pressed = "__block";
    }

    // ------------------------------------------------------------------ scrolling

    /** Begins a vertically scrolling region. Returns the current scroll offset. */
    public float beginScroll(String id, float x, float y, float w, float h, float contentH) {
        float[] s = scroll.get(id);
        if (s == null) {
            s = new float[3];
            scroll.put(id, s);
        }
        s[2] = Math.max(0f, contentH - h);
        if (enabled && ptr != null && ptr.down && inside(ptr.startX, ptr.startY, x, y, w, h)) {
            if (!dragging && Math.abs(ptr.y - ptr.startY) > 14f) {
                dragging = true;
                scrollId = id;
                lastY = ptr.y;
                pressed = null;
            }
            if (dragging && id.equals(scrollId)) {
                float dy = ptr.y - lastY;
                lastY = ptr.y;
                float over = (s[0] < 0 || s[0] > s[2]) ? 0.4f : 1f;
                s[0] -= dy * over;
                s[1] = -dy / Math.max(0.008f, 1f / 60f) * 0.9f;
            }
        }
        b.clip(x, y, w, h);
        clipOn = true;
        clipX = x;
        clipY = y;
        clipW = w;
        clipH = h;
        return s[0];
    }

    public void endScroll() {
        b.unclip();
        clipOn = false;
    }

    // widgets scrolled out of view must not take presses
    private boolean clipOn;
    private float clipX, clipY, clipW, clipH;

    private boolean inClip(float px, float py) {
        return !clipOn || inside(px, py, clipX, clipY, clipW, clipH);
    }

    public void resetScroll(String id) {
        scroll.remove(id);
    }

    /** Jumps a scroll region to an offset (it settles inside the content once the region is drawn). */
    public void setScroll(String id, float offset) {
        scroll.put(id, new float[]{offset, 0f, offset});
    }

    // ------------------------------------------------------------------ styled widgets

    public static int shade(int rgb, float f) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, bl = rgb & 255;
        if (f < 1f) {
            r = (int) (r * f);
            g = (int) (g * f);
            bl = (int) (bl * f);
        } else {
            float t = f - 1f;
            r = (int) (r + (255 - r) * t);
            g = (int) (g + (255 - g) * t);
            bl = (int) (bl + (255 - bl) * t);
        }
        return (rgb & 0xFF000000) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, bl);
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Draws a chunky 3D-looking pill button and returns true when clicked. */
    public boolean button(String id, float x, float y, float w, float h, int color, String label, float textSize) {
        return button(id, x, y, w, h, color, label, textSize, true);
    }

    public boolean button(String id, float x, float y, float w, float h, int color, String label, float textSize, boolean active) {
        boolean click = hit(id, x, y, w, h) && active;
        drawButton(id, x, y, w, h, color, label, textSize, active);
        return click;
    }

    public void drawButton(String id, float x, float y, float w, float h, int color, String label, float textSize, boolean active) {
        boolean down = isPressed(id) && holding(x - 12, y - 12, w + 24, h + 24);
        float bnc = bounce(id);
        float depth = Math.min(12f, h * 0.14f);
        float press = down ? depth * 0.75f : 0f;
        float sc = 1f + (float) Math.sin(bnc * Math.PI) * 0.06f;
        float cx = x + w / 2, cy = y + h / 2;
        float sw = w * sc, sh = h * sc;
        int face = active ? opaque(color) : 0xFFB8B0C8;
        int side = shade(face, 0.68f);
        float r = Math.min(sh / 2f, 26f);
        b.shadow(cx - sw / 2, cy - sh / 2 + depth + 6, sw, sh, r, 0x40200040, 10f);
        b.shape(cx, cy + depth / 2 + press / 2, sw, sh - press + depth, r, side, 0xFF2A1840, 0, 0, 0, 0);
        b.shape(cx, cy - depth / 2 + press, sw, sh - depth, r, face, 0xFF2A1840, 0, 0.55f, 0, 0);
        // shine
        b.shape(cx, cy - depth / 2 + press - (sh - depth) * 0.22f, sw - r * 1.2f, (sh - depth) * 0.22f, (sh - depth) * 0.11f, 0x40FFFFFF, 0, 0, 0, 0, 0);
        if (label != null) {
            b.textFit(b.title, label, cx, cy - depth / 2 + press + 2, textSize * sc, sw - 24, 0xFFFFFFFF, UIBatch.CENTER, 0xFF2A1840, textSize * 0.11f);
        }
    }

    /** Button whose label is "prefix [icon] label" (e.g. "BUY (coin) 600"); prefix and label may be null. */
    public boolean iconButton(String id, float x, float y, float w, float h, int color, String prefix, int icon, String label, float textSize, boolean active) {
        boolean click = hit(id, x, y, w, h) && active;
        drawButton(id, x, y, w, h, color, null, textSize, active);
        // the face position, as drawButton places it
        boolean down = isPressed(id) && holding(x - 12, y - 12, w + 24, h + 24);
        float depth = Math.min(12f, h * 0.14f);
        float press = down ? depth * 0.75f : 0f;
        float sc = 1f + (float) Math.sin(bounce(id) * Math.PI) * 0.06f;
        iconLabel(x + w / 2, y + h / 2 - depth / 2 + press + 2, w * sc - 28, prefix, icon, label, textSize * sc, 0xFFFFFFFF, 0xFF2A1840, textSize * 0.11f);
        return click;
    }

    public static final int ICON_NONE = 0, ICON_COIN = 1, ICON_RIM = 2, ICON_LOCK = 3, ICON_CHECK = 4, ICON_STAR = 5;

    /** Draws "prefix [icon] label" centered at (cx, cy) in the title font, shrunk to fit maxW. */
    public void iconLabel(float cx, float cy, float maxW, String prefix, int icon, String label, float size, int color, int outlineColor, float outline) {
        float gap = size * 0.2f;
        float iw = icon != ICON_NONE ? size * 0.95f : 0f;
        float pw = prefix != null ? b.title.width(prefix, size) + gap : 0f;
        float lw = label != null ? b.title.width(label, size) : 0f;
        float ig = icon != ICON_NONE && label != null ? gap : 0f;
        float total = pw + iw + ig + lw;
        if (total > maxW && total > 0) {
            float k = maxW / total;
            size *= k;
            iw *= k;
            pw *= k;
            ig *= k;
            total = maxW;
        }
        float x = cx - total / 2;
        if (prefix != null) {
            b.text(b.title, prefix, x, cy, size, color, UIBatch.LEFT, outlineColor, outline);
            x += pw;
        }
        if (icon != ICON_NONE) {
            icon(icon, x + iw / 2, cy, iw, color);
            x += iw + ig;
        }
        if (label != null) b.text(b.title, label, x, cy, size, color, UIBatch.LEFT, outlineColor, outline);
    }

    /** One of the ICON_* icons, s units across. Lock, check and star use the given color. */
    public void icon(int icon, float cx, float cy, float s, int color) {
        switch (icon) {
            case ICON_COIN: coin(cx, cy, s * 0.44f); break;
            case ICON_RIM: rim(cx, cy, s * 0.46f); break;
            case ICON_LOCK: iconLock(cx, cy + s * 0.04f, s * 0.95f, color); break;
            case ICON_CHECK: iconCheck(cx, cy, s, color); break;
            case ICON_STAR: star(cx, cy, s * 0.5f, color); break;
            default: break;
        }
    }

    /** Round icon button (e.g. back, pause). Draw the icon yourself at the returned center. */
    public boolean roundButton(String id, float cx, float cy, float r, int color) {
        boolean click = hit(id, cx - r, cy - r, r * 2, r * 2);
        boolean down = isPressed(id) && holding(cx - r - 12, cy - r - 12, r * 2 + 24, r * 2 + 24);
        float depth = r * 0.16f;
        float press = down ? depth * 0.75f : 0f;
        float sc = 1f + (float) Math.sin(bounce(id) * Math.PI) * 0.08f;
        float rr = r * sc;
        b.shadow(cx - rr, cy - rr + depth + 4, rr * 2, rr * 2, rr, 0x40200040, 8f);
        b.circle(cx, cy + depth / 2, rr, shade(opaque(color), 0.68f));
        b.shape(cx, cy - depth / 2 + press, rr * 2, rr * 2, rr, opaque(color), 0xFF2A1840, 0, 0.5f, 0, 0);
        lastRoundPress = press - depth / 2;
        return click;
    }

    /** Vertical offset of the face of the last round button (to place its icon). */
    public float lastRoundPress;

    public void panel(float x, float y, float w, float h, int color) {
        b.shadow(x, y + 10, w, h, 28, 0x40200040, 18f);
        b.shape(x + w / 2, y + h / 2, w, h, 28, opaque(color), 0xFFFFFFFF, 0, 0.12f, 0, 0);
    }

    /** On/off switch. Returns the new value. */
    public boolean toggle(String id, float x, float y, float w, float h, boolean value) {
        if (hit(id, x, y, w, h)) value = !value;
        int bg = value ? 0xFF4CD964 : 0xFFB8B0C8;
        b.shape(x + w / 2, y + h / 2 + 4, w, h, h / 2, shade(bg, 0.7f), 0, 0, 0, 0, 0);
        b.shape(x + w / 2, y + h / 2, w, h, h / 2, bg, 0, 0, 0.3f, 0, 0);
        float kx = value ? x + w - h / 2 : x + h / 2;
        b.circle(kx, y + h / 2 + 3, h / 2 - 5, 0x50200040);
        b.shape(kx, y + h / 2, h - 10, h - 10, h / 2, 0xFFFFFFFF, 0, 0, 0.3f, 0, 0);
        return value;
    }

    // ------------------------------------------------------------------ icons drawn from shapes

    public void iconBack(float cx, float cy, float s, int color) {
        b.line(cx - s * 0.25f, cy, cx + s * 0.2f, cy - s * 0.38f, s * 0.2f, color);
        b.line(cx - s * 0.25f, cy, cx + s * 0.2f, cy + s * 0.38f, s * 0.2f, color);
    }

    public void iconPause(float cx, float cy, float s, int color) {
        b.roundRect(cx - s * 0.3f, cy - s * 0.35f, s * 0.2f, s * 0.7f, s * 0.08f, color);
        b.roundRect(cx + s * 0.1f, cy - s * 0.35f, s * 0.2f, s * 0.7f, s * 0.08f, color);
    }

    public void iconClose(float cx, float cy, float s, int color) {
        b.line(cx - s * 0.3f, cy - s * 0.3f, cx + s * 0.3f, cy + s * 0.3f, s * 0.2f, color);
        b.line(cx - s * 0.3f, cy + s * 0.3f, cx + s * 0.3f, cy - s * 0.3f, s * 0.2f, color);
    }

    public void iconPlay(float cx, float cy, float s, int color) {
        b.line(cx - s * 0.15f, cy - s * 0.35f, cx + s * 0.3f, cy, s * 0.2f, color);
        b.line(cx - s * 0.15f, cy + s * 0.35f, cx + s * 0.3f, cy, s * 0.2f, color);
        b.line(cx - s * 0.15f, cy - s * 0.35f, cx - s * 0.15f, cy + s * 0.35f, s * 0.2f, color);
    }

    public void iconCheck(float cx, float cy, float s, int color) {
        b.line(cx - s * 0.32f, cy, cx - s * 0.08f, cy + s * 0.25f, s * 0.18f, color);
        b.line(cx - s * 0.08f, cy + s * 0.25f, cx + s * 0.35f, cy - s * 0.28f, s * 0.18f, color);
    }

    public void iconGear(float cx, float cy, float s, int color, int hole) {
        for (int i = 0; i < 8; i++) {
            float a = (float) (i * Math.PI / 4);
            b.shape(cx + (float) Math.cos(a) * s * 0.36f, cy + (float) Math.sin(a) * s * 0.36f, s * 0.22f, s * 0.22f, s * 0.05f, color, 0, 0, 0, 0, a);
        }
        b.circle(cx, cy, s * 0.36f, color);
        b.circle(cx, cy, s * 0.14f, hole);
    }

    public void iconLock(float cx, float cy, float s, int color) {
        b.shape(cx, cy - s * 0.18f, s * 0.5f, s * 0.55f, s * 0.25f, 0, color, s * 0.12f, 0, 0, 0);
        b.roundRect(cx - s * 0.36f, cy - s * 0.08f, s * 0.72f, s * 0.52f, s * 0.1f, color);
    }

    public void coin(float cx, float cy, float r) {
        b.circle(cx, cy + r * 0.18f, r, 0xFFC77800);
        b.shape(cx, cy, r * 2, r * 2, r, 0xFFFFD23F, 0xFFE89A00, r * 0.16f, 0.4f, 0, 0);
        b.shape(cx, cy, r * 0.9f, r * 0.9f, r * 0.45f, 0, 0xFFFFF0A0, r * 0.12f, 0, 0, 0);
    }

    /** A Rim (the premium currency): dark tire around a shiny chrome-and-cyan hub with spokes. */
    public void rim(float cx, float cy, float r) {
        b.circle(cx, cy + r * 0.18f, r, 0xFF0E1426);
        b.shape(cx, cy, r * 2, r * 2, r, 0xFF323A52, 0xFF161C30, r * 0.14f, 0.35f, 0, 0);
        b.shape(cx, cy, r * 1.36f, r * 1.36f, r * 0.68f, 0xFFF2FBFF, 0xFF8FE4FF, r * 0.12f, 0.55f, 0, 0);
        for (int i = 0; i < 5; i++) {
            float a = (float) (i * Math.PI * 2 / 5 - Math.PI / 2);
            b.shape(cx + (float) Math.cos(a) * r * 0.32f, cy + (float) Math.sin(a) * r * 0.32f, r * 0.5f, r * 0.17f, r * 0.08f, 0xFF4FC8F2, 0, 0, 0, 0, a);
        }
        b.circle(cx, cy, r * 0.2f, 0xFF2E9FE0);
        b.circle(cx - r * 0.42f, cy - r * 0.5f, r * 0.13f, 0xB0FFFFFF);
    }

    /** Achievement medal: ribbon tails and a disc with a star. */
    public void medal(float cx, float cy, float r, int color) {
        b.shape(cx - r * 0.38f, cy + r * 0.85f, r * 0.42f, r * 0.95f, r * 0.08f, 0xFFFF4FA3, 0, 0, 0, 0, 0.35f);
        b.shape(cx + r * 0.38f, cy + r * 0.85f, r * 0.42f, r * 0.95f, r * 0.08f, 0xFF3BA8FF, 0, 0, 0, 0, -0.35f);
        b.circle(cx, cy + r * 0.12f, r, shade(opaque(color), 0.7f));
        b.shape(cx, cy, r * 2, r * 2, r, opaque(color), 0xFFFFFFFF, r * 0.12f, 0.45f, 0, 0);
        star(cx, cy, r * 0.62f, 0xFFFFFFFF);
    }

    /** Gift box with a ribbon. */
    public void gift(float cx, float cy, float s, int color, int ribbon) {
        b.roundRect(cx - s * 0.4f, cy - s * 0.1f, s * 0.8f, s * 0.5f, s * 0.06f, color);
        b.roundRect(cx - s * 0.46f, cy - s * 0.28f, s * 0.92f, s * 0.22f, s * 0.06f, shade(opaque(color), 1.18f));
        b.rect(cx - s * 0.08f, cy - s * 0.28f, s * 0.16f, s * 0.68f, ribbon);
        b.shape(cx - s * 0.16f, cy - s * 0.36f, s * 0.3f, s * 0.16f, s * 0.08f, ribbon, 0, 0, 0, 0, 0.5f);
        b.shape(cx + s * 0.16f, cy - s * 0.36f, s * 0.3f, s * 0.16f, s * 0.08f, ribbon, 0, 0, 0, 0, -0.5f);
    }

    public void star(float cx, float cy, float r, int color) {
        for (int i = 0; i < 5; i++) {
            float a = (float) (i * Math.PI * 2 / 5 - Math.PI / 2);
            b.shape(cx + (float) Math.cos(a) * r * 0.45f, cy + (float) Math.sin(a) * r * 0.45f, r * 1.0f, r * 0.36f, r * 0.12f, color, 0, 0, 0, 0, a);
        }
        b.circle(cx, cy, r * 0.42f, color);
    }
}
