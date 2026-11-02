#!/usr/bin/env python3
"""Synthesizes the engine loop (soft, low hum with a gentle airy layer).

Output: src/main/resources/assets/puddlejumper/sounds/engine.ogg (mono, 44.1 kHz, seamless loop)

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
PEAK = 0.32                 # quiet on purpose; mostly low frequencies, so it sounds softer than its level


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
    base = 110.0
    # hum: fundamental with gently falling harmonics, slightly detuned pairs for a slow, smooth beat
    hum = np.zeros(N)
    for k, amp in ((1, 1.0), (2, 0.42), (3, 0.16), (4, 0.07), (6, 0.025)):
        hum += tone(base * k, amp, rng.uniform(0, 2 * np.pi))
        hum += tone(base * k + 0.5, amp * 0.45, rng.uniform(0, 2 * np.pi))
    hum *= slow_lfo(2, 0.06)
    # airy layer: low rumble and a faint, soft hiss, slowly breathing
    air = band_noise(rng, 40, 260, 0.35) * slow_lfo(1, 0.15, 1.0)
    air += band_noise(rng, 300, 1800, 0.06) * slow_lfo(3, 0.25, 2.0)
    # a quiet high "energy" shimmer, typical for Ancient tech, very low in the mix
    shimmer = tone(base * 8 + 1.0, 0.012) * slow_lfo(2, 0.5, 0.5)
    sig = hum + air + shimmer
    return sig / np.abs(sig).max() * PEAK


def main():
    pcm = (synth() * 32767).astype('<i2')
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
