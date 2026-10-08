#!/usr/bin/env python3
"""Bakes the game's two fonts into one signed-distance-field atlas.

Output (into app/src/main/assets):
  font.png  - grayscale SDF atlas (0.5 = glyph edge)
  font.txt  - glyph metrics, one font block per font

Requires: pillow, numpy, scipy
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy.ndimage import distance_transform_edt

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONTS = [
    ("title", os.path.join(ROOT, "tools/fonts/LuckiestGuy-Regular.ttf")),
    ("body", os.path.join(ROOT, "tools/fonts/LilitaOne-Regular.ttf")),
]
EM = 56          # atlas glyph size in px
SPREAD = 8       # distance (px) mapped to the 0..0.5 range
UPSCALE = 4      # render at higher res for an accurate field
ATLAS_W = 1024
CHARS = [chr(c) for c in range(32, 127)]


def sdf_for(mask):
    inside = distance_transform_edt(mask)
    outside = distance_transform_edt(~mask)
    d = (inside - outside) / UPSCALE
    return np.clip(0.5 + d / (2.0 * SPREAD), 0.0, 1.0)


def bake(font_path):
    hi = ImageFont.truetype(font_path, EM * UPSCALE)
    ascent, descent = hi.getmetrics()
    glyphs = []
    for ch in CHARS:
        adv = hi.getlength(ch) / UPSCALE
        bbox = hi.getbbox(ch)
        if ch == " " or bbox[2] <= bbox[0]:
            glyphs.append((ch, None, 0, 0, adv))
            continue
        pad = SPREAD * UPSCALE + UPSCALE
        w = bbox[2] - bbox[0] + pad * 2
        h = bbox[3] - bbox[1] + pad * 2
        img = Image.new("L", (w, h), 0)
        ImageDraw.Draw(img).text((pad - bbox[0], pad - bbox[1]), ch, font=hi, fill=255)
        mask = np.array(img) > 127
        field = sdf_for(mask)
        small = Image.fromarray((field * 255).astype(np.uint8)).resize(
            (max(1, w // UPSCALE), max(1, h // UPSCALE)), Image.LANCZOS)
        xoff = (bbox[0] - pad) / UPSCALE
        yoff = (bbox[1] - pad) / UPSCALE
        glyphs.append((ch, small, xoff, yoff, adv))
    return glyphs, ascent / UPSCALE, descent / UPSCALE


def main():
    baked = [(name, *bake(path)) for name, path in FONTS]
    # shelf packing
    x = y = shelf = 0
    placed = []
    for name, glyphs, asc, desc in baked:
        for ch, img, xoff, yoff, adv in glyphs:
            if img is None:
                placed.append((name, ch, 0, 0, 0, 0, 0, 0, adv))
                continue
            w, h = img.size
            if x + w + 1 > ATLAS_W:
                x = 0
                y += shelf + 1
                shelf = 0
            placed.append((name, ch, x, y, w, h, xoff, yoff, adv, img))
            x += w + 1
            shelf = max(shelf, h)
    height = 1
    while height < y + shelf + 1:
        height *= 2
    atlas = Image.new("L", (ATLAS_W, height), 0)
    for p in placed:
        if len(p) == 10:
            atlas.paste(p[9], (p[2], p[3]))
    out = os.path.join(ROOT, "app/src/main/assets")
    os.makedirs(out, exist_ok=True)
    atlas.save(os.path.join(out, "font.png"), optimize=True)
    with open(os.path.join(out, "font.txt"), "w") as f:
        f.write("atlas %d %d em %d spread %d\n" % (ATLAS_W, height, EM, SPREAD))
        for name, glyphs, asc, desc in baked:
            f.write("font %s ascent %.2f descent %.2f\n" % (name, asc, desc))
            for p in placed:
                if p[0] != name:
                    continue
                f.write("g %d %d %d %d %d %.2f %.2f %.2f\n" % (ord(p[1]), p[2], p[3], p[4], p[5], p[6], p[7], p[8]))
    print("atlas %dx%d" % (ATLAS_W, height))


if __name__ == "__main__":
    main()
