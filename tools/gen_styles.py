"""Tries the village in a few art styles: the valley map and one house each, into art/styles/.

    python tools/gen_styles.py
"""
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_art  # noqa: E402

OUT = gen_art.ROOT / "art" / "styles"
gen_art.OUT = OUT

HEAD = gen_art.HEAD
STYLES = {
    "muted": "Style: calm pixel art with a small restrained palette of about 20 colours: soft desaturated sage greens, dusty blues, warm greys "
             "and faded terracotta, low contrast, gentle evening light, quiet and airy, few details, lots of empty calm space, "
             "no shiny highlights, no outlines on everything, like a faded storybook page.",
    "watercolor": "Style: a hand-drawn storybook illustration in fine brown ink lines with loose transparent watercolour washes on warm cream paper, "
                  "soft muted colours, visible paper grain, uneven hand-made lines, gentle and quiet, NOT pixel art, NOT 3D, NOT glossy.",
    "print": "Style: a risograph / linocut print in only three inks – deep teal green, warm brick red and mustard yellow – on cream paper, "
             "flat shapes, bold simple forms, slight print misregistration and grain, decorative and graphic, NOT pixel art, NOT gradients.",
}
VALLEY = """Image: the background map of a small village, portrait 2:3.
{style}
A valley seen from above at a gentle three-quarter angle. A dirt path winds in a soft S-shape from the top edge to the bottom edge;
a little stream crosses it once under a small plank bridge; a few trees and bushes at the edges, hills only in the top strip.
NO buildings, NO animals, NO people, NO text. Beside the path leave EIGHT open flat grassy places for houses that are added later:
four on the left of the path and four on the right, alternating down the slope, each about a quarter of the image wide; they should feel
natural (open meadow), not drawn as ovals or outlined patches."""
HOUSE = """Image: one small building for the same village map, seen from the front and slightly above.
{style}
A small Japanese-style tea house on a mossy rock ledge, curved tiled roof with upturned corners, a round moon window, a wooden veranda,
a paper lantern. NO faces, NO characters, NO text. One building centered on a flat solid pure magenta (#FF00FF) background with clear margin."""


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    jobs = []
    for k, st in STYLES.items():
        jobs.append((f"{k}_valley", HEAD + VALLEY.format(style=st)))
        jobs.append((f"{k}_house", HEAD + HOUSE.format(style=st)))
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(gen_art.run, jobs))


if __name__ == "__main__":
    main()
