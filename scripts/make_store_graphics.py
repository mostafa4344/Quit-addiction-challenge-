#!/usr/bin/env python3
"""Makes the Google Play graphics in play-store/.

    python3 scripts/make_store_graphics.py [raw-screenshots-dir]

- icon-512.png: the launcher icon (same flame as res/drawable/ic_launcher_foreground.xml)
- feature-graphic.png: 1024 x 500 banner
- screenshots/NN-name.png: 1080 x 1920 phone screenshots with a caption, from raw emulator
  captures (StoreScreenshots.kt). Play needs the long side at most twice the short side, which
  raw 1080 x 2400 captures are not.
Needs Pillow and the Inter font (falls back to DejaVu Sans).
"""
import os
import re
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "play-store")
TEAL = (0, 137, 123)
TEAL_DARK = (0, 77, 64)
ORANGE = (255, 183, 77)
WHITE = (255, 255, 255)

# Path data copied from ic_launcher_foreground.xml (108 x 108 viewport).
FLAME = "M54,28 C60,38 70,44 70,58 C70,68 63,76 54,76 C45,76 38,68 38,58 C38,50 43,45 46,40 C47,46 50,49 53,50 C51,42 52,35 54,28 Z"
INNER = "M54,52 C57,57 62,60 62,66 C62,71 58,74 54,74 C50,74 46,71 46,66 C46,61 51,58 54,52 Z"

CAPTIONS = {
    "01-home": ("Every habit, one challenge", "Set a goal in days or hours and watch your streak grow"),
    "02-progress": ("See how far you've come", "Live timer, progress ring and milestone badges"),
    "03-stats": ("A slip is not the end", "Only the streak restarts. Your total clean time stays"),
    "04-sos-start": ("SOS when an urge hits", "Rate it, then ride it out with a guided wave"),
    "05-sos-surf": ("Urges pass. Watch it happen", "Breathe along until the wave goes by"),
    "06-insights": ("Know what pulls you back", "Stress, boredom, late nights: see your pressures"),
    "07-insights-when": ("Know when you're at risk", "A weekly map of your risky hours, with your own plans"),
    "08-protection": ("Daily check-ins you choose", "Pick how many times a day and when"),
    "09-break-screen": ("App time limits that work", "A full-screen break reminder when time is up"),
    "10-new-challenge": ("Private by design", "No account, no ads, no internet. Data stays on your phone"),
}


def font(size, bold=True):
    names = ["Inter-Bold.otf" if bold else "Inter-Medium.otf"]
    for d in ["/usr/share/fonts/opentype/inter", "/usr/share/fonts/truetype/inter"]:
        for n in names:
            p = os.path.join(d, n)
            if os.path.exists(p):
                return ImageFont.truetype(p, size)
    return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""), size)


def path_points(d, scale, dx, dy, steps=40):
    """Flattens an M / C / Z path into a polygon."""
    nums = [float(n) for n in re.findall(r"-?\d+(?:\.\d+)?", d)]
    cmds = re.findall(r"[MCZ]", d)
    pts, i, cur = [], 0, None
    for c in cmds:
        if c == "M":
            cur = (nums[i], nums[i + 1]); i += 2
            pts.append(cur)
        elif c == "C":
            p1, p2, p3 = (nums[i], nums[i + 1]), (nums[i + 2], nums[i + 3]), (nums[i + 4], nums[i + 5]); i += 6
            for s in range(1, steps + 1):
                t = s / steps
                a, b, cc, dd = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t ** 2, t ** 3
                pts.append((a * cur[0] + b * p1[0] + cc * p2[0] + dd * p3[0], a * cur[1] + b * p1[1] + cc * p2[1] + dd * p3[1]))
            cur = p3
    return [((x - dx) * scale, (y - dy) * scale) for x, y in pts]


def flame(size, transparent=False):
    """The icon at size x size: the middle 72 units of the 108-unit adaptive icon, like a launcher shows it."""
    ss = 4
    big = size * ss
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0) if transparent else TEAL + (255,))
    draw = ImageDraw.Draw(img)
    scale = big / 72.0
    draw.polygon(path_points(FLAME, scale, 18, 18), fill=WHITE)
    draw.polygon(path_points(INNER, scale, 18, 18), fill=ORANGE)
    return img.resize((size, size), Image.LANCZOS)


def gradient(w, h, top, bottom):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[k] + (bottom[k] - top[k]) * t) for k in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def icon():
    flame(512).convert("RGB").save(os.path.join(OUT, "icon-512.png"))


def feature_graphic():
    w, h = 1024, 500
    img = gradient(w, h, (0, 150, 136), TEAL_DARK).convert("RGBA")
    badge = rounded(flame(300), 66)
    shadow = Image.new("RGBA", (340, 340), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([20, 26, 320, 326], 66, fill=(0, 0, 0, 90))
    img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(12)), (40, 80))
    img.alpha_composite(badge, (60, 100))
    d = ImageDraw.Draw(img)
    d.text((410, 120), "Quit Challenge", font=font(72), fill=WHITE)
    d.text((412, 215), "Beat any habit,", font=font(44, False), fill=WHITE)
    d.text((412, 268), "one day at a time.", font=font(44, False), fill=WHITE)
    d.text((412, 350), "Private  ·  No account  ·  No ads", font=font(28, False), fill=(178, 223, 219))
    img.convert("RGB").save(os.path.join(OUT, "feature-graphic.png"))


def wrap(draw, text, fnt, width):
    lines, line = [], ""
    for word in text.split():
        trial = (line + " " + word).strip()
        if draw.textlength(trial, font=fnt) <= width:
            line = trial
        else:
            lines.append(line); line = word
    lines.append(line)
    return lines


def screenshots(raw_dir):
    out_dir = os.path.join(OUT, "screenshots")
    os.makedirs(out_dir, exist_ok=True)
    W, H = 1080, 1920
    for name, (title, sub) in CAPTIONS.items():
        src = os.path.join(raw_dir, name + ".png")
        if not os.path.exists(src):
            print("missing", src)
            continue
        canvas = gradient(W, H, (0, 150, 136), TEAL_DARK).convert("RGBA")
        d = ImageDraw.Draw(canvas)
        y = 70
        for line in wrap(d, title, font(64), W - 120):
            d.text((W // 2, y), line, font=font(64), fill=WHITE, anchor="ma"); y += 78
        for line in wrap(d, sub, font(36, False), W - 160):
            d.text((W // 2, y + 6), line, font=font(36, False), fill=(208, 236, 233), anchor="ma"); y += 48
        shot = Image.open(src).convert("RGB")
        top = y + 40
        avail = H - top - 60
        scale = avail / shot.size[1]
        sw, sh = int(shot.size[0] * scale), int(shot.size[1] * scale)
        shot = rounded(shot.resize((sw, sh), Image.LANCZOS), 36)
        x = (W - sw) // 2
        frame = Image.new("RGBA", (sw + 24, sh + 24), (0, 0, 0, 0))
        ImageDraw.Draw(frame).rounded_rectangle([0, 0, sw + 23, sh + 23], 46, fill=(20, 20, 20, 255))
        canvas.alpha_composite(frame, (x - 12, top - 12))
        canvas.alpha_composite(shot, (x, top))
        canvas.convert("RGB").save(os.path.join(out_dir, name + ".png"))
        print("framed", name)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    icon()
    feature_graphic()
    if len(sys.argv) > 1:
        screenshots(sys.argv[1])
