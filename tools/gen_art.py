"""Generates the village art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py              # everything missing
    python tools/gen_art.py valley tea   # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying).\n\n"
STYLE = (
    "Style: calm pixel art with a restrained palette of about 24 colours: fresh soft greens, clear light sky blues, warm creams, "
    "sandy beige and soft terracotta; bright gentle daylight, cheerful but not oversaturated; soft low-contrast shading, "
    "thin outlines only where needed, airy, few details, lots of calm open space, no shiny highlights, like a sunny storybook page."
)
OBJ = STYLE + " These are plain buildings and objects: NO faces, NO eyes, NO characters, NO animals, NO people, NO text or letters anywhere."
VIEW = "Seen from the front and slightly above (three-quarter top-down view, the same angle as a cozy farming game map)."

HOUSES = {
    "cinema": "a tiny old-fashioned village cinema with a curved art-deco front, a marquee board with only light bulbs around it (NO letters), "
              "a big film reel emblem above the door, a small round projection window, red velvet curtain seen through the glass door, a popcorn cart by the entrance",
    "tea": "a small Japanese-style tea house standing on a grey mossy rock ledge, a curved red tiled pagoda roof with upturned corners, "
           "cream paper walls with dark wooden posts, a big round moon window, a little wooden veranda with a cushion, a red paper lantern by the door",
    "gym": "a sturdy red-brick gym building with a flat tar roof, a row of half-sunken basement windows at ground level, a green steel door, "
           "a wooden sign board on the roof showing only a picture of a dumbbell, a brick chimney, a pair of kettlebells by the door",
    "burrow": "a cozy burrow home dug into a round grassy hillock under the roots of a small oak tree, a round green wooden door with a brass knob, "
              "two small round windows, a crooked chimney pipe poking out of the grass, a little wooden mailbox and a basket of acorns",
    "tower": "a tall slender stone tower of a magic academy, a deep purple conical roof with a small golden star on the tip, arched windows, "
             "a tiny balcony with a brass telescope, ivy climbing the stones, an arched wooden door",
    "tavern": "a cozy two-storey timber-framed tavern with white plaster and dark beams, a steep red roof, a hanging shield-shaped sign with only "
              "a picture of a six-sided die, barrels and a small bench by the door, a stone chimney",
    "studio": "a little pastel-pink cottage that is a music studio, a big shiny brass gramophone horn on the roof, a round window shaped like a vinyl record, "
              "a blue door, a few flower pots",
    "desk": "a small house shaped like one giant mechanical keyboard key: square cream keycap walls with a big rounded dark blue keycap as the roof, "
            "small windows on the sides, a quill pen weathervane, a stack of paper by the door",
    "reading": "a crooked whimsical reading hut built from stacked giant old books, a pointed violet roof like a witch hat, a round window, "
               "a candle lantern by the door, a crescent moon weathervane, a few loose pages",
    "lodge": "a small cozy log lodge at the edge of a forest: walls of dark round logs, a steep mossy shingle roof, a wide porch with a lantern "
             "hanging by the door, a stone chimney, a neat pile of firewood, a low wooden fence with an open gate, a few pinecones on the porch steps",
}


def house_prompt(desc):
    return HEAD + f"""Image: two versions of the same building for a cozy village map, side by side.
{OBJ}
{VIEW}
Layout: 2 columns x 1 row of equal cells on a flat solid pure magenta (#FF00FF) background. One building per cell, centered, standing on a small
patch of grass, with clear margin; nothing touches the cell borders. No grid lines, no shadows on the background.
Left cell: the building by day, windows show dark glass.
Right cell: EXACTLY the same building, same size and position, by night: every window glows warm yellow-orange, lanterns are lit, the walls a bit darker and bluish.
The building: {desc}."""


OTHER = {
    "valley": HEAD + f"""Image: the background map of a cozy pixel village, portrait 2:3.
{STYLE}
A green summer valley seen from above at a three-quarter top-down angle like a farming game map. A sandy dirt path winds in a gentle S-shape
from the top edge down to the bottom edge through the middle. A little stream crosses the path once, with a small wooden plank bridge.
A few trees and bushes grow along the left and right edges; distant hills and a little sky only in the top strip.
IMPORTANT: NO buildings, NO animals, NO people, NO text. Beside the path leave EIGHT big flat empty light-green grass clearings for houses that are
added later: four along the left side of the path and four along the right side, alternating as the path goes down, each clearing about a quarter
of the image wide, with nothing on them. The clearings must feel natural (open meadow), not drawn as ovals or outlined patches.""",
    "plot": HEAD + f"""Image: two versions of an empty building plot for a cozy village map, side by side.
{OBJ}
{VIEW}
Layout: 2 columns x 1 row of equal cells on a flat solid pure magenta (#FF00FF) background. One plot per cell, centered, with clear margin;
nothing touches the cell borders. No grid lines.
Left cell: an empty plot: a square patch of bare earth, a low wooden fence around it, a wooden signpost with a blank board.
Right cell: the same plot with moving-in things on it: stacked wooden crates, a small hand cart, a rolled rug, the same blank signpost.""",
    "decor": HEAD + f"""Image: a sprite sheet of 16 village decorations for a cozy village map, each a separate object.
{OBJ}
{VIEW}
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin;
nothing touches the cell borders. No grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a round stone well with a little wooden roof and a bucket, 2 an iron street lamp with a warm glowing lantern, 3 a wooden park bench,
4 a flower bed with tulips in a wooden frame, 5 a small round stone fountain with water, 6 a small wooden windmill with sails,
7 a wooden signpost with three blank arrow boards, 8 a red post box on a post, 9 a hay cart with a pumpkin,
10 a striped red-and-white fair tent with a little flag, 11 a campfire ringed with stones with two log seats, 12 an apple tree with red apples,
13 two straw beehives among flowers, 14 a wooden notice board with blank pinned papers, 15 a small white wooden gazebo,
16 a striped hot air balloon tethered to the ground with ropes.""",
    "icons": HEAD + f"""Image: a sprite sheet of 4 small game icons for a cozy village game.
{STYLE} Plain objects, NO faces, NO eyes, NO text.
Layout: 4 columns x 1 row of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin.
Objects in order: 1 a single glowing firefly, warm yellow light around its tail, 2 a glass jar full of glowing fireflies with a cork,
3 a rolled parchment quest scroll tied with a red ribbon, 4 a small string of colourful triangle festival flags.""",
}

OTHER["paper"] = HEAD + """Image: a seamless tileable texture of warm old cream parchment paper for a cozy pixel game, square.
""" + STYLE + """ Only the paper: soft fibres, faint speckles and gentle tone variation, low contrast, light colour. NO objects, NO text, NO edges, NO stains in the corners, NO vignette."""

OTHER["frames"] = HEAD + """Image: a sprite sheet of 8 decorative UI ornaments for a cozy pixel village game.
""" + OBJ + """
Layout: 4 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin;
nothing touches the cell borders. No grid lines.
Objects in order: 1 an ornate oval golden portrait frame, its empty inside filled with flat pure magenta (#FF00FF),
2 a white postage stamp border with perforated zigzag edges, its empty inside filled with flat pure magenta,
3 a red wax seal with an embossed leaf, 4 a small blank wooden name plaque with two nails,
5 a blank paper wall calendar page hanging on a nail, with a red top binding and an empty white page,
6 a blank cream ribbon banner with folded ends, 7 a shiny gold star, 8 an inked round postmark stamp circle with wavy lines, dark blue ink."""

OTHER["masthead"] = HEAD + """Image: a decorative header vignette for a little village newspaper, a wide horizontal strip in the middle of the image.
""" + STYLE + """ NO text, NO letters, NO people, NO animals.
The strip shows a tiny pixel valley village: rolling green hills, a winding path, a few small cottages with red roofs, a windmill, a sun with rays,
framed on the left and right by two small round trees, like a newspaper masthead illustration. Everything outside the strip is flat solid pure magenta (#FF00FF)."""

OTHER["glyphs"] = HEAD + """Image: a sprite sheet of 16 small game icons for a cozy pixel village game, all the same size and style.
""" + STYLE + """ Plain objects, NO faces, NO eyes, NO text or numbers.
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One icon per cell, centered, filling about
70% of the cell, with clear margin; nothing touches the cell borders. No grid lines.
Icons in order (left to right, top to bottom):
1 a small campfire flame, 2 a golden trophy cup, 3 an hourglass with sand, 4 a clipboard with three ticked boxes,
5 a sun rising over a hill, 6 a bright round midday sun, 7 a crescent moon with two little stars, 8 a small paper calendar page with a red top,
9 a green arrow pointing up and to the right, 10 a red arrow pointing down and to the right, 11 a speckled egg, 12 a small green sprout in soil,
13 a golden crown, 14 a closed brown leather book, 15 a magnifying glass, 16 two small houses connected by a dotted path."""

OTHER["nav"] = HEAD + """Image: a sprite sheet of 3 navigation icons for a cozy pixel village game, all the same size and style.
""" + STYLE + """ Plain objects, NO faces, NO eyes, NO text.
Layout: 3 columns x 1 row of equal square cells on a flat solid pure magenta (#FF00FF) background. One icon per cell, centered, filling about
75% of the cell, with clear margin; nothing touches the cell borders. No grid lines.
Icons in order: 1 a small cozy cottage with a red roof and a puff of chimney smoke, 2 an open stamp album with a few colourful postage stamps on its pages,
3 a folded village newspaper next to a small desk calendar."""

OTHER["appicon"] = HEAD + """Image: the subject of an app launcher icon for a cozy pixel village game, one object.
""" + STYLE + """ NO text, NO letters, NO frame, NO background scene, NO faces.
A tiny village on a small round green hill: a little cottage with a red roof and a lit window, a small windmill behind it, a round tree,
and one big glowing firefly hovering above the roof. Compact and bold, readable at small size, centered,
filling about 80% of the image, on a flat solid pure magenta (#FF00FF) background with clear margin. Square image."""


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
    p = image_of((r.stdout or "") + (r.stderr or ""))
    if p is None:
        print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = list(OTHER.items()) + [(k, house_prompt(v)) for k, v in HOUSES.items()]
    jobs = [j for j in jobs if not want or j[0] in want]
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
