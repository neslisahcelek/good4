"""Exports landing-page images at the exact pixel sizes they are shown, so the browser only
rotates them and never downscales (downscaling + rotation makes text wobbly).

  python3 tools/export-landing-screens.py

Screens: design/landing-screens/*.png (full-resolution simulator screenshots).
Writes:  firebase/v2/web/public/landing/<name>@{1,2,3}x.webp
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SCREENS = ROOT / "design/landing-screens"
OUT = ROOT / "firebase/v2/web/public/landing"

# CSS width of the iPhone screen in LandingPage.css: 290px device minus 3.5% bezel on each side.
SCREEN_CSS_WIDTH = 270


def save(image: Image.Image, width: int, path: Path) -> None:
    height = round(image.height * width / image.width)
    image.resize((width, height), Image.Resampling.LANCZOS).save(path, quality=92, method=6)


for source in sorted(SCREENS.glob("*.png")):
    shot = Image.open(source).convert("RGB")
    for scale in (1, 2, 3):
        save(shot, SCREEN_CSS_WIDTH * scale, OUT / f"{source.stem}@{scale}x.webp")
    print(source.stem)

