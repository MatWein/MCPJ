#!/usr/bin/env python3
"""Builds the jumper projector block textures (32x32), in the style of the charging pad (make_pad_textures.py).

Output: core/src/main/resources/assets/mcpj/textures/block/
  jumper_projector_top_{idle,building}.png    emitter lens with a ring of lights, arrow (points north = up)
  jumper_projector_side_{idle,building}.png   steel panel with a glowing band of Ancient-style lines
The bottom uses charging_pad_bottom.png.
"""
import math
import os

from make_pad_textures import N, OUT, frame, glow, steel

MODES = {
    # lens core, lens ring, lights, band
    'idle': ((60, 90, 110), (50, 60, 70), (70, 80, 90), (45, 70, 85)),
    'building': ((210, 250, 255), (90, 200, 240), (150, 230, 255), (110, 220, 255)),
}


def top(mode):
    core, ring, light, _ = MODES[mode]
    img = steel(4, base=(46, 50, 54), var=4)
    px = img.load()
    frame(img, light=(66, 70, 74), dark=(28, 30, 33))
    cx = cy = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - cx, y - cy)
            if d < 4.5:
                t = d / 4.5
                px[x, y] = tuple(int(c * (1 - 0.35 * t)) for c in core)
            elif d < 6.5:
                px[x, y] = ring
            elif d < 7.5:
                px[x, y] = (30, 33, 36)
    # ring of lights around the lens
    for k in range(8):
        a = k * math.pi / 4
        glow(px, int(round(cx + 10 * math.cos(a))), int(round(cy + 10 * math.sin(a))), light)
    # arrow pointing north at the top edge
    for k in range(4):
        for x in (15 - k, 16 + k):
            px[x, 2 + k] = light
    return img


def side(mode):
    _, _, light, band = MODES[mode]
    img = steel(5)
    px = img.load()
    frame(img)
    for x in range(N):
        px[x, 1] = (80, 84, 88)
        px[x, 2] = (36, 39, 42)
    # band with Ancient-style line pattern
    for y in range(11, 21):
        for x in range(3, N - 3):
            px[x, y] = (30, 34, 38)
    for x in range(4, N - 4):
        if x % 6 in (0, 1, 2, 3):
            px[x, 13] = band
            px[x, 18] = band
        if x % 6 in (2, 5):
            for y in range(14, 18):
                px[x, y] = band
    for x in (5, N - 6):
        glow(px, x, 6, light)
        glow(px, x, N - 7, light)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for mode in MODES:
        top(mode).save(os.path.join(OUT, f'jumper_projector_top_{mode}.png'))
        side(mode).save(os.path.join(OUT, f'jumper_projector_side_{mode}.png'))
    print('Textures written to', OUT)


if __name__ == '__main__':
    main()
