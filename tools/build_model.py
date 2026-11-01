#!/usr/bin/env python3
"""Erzeugt das verbesserte Puddle-Jumper-Modell + Textur-Atlas aus assets/.

Ausgabe (src/main/resources/assets/puddlejumper/):
  models/entity/puddle_jumper.obj   Gruppen: hull, hull_glow, pod_left/right (+ _glow Düsen, _ports Drohnen-Ports)
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

TW, TH = 1024, 2048          # Atlas-Größe
ORIG_H = 1024                # Original (2x hochskaliert) belegt y 0..1023

# Atlas-Regionen für neue Teile (Pixel: x0, y0, x1, y1)
REG_POD = (512, 1024, 1024, 1280)
REG_GLOW = (512, 1280, 768, 1536)
REG_PORT = (768, 1280, 1024, 1536)
REG_OLIVE = (0, 1024, 512, 2048)
REG_NOZZLE = (512, 1536, 768, 2048)
REG_PLUME = (768, 1536, 1024, 2048)   # Ionenfahne (u = quer, v = Länge); wird im Java-Code gerendert

# Gondeln (Modelleinheiten) - müssen zum Java-Code passen (PuddleJumperEntity.POD_*)
POD_X, POD_Y = 4.95, 4.3
POD_REAR_Z, POD_FRONT_Z = -12.2, 1.0



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
        a = np.abs(n)
        for p in q:
            if a[1] >= a[0] and a[1] >= a[2]:
                uvs.append((p[0], p[2]))
            elif a[0] >= a[2]:
                uvs.append((p[2], p[1]))
            else:
                uvs.append((p[0], p[1]))
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
    """Kurze, kastenförmige Antriebsgondel (ca. halbe Schiffslänge) wie in der Serie.

    side = +1 (links, +x) / -1 (rechts). Gruppen: pod_* (grau), pod_*_glow (blaue Düsenfenster,
    leuchten nur in Bewegung), pod_*_ports (gelbe Drohnen-Ports, leuchten wenn ausgefahren).
    """
    g = 'pod_left' if side > 0 else 'pod_right'
    gg, gp = g + '_glow', g + '_ports'
    w, h, c = 0.85, 1.15, 0.38          # halbe Breite/Höhe, Fase
    prof = [(-w + c, h), (w - c, h), (w, h - c), (w, -h + c), (w - c, -h), (-w + c, -h), (-w, -h + c), (-w, h - c)]
    nprof = len(prof)
    cx, cy = side * POD_X, POD_Y
    z_rear, z_front = POD_REAR_Z, POD_FRONT_Z
    # Ringe: (z, Skalierung) - vorne abgerundet
    rings = [(z_rear, 1.0), (z_rear + 0.35, 1.04), (z_front - 1.4, 1.04), (z_front - 0.55, 0.9), (z_front, 0.62)]
    perim = [0.0]
    for i in range(nprof):
        a, b = np.array(prof[i]), np.array(prof[(i + 1) % nprof])
        perim.append(perim[-1] + np.linalg.norm(b - a))
    L = z_front - z_rear

    def P(i, r):
        z, s = rings[r]
        x, y = prof[i % nprof]
        return np.array([cx + x * s, cy + y * s, z])

    for r in range(len(rings) - 1):
        for i in range(nprof):
            q = [P(i, r), P(i + 1, r), P(i + 1, r + 1), P(i, r + 1)]
            s0, s1 = perim[i] / perim[-1], perim[i + 1] / perim[-1]
            t0, t1 = (rings[r][0] - z_rear) / L, (rings[r + 1][0] - z_rear) / L
            uv = [region_uv(REG_POD, t0, s0), region_uv(REG_POD, t0, s1), region_uv(REG_POD, t1, s1), region_uv(REG_POD, t1, s0)]
            n = np.cross(q[1] - q[0], q[2] - q[0])
            out = (q[0] + q[2]) / 2 - np.array([cx, cy, (q[0][2] + q[2][2]) / 2])
            if n @ out < 0:
                q, uv = q[::-1], uv[::-1]
            mesh.quad(g, q, uv)
    # Nasenkappe (flach, leicht gewölbt)
    tip = np.array([cx, cy, z_front + 0.12])
    last = len(rings) - 1
    for i in range(nprof):
        a, b = P(i, last), P(i + 1, last)
        tri = [a, b, tip] if np.cross(b - a, tip - a)[2] > 0 else [b, a, tip]
        mesh.tri(g, tri, [region_uv(REG_POD, 0.99, 0.3)] * 3)
    # Heck: dunkle Abschlussplatte
    center = np.array([cx, cy, z_rear])
    dark = region_uv(REG_POD, 0.003, 0.5)
    for i in range(nprof):
        a, b = P(i, 0), P(i + 1, 0)
        tri = [a, b, center] if np.cross(b - a, center - a)[2] < 0 else [b, a, center]
        mesh.tri(g, tri, [dark] * 3)
    # Zwei hochkante Düsenfenster pro Gondel, deutlich vor der Platte (kein Z-Fighting)
    zg = z_rear - 0.1
    for ox in (-0.38, 0.38):
        x0, x1 = cx + ox - 0.26, cx + ox + 0.26
        y0, y1 = cy - 0.8, cy + 0.8
        q = [np.array([x1, y0, zg]), np.array([x0, y0, zg]), np.array([x0, y1, zg]), np.array([x1, y1, zg])]
        if np.cross(q[1] - q[0], q[2] - q[0])[2] > 0:
            q = q[::-1]
        uv = [region_uv(REG_NOZZLE, 0, 1), region_uv(REG_NOZZLE, 1, 1), region_uv(REG_NOZZLE, 1, 0), region_uv(REG_NOZZLE, 0, 0)]
        mesh.quad(gg, q, uv)
        # Rahmen um das Fenster (grau, minimal vor der Platte)
        mesh.quad(g, [p + np.array([0, 0, 0.05]) for p in q], [dark] * 4)
    # Drohnen-Ports auf der Innenseite
    xin = cx - side * (w * 1.04 + 0.03)
    for k in range(3):
        z0 = z_rear + 1.6 + k * 2.6
        z1 = z0 + 1.7
        y0, y1 = cy - 0.6, cy + 0.6
        q = [np.array([xin, y0, z0]), np.array([xin, y0, z1]), np.array([xin, y1, z1]), np.array([xin, y1, z0])]
        if np.cross(q[1] - q[0], q[2] - q[0])[0] * side > 0:
            q = q[::-1]
        uvs = [region_uv(REG_PORT, 0.05, 0.95), region_uv(REG_PORT, 0.95, 0.95), region_uv(REG_PORT, 0.95, 0.05), region_uv(REG_PORT, 0.05, 0.05)]
        mesh.quad(gp, q, uvs)
    # Ausleger (olivfarben wie der Rumpf), sichtbar wenn ausgefahren
    for zc in (z_rear + 2.5, z_front - 3.0):
        x_a, x_b = sorted([cx - side * 0.8, cx - side * 2.4])
        add_box(mesh, g, (x_a, cy - 0.22, zc - 0.7), (x_b, cy + 0.22, zc + 0.7), REG_OLIVE)


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
def shift(a, dx, dy):
    return np.roll(np.roll(a, dy, axis=0), dx, axis=1)


def smooth_noise(rng, h, w, scale):
    small = rng.normal(0, 1, (max(2, h // scale), max(2, w // scale))).astype(np.float32)
    img = Image.fromarray(((small - small.min()) / (np.ptp(small) + 1e-6) * 255).astype(np.uint8))
    return np.asarray(img.resize((w, h), Image.BICUBIC), dtype=np.float32) / 255.0 - 0.5


def olive_from_luma(L, rng):
    """Graustufen-Paneeltextur -> Oliv-Khaki-Lack der Serie mit eingravierten Linien."""
    h, w = L.shape
    t = (L - np.median(L)) / (L.std() + 1e-6)
    base = np.array([96, 92, 58], np.float32)
    dark = np.array([36, 33, 22], np.float32)
    light = np.array([138, 133, 96], np.float32)
    k_dark = np.clip(-t / 2.0, 0, 1)[..., None]
    k_light = np.clip(t / 2.5, 0, 1)[..., None]
    col = base + (dark - base) * k_dark + (light - base) * k_light
    # Gravur-Effekt: Kanten mit Licht oben-links / Schatten unten-rechts
    emb = (shift(L, 1, 1) - shift(L, -1, -1))
    col += np.clip(emb, -60, 60)[..., None] * 0.35
    # Lackfleckigkeit + feines Korn
    col *= (1 + 0.10 * smooth_noise(rng, h, w, 48))[..., None]
    col += rng.normal(0, 3.0, (h, w, 1))
    return col


def build_texture():
    rng = np.random.default_rng(42)
    src = Image.open(SRC_TEX).convert('L')
    up = src.resize((1024, 1024), Image.LANCZOS)
    L = np.asarray(up, dtype=np.float32)
    arr = olive_from_luma(L, rng)

    # Cockpitfenster (dunkler Fleck oben links): dunkles Glas mit kupferfarbenem Rahmen
    yy, xx = np.mgrid[0:1024, 0:1024]
    win = (L < 45) & (xx > 20) & (xx < 240) & (yy > 180) & (yy < 330)
    if win.any():
        wimg = Image.fromarray((win * 255).astype(np.uint8))
        # Rahmen liegt innerhalb der Fensterfläche (dort ist die UV-Insel sicher sichtbar)
        glass_mask = np.asarray(wimg.filter(ImageFilter.MinFilter(19)), dtype=bool)
        ring = win & ~glass_mask
        ring_inner = ring & ~np.asarray(wimg.filter(ImageFilter.MinFilter(11)), dtype=bool)
        copper = np.array([140, 84, 46], np.float32) * (1 + 0.15 * smooth_noise(rng, 1024, 1024, 6))[..., None]
        arr[ring] = copper[ring]
        arr[ring_inner] = copper[ring_inner] * 0.7
        cy_, cx_ = yy[win].mean(), xx[win].mean()
        r = np.sqrt(((xx - cx_) / 110) ** 2 + ((yy - cy_) / 70) ** 2)
        glass = np.stack([30 + 70 * np.clip(1 - r, 0, 1) ** 2] * 3, axis=-1) * np.array([0.95, 0.98, 1.05])
        arr[glass_mask] = glass[glass_mask]

    atlas = np.zeros((TH, TW, 3), dtype=np.float32)
    atlas[:1024] = arr

    def region(reg):
        x0, y0, x1, y1 = reg
        h, w = y1 - y0, x1 - x0
        yy, xx = np.mgrid[0:h, 0:w]
        return x0, y0, x1, y1, h, w, yy, xx

    # Gondel: gleicher Oliv-Lack wie der Rumpf, mit Lamellen/Gittern und Paneelkanten
    # (x = Länge hinten->vorne, y = Umfang)
    x0, y0, x1, y1, h, w, yy, xx = region(REG_POD)
    luma = 128 + rng.normal(0, 3, (h, w))
    grill = ((xx > w * 0.08) & (xx < w * 0.22) | (xx > w * 0.62) & (xx < w * 0.80)) & ((yy % 16) < 6)
    luma[grill] = 55
    luma[((xx % (w // 6)) < 2) | ((yy % (h // 8)) < 2)] = 70
    pod = olive_from_luma(luma, rng)
    pod[:, :8] = 34                                        # dunkle Heckplatte (Düsen)
    atlas[y0:y1, x0:x1] = pod

    # Glow (Rumpflichter/Display): weiß -> cyan, radial
    x0, y0, x1, y1, h, w, yy, xx = region(REG_GLOW)
    r = np.sqrt(((xx - w / 2) / (w / 2)) ** 2 + ((yy - h / 2) / (h / 2)) ** 2)
    k = np.clip(1 - r, 0, 1)
    atlas[y0:y1, x0:x1] = np.stack([90 + 165 * k ** 1.5, 180 + 75 * k, 255 * np.ones_like(k)], axis=-1)

    # Drohnen-Ports: goldgelbe Waben
    x0, y0, x1, y1, h, w, yy, xx = region(REG_PORT)
    cell = (((xx // 32) + (yy // 32)) % 2)
    port = np.stack([230 + 25 * cell, 160 + 40 * cell, 40 + 20 * cell], axis=-1).astype(np.float32)
    port[((xx % 32) < 3) | ((yy % 32) < 3)] = [90, 60, 10]
    atlas[y0:y1, x0:x1] = port

    # Düsenfenster: gleichmäßiges Blau, Mitte heller, weicher Rand (kein Muster -> kein Flimmern)
    x0, y0, x1, y1, h, w, yy, xx = region(REG_NOZZLE)
    ex = np.clip(1 - np.abs(xx - w / 2) / (w / 2), 0, 1) ** 0.5
    ey = np.clip(1 - np.abs(yy - h / 2) / (h / 2), 0, 1) ** 0.3
    k = ex * ey
    atlas[y0:y1, x0:x1] = np.stack([60 + 140 * k, 120 + 120 * k, 200 + 55 * k], axis=-1)

    # Oliv für Unterseite und Ausleger: Rumpflack mit Paneellinien im Antiker-Stil
    x0, y0, x1, y1, h, w, yy, xx = region(REG_OLIVE)
    luma = np.full((h, w), 128.0)
    luma[((xx % 128) < 3) | ((yy % 160) < 3)] = 60
    steps = (np.abs(xx - w * 0.5 - 40 * np.sign(np.sin(yy / h * math.pi * 6))) < 2)
    luma[steps] = 60
    atlas[y0:y1, x0:x1] = olive_from_luma(luma + rng.normal(0, 4, (h, w)), rng)

    # Ionenfahne: weißblauer Kern an der Düse, weich ausblendend (additiv: schwarz = unsichtbar)
    x0, y0, x1, y1, h, w, yy, xx = region(REG_PLUME)
    u = xx / (w - 1); v = yy / (h - 1)
    across = np.clip(1 - np.abs(2 * u - 1), 0, 1) ** 1.4
    along = (1 - v) ** 1.3
    streak = 0.85 + 0.15 * np.sin(u * math.pi * 7) ** 2
    k = across * along * streak
    atlas[y0:y1, x0:x1] = np.stack([(90 + 150 * k) * k, (150 + 105 * k) * k, 255 * k], axis=-1)

    return Image.fromarray(np.clip(atlas, 0, 255).astype(np.uint8), 'RGB')


# ---------------------------------------------------------------- Hauptmodell
def build_jumper():
    vs, vts, vns, faces = load_original()
    mesh = Mesh()

    for f in faces:
        pts = vs[[t[0] for t in f]]
        uvs = [tuple(vts[t[1]]) for t in f]
        uv2 = [remap_orig_uv(uv) for uv in uvs]
        fn = np.cross(pts[1] - pts[0], pts[2] - pts[0])
        fn = fn / (np.linalg.norm(fn) + 1e-12)
        if fn[1] < -0.9:
            # Unterseite: gleicher Oliv-Lack wie der Rumpf, planar projiziert (x = Breite, z = Länge)
            uv2 = [region_uv(REG_OLIVE, float(np.clip((p[0] + 4.8) / 9.6, 0, 1)),
                             float(np.clip((14.5 - p[2]) / 29.0, 0, 1))) for p in pts]
        mesh.tri('hull', pts, uv2)

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
