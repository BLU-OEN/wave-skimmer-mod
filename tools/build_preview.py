"""Builds wave-skimmer-design.html: an interactive 3D preview of the real model and textures.

Run from the project root after generate_assets.py:  python tools/build_preview.py
"""
import base64
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/waveskimmer"

model = json.loads((ASSETS / "models/block/wave_skimmer.json").read_text())
textures = {
    name: "data:image/png;base64," + base64.b64encode((ASSETS / f"textures/block/{name}.png").read_bytes()).decode()
    for name in (key for key in model["textures"] if key != "particle")
}

html = (ROOT / "tools/preview_template.html").read_text(encoding="utf-8")
html = html.replace("__MODEL_JSON__", json.dumps(model)).replace("__TEXTURES_JSON__", json.dumps(textures))
out = ROOT / "wave-skimmer-design.html"
out.write_text(html, encoding="utf-8")
print(f"Wrote {out.relative_to(ROOT)}")
