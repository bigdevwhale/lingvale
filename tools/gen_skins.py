"""Generates the alternative house designs sold in the shop, with the Codex CLI image tool (see gen_art.py).

Each house gets one sheet art/sheets/skin_<look>.png: 2 columns (day, night) x 2 rows (design 1, design 2).
Cut them with `python tools/cut.py` into house_<look>_s<design>_<0|1>.

    python tools/gen_skins.py              # everything missing
    python tools/gen_skins.py tea cinema   # only these
"""
import sys
from concurrent.futures import ThreadPoolExecutor

from gen_art import HEAD, OBJ, VIEW, run

SKINS = {
    "tea": ("a round greenhouse tea pavilion of glass panes in a white iron frame with a copper dome, potted tea bushes inside, a little stone path",
            "a small mountain tea hut of dark wood with a thick thatched roof, a stone lantern, a bamboo fence and a tiny stone basin with water"),
    "gym": ("a wooden martial-arts dojo with a sloped grey tiled roof, a veranda with a hanging punching bag and a rack of wooden staffs",
            "a sporty boathouse of blue painted planks by a little wooden dock, crossed oars over the door, a small rowing boat at the dock"),
    "burrow": ("a mushroom house: a giant red mushroom cap with white spots as the roof, a round wooden door and small windows in the thick stem, a tiny fence",
               "a little stone cottage with a green turf roof full of flowers, a round window like a big gold coin, a small vegetable patch and a wheelbarrow"),
    "tower": ("a small round observatory: a white stone base with a big silver dome, a long brass telescope poking out of the open slit, a spiral stair outside",
              "a slender spire of pale blue crystal on a round mossy stone base, tiny floating lights around the tip, an arched silver door"),
    "tavern": ("a cosy log-cabin inn with a wide porch, a rocking chair, hanging lanterns and a sign shaped like a wagon wheel (NO letters)",
               "a small stone keep turned into a tavern: grey stone walls, a round corner turret with a plain red flag, a heavy wooden door, barrels"),
    "studio": ("a little retro diner-style house shaped like a jukebox: a rounded arched top with colourful glowing stripes, chrome trim, a round door",
               "a white wooden bandstand-cottage with a round roof, big open doors showing a small piano inside, a music-note weathervane"),
    "desk": ("a short white-and-red striped lighthouse with a small attached cottage, a round lamp room on top, a writing desk visible in the window",
             "a little house shaped like an old typewriter: a cream metal body as walls, round keys as porch steps, a big sheet of paper sticking out of the roof like a sail"),
    "reading": ("a treehouse library built into a big leafy oak: a wooden cabin among the branches, a rope ladder, a little balcony with a stack of books",
                "a cosy round stone library tower with a domed copper roof, a big round stained-glass window with flower patterns only, a chimney of stacked books"),
    "cinema": ("an open-air cinema in a red-and-white striped circus tent, a film projector on a little pole in front, strings of bunting",
               "a grand art-deco movie palace with golden trim, a tall vertical sign covered only in light bulbs (NO letters), a red carpet at the door"),
}


def prompt(a, b):
    return HEAD + f"""Image: two different buildings for a cozy village map, each shown by day and by night.
{OBJ}
{VIEW}
Layout: 2 columns x 2 rows of equal cells on a flat solid pure magenta (#FF00FF) background. One building per cell, centered, standing on a small
patch of grass, with clear margin; nothing touches the cell borders. No grid lines, no shadows on the background.
Left column: by day, windows show dark glass. Right column: EXACTLY the same building as on its left, same size and position, by night:
every window glows warm yellow-orange, lanterns are lit, the walls a bit darker and bluish.
Top row building: {a}.
Bottom row building: {b}."""


def main():
    want = sys.argv[1:]
    jobs = [(f"skin_{k}", prompt(a, b)) for k, (a, b) in SKINS.items() if not want or k in want]
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
