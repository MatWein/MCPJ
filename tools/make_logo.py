#!/usr/bin/env python3
"""Renders the mod logo (512x512): the jumper in front quarter view on a dark blue glow.

  python3 tools/make_logo.py

Output: core/src/main/resources/mcpj_logo.png (mod list icon on Forge, NeoForge and Fabric; also usable as the
CurseForge project avatar).
"""
import math
import os

import numpy as np
from PIL import Image, ImageFilter

from preview import ASSETS, ROOT, render

SIZE = 512
OUT = os.path.join(ROOT, 'core', 'src', 'main', 'resources', 'mcpj_logo.png')


def background(size):
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.hypot(x - size * 0.5, y - size * 0.46) / (size * 0.75)
    t = np.clip(r, 0, 1)[..., None]
    inner = np.array([38, 92, 150], dtype=np.float32)
    outer = np.array([6, 12, 28], dtype=np.float32)
    rgb = inner * (1 - t) ** 1.6 + outer * (1 - (1 - t) ** 1.6)
    # a soft ring of light behind the ship
    ring = np.exp(-((r * 0.75 - 0.36) / 0.035) ** 2)[..., None]
    rgb += ring * np.array([70, 150, 210], dtype=np.float32) * 0.55
    alpha = np.full((size, size, 1), 255, dtype=np.float32)
    return Image.fromarray(np.clip(np.concatenate([rgb, alpha], axis=2), 0, 255).astype(np.uint8), 'RGBA')


def main():
    obj = os.path.join(ASSETS, 'models', 'entity', 'puddle_jumper.obj')
    tex = Image.open(os.path.join(ASSETS, 'textures', 'entity', 'puddle_jumper.png'))
    ship = render(obj, tex, math.radians(35), math.radians(22), SIZE * 2, open_state=True, alpha=True, margin=0.95)
    ship = ship.crop(ship.getbbox())
    width = int(SIZE * 0.88)
    ship = ship.resize((width, int(ship.height * width / ship.width)), Image.LANCZOS)
    logo = background(SIZE)
    # dark drop shadow under the ship
    shadow = Image.new('RGBA', ship.size, (0, 0, 0, 0))
    shadow.putalpha(ship.getchannel('A').point(lambda a: a * 0.55))
    shadow = shadow.filter(ImageFilter.GaussianBlur(10))
    pos = ((SIZE - ship.width) // 2, (SIZE - ship.height) // 2 + 10)
    logo.alpha_composite(shadow, (pos[0] + 6, pos[1] + 16))
    logo.alpha_composite(ship, pos)
    logo.convert('RGB').save(OUT, optimize=True)
    print('wrote', OUT)


if __name__ == '__main__':
    main()
