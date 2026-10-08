package com.gogogo.game.engine;

/** Signed-distance-field font metrics loaded from font.txt (see tools/gen_font.py). */
public final class Font {
    public final String name;
    public final float ascent, descent;
    final float[] gx = new float[128], gy = new float[128], gw = new float[128], gh = new float[128];
    final float[] xoff = new float[128], yoff = new float[128], adv = new float[128];
    final boolean[] has = new boolean[128];
    /** Ink extent of capital letters relative to the line top, in atlas px. */
    float capTop, capBottom;

    static int atlasW, atlasH, em, spread;

    private Font(String name, float ascent, float descent) {
        this.name = name;
        this.ascent = ascent;
        this.descent = descent;
    }

    /** Parses all fonts in the metrics file. */
    public static Font[] parse(String txt) {
        java.util.ArrayList<Font> fonts = new java.util.ArrayList<Font>();
        Font cur = null;
        for (String line : txt.split("\n")) {
            String[] t = line.trim().split(" ");
            if (t.length == 0 || t[0].length() == 0) continue;
            if (t[0].equals("atlas")) {
                atlasW = Integer.parseInt(t[1]);
                atlasH = Integer.parseInt(t[2]);
                em = Integer.parseInt(t[4]);
                spread = Integer.parseInt(t[6]);
            } else if (t[0].equals("font")) {
                cur = new Font(t[1], Float.parseFloat(t[3]), Float.parseFloat(t[5]));
                fonts.add(cur);
            } else if (t[0].equals("g") && cur != null) {
                int c = Integer.parseInt(t[1]);
                if (c < 0 || c >= 128) continue;
                cur.gx[c] = Float.parseFloat(t[2]);
                cur.gy[c] = Float.parseFloat(t[3]);
                cur.gw[c] = Float.parseFloat(t[4]);
                cur.gh[c] = Float.parseFloat(t[5]);
                cur.xoff[c] = Float.parseFloat(t[6]);
                cur.yoff[c] = Float.parseFloat(t[7]);
                cur.adv[c] = Float.parseFloat(t[8]);
                cur.has[c] = true;
            }
        }
        for (Font f : fonts) {
            int h = 'H';
            float pad = spread + 1;
            f.capTop = f.yoff[h] + pad;
            f.capBottom = f.yoff[h] + f.gh[h] - pad;
        }
        return fonts.toArray(new Font[0]);
    }

    /** Width in UI units of text drawn at the given size (em height). */
    public float width(CharSequence s, float size) {
        float k = size / em;
        float w = 0f;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 128 || !has[c]) c = '?';
            w += adv[c];
        }
        return w * k;
    }

    /** Height of capital letters at this size. */
    public float capHeight(float size) {
        return (capBottom - capTop) * size / em;
    }
}
