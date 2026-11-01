#!/usr/bin/env python3
"""Erzeugt die GameTest-Struktur 'platform' (24x10x24, Steinboden) als gzip-NBT."""
import gzip, os, struct

def tag_header(t, name):
    b = name.encode()
    return struct.pack('>bH', t, len(b)) + b

def payload_int(v): return struct.pack('>i', v)
def payload_str(s):
    b = s.encode(); return struct.pack('>H', len(b)) + b

def compound(items):  # items: list of (type, name, payload_bytes)
    out = b''.join(tag_header(t, n) + p for t, n, p in items)
    return out + b'\x00'

def list_of(t, payloads):
    return struct.pack('>bi', t, len(payloads)) + b''.join(payloads)

W, H, D = 24, 10, 24
blocks = []
for x in range(W):
    for z in range(D):
        pos = list_of(3, [payload_int(x), payload_int(0), payload_int(z)])
        blocks.append(compound([(9, 'pos', pos), (3, 'state', payload_int(0))]))
palette = [compound([(8, 'Name', payload_str('minecraft:smooth_stone'))])]
root = compound([
    (3, 'DataVersion', payload_int(3465)),
    (9, 'size', list_of(3, [payload_int(W), payload_int(H), payload_int(D)])),
    (9, 'palette', list_of(10, palette)),
    (9, 'blocks', list_of(10, blocks)),
    (9, 'entities', list_of(10, [])),
])
data = tag_header(10, '') + root
out = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'puddlejumper', 'structures', 'platform.nbt')
os.makedirs(os.path.dirname(out), exist_ok=True)
with gzip.open(out, 'wb') as f:
    f.write(data)
print('ok', os.path.getsize(out))
