#!/usr/bin/env python3
"""Synthesizes the engine loop: a pulsing low drone with a growl and a softly swelling high "singing" tone.

Output: src/main/resources/assets/puddlejumper/sounds/engine.ogg (mono, 44.1 kHz, seamless loop)

Layers: detuned low tones around 105-110 Hz that beat at about 4.5 Hz (pulse), a 35 Hz amplitude modulation
(growl), a few higher partials, and two tones around 660 and 850 Hz with a slow swell (the energy "whine").
Every component is periodic over the loop length (whole numbers of cycles, circular noise filtering),
so the loop has no click at the seam. Pitch and volume follow the flight speed in JumperEngineSound.
Requires numpy and ffmpeg (Vorbis encoder).
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'puddlejumper', 'sounds', 'engine.ogg')

RATE = 44100
SECONDS = 4.0
N = int(RATE * SECONDS)
T = np.arange(N) / RATE
TARGET_RMS = 0.13           # loudness of the sample (about -17.7 dBFS)


def periodic_freq(f):
    """Nearest frequency with a whole number of cycles in the loop."""
    return round(f * SECONDS) / SECONDS


def tone(f, amp, phase=0.0):
    return amp * np.sin(2 * np.pi * periodic_freq(f) * T + phase)


def band_noise(rng, lo, hi, amp):
    """White noise band-limited by FFT (circular, so it loops seamlessly), soft roll-off at the edges."""
    spec = np.fft.rfft(rng.normal(0, 1, N))
    f = np.fft.rfftfreq(N, 1 / RATE)
    gain = np.clip((f - lo * 0.7) / (lo * 0.3), 0, 1) * np.clip((hi * 1.4 - f) / (hi * 0.4), 0, 1)
    sig = np.fft.irfft(spec * gain, N)
    return amp * sig / (np.abs(sig).max() + 1e-9)


def slow_lfo(cycles, depth, phase=0.0):
    return 1 + depth * np.sin(2 * np.pi * cycles * T / SECONDS + phase)


def synth():
    rng = np.random.default_rng(1729)
    phase = lambda: rng.uniform(0, 2 * np.pi)
    # low drone: detuned tones beating at about 4.5 Hz, with a few partials above
    low = np.zeros(N)
    for f, amp in ((104.0, 0.75), (108.5, 0.7), (110.5, 0.45), (126.0, 0.5), (134.5, 0.75), (144.0, 0.55),
                   (153.0, 0.5), (165.5, 0.5), (191.5, 0.65), (220.0, 0.6), (243.0, 0.45), (330.0, 0.5),
                   (372.0, 0.45), (443.0, 0.4)):
        low += tone(f, amp, phase())
    growl = 1 + 0.22 * np.sin(2 * np.pi * periodic_freq(35.0) * T + phase())
    low *= growl * slow_lfo(4, 0.08)
    low += band_noise(rng, 80, 450, 0.5)
    # singing layer: two main tones with gentle vibrato and a slow swell, plus weaker neighbours
    sing = np.zeros(N)
    for f, amp, swell_phase in ((662.0, 1.0, 0.0), (853.0, 0.85, np.pi), (628.0, 0.3, 1.0), (760.0, 0.25, 2.0),
                                (828.0, 0.3, 2.5), (1320.0, 0.4, 0.5), (1706.0, 0.3, 3.0)):
        vibrato = 0.003 * f / (2 * np.pi * 2 / SECONDS) * np.sin(2 * np.pi * 2 * T / SECONDS)
        sing += amp * np.sin(2 * np.pi * periodic_freq(f) * T + vibrato + phase()) * slow_lfo(4, 0.3, swell_phase)
    sing += band_noise(rng, 500, 1100, 0.6) + band_noise(rng, 1100, 2200, 0.25)
    low /= np.sqrt((low ** 2).mean())
    sing /= np.sqrt((sing ** 2).mean())
    # energy split roughly 70 % low, 30 % singing
    sig = 0.85 * low + 0.5 * sing
    return sig / np.sqrt((sig ** 2).mean()) * TARGET_RMS


def main():
    sig = synth()
    assert np.abs(sig).max() < 1.0, 'clipping'
    pcm = (sig * 32767).astype('<i2')
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, 'engine.wav')
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '4',
                        '-map_metadata', '-1', OUT], check=True)
    print('ok', os.path.getsize(OUT))


if __name__ == '__main__':
    main()
