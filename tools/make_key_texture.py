#!/usr/bin/env python3
"""Builds the Puddle Jumper key item texture (16x16): a dark tech ID card with a chip and glowing status strip.

Output: core/src/main/resources/assets/mcpj/textures/item/jumper_key.png
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'core', 'src', 'main', 'resources', 'assets', 'mcpj', 'textures', 'item', 'jumper_key.png')

# . empty, O outline, H highlight edge, B body, D dark body, T glow, W glow core,
# G gold, Y gold light, N gold dark, L data line, S slot
CARD = [
    '................',
    '................',
    '..OOOOOOOOOOOO..',
    '.OHHHHHHSSHHHHO.',
    '.OBTTWTTTTTTWBO.',
    '.OBBBBBBBBBBBDO.',
    '.OBYYGGNBLLLLDO.',
    '.OBGNGNGBBBBBDO.',
    '.OBGNGNGBLLLBDO.',
    '.OBGGGNNBBBBBDO.',
    '.OBBBBBBBLLTBDO.',
    '.ODDDDDDDDDDDDO.',
    '..OOOOOOOOOOOO..',
    '................',
    '................',
    '................',
]

COLORS = {
    '.': (0, 0, 0, 0),
    'O': (16, 24, 32, 255),
    'H': (108, 128, 142, 255),
    'B': (62, 78, 92, 255),
    'D': (40, 52, 64, 255),
    'T': (64, 200, 225, 255),
    'W': (205, 255, 255, 255),
    'G': (226, 170, 58, 255),
    'Y': (255, 224, 130, 255),
    'N': (150, 100, 32, 255),
    'L': (138, 190, 204, 255),
    'S': (16, 24, 32, 255),
}


def main():
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y, row in enumerate(CARD):
        for x, cell in enumerate(row):
            px[x, y] = COLORS[cell]
    img.save(OUT)


if __name__ == '__main__':
    main()
