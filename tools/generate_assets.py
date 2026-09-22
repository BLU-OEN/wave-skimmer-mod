"""Generates the Wave Skimmer's black & white textures and its block model.

Run from the project root:  python tools/generate_assets.py
"""
import json
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/waveskimmer"
TEX_DIR = ASSETS / "textures/block"
MODEL_PATH = ASSETS / "models/block/wave_skimmer.json"

BLACK = (24, 24, 27)
BLACK_L = (44, 44, 49)
BLACK_D = (11, 11, 13)
GREY = (96, 96, 102)
GREY_D = (58, 58, 63)
WHITE = (234, 234, 236)
WHITE_L = (250, 250, 250)
WHITE_D = (196, 196, 202)


def write_png(path, pixels):
    """pixels: 16 rows of 16 (r, g, b) tuples."""
    raw = b"".join(b"\x00" + bytes(c for px in row for c in (*px, 255)) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def canvas(color, noise=4, seed=0):
    rng = random.Random(seed)
    return [[tuple(max(0, min(255, c + rng.randint(-noise, noise))) for c in color) for _ in range(16)]
            for _ in range(16)]


def bevel(img, light, dark):
    for i in range(16):
        img[0][i] = light
        img[i][0] = light
        img[15][i] = dark
        img[i][15] = dark


def black_panel():
    img = canvas(BLACK, seed=1)
    bevel(img, BLACK_L, BLACK_D)
    return img


def white_panel():
    img = canvas(WHITE, seed=2)
    bevel(img, WHITE_L, WHITE_D)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img[y][x] = GREY
    return img


def body_front():
    """White housing face with a black band and status lights."""
    img = white_panel()
    for y in range(6, 10):
        for x in range(1, 15):
            img[y][x] = BLACK
    for x in range(1, 15):
        img[5][x] = WHITE_D
    img[7][4] = img[7][5] = (80, 200, 255)   # blue "generating" light
    img[7][10] = img[7][11] = WHITE_L
    return img


def hull_side():
    """Black hull with a white waterline stripe near the top."""
    img = canvas(BLACK, seed=3)
    for x in range(16):
        img[0][x] = BLACK_L
        img[1][x] = WHITE
        img[2][x] = WHITE_D
        for y in range(11, 16):
            img[y][x] = BLACK_D
    return img


def engine():
    """Black engine block with cooling fins."""
    img = canvas(BLACK, seed=4)
    for y in range(16):
        for x in range(16):
            if y % 3 == 1:
                img[y][x] = BLACK_L
            elif y % 3 == 2:
                img[y][x] = BLACK_D
    for y in range(16):
        img[y][0] = img[y][15] = GREY_D
    return img


def port():
    """Cable port: white collar ring around a dark socket with pins."""
    img = canvas(BLACK, noise=2, seed=5)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 3.2:
                img[y][x] = BLACK_D
            elif d <= 5.0:
                img[y][x] = GREY_D
            elif d <= 7.0:
                img[y][x] = WHITE if d <= 6.2 else WHITE_D
    for x, y in ((6, 6), (9, 6), (7, 9), (8, 9)):
        img[y][x] = GREY
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        img[y][x] = GREY
    return img


def intake():
    """Water intake / outlet grille."""
    img = canvas(BLACK_D, noise=2, seed=6)
    bevel(img, BLACK_L, BLACK_L)
    for y in range(1, 15):
        for x in range(2, 14, 3):
            img[y][x] = GREY
    return img


def blade():
    """Skimmer blade: black & white diagonal stripes."""
    img = canvas(WHITE, noise=3, seed=7)
    for y in range(16):
        for x in range(16):
            if ((x + y) // 4) % 2 == 0:
                img[y][x] = BLACK
    return img


TEXTURES = {
    "black": black_panel, "white": white_panel, "body_front": body_front, "hull_side": hull_side,
    "engine": engine, "port": port, "intake": intake, "blade": blade,
}


# --- model -------------------------------------------------------------------------------------

def face_size(face, frm, to):
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz)}[face]


def uv(face, frm, to, mode):
    if mode == "full":
        return [0, 0, 16, 16]
    w, h = (min(16, s) for s in face_size(face, frm, to))
    u0 = (16 - w) / 2
    v0 = 0 if mode == "top" else (16 - h) / 2
    return [u0, v0, u0 + w, v0 + h]


def box(name, frm, to, textures, modes=None, rotation=None):
    """textures: face -> texture key, with '*' as the default for unlisted faces."""
    modes = modes or {}
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        tex = textures.get(face, textures.get("*"))
        faces[face] = {"uv": uv(face, frm, to, modes.get(face, "crop")), "texture": "#" + tex}
    element = {"name": name, "from": frm, "to": to, "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


# North is the front (intake + skimmer blade). The side cable port is on the east.
ELEMENTS = [
    # Twin floats and the hull, sitting a few pixels down into the water
    box("left_float", [0, -3, 1], [3, 1, 15], {"*": "black", "east": "hull_side", "west": "hull_side"},
        {"east": "top", "west": "top"}),
    box("right_float", [13, -3, 1], [16, 1, 15], {"*": "black", "east": "hull_side", "west": "hull_side"},
        {"east": "top", "west": "top"}),
    box("hull", [3, -2, 2], [13, 2, 14],
        {"*": "black", "east": "hull_side", "west": "hull_side", "north": "intake", "south": "intake", "up": "white"},
        {"east": "top", "west": "top", "north": "full", "south": "full"}),
    box("skimmer_blade", [3, -2.5, -1], [13, -1.5, 4], {"*": "black", "up": "blade", "down": "blade"},
        {"up": "full", "down": "full"}, rotation={"origin": [8, -2, 4], "axis": "x", "angle": 22.5}),
    box("rear_outlet", [5, -2, 14], [11, 1, 16], {"*": "black", "south": "intake"}, {"south": "full"}),

    # Deck and white housing
    box("deck", [2, 2, 3], [14, 3, 13], {"*": "black"}),
    box("housing", [3, 3, 4], [13, 8, 12], {"*": "white", "north": "body_front"}),
    box("side_port", [13, 3.5, 6], [15, 7.5, 10], {"*": "black", "east": "port"}, {"east": "full"}),

    # Small engine on top with its own cable port
    box("engine", [5, 8, 5], [11, 12, 11], {"*": "engine", "up": "black", "down": "black"}),
    box("flywheel_left", [4, 9, 6], [5, 12, 10], {"*": "white"}),
    box("flywheel_right", [11, 9, 6], [12, 12, 10], {"*": "white"}),
    box("exhaust", [9, 8, 11], [10.5, 13.5, 12.5], {"*": "black", "up": "intake"}),
    box("engine_cap", [5, 12, 5], [11, 13, 11], {"*": "white"}),
    box("top_port", [6, 13, 6], [10, 15, 10], {"*": "black", "up": "port"}, {"up": "full"}),
]

MODEL = {
    "parent": "minecraft:block/block",
    "ambientocclusion": False,
    "render_type": "minecraft:cutout",
    "textures": {"particle": "waveskimmer:block/white",
                 **{k: f"waveskimmer:block/{k}" for k in TEXTURES}},
    "elements": ELEMENTS,
}


def main():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    for name, fn in TEXTURES.items():
        write_png(TEX_DIR / f"{name}.png", fn())
    MODEL_PATH.write_text(json.dumps(MODEL, indent=2) + "\n")
    print(f"Wrote {len(TEXTURES)} textures and {MODEL_PATH.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
