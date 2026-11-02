#!/usr/bin/env python3
"""Builds the charging pad block textures (32x32, tileable).

Output: src/main/resources/assets/mcpj/textures/block/
  charging_pad_top_{off,powered,charging}.png   landing field: edge lights, center line, arrow (points north = up)
  charging_pad_side_{off,powered,charging}.png  steel panel with energy bar
  charging_pad_bottom.png
"""
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'mcpj', 'textures', 'block')
N = 32

MODES = {
    # marking, edge light, energy bar
    'off': ((70, 74, 78), (55, 60, 64), (40, 46, 50)),
    'powered': ((90, 200, 230), (255, 196, 80), (60, 170, 210)),
    'charging': ((190, 250, 255), (255, 235, 150), (150, 240, 255)),
}


def steel(seed, base=(58, 62, 66), var=6):
    rnd = random.Random(seed)
    img = Image.new('RGB', (N, N))
    px = img.load()
    for y in range(N):
        for x in range(N):
            v = rnd.randint(-var, var)
            px[x, y] = tuple(max(0, min(255, c + v)) for c in base)
    return img


def frame(img, light=(78, 82, 86), dark=(34, 37, 40)):
    px = img.load()
    for i in range(N):
        px[i, 0] = light
        px[0, i] = light
        px[i, N - 1] = dark
        px[N - 1, i] = dark


def glow(px, x, y, col, r=1):
    """Light dot with a soft halo."""
    for dy in range(-r - 1, r + 2):
        for dx in range(-r - 1, r + 2):
            xx, yy = x + dx, y + dy
            if not (0 <= xx < N and 0 <= yy < N):
                continue
            d = max(abs(dx), abs(dy))
            if d <= r - 1 or (r == 1 and d == 0):
                px[xx, yy] = col
            elif d <= r + 1:
                base = px[xx, yy]
                a = 0.45 if d == r else 0.2
                px[xx, yy] = tuple(int(b * (1 - a) + c * a) for b, c in zip(base, col))


def top(mode):
    mark, edge, _ = MODES[mode]
    img = steel(1, base=(44, 48, 52), var=4)
    px = img.load()
    # plate seams (2x2 plates)
    for i in range(N):
        px[i, 15] = (30, 33, 36)
        px[15, i] = (30, 33, 36)
        px[i, 16] = (60, 64, 68)
        px[16, i] = (60, 64, 68)
    frame(img, light=(66, 70, 74), dark=(28, 30, 33))
    # dashed center line across the seams
    for y in range(2, N - 2):
        if (y // 4) % 2 == 0:
            for x in (15, 16):
                px[x, y] = mark
    # chevron pointing north
    for k in range(7):
        for t in range(2):
            y = 10 + k + t
            for x in (15 - k - 2, 16 + k + 2):
                if 0 <= x < N and y < N:
                    px[x, y] = mark
    # edge lights; several pads in a row form a runway
    for y in (4, 12, 20, 28):
        glow(px, 3, y, edge)
        glow(px, 28, y, edge)
    return img


def side(mode):
    _, _, bar = MODES[mode]
    img = steel(2)
    px = img.load()
    frame(img)
    # top edge (landing surface)
    for x in range(N):
        px[x, 1] = (80, 84, 88)
        px[x, 2] = (36, 39, 42)
    # energy bar
    for y in (13, 14, 15, 16, 17, 18):
        for x in range(4, N - 4):
            px[x, y] = bar if 14 <= y <= 17 else tuple(int(c * 0.55) for c in bar)
    # rivets
    for x, y in ((3, 5), (N - 4, 5), (3, N - 5), (N - 4, N - 5)):
        px[x, y] = (96, 100, 104)
        px[x + 1, y + 1] = (30, 32, 35)
    return img


def bottom():
    img = steel(3, base=(52, 55, 58))
    frame(img)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for mode in MODES:
        top(mode).save(os.path.join(OUT, f'charging_pad_top_{mode}.png'))
        side(mode).save(os.path.join(OUT, f'charging_pad_side_{mode}.png'))
    bottom().save(os.path.join(OUT, 'charging_pad_bottom.png'))
    print('Textures written to', OUT)


if __name__ == '__main__':
    main()
