#!/usr/bin/env python3
"""Renders the launcher icons (legacy + adaptive foreground) into app/src/main/res."""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
FONT = os.path.join(ROOT, "tools/fonts/LuckiestGuy-Regular.ttf")
DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
TILE_COLORS = [(255, 79, 163), (255, 210, 59), (59, 184, 255), (94, 230, 90), (255, 138, 43), (164, 107, 255)]


def gradient(size, top, bottom):
    img = Image.new("RGB", (size, size))
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / max(1, size - 1)
        d.line([(0, y), (size, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img


def background(size):
    img = gradient(size, (124, 92, 255), (255, 79, 163)).convert("RGBA")
    d = ImageDraw.Draw(img)
    # checkered tiles in perspective-ish rows at the bottom
    n = 6
    cell = size / n
    for r in range(2):
        for c in range(n + 1):
            col = TILE_COLORS[(r * 3 + c) % len(TILE_COLORS)]
            y0 = size * (0.70 + r * 0.16)
            pad = cell * 0.08
            x0 = c * cell - (cell / 2 if r % 2 else 0)
            d.rounded_rectangle([x0 + pad, y0 + pad, x0 + cell - pad, y0 + size * 0.15 - pad], radius=cell * 0.18, fill=col + (255,))
    return img


def go_text(size, scale=1.0):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    font = ImageFont.truetype(FONT, int(size * 0.42 * scale))
    text = "GO!"
    d = ImageDraw.Draw(layer)
    bbox = d.textbbox((0, 0), text, font=font)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (size - w) / 2 - bbox[0]
    y = (size - h) / 2 - bbox[1] - size * 0.04
    stroke = max(2, int(size * 0.035 * scale))
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text((x, y + size * 0.03), text, font=font, fill=(42, 24, 64, 170), stroke_width=stroke, stroke_fill=(42, 24, 64, 170))
    shadow = shadow.filter(ImageFilter.GaussianBlur(size * 0.01))
    layer.alpha_composite(shadow)
    d.text((x, y), text, font=font, fill=(255, 225, 77), stroke_width=stroke, stroke_fill=(42, 24, 64))
    return layer


def legacy(size):
    big = size * 4
    img = background(big)
    img.alpha_composite(go_text(big, 1.0))
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, big - 1, big - 1], radius=big * 0.22, fill=255)
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out.resize((size, size), Image.LANCZOS)


def main():
    for name, k in DENS.items():
        d = os.path.join(RES, "mipmap-" + name)
        os.makedirs(d, exist_ok=True)
        legacy(int(48 * k)).save(os.path.join(d, "ic_launcher.png"))
        fg_size = int(108 * k)
        big = fg_size * 4
        fg = go_text(big, 0.72).resize((fg_size, fg_size), Image.LANCZOS)
        fg.save(os.path.join(d, "ic_launcher_fg.png"))
        bg = background(big).resize((fg_size, fg_size), Image.LANCZOS)
        bg.save(os.path.join(d, "ic_launcher_bg.png"))
    a = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(a, exist_ok=True)
    with open(os.path.join(a, "ic_launcher.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@mipmap/ic_launcher_bg" />\n'
                '    <foreground android:drawable="@mipmap/ic_launcher_fg" />\n'
                '</adaptive-icon>\n')
    # store-size icon for the README / listing
    legacy(512).save(os.path.join(ROOT, "docs", "icon.png")) if os.path.isdir(os.path.join(ROOT, "docs")) else None


if __name__ == "__main__":
    main()
