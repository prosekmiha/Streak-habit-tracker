"""Sestavi pregledno sliko vseh zrenderiranih poz (za vizualno preverjanje)."""
import glob, os, sys
from PIL import Image, ImageDraw
src = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "res", "drawable-nodpi")
dst = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "out", "contact.png")
files = sorted(glob.glob(os.path.join(src, "mascot_*.webp")))
w, h = 240, 260
sheet = Image.new("RGB", (w * 5, (h + 20) * ((len(files) + 4) // 5)), "#4E55E0")
d = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(f).convert("RGBA").resize((w, h))
    x, y = (i % 5) * w, (i // 5) * (h + 20)
    sheet.paste(im, (x, y), im)
    d.text((x + 8, y + h + 4), os.path.basename(f)[7:-5], fill="white")
sheet.save(dst)
print(dst)
