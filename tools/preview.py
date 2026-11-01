#!/usr/bin/env python3
"""Software-Rasterizer zur Vorschau des generierten OBJ (und zum Rendern des Item-Icons).

  python3 tools/preview.py out.png [--open]   # 2x3 Ansichten; --open: Klappe auf, Gondeln aus
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'puddlejumper')

HINGE_Y, HINGE_Z = 0.19, -10.5   # muss zum Java-Renderer passen


def load_groups(path):
    vs, vts, groups, cur = [], [], {}, 'default'
    for line in open(path):
        p = line.split()
        if not p:
            continue
        if p[0] == 'v':
            vs.append([float(x) for x in p[1:4]])
        elif p[0] == 'vt':
            vts.append([float(x) for x in p[1:3]])
        elif p[0] == 'o':
            cur = p[1]
        elif p[0] == 'f':
            idx = [tuple(int(x) - 1 for x in t.split('/')[:2]) for t in p[1:]]
            for i in range(1, len(idx) - 1):
                groups.setdefault(cur, []).append((idx[0], idx[i], idx[i + 1]))
    return np.array(vs), np.array(vts), groups


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def group_transform(name, vs, open_state):
    if not open_state:
        return vs
    if name.startswith('hatch'):
        # Rampe um Scharnier (unten) nach hinten/unten drehen
        p = vs - np.array([0, HINGE_Y, HINGE_Z])
        return p @ rot_x(math.radians(-62)).T + np.array([0, HINGE_Y, HINGE_Z])
    if name.startswith('pod_left'):
        return vs + np.array([1.6, -0.2, 0])
    if name.startswith('pod_right'):
        return vs + np.array([-1.6, -0.2, 0])
    return vs


def view_matrix(yaw, pitch):
    cy, sy = math.cos(yaw), math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return Rx @ Ry


def render(path_obj, tex, yaw, pitch, size, open_state=False, bg=(40, 40, 40), alpha=False, margin=1.0):
    vs, vts, groups = load_groups(path_obj)
    tex = np.asarray(tex.convert('RGB'), dtype=np.float32)
    th, tw = tex.shape[:2]
    img = np.zeros((size, size, 4), dtype=np.float32)
    img[..., :3] = bg
    img[..., 3] = 0 if alpha else 255
    zb = np.full((size, size), -1e9, dtype=np.float32)
    R = view_matrix(yaw, pitch)
    light = np.array([0.35, 0.85, 0.4]); light /= np.linalg.norm(light)
    allv = np.concatenate([group_transform(g, vs, open_state) for g in groups])
    center = (allv.min(0) + allv.max(0)) / 2
    ext = np.linalg.norm(allv.max(0) - allv.min(0))
    scale = size * margin / ext
    for gname, faces in groups.items():
        glow = gname.endswith('_glow')
        V = group_transform(gname, vs, open_state)
        P = (V - center) @ R.T * scale
        sx, sy, sz = P[:, 0] + size / 2, -P[:, 1] + size / 2, P[:, 2]
        for (a, ta), (b, tb), (c, tc) in faces:
            n = np.cross(V[b] - V[a], V[c] - V[a])
            nl = np.linalg.norm(n)
            if nl == 0:
                continue
            n = n / nl
            # Backface-Culling wie im Spiel (Kamera schaut entlang -z im View-Raum)
            if (R @ n)[2] <= 0:
                continue
            shade = 1.0 if glow else 0.4 + 0.6 * max(0.0, float(n @ light)) + 0.1
            x0, y0 = sx[[a, b, c]], sy[[a, b, c]]
            minx, maxx = int(max(0, math.floor(x0.min()))), int(min(size - 1, math.ceil(x0.max())))
            miny, maxy = int(max(0, math.floor(y0.min()))), int(min(size - 1, math.ceil(y0.max())))
            if minx > maxx or miny > maxy:
                continue
            xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
            d = (y0[1] - y0[2]) * (x0[0] - x0[2]) + (x0[2] - x0[1]) * (y0[0] - y0[2])
            if abs(d) < 1e-9:
                continue
            w0 = ((y0[1] - y0[2]) * (xs - x0[2]) + (x0[2] - x0[1]) * (ys - y0[2])) / d
            w1 = ((y0[2] - y0[0]) * (xs - x0[2]) + (x0[0] - x0[2]) * (ys - y0[2])) / d
            w2 = 1 - w0 - w1
            m = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
            z = w0 * sz[a] + w1 * sz[b] + w2 * sz[c]
            sub = zb[miny:maxy + 1, minx:maxx + 1]
            m &= z > sub
            if not m.any():
                continue
            uv = w0[..., None] * vts[ta] + w1[..., None] * vts[tb] + w2[..., None] * vts[tc]
            u = np.clip(uv[..., 0] * tw, 0, tw - 1).astype(int)
            v = np.clip((1 - uv[..., 1]) * th, 0, th - 1).astype(int)
            col = tex[v, u] * shade
            sub[m] = z[m]
            tgt = img[miny:maxy + 1, minx:maxx + 1]
            tgt[..., :3][m] = col[m]
            tgt[..., 3][m] = 255
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


def render_icon(obj, texpath):
    tex = Image.open(texpath)
    big = render(obj, tex, math.radians(35), math.radians(25), 512, alpha=True, margin=0.95)
    x0, y0, x1, y1 = big.getbbox()
    side = max(x1 - x0, y1 - y0) + 8
    sq = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    sq.paste(big.crop((x0, y0, x1, y1)), ((side - (x1 - x0)) // 2, (side - (y1 - y0)) // 2))
    return sq.resize((32, 32), Image.LANCZOS)


def main():
    out = sys.argv[1]
    open_state = '--open' in sys.argv
    obj = os.path.join(ASSETS, 'models', 'entity', 'puddle_jumper.obj')
    tex = Image.open(os.path.join(ASSETS, 'textures', 'entity', 'puddle_jumper.png'))
    views = [(0, 0, 'Bug'), (math.pi, 0, 'Heck'), (math.pi / 2, 0, 'Seite'),
             (math.radians(35), math.radians(25), 'vorn schräg'), (math.radians(215), math.radians(25), 'hinten schräg'),
             (0, math.radians(89), 'oben')]
    S = 420
    W = Image.new('RGB', (S * 3, S * 2))
    for i, (y, p, label) in enumerate(views):
        t = render(obj, tex, y, p, S, open_state).convert('RGB')
        ImageDraw.Draw(t).text((5, 5), label, fill=(255, 255, 0))
        W.paste(t, ((i % 3) * S, (i // 3) * S))
    W.save(out)


if __name__ == '__main__':
    main()
