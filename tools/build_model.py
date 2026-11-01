#!/usr/bin/env python3
"""Erzeugt das verbesserte Puddle-Jumper-Modell + Textur-Atlas aus assets/.

Ausgabe (src/main/resources/assets/puddlejumper/):
  models/entity/puddle_jumper.obj   Gruppen: hull, interior, hatch, pod_left, pod_right,
                                    *_glow (leuchtend, additiv gerendert)
  models/entity/drone.obj
  textures/entity/puddle_jumper.png (1024x1536 Atlas)
  textures/entity/drone.png
  textures/item/puddle_jumper.png   (Item-Icon, gerendert)

Koordinaten bleiben in Modelleinheiten (Bug = +Z, Boden = y 0). Die Skalierung auf
Blöcke passiert im Java-Renderer.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_OBJ = os.path.join(ROOT, 'assets', 'puddlejumper.obj')
SRC_TEX = os.path.join(ROOT, 'assets', 'puddlejumper.png')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'puddlejumper')

TW, TH = 1024, 1536          # Atlas-Größe
ORIG_H = 1024                # Original (2x hochskaliert) belegt y 0..1023

# Atlas-Regionen für neue Teile (Pixel: x0, y0, x1, y1)
REG_INTERIOR = (0, 1024, 512, 1536)
REG_POD = (512, 1024, 1024, 1280)
REG_GLOW = (512, 1280, 768, 1536)
REG_PORT = (768, 1280, 1024, 1536)

# Ebene der Heckwand (Normale zeigt nach hinten/unten)
REAR_N = np.array([0.0, -0.37, -0.93]); REAR_N /= np.linalg.norm(REAR_N)


class Mesh:
    def __init__(self):
        self.groups = {}  # name -> list of tris: ((p,uv,n) x3)

    def tri(self, group, ps, uvs, n=None):
        ps = [np.asarray(p, float) for p in ps]
        if n is None:
            n = np.cross(ps[1] - ps[0], ps[2] - ps[0])
            l = np.linalg.norm(n)
            if l < 1e-12:
                return
            n = n / l
        self.groups.setdefault(group, []).append((ps, uvs, n))

    def quad(self, group, ps, uvs):
        self.tri(group, [ps[0], ps[1], ps[2]], [uvs[0], uvs[1], uvs[2]])
        self.tri(group, [ps[0], ps[2], ps[3]], [uvs[0], uvs[2], uvs[3]])

    def write(self, path, header=''):
        lines = ['# Puddle Jumper - generiert von tools/build_model.py', header]
        vi = 1
        for g, tris in self.groups.items():
            lines.append(f'o {g}')
            for ps, uvs, n in tris:
                for p in ps:
                    lines.append('v %.5f %.5f %.5f' % tuple(p))
                for uv in uvs:
                    lines.append('vt %.6f %.6f' % (uv[0], uv[1]))
                lines.append('vn %.4f %.4f %.4f' % tuple(n))
                ni = (vi + 2) // 3
                lines.append(f'f {vi}/{vi}/{ni} {vi+1}/{vi+1}/{ni} {vi+2}/{vi+2}/{ni}')
                vi += 3
        # Normalen-Indizes oben setzen voraus, dass pro Dreieck genau 1 vn kommt
        with open(path, 'w') as f:
            f.write('\n'.join(lines) + '\n')


def px_to_uv(x, y):
    """Atlas-Pixel -> OBJ-UV (v von unten)."""
    return (x / TW, 1.0 - y / TH)


def region_uv(reg, s, t):
    """s,t in 0..1 innerhalb einer Atlas-Region (t=0 oben)."""
    x0, y0, x1, y1 = reg
    return px_to_uv(x0 + (x1 - x0) * s, y0 + (y1 - y0) * t)


# ---------------------------------------------------------------- Original laden
def load_original():
    vs, vts, vns, faces = [], [], [], []
    for line in open(SRC_OBJ):
        p = line.split()
        if not p:
            continue
        if p[0] == 'v':
            vs.append([float(x) for x in p[1:4]])
        elif p[0] == 'vt':
            vts.append([float(x) for x in p[1:3]])
        elif p[0] == 'vn':
            vns.append([float(x) for x in p[1:4]])
        elif p[0] == 'f':
            faces.append([tuple(int(x) - 1 for x in t.split('/')) for t in p[1:]])
    return np.array(vs), np.array(vts), np.array(vns), faces


def remap_orig_uv(uv):
    # Original belegt oberen 1024er Block: v' so, dass Pixel-y = (1-v)*1024
    return (uv[0], 1.0 - (1.0 - uv[1]) * ORIG_H / TH)


def is_hatch_face(pts, uvs):
    n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
    l = np.linalg.norm(n)
    if l < 1e-12:
        return False
    n /= l
    if n @ REAR_N < 0.995:
        return False
    if abs((pts @ REAR_N).mean() - 9.5) > 0.3:
        return False
    # Rahmenstreifen (UV-Spalte rechts) bleibt am Rumpf
    if all(u > 0.96 and v > 0.45 for u, v in uvs):
        return False
    return True


def interior_uv(p, n):
    """Planare Projektion in die Innenraum-Region (Kachelung über fract)."""
    a = np.abs(n)
    if a[1] >= a[0] and a[1] >= a[2]:
        s, t = p[0] / 6.0, p[2] / 6.0
    elif a[0] >= a[2]:
        s, t = p[2] / 6.0, p[1] / 6.0
    else:
        s, t = p[0] / 6.0, p[1] / 6.0
    return s, t


def add_box(mesh, group, lo, hi, reg, tile=4.0):
    """Achsenparallele Box, nach außen gerichtete Flächen, UV planar in Region."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    P = lambda x, y, z: np.array([x, y, z])
    faces = [
        ([P(x0, y1, z0), P(x0, y1, z1), P(x1, y1, z1), P(x1, y1, z0)]),  # top
        ([P(x0, y0, z0), P(x1, y0, z0), P(x1, y0, z1), P(x0, y0, z1)]),  # bottom
        ([P(x0, y0, z1), P(x1, y0, z1), P(x1, y1, z1), P(x0, y1, z1)]),  # +z
        ([P(x1, y0, z0), P(x0, y0, z0), P(x0, y1, z0), P(x1, y1, z0)]),  # -z
        ([P(x1, y0, z1), P(x1, y0, z0), P(x1, y1, z0), P(x1, y1, z1)]),  # +x
        ([P(x0, y0, z0), P(x0, y0, z1), P(x0, y1, z1), P(x0, y1, z0)]),  # -x
    ]
    for q in faces:
        n = np.cross(q[1] - q[0], q[2] - q[0]); n /= np.linalg.norm(n)
        uvs = []
        for p in q:
            s, t = interior_uv(p, n)
            uvs.append((s, t))
        # in Region einpassen (Box-Flächen sind klein -> keine Kachelung nötig)
        su = [u for u, _ in uvs]; tv = [v for _, v in uvs]
        ms, mt = min(su), min(tv)
        rs = max(max(su) - ms, 1e-6); rt = max(max(tv) - mt, 1e-6)
        sc = 1.0 / max(rs, rt, 1.0 / tile * 4)
        uvs2 = [region_uv(reg, min(1, (u - ms) * sc * 0.98 + 0.01), min(1, (v - mt) * sc * 0.98 + 0.01)) for u, v in uvs]
        mesh.quad(group, q, uvs2)


# ---------------------------------------------------------------- Gondeln
def pod_profile():
    """Querschnitt (x,y) relativ zur Gondelachse; Gondel ist schmal und hoch."""
    w, h = 0.75, 1.05
    c = 0.35
    return [(-w + c, h), (w - c, h), (w, h - c), (w, -h + c), (w - c, -h), (-w + c, -h), (-w, -h + c), (-w, h - c)]


def add_pod(mesh, side):
    """side = +1 (links, +x) / -1 (rechts). Gondel entlang z, Lokalursprung am Gelenk."""
    g = 'pod_left' if side > 0 else 'pod_right'
    gg = g + '_glow'
    prof = pod_profile()
    cx, cy = side * 4.85, 4.3      # Achse im eingefahrenen Zustand
    z_front, z_rear = 5.0, -11.6   # Gondel geht bis knapp vor die Heckkante
    taper_front = 0.45             # vorne verjüngt
    rings = [(z_rear, 1.0), (z_rear + 0.6, 1.0), (z_front - 2.4, 1.0), (z_front, taper_front)]
    nprof = len(prof)
    perim = 0
    seg = []
    for i in range(nprof):
        a, b = np.array(prof[i]), np.array(prof[(i + 1) % nprof])
        seg.append(perim); perim += np.linalg.norm(b - a)
    seg.append(perim)

    def P(i, ring):
        z, s = rings[ring]
        x, y = prof[i % nprof]
        return np.array([cx + x * s, cy + y * s, z])

    L = z_front - z_rear
    for r in range(len(rings) - 1):
        for i in range(nprof):
            a, b = P(i, r), P(i + 1, r)
            c, d = P(i + 1, r + 1), P(i, r + 1)
            s0, s1 = seg[i] / perim, seg[i + 1] / perim
            t0 = (rings[r][0] - z_rear) / L
            t1 = (rings[r + 1][0] - z_rear) / L
            # Gondel-Region: s entlang Länge, t entlang Umfang
            uv = [region_uv(REG_POD, t0, s0), region_uv(REG_POD, t0, s1),
                  region_uv(REG_POD, t1, s1), region_uv(REG_POD, t1, s0)]
            q = [a, b, c, d]
            n = np.cross(q[1] - q[0], q[2] - q[0])
            # Außenseite zeigt weg von der Achse
            mid = sum(q) / 4 - np.array([cx, cy, mid_z(q)])
            if n @ mid < 0:
                q = q[::-1]; uv = uv[::-1]
            mesh.quad(g, q, uv)
    # Nasenkappe
    tip = np.array([cx, cy, z_front + 0.5])
    for i in range(nprof):
        a, b = P(i, len(rings) - 1), P(i + 1, len(rings) - 1)
        tri = [a, b, tip]
        n = np.cross(b - a, tip - a)
        uvs = [region_uv(REG_POD, 0.97, 0.1), region_uv(REG_POD, 0.97, 0.2), region_uv(REG_POD, 1.0, 0.15)]
        if n[2] < 0:
            tri = [b, a, tip]
        mesh.tri(g, tri, uvs)
    # Heck: Düsenfläche (leuchtend) leicht eingelassen + Rahmen
    inset = 0.78
    for i in range(nprof):
        a, b = P(i, 0), P(i + 1, 0)
        ai = np.array([cx + prof[i][0] * inset, cy + prof[i][1] * inset, z_rear + 0.15])
        bi = np.array([cx + prof[(i + 1) % nprof][0] * inset, cy + prof[(i + 1) % nprof][1] * inset, z_rear + 0.15])
        q = [a, ai, bi, b]
        n = np.cross(q[1] - q[0], q[2] - q[0])
        uvs = [region_uv(REG_POD, 0.0, 0.5)] * 4
        if n[2] > 0:
            q = q[::-1]
        mesh.quad(g, q, uvs)
    center = np.array([cx, cy, z_rear + 0.15])
    for i in range(nprof):
        ai = np.array([cx + prof[i][0] * inset, cy + prof[i][1] * inset, z_rear + 0.15])
        bi = np.array([cx + prof[(i + 1) % nprof][0] * inset, cy + prof[(i + 1) % nprof][1] * inset, z_rear + 0.15])
        tri = [ai, bi, center]
        n = np.cross(bi - ai, center - ai)
        uvs = [region_uv(REG_GLOW, 0.5 + prof[i][0] * 0.6, 0.5 + prof[i][1] * 0.45),
               region_uv(REG_GLOW, 0.5 + prof[(i + 1) % nprof][0] * 0.6, 0.5 + prof[(i + 1) % nprof][1] * 0.45),
               region_uv(REG_GLOW, 0.5, 0.5)]
        if n[2] > 0:
            tri = [bi, ai, center]; uvs = [uvs[1], uvs[0], uvs[2]]
        # leuchtet, und eine dunkle Basis darunter, damit es auch ohne Glow sichtbar ist
        mesh.tri(gg, [t + np.array([0, 0, -0.02]) for t in tri], uvs)
        mesh.tri(g, tri, [region_uv(REG_POD, 0.0, 0.5)] * 3)
    # Drohnen-Ports auf der Innenseite (zum Rumpf hin), leuchten gelb wenn ausgefahren
    xin = cx - side * 0.75 - side * 0.01
    for k in range(4):
        z0 = -8.6 + k * 2.6
        z1 = z0 + 1.6
        y0, y1 = cy - 0.55, cy + 0.55
        q = [np.array([xin, y0, z0]), np.array([xin, y0, z1]), np.array([xin, y1, z1]), np.array([xin, y1, z0])]
        n = np.cross(q[1] - q[0], q[2] - q[0])
        if n[0] * side > 0:  # muss zur Achse/innen zeigen (-side)
            q = q[::-1]
        uvs = [region_uv(REG_PORT, 0.05, 0.95), region_uv(REG_PORT, 0.95, 0.95), region_uv(REG_PORT, 0.95, 0.05), region_uv(REG_PORT, 0.05, 0.05)]
        mesh.quad(gg, q, uvs)
    # Ausleger (Strebe), sichtbar wenn ausgefahren: zwei flache Boxen zum Rumpf
    for zc in (-6.0, 1.0):
        x_a, x_b = sorted([cx - side * 0.7, cx - side * 2.4])
        add_box(mesh, g, (x_a, cy - 0.2, zc - 0.5), (x_b, cy + 0.2, zc + 0.5), REG_POD)


def mid_z(q):
    return sum(p[2] for p in q) / 4


# ---------------------------------------------------------------- Drohne
def build_drone():
    m = Mesh()
    # Länglicher, tintenfischartiger Körper: Rotationskörper entlang +z
    prof = [(-0.9, 0.0), (-0.7, 0.18), (-0.3, 0.3), (0.2, 0.32), (0.6, 0.24), (0.9, 0.0)]
    seg = 10
    for i in range(len(prof) - 1):
        z0, r0 = prof[i]; z1, r1 = prof[i + 1]
        for k in range(seg):
            a0 = 2 * math.pi * k / seg; a1 = 2 * math.pi * (k + 1) / seg
            p = lambda z, r, a: np.array([math.cos(a) * r, math.sin(a) * r, z])
            q = [p(z0, r0, a0), p(z0, r0, a1), p(z1, r1, a1), p(z1, r1, a0)]
            t0 = i / (len(prof) - 1); t1 = (i + 1) / (len(prof) - 1)
            uv = [(k / seg, t0), ((k + 1) / seg, t0), ((k + 1) / seg, t1), (k / seg, t1)]
            n = np.cross(q[1] - q[0], q[2] - q[0])
            outward = (q[0] + q[2]) / 2; outward[2] = 0
            if np.linalg.norm(n) < 1e-9:
                n = np.cross(q[2] - q[1], q[3] - q[1])
            if n @ outward < 0:
                q = q[::-1]; uv = uv[::-1]
            m.quad('drone', q, [(u * 0.5, 0.5 + v * 0.5) for u, v in uv])
    # Glühender Kern (größere Hülle, additiv)
    for i in range(len(prof) - 1):
        z0, r0 = prof[i]; z1, r1 = prof[i + 1]
        for k in range(seg):
            a0 = 2 * math.pi * k / seg; a1 = 2 * math.pi * (k + 1) / seg
            p = lambda z, r, a: np.array([math.cos(a) * r * 1.45, math.sin(a) * r * 1.45, z * 1.2])
            q = [p(z0, r0, a0), p(z0, r0, a1), p(z1, r1, a1), p(z1, r1, a0)]
            n = np.cross(q[1] - q[0], q[2] - q[0])
            outward = (q[0] + q[2]) / 2; outward[2] = 0
            if np.linalg.norm(n) < 1e-9:
                n = np.cross(q[2] - q[1], q[3] - q[1])
            if n @ outward < 0:
                q = q[::-1]
            t = (i + 0.5) / (len(prof) - 1)
            m.quad('drone_glow', q, [(0.5 + 0.5 * k / seg, 0.5 + 0.5 * t)] * 4)
    # Tentakel (3 dünne Schweife nach hinten)
    for k in range(3):
        a = 2 * math.pi * k / 3
        base = np.array([math.cos(a) * 0.15, math.sin(a) * 0.15, -0.8])
        tip = np.array([math.cos(a) * 0.35, math.sin(a) * 0.35, -1.6])
        side = np.array([-math.sin(a), math.cos(a), 0]) * 0.06
        m.quad('drone_glow', [base - side, base + side, tip + side * 0.3, tip - side * 0.3], [(0.75, 0.75)] * 4)
        m.quad('drone_glow', [base + side, base - side, tip - side * 0.3, tip + side * 0.3], [(0.75, 0.75)] * 4)
    return m


def drone_texture():
    W = 64
    img = Image.new('RGBA', (W, W), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    # OBJ-v 0.5..1 landet nach der Spiegelung (MC-v = 1 - v) in der oberen Bildhälfte.
    # Körper (oben links): goldgelb mit dunklen Bändern
    for y in range(0, 32):
        t = y / 32
        col = (int(255 * (0.85 + 0.15 * t)), int(200 * (0.7 + 0.3 * t)), int(60 * t), 255)
        d.line([(0, y), (31, y)], fill=col)
    for y in range(4, 32, 7):
        d.line([(0, y), (31, y)], fill=(150, 90, 10, 255))
    # Glow (oben rechts): warmes Orange-Gelb, Mitte heller
    for y in range(0, 32):
        for x in range(32, 64):
            t = 1 - abs((y - 16) / 16)
            img.putpixel((x, y), (255, int(170 + 70 * t), int(40 + 80 * t), 255))
    return img


# ---------------------------------------------------------------- Textur-Atlas
def build_texture():
    src = Image.open(SRC_TEX).convert('RGB')
    up = src.resize((1024, 1024), Image.LANCZOS)
    arr = np.asarray(up, dtype=np.float32)

    # Antiker Look: leicht kühler, bläulich-grüner Metallton + Kontrast
    lum = arr.mean(axis=2, keepdims=True)
    tint = np.array([0.93, 1.0, 1.05])
    arr = (lum + (arr - lum) * 1.1) * tint
    arr = (arr - 128) * 1.12 + 132

    # Feine Mikro-Textur (Metallrauschen) + leichte Vertikal-Schleifspuren
    rng = np.random.default_rng(42)
    noise = rng.normal(0, 4.0, (1024, 1024, 1))
    streak = np.repeat(rng.normal(0, 2.5, (1, 1024, 1)), 1024, axis=0)
    arr = arr + noise + streak

    # Kanten der Paneele schärfen (Unsharp-Mask auf Basis des Originals)
    tmp = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
    tmp = tmp.filter(ImageFilter.UnsharpMask(radius=2, percent=80, threshold=2))
    arr = np.asarray(tmp, dtype=np.float32)

    # Cockpitfenster (dunkler Fleck oben links) zu getöntem Glas mit Reflexion machen
    src_a = np.asarray(up, dtype=np.float32).mean(axis=2)
    yy, xx = np.mgrid[0:1024, 0:1024]
    win = (src_a < 45) & (xx > 20) & (xx < 240) & (yy > 180) & (yy < 330)
    if win.any():
        g = (yy - 180) / 150.0
        glass = np.stack([18 + 30 * g, 42 + 55 * g, 58 + 70 * g], axis=-1)
        # diagonale Reflexionsstreifen
        refl = ((xx + yy * 0.8) % 90 < 10) * 55 + ((xx + yy * 0.8) % 90 < 4) * 40
        glass = glass + refl[..., None] * np.array([0.8, 0.95, 1.0])
        arr[win] = glass[win]

    atlas = np.zeros((TH, TW, 3), dtype=np.float32)
    atlas[:1024] = arr

    # Innenraum: dunkle Bodenplatten mit Rippen und Leuchtstreifen
    x0, y0, x1, y1 = REG_INTERIOR
    h, w = y1 - y0, x1 - x0
    yy, xx = np.mgrid[0:h, 0:w]
    base = 58 + rng.normal(0, 3, (h, w))
    plate = ((xx % 128) < 3) | ((yy % 128) < 3)
    rib = ((xx % 32) < 2)
    inter = base - plate * 25 - rib * 10
    inter = np.stack([inter * 0.95, inter * 1.0, inter * 1.04], axis=-1)
    light = (yy % 256 > 120) & (yy % 256 < 126)
    inter[light] = [150, 190, 200]
    atlas[y0:y1, x0:x1] = inter

    # Gondel: Rumpfmetall mit Längsrillen, Ancient-Ornamentlinien
    x0, y0, x1, y1 = REG_POD
    h, w = y1 - y0, x1 - x0
    yy, xx = np.mgrid[0:h, 0:w]
    pod = 120 + rng.normal(0, 4, (h, w)) + 10 * np.sin(yy / h * math.pi)
    pod -= (((yy % 32) < 2) * 35)
    pod -= (((xx % 170) < 3) * 30)
    orn = (np.abs(((xx * 0.5 + yy) % 96) - 48) < 1.5) & (yy > 40) & (yy < h - 40)
    pod -= orn * 20
    pod = np.stack([pod * 0.94, pod * 1.0, pod * 1.04], axis=-1)
    atlas[y0:y1, x0:x1] = pod

    # Glow: radialer Verlauf weiß -> cyan
    x0, y0, x1, y1 = REG_GLOW
    h, w = y1 - y0, x1 - x0
    yy, xx = np.mgrid[0:h, 0:w]
    r = np.sqrt(((xx - w / 2) / (w / 2)) ** 2 + ((yy - h / 2) / (h / 2)) ** 2)
    k = np.clip(1 - r, 0, 1)
    glow = np.stack([90 + 165 * k ** 1.5, 180 + 75 * k, 255 * np.ones_like(k)], axis=-1)
    atlas[y0:y1, x0:x1] = glow

    # Drohnen-Ports: goldgelbe Waben
    x0, y0, x1, y1 = REG_PORT
    h, w = y1 - y0, x1 - x0
    yy, xx = np.mgrid[0:h, 0:w]
    cell = (((xx // 32) + (yy // 32)) % 2)
    edge = ((xx % 32) < 3) | ((yy % 32) < 3)
    port = np.stack([230 + 25 * cell, 160 + 40 * cell, 40 + 20 * cell], axis=-1).astype(np.float32)
    port[edge] = [90, 60, 10]
    atlas[y0:y1, x0:x1] = port

    return Image.fromarray(np.clip(atlas, 0, 255).astype(np.uint8), 'RGB')


# ---------------------------------------------------------------- Hauptmodell
def build_jumper():
    vs, vts, vns, faces = load_original()
    mesh = Mesh()

    # Vertex-Normalen gemittelt pro Position (für Innenraum-Offset)
    acc = np.zeros_like(vs)
    for f in faces:
        pts = vs[[t[0] for t in f]]
        n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
        for t in f:
            acc[t[0]] += n
    vnorm = acc / (np.linalg.norm(acc, axis=1, keepdims=True) + 1e-12)

    hatch_tris = []
    for f in faces:
        pts = vs[[t[0] for t in f]]
        uvs = [tuple(vts[t[1]]) for t in f]
        uv2 = [remap_orig_uv(uv) for uv in uvs]
        if is_hatch_face(pts, uvs):
            mesh.tri('hatch', pts, uv2)
            hatch_tris.append(pts)
            continue
        mesh.tri('hull', pts, uv2)
        # Innenhülle: gespiegelt, nach innen versetzt
        inner = [vs[t[0]] - vnorm[t[0]] * 0.18 for t in f]
        inner = [np.array([p[0], max(p[1], 0.18), p[2]]) for p in inner]
        n = np.cross(inner[1] - inner[0], inner[2] - inner[0])
        if np.linalg.norm(n) < 1e-9:
            continue
        n /= np.linalg.norm(n)
        iuv = []
        for p in inner[::-1]:
            s, t = interior_uv(p, n)
            iuv.append((s, t))
        # Kachelung innerhalb der Region: fract
        base_s = math.floor(min(u for u, _ in iuv)); base_t = math.floor(min(v for _, v in iuv))
        iuv = [region_uv(REG_INTERIOR, min(0.999, max(0.001, (u - base_s) % 1.0 if (u - base_s) < 1 else 0.999)),
                         min(0.999, max(0.001, (v - base_t) % 1.0 if (v - base_t) < 1 else 0.999))) for u, v in iuv]
        mesh.tri('interior', inner[::-1], iuv)

    # Heckklappe: Rückseite (Innenseite = Rampenoberfläche) + Kantenstärke
    th = 0.22
    for pts in hatch_tris:
        back = [p - REAR_N * th for p in pts][::-1]
        n = -REAR_N
        uvs = []
        for p in back:
            s, t = p[0] / 7.0 + 0.5, (p[1]) / 7.5
            uvs.append(region_uv(REG_INTERIOR, min(.999, max(.001, s)), min(.999, max(.001, 1 - t))))
        mesh.tri('hatch', back, uvs)
    # Klappen-Randkanten: Außenkontur bestimmen (Kanten, die nur einmal vorkommen)
    edges = {}
    def key(p):
        return tuple(np.round(p, 4))
    for pts in hatch_tris:
        for i in range(3):
            a, b = key(pts[i]), key(pts[(i + 1) % 3])
            k = (a, b)
            if (b, a) in edges:
                edges[(b, a)] += 1
            else:
                edges[k] = edges.get(k, 0) + 1
    for (a, b), c in edges.items():
        if c != 1:
            continue
        a = np.array(a); b = np.array(b)
        a2, b2 = a - REAR_N * th, b - REAR_N * th
        q = [b, a, a2, b2]
        uv = [region_uv(REG_INTERIOR, 0.02, 0.02)] * 4
        mesh.quad('hatch', q, uv)

    # Innenausstattung: Boden, Cockpitsitze, Konsole, Schott, Seitenbänke
    R = REG_INTERIOR
    add_box(mesh, 'interior', (-3.2, 0.18, -10.0), (3.2, 0.32, 10.5), R)              # Bodenplatte
    for sx in (-1.6, 1.6):                                                             # Pilot/Copilot
        add_box(mesh, 'interior', (sx - 0.7, 0.32, 5.6), (sx + 0.7, 1.5, 7.0), R)
        add_box(mesh, 'interior', (sx - 0.7, 1.5, 5.6), (sx + 0.7, 3.6, 5.95), R)
    add_box(mesh, 'interior', (-2.9, 0.32, 7.9), (2.9, 1.7, 8.8), R)                   # Konsole
    add_box(mesh, 'interior_glow', (-2.4, 1.71, 7.95), (2.4, 1.75, 8.7), REG_GLOW)     # Display
    # Schott mit Durchgang (unten breit, oben schmaler, damit nichts durch die Hülle ragt)
    add_box(mesh, 'interior', (-3.9, 0.32, 2.4), (-1.3, 3.6, 2.7), R)
    add_box(mesh, 'interior', (1.3, 0.32, 2.4), (3.9, 3.6, 2.7), R)
    add_box(mesh, 'interior', (-2.8, 3.6, 2.4), (-1.3, 5.6, 2.7), R)
    add_box(mesh, 'interior', (1.3, 3.6, 2.4), (2.8, 5.6, 2.7), R)
    add_box(mesh, 'interior', (-1.3, 4.9, 2.4), (1.3, 5.6, 2.7), R)                    # Türsturz
    # Seitenbänke im Heckabteil
    add_box(mesh, 'interior', (-3.9, 0.32, -9.3), (-3.1, 1.5, 1.8), R)
    add_box(mesh, 'interior', (3.1, 0.32, -9.3), (3.9, 1.5, 1.8), R)

    # Rumpf-Lichter (Positionslichter + Heckleiste)
    for sx in (-1, 1):
        add_box(mesh, 'hull_glow', (sx * 4.3 - 0.15, 1.0, 12.2), (sx * 4.3 + 0.15, 1.25, 12.6), REG_GLOW)

    add_pod(mesh, +1)
    add_pod(mesh, -1)
    return mesh


def render_icon(path):
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import preview
    img = preview.render_icon(os.path.join(OUT, 'models', 'entity', 'puddle_jumper.obj'),
                              os.path.join(OUT, 'textures', 'entity', 'puddle_jumper.png'))
    img.save(path)


def main():
    for d in ('models/entity', 'textures/entity', 'textures/item'):
        os.makedirs(os.path.join(OUT, d), exist_ok=True)
    build_texture().save(os.path.join(OUT, 'textures', 'entity', 'puddle_jumper.png'))
    build_jumper().write(os.path.join(OUT, 'models', 'entity', 'puddle_jumper.obj'))
    build_drone().write(os.path.join(OUT, 'models', 'entity', 'drone.obj'))
    drone_texture().save(os.path.join(OUT, 'textures', 'entity', 'drone.png'))
    render_icon(os.path.join(OUT, 'textures', 'item', 'puddle_jumper.png'))
    print('ok')


if __name__ == '__main__':
    main()
