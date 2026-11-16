"""Cuts the Codex sheets in art/sheets into app drawables.

    python tools/cut.py

valley_bg (the map), house_<look>_0/1 (day / night), plot_0/1 (empty / moving in), decor_<id>, icon_<id>.
The magenta background is keyed out; each object is cropped to itself.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

MAX = 512


def key(im):
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = min(r, b) - g  # how magenta
            if m > 90:
                px[x, y] = (0, 0, 0, 0)
            elif m > 40:
                # a fringe: pull the pink out and fade it
                k = (m - 40) / 50
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(255 * (1 - k)))
    return im


def border_clean(cell):
    """Drops bits of the neighbouring cells: anything opaque that touches the cell's edge."""
    import numpy as np
    from scipy import ndimage
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = max(range(1, n + 1), key=lambda k: sizes[k]) if n else 0
    for k in edge:
        if k != big:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(sheet, cols, rows, mx=MAX):
    im = key(Image.open(sheet))
    w, h = im.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = border_clean(im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch))))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            s = mx / max(cell.size)
            if s < 1:
                cell = cell.resize((max(1, round(cell.width * s)), max(1, round(cell.height * s))), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


LOOKS = ["tea", "gym", "burrow", "tower", "tavern", "studio", "desk", "reading", "cinema"]
DECOR = ["well", "lamp", "bench", "flowers", "fountain", "windmill", "signpost", "postbox",
         "haycart", "tent", "campfire", "appletree", "beehives", "board", "gazebo", "balloon"]
ICONS = ["firefly", "jar", "scroll", "flags"]
GLYPHS = ["flame", "trophy", "hourglass", "checklist", "sunrise", "sun", "moon", "calendar_small",
          "up", "down", "egg", "sprout", "crown", "book", "magnifier", "link"]
NAV = ["nav_village", "nav_album", "nav_days"]
FRAMES = ["frame_oval", "frame_stamp", "seal", "plaque", "calendar", "banner", "star", "postmark"]


def skins():
    """The house designs from the shop (tools/gen_skins.py): house_<look>_s<design>_<0|1>."""
    for look in LOOKS:
        f = SHEETS / f"skin_{look}.png"
        if f.exists():
            for n, fr in enumerate(frames(f, 2, 2)):
                save(fr, f"house_{look}_s{n // 2 + 1}_{n % 2}")
            print("skins", look)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    skins()
    for look in LOOKS:
        f = SHEETS / f"{look}.png"
        if f.exists():
            for n, fr in enumerate(frames(f, 2, 1)):
                save(fr, f"house_{look}_{n}")
            print("house", look)
    if (SHEETS / "plot.png").exists():
        for n, fr in enumerate(frames(SHEETS / "plot.png", 2, 1)):
            save(fr, f"plot_{n}")
        print("plot")
    if (SHEETS / "decor.png").exists():
        for name, fr in zip(DECOR, frames(SHEETS / "decor.png", 4, 4)):
            fr.thumbnail((256, 256))
            save(fr, f"decor_{name}")
        print("decor")
    if (SHEETS / "icons.png").exists():
        for name, fr in zip(ICONS, frames(SHEETS / "icons.png", 4, 1)):
            fr.thumbnail((160, 160))
            save(fr, f"icon_{name}")
        print("icons")
    if (SHEETS / "frames.png").exists():
        for name, fr in zip(FRAMES, frames(SHEETS / "frames.png", 4, 2)):
            fr.thumbnail((320, 320))
            save(fr, name)
        print("frames")
    if (SHEETS / "glyphs.png").exists():
        for name, fr in zip(GLYPHS, frames(SHEETS / "glyphs.png", 4, 4)):
            fr.thumbnail((128, 128))
            save(fr, f"g_{name}")
        print("glyphs")
    if (SHEETS / "nav.png").exists():
        for name, fr in zip(NAV, frames(SHEETS / "nav.png", 3, 1)):
            fr.thumbnail((160, 160))
            save(fr, name)
        print("nav")
    if (SHEETS / "masthead.png").exists():
        save(frames(SHEETS / "masthead.png", 1, 1, mx=1100)[0], "masthead")
        print("masthead")
    if (SHEETS / "paper.png").exists():
        g = Image.open(SHEETS / "paper.png").convert("RGB").resize((512, 512), Image.LANCZOS)
        g = Image.blend(g, Image.new("RGB", g.size, (244, 236, 216)), .62)  # calmer: the paper sits under text
        g.save(OUT / "paper_tex.webp", "WEBP", quality=88, method=6)
        print("paper")
    if (SHEETS / "valley.png").exists():
        g = Image.open(SHEETS / "valley.png").convert("RGB")
        g.save(OUT / "valley_bg.webp", "WEBP", quality=90, method=6)
        print("valley")


if __name__ == "__main__":
    main()
