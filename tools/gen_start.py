#!/usr/bin/env python3
"""Synthesizes the start-light sounds (start_red, start_yellow, start_go) into app/src/main/assets/sfx.

A classic racing start: two short beeps for the red and yellow light, then a bright, longer
beep with a whoosh and a horn stab when the light turns green. Uses the helpers in gen_audio.py.
Requires: numpy, soundfile (with OGG/Vorbis support).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_audio import SR, at, decay, env, lowpass, mix, noise, note, save, saw, sine, square, sweep  # noqa: E402


def light_beep(n, dur=0.34):
    """Round, punchy start-light beep: sine body with a little square bite and an octave shimmer."""
    f = note(n)
    body = sine(f, dur) + 0.22 * square(f, dur, 0.5) + 0.18 * sine(f * 2, dur)
    click = noise(0.012) * decay(0.012, 300) * 0.25
    return mix(body * env(dur, 0.004, 0.06, 0.75, 0.09), click)


def go_light():
    """Higher, longer beep that swells a bit, plus a whoosh and a horn stab."""
    d = 0.95
    f = note(88) * (1 + 0.006 * np.sin(2 * np.pi * 6.0 * np.arange(int(SR * d)) / SR))
    beep = (sine(f, d) + 0.25 * square(f, d, 0.5) + 0.2 * sine(f * 2, d)) * env(d, 0.004, 0.12, 0.8, 0.35)
    # rising whoosh under the beep
    w = noise(0.7)
    w = lowpass(w, sweep(400, 5000, 0.7)) * env(0.7, 0.25, 0.1, 0.8, 0.35) * 0.5
    # short horn stab (a bright major chord with a quick fall-off)
    hd = 0.55
    horn = np.zeros(int(SR * hd))
    for k, n in enumerate((64, 68, 71)):
        horn = mix(horn, saw(note(n) * (1 + 0.003 * k), hd) * 0.4)
    horn = lowpass(horn, 2600) * env(hd, 0.01, 0.15, 0.55, 0.25)
    return mix(beep, at(w, 0.0), at(horn * 0.55, 0.04))


def main():
    save("start_red", light_beep(76), 0.8)
    save("start_yellow", light_beep(76), 0.8)
    save("start_go", go_light(), 0.85)
    print("start sounds written")


if __name__ == "__main__":
    main()
