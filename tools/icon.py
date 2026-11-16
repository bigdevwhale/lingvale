"""The launcher icon: the little village from art/sheets/appicon.png in the adaptive icon's safe zone, and docs/logo.png.

    python tools/icon.py
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from cut import frames, SHEETS  # noqa: E402

TOP, LOW = (150, 206, 232), (214, 238, 222)  # a soft sky over the hill; icon_bg is the middle of it

subject = frames(SHEETS / "appicon.png", 1, 1, mx=1024)[0]
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
s = 290 / max(subject.size)
a = subject.resize((round(subject.width * s), round(subject.height * s)), Image.LANCZOS)
fg.paste(a, ((432 - a.width) // 2, (432 - a.height) // 2 + 4), a)
out = ROOT / "app/src/main/res/drawable-nodpi"
fg.save(out / "ic_launcher_fg.webp", "WEBP", lossless=True)

bg = Image.new("RGBA", (432, 432))
d = ImageDraw.Draw(bg)
for y in range(432):
    t = y / 431
    d.line([(0, y), (431, y)], fill=tuple(round(TOP[i] + (LOW[i] - TOP[i]) * t) for i in range(3)) + (255,))
bg.save(out / "ic_launcher_bg.webp", "WEBP", lossless=True)

logo = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
m = Image.new("L", (512, 512), 0); ImageDraw.Draw(m).rounded_rectangle((0, 0, 511, 511), 112, fill=255)
logo.paste(bg.resize((512, 512)), (0, 0), m)
big = fg.resize((512 * 432 // 330, 512 * 432 // 330), Image.LANCZOS)
logo.alpha_composite(big, ((512 - big.width) // 2, (512 - big.height) // 2))
(ROOT / "docs").mkdir(exist_ok=True)
logo.save(ROOT / "docs/logo.png")
print("icon ok")
