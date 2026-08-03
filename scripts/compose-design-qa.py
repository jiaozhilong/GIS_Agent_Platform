from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "reference" / "ui-design-board.png"
OUT = ROOT / "design-qa-artifacts"

mapping = [
    ("login", 0, 0, "01-login.png"),
    ("dashboard", 1, 0, "02-dashboard.png"),
    ("project-overview", 0, 1, "03-project-overview.png"),
    ("requirements", 1, 1, "04-requirements.png"),
    ("product-match", 0, 2, "05-product-match.png"),
    ("proposal", 1, 2, "07-proposal.png"),
]

source = Image.open(SOURCE).convert("RGB")
half = source.width // 2
third = source.height // 3
pane_w, pane_h, label_h = 720, 512, 28

def contain(image: Image.Image, width: int, height: int) -> Image.Image:
    copy = image.copy()
    copy.thumbnail((width, height), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (width, height), "#02070d")
    canvas.paste(copy, ((width - copy.width) // 2, (height - copy.height) // 2))
    return canvas

for name, col, row, implementation_name in mapping:
    crop = source.crop((col * half, row * third, (col + 1) * half, (row + 1) * third))
    implementation = Image.open(OUT / implementation_name).convert("RGB")
    result = Image.new("RGB", (pane_w * 2, pane_h + label_h), "#02070d")
    result.paste(contain(crop, pane_w, pane_h), (0, label_h))
    result.paste(contain(implementation, pane_w, pane_h), (pane_w, label_h))
    draw = ImageDraw.Draw(result)
    draw.text((12, 7), "SOURCE DESIGN", fill="#7bdcff")
    draw.text((pane_w + 12, 7), "IMPLEMENTATION", fill="#7bdcff")
    draw.line((pane_w, 0, pane_w, pane_h + label_h), fill="#1d5d85", width=2)
    result.save(OUT / f"compare-{name}.png", quality=92)

print(f"source={source.size}; pairs={len(mapping)}")
