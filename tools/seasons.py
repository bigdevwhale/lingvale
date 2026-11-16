"""Recolours the valley map into the seasons sold in the shop, keeping every pixel where it is (the houses' clearings,
the path and the stream stay put): valley_<season>.webp next to valley_bg.webp.

    python tools/seasons.py
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"


def hsv(a):
    return np.array(Image.fromarray(a).convert("HSV")).astype(np.float32) / 255


def rgb(h):
    return np.array(Image.fromarray((np.clip(h, 0, 1) * 255).astype(np.uint8), "HSV").convert("RGB"))


def masks(a):
    """Grass, foliage (trees and bushes), water, path and sky of the valley picture."""
    h = hsv(a)
    H, S, V = h[..., 0] * 360, h[..., 1], h[..., 2]
    r, g, b = [a[..., i].astype(int) for i in range(3)]
    green = (H > 55) & (H < 160) & (S > .25)
    water = (b > 170) & (r < 150) & (b > g)
    path = (r > 190) & (g > 140) & (g < 220) & (b < 175) & ~green
    sky = np.zeros_like(green); sky[: int(a.shape[0] * .075)] = True
    sky &= (b > r) & ~green
    # Foliage: green pixels darker or more saturated than the meadow, grown a little so whole crowns move together.
    meadow_v = np.median(V[green])
    dark = green & ((V < meadow_v - .1) | (S > np.median(S[green]) + .12))
    crowns = ndimage.binary_opening(dark, iterations=1)
    crowns = ndimage.binary_fill_holes(ndimage.binary_dilation(crowns, iterations=6)) & green
    # only big blobs are trees; small dark tufts in the meadow stay grass
    lab, n = ndimage.label(crowns)
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    foliage = np.isin(lab, [k for k in range(1, n + 1) if sizes[k] > 900])
    trunk = (H > 10) & (H < 40) & (S > .35) & (V < .55) & ~path
    return h, green & ~foliage, foliage, water, path, sky, trunk


def recolour(a, season):
    h, grass, foliage, water, path, sky, trunk = masks(a)
    out = h.copy()
    H, S, V = out[..., 0], out[..., 1], out[..., 2]

    def put(m, hue=None, sat=None, val=None):
        if hue is not None: H[m] = hue(H[m], S[m], V[m]) if callable(hue) else hue
        if sat is not None: S[m] = sat(S[m]) if callable(sat) else sat
        if val is not None: V[m] = val(V[m]) if callable(val) else val

    if season == "autumn":
        put(grass, hue=lambda h, s, v: 60 / 360 + (h - 85 / 360) * .4, sat=lambda s: np.clip(s * 1.05, 0, 1), val=lambda v: v * .99)
        # crowns from deep red to orange to gold by brightness
        put(foliage, hue=lambda h, s, v: (8 + 40 * np.clip((v - .3) / .6, 0, 1)) / 360, sat=lambda s: np.clip(s * 1.05, 0, .85))
        put(path, val=lambda v: v * .95)
        put(water, hue=205 / 360, sat=lambda s: s * .8)
        put(sky, sat=lambda s: s * .7)
    elif season == "winter":
        put(grass, hue=205 / 360, sat=lambda s: s * .1, val=lambda v: np.clip(.88 + (v - .7) * .45, 0, 1))
        put(foliage, hue=lambda h, s, v: 160 / 360 + h * 0, sat=lambda s: s * .45, val=lambda v: v * .75)
        # snow caps on the lighter half of each crown
        put(foliage & (V > np.percentile(V[foliage], 60)), hue=205 / 360, sat=.1, val=lambda v: np.clip(v + .25, 0, .97))
        put(path, hue=lambda h, s, v: h, sat=lambda s: s * .35, val=lambda v: np.clip(v * 1.02, 0, 1))
        put(water, hue=195 / 360, sat=lambda s: s * .45, val=lambda v: np.clip(v * 1.12, 0, 1))
        put(sky, sat=lambda s: s * .45, val=lambda v: np.clip(v * 1.03, 0, 1))
        put(trunk, sat=lambda s: s * .6)
    elif season == "spring":
        put(grass, hue=lambda h, s, v: h + 12 / 360, sat=lambda s: np.clip(s * .95, 0, 1), val=lambda v: np.clip(v * 1.02, 0, 1))
        # blossoming crowns: pink, the shadows a deeper rose
        put(foliage, hue=lambda h, s, v: (335 + 12 * np.clip((v - .3) / .6, 0, 1)) / 360, sat=lambda s: np.clip(s * .55, .2, .6),
            val=lambda v: np.clip(v * 1.12, 0, 1))
    elif season == "dusk":
        # a lilac evening valley: the meadow goes teal, the crowns violet, the water and the sky pink-violet
        put(grass, hue=lambda h, s, v: h + 40 / 360, sat=lambda s: s * .55, val=lambda v: v * .86)
        put(foliage, hue=lambda h, s, v: (270 + (h * 360 - 100) * .3) / 360, sat=lambda s: s * .7, val=lambda v: v * .9)
        put(water, hue=300 / 360, sat=lambda s: s * .6)
        put(sky, hue=lambda h, s, v: 290 / 360 + h * 0, sat=lambda s: s * .5)
        put(path, hue=lambda h, s, v: h + 5 / 360, sat=lambda s: s * .7, val=lambda v: v * .92)
    out[..., 0] = H % 1
    return rgb(out)


def paths(a):
    """The path laid with stone or brick, as overlays for any season: path_<kind>.webp, transparent off the path."""
    _, _, _, _, path, _, _ = masks(a)
    path = ndimage.binary_closing(path, iterations=2)
    path = ndimage.binary_opening(path, iterations=1)
    hgt, wid = path.shape
    path[int(.500 * hgt):int(.570 * hgt), int(.41 * wid):int(.57 * wid)] = False   # the bridge keeps its planks
    lab, n = ndimage.label(path)   # only the path itself, not the sandy banks of the stream
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    path = np.isin(lab, [k for k in range(1, n + 1) if sizes[k] > 20000])
    lum = a.astype(np.float32).mean(axis=2) / 255
    hgt, wid = path.shape
    yy, xx = np.mgrid[0:hgt, 0:wid]
    rng = np.random.default_rng(3)
    # cobbles: the nearest of scattered seeds, darker where two cells meet
    pts = np.stack([rng.uniform(0, hgt, 9000), rng.uniform(0, wid, 9000)], 1)
    from scipy.spatial import cKDTree
    d, idx = cKDTree(pts).query(np.stack([yy.ravel(), xx.ravel()], 1), k=2)
    edge = ((d[:, 1] - d[:, 0]) < 1.6).reshape(hgt, wid)
    tone = rng.uniform(-.06, .06, len(pts))[idx[:, 0]].reshape(hgt, wid)
    base = .62 + (lum - lum[path].mean()) * .6 + tone
    stone = np.stack([base * .97, base, base * 1.04], -1)
    stone[edge] *= .72
    # bricks: 18 x 9 px in a running bond, light mortar between
    row = yy // 9
    mortar = (yy % 9 == 0) | (((xx + (row % 2) * 9) % 18) == 0)
    bt = .55 + (lum - lum[path].mean()) * .5 + rng.uniform(-.05, .05, (hgt // 9 + 1, wid // 18 + 2))[row, (xx + (row % 2) * 9) // 18]
    brick = np.stack([bt * 1.25, bt * .68, bt * .52], -1)
    brick[mortar] = [.86, .80, .70]
    for name, img in (("stone", stone), ("brick", brick)):
        out = np.zeros((hgt, wid, 4), np.uint8)
        out[..., :3] = (np.clip(img, 0, 1) * 255).astype(np.uint8)
        out[..., 3] = path * 255
        Image.fromarray(out, "RGBA").save(RES / f"path_{name}.webp", "WEBP", quality=90, method=6)
        print("path", name)


def main():
    a = np.array(Image.open(RES / "valley_bg.webp").convert("RGB"))
    paths(a)
    for season in ("autumn", "winter", "spring", "dusk"):
        Image.fromarray(recolour(a, season)).save(RES / f"valley_{season}.webp", "WEBP", quality=90, method=6)
        print(season)


if __name__ == "__main__":
    main()
