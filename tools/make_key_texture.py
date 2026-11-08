#!/usr/bin/env python3
"""Builds the Puddle Jumper key item texture (16x16): a plain card in the hull's bronze with its stepped panel line.

Output: core/src/main/resources/assets/mcpj/textures/item/jumper_key.png
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'core', 'src', 'main', 'resources', 'assets', 'mcpj', 'textures', 'item', 'jumper_key.png')

# . empty, O outline, H lit edge, B hull, D shaded edge, G panel groove, L lit side of the groove, A cockpit orange
CARD = [
    '................',
    '................',
    '................',
    '..OOOOOOOOOOOO..',
    '.OHHHHHHHHHHHHO.',
    '.OHBBBBBBBBBBDO.',
    '.OHGGGGGBBBBBDO.',
    '.OHLLLLGGGGBBDO.',
    '.OHBBBBLLLGGGDO.',
    '.OHBBBBBBBLLLDO.',
    '.OHAABBBBBBBBDO.',
    '.ODDDDDDDDDDDDO.',
    '..OOOOOOOOOOOO..',
    '................',
    '................',
    '................',
]

COLORS = {
    '.': (0, 0, 0, 0),
    'O': (44, 40, 22, 255),
    'H': (172, 160, 106, 255),
    'B': (128, 120, 76, 255),
    'D': (92, 87, 54, 255),
    'G': (58, 54, 32, 255),
    'L': (160, 149, 98, 255),
    'A': (230, 160, 40, 255),
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
