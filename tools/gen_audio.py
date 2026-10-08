#!/usr/bin/env python3
"""Synthesizes all sound effects and the two music loops (OGG Vorbis) into app/src/main/assets/sfx.

Everything is generated from scratch with numpy, so there are no third-party samples.
Requires: numpy, soundfile (with OGG/Vorbis support).
"""
import os
import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/assets/sfx")
SR = 44100
rng = np.random.default_rng(1234)


# ------------------------------------------------------------------ building blocks

def t_of(dur):
    return np.arange(int(SR * dur)) / SR


def phase(freq, dur):
    """Phase accumulator for a (possibly time-varying) frequency in Hz."""
    f = np.broadcast_to(np.asarray(freq, dtype=np.float64), (int(SR * dur),))
    return 2 * np.pi * np.cumsum(f) / SR


def sine(freq, dur):
    return np.sin(phase(freq, dur))


def square(freq, dur, duty=0.5):
    p = (phase(freq, dur) / (2 * np.pi)) % 1.0
    return np.where(p < duty, 1.0, -1.0)


def saw(freq, dur):
    p = (phase(freq, dur) / (2 * np.pi)) % 1.0
    return 2 * p - 1


def tri(freq, dur):
    p = (phase(freq, dur) / (2 * np.pi)) % 1.0
    return 2 * np.abs(2 * p - 1) - 1


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def sweep(f0, f1, dur, curve=1.0):
    t = np.linspace(0, 1, int(SR * dur))
    return f0 + (f1 - f0) * t ** curve


def expsweep(f0, f1, dur):
    t = np.linspace(0, 1, int(SR * dur))
    return f0 * (f1 / f0) ** t


def env(dur, a=0.005, d=0.1, s=0.6, r=0.1):
    n = int(SR * dur)
    e = np.ones(n) * s
    na, nd, nr = int(SR * a), int(SR * d), int(SR * r)
    na = min(na, n)
    e[:na] = np.linspace(0, 1, na)
    nd = min(nd, n - na)
    e[na:na + nd] = np.linspace(1, s, nd)
    nr = min(nr, n)
    if nr > 0:
        e[n - nr:] *= np.linspace(1, 0, nr)
    return e


def decay(dur, k=8.0):
    t = t_of(dur)
    e = np.exp(-k * t)
    a = min(len(e), int(SR * 0.003))
    e[:a] *= np.linspace(0, 1, a)
    return e


def lowpass(x, cutoff):
    """One-pole low-pass; cutoff may be an array."""
    c = np.broadcast_to(np.asarray(cutoff, dtype=np.float64), x.shape)
    a = 1 - np.exp(-2 * np.pi * c / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += a[i] * (x[i] - acc)
        y[i] = acc
    return y


def bandpass(x, f, q=4.0):
    """Simple biquad band-pass at fixed frequency."""
    w = 2 * np.pi * f / SR
    alpha = np.sin(w) / (2 * q)
    b0, b1, b2 = alpha, 0, -alpha
    a0, a1, a2 = 1 + alpha, -2 * np.cos(w), 1 - alpha
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i in range(len(x)):
        y0 = (b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1 = x1, x[i]
        y2, y1 = y1, y0
        y[i] = y0
    return y


def pad(x, dur):
    n = int(SR * dur)
    if len(x) >= n:
        return x[:n]
    return np.concatenate([x, np.zeros(n - len(x))])


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def at(x, offset, total=None):
    n = int(SR * offset)
    y = np.concatenate([np.zeros(n), x])
    return pad(y, total) if total else y


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def note(n):
    """MIDI note number to Hz."""
    return 440.0 * 2 ** ((n - 69) / 12)


def save(name, x, peak=0.85):
    os.makedirs(OUT, exist_ok=True)
    x = norm(x, peak).astype(np.float32)
    sf.write(os.path.join(OUT, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")


# ------------------------------------------------------------------ effects

def fx_click():
    d = 0.06
    return sine(expsweep(1100, 500, d), d) * decay(d, 60) + noise(d) * decay(d, 200) * 0.3


def fx_pop():
    d = 0.09
    return sine(expsweep(350, 1300, d), d) * decay(d, 40)


def fx_coin():
    a = square(note(83), 0.07, 0.25) * decay(0.07, 20)
    b = square(note(88), 0.25, 0.25) * decay(0.25, 12)
    return mix(a * 0.6, at(b * 0.6, 0.06))


def fx_buy():
    out = np.zeros(int(SR * 0.7))
    for i, n in enumerate([72, 76, 79, 84]):
        s = (square(note(n), 0.25, 0.3) * 0.5 + tri(note(n + 12), 0.25) * 0.5) * decay(0.25, 10)
        out = mix(out, at(s, i * 0.06))
    sh = noise(0.5) * decay(0.5, 6)
    sh = sh - lowpass(sh, 5000)
    return mix(out, at(sh * 0.25, 0.18))


def fx_nope():
    a = square(sweep(260, 220, 0.14), 0.14, 0.4) * env(0.14, 0.005, 0.05, 0.7, 0.03)
    b = square(sweep(200, 140, 0.24), 0.24, 0.4) * env(0.24, 0.005, 0.08, 0.7, 0.08)
    return lowpass(mix(a, at(b, 0.16)), 2500)


def fx_beep():
    d = 0.18
    return (square(note(76), d, 0.5) * 0.4 + sine(note(76), d)) * env(d, 0.003, 0.05, 0.6, 0.06)


def fx_go():
    d = 0.6
    f = sweep(note(81), note(88), d, 0.3)
    s = square(f, d, 0.5) * 0.35 + sine(f * 1.5, d) * 0.4 + sine(f, d)
    return s * env(d, 0.004, 0.1, 0.6, 0.3)


def fx_tick():
    d = 0.05
    return sine(1700, d) * decay(d, 90) + noise(d) * decay(d, 300) * 0.4


def fx_drop():
    d = 0.8
    n = noise(d)
    n = lowpass(n, np.linspace(4000, 300, int(SR * d)))
    low = sine(expsweep(220, 45, d), d)
    return n * env(d, 0.01, 0.2, 0.6, 0.4) * 0.8 + low * decay(d, 4) * 0.9


def fx_land():
    d = 0.18
    return sine(expsweep(150, 55, d), d) * decay(d, 22) + lowpass(noise(d), 1200) * decay(d, 60) * 0.8


def fx_bump():
    d = 0.3
    f = 320 + 140 * np.sin(np.linspace(0, 9 * np.pi, int(SR * d))) * np.linspace(1, 0, int(SR * d))
    return sine(f, d) * decay(d, 10) + tri(f * 0.5, d) * decay(d, 14) * 0.5


def fx_bonk():
    d = 0.35
    wood = sine(expsweep(900, 600, 0.06), 0.06) * decay(0.06, 60)
    f = expsweep(520, 160, d)
    boing = tri(f * (1 + 0.04 * np.sin(np.linspace(0, 40, int(SR * d)))), d) * decay(d, 8)
    return mix(wood * 1.2, boing * 0.9, noise(0.03) * 0.6)


def fx_boost():
    d = 0.55
    f = expsweep(70, 210, d)
    eng = lowpass(saw(f, d) + saw(f * 1.01, d), 1800) * env(d, 0.01, 0.2, 0.7, 0.2)
    wh = noise(d)
    wh = lowpass(wh, np.linspace(800, 6000, int(SR * d))) * env(d, 0.05, 0.2, 0.6, 0.2)
    return eng * 0.7 + wh * 0.6


def fx_fall():
    # slide whistle down
    d = 1.0
    f = expsweep(1500, 280, d) * (1 + 0.025 * np.sin(np.linspace(0, 2 * np.pi * 7, int(SR * d))))
    s = sine(f, d) + 0.15 * sine(f * 2, d)
    return s * env(d, 0.02, 0.1, 0.85, 0.25) + lowpass(noise(d), 2000) * 0.05


def fx_win():
    seq = [(72, 0.0, 0.14), (76, 0.14, 0.14), (79, 0.28, 0.14), (84, 0.42, 0.5)]
    out = np.zeros(int(SR * 1.6))
    for n, start, dur in seq:
        s = (square(note(n), dur, 0.35) * 0.35 + tri(note(n), dur) * 0.6) * env(dur, 0.005, 0.05, 0.7, 0.06)
        out = mix(out, at(s, start))
    chord = sum(tri(note(n), 0.9) for n in [72, 76, 79, 84]) * env(0.9, 0.01, 0.2, 0.6, 0.5) * 0.35
    out = mix(out, at(chord, 0.6))
    return out


def fx_lose():
    seq = [(67, 0.0, 0.32), (66, 0.38, 0.32), (65, 0.76, 0.32), (64, 1.14, 1.0)]
    out = np.zeros(int(SR * 2.3))
    for i, (n, start, dur) in enumerate(seq):
        vib = 1 + (0.03 * np.sin(np.linspace(0, 2 * np.pi * 6 * dur, int(SR * dur))) if i == 3 else 0)
        f = note(n - 12) * vib
        s = saw(f, dur) + saw(f * 1.005, dur)
        s = lowpass(s, 900 + 600 * env(dur, 0.05, 0.15, 0.4, 0.1)) * env(dur, 0.03, 0.1, 0.8, 0.12)
        out = mix(out, at(s, start))
    return out


def fx_quack():
    d = 0.26
    f = expsweep(560, 420, d)
    src = saw(f, d) + 0.5 * square(f, d, 0.3)
    q = bandpass(src, 1100, 3.0) * 1.2 + bandpass(src, 2600, 5.0) * 0.6 + bandpass(src, 700, 2.0) * 0.4
    e = env(d, 0.01, 0.05, 0.8, 0.09)
    return q * e


def fx_unlock():
    out = np.zeros(int(SR * 1.6))
    notes = [72, 76, 79, 83, 84, 88, 91, 96]
    for i, n in enumerate(notes):
        s = (sine(note(n), 0.5) + 0.3 * sine(note(n) * 2, 0.5)) * decay(0.5, 6)
        out = mix(out, at(s * 0.6, i * 0.08))
    sp = noise(1.4)
    sp = (sp - lowpass(sp, 6000)) * decay(1.4, 3) * 0.3
    return mix(out, at(sp, 0.1))


def fx_whoosh():
    d = 0.35
    n = noise(d)
    n = lowpass(n, np.linspace(500, 5000, int(SR * d))) * env(d, 0.08, 0.1, 0.8, 0.15)
    return n


def fx_honk():
    d = 0.32
    s = square(400, d, 0.45) + square(503, d, 0.45)
    return lowpass(s, 2200) * env(d, 0.01, 0.05, 0.8, 0.06)


# ------------------------------------------------------------------ music

def drum_kick():
    d = 0.22
    return sine(expsweep(150, 45, d), d) * decay(d, 14)


def drum_snare():
    d = 0.18
    n = noise(d)
    return (n - lowpass(n, 900)) * decay(d, 18) * 0.8 + sine(190, d) * decay(d, 30) * 0.4


def drum_hat(open_=False):
    d = 0.12 if open_ else 0.04
    n = noise(d)
    return (n - lowpass(n, 7000)) * decay(d, 25 if open_ else 90) * 0.5


def place(buf, x, start_s, gain=1.0):
    i = int(start_s * SR)
    if i >= len(buf):
        return
    n = min(len(x), len(buf) - i)
    buf[i:i + n] += x[:n] * gain


def song(bpm, bars, chords, lead, bass_style, hat16, seed):
    beat = 60.0 / bpm
    total = bars * 4 * beat
    loop_n = int(SR * total)
    buf = np.zeros(loop_n + SR * 2)  # room for ringing tails
    kick, snare, hat, ohat = drum_kick(), drum_snare(), drum_hat(), drum_hat(True)
    for bar in range(bars):
        t0 = bar * 4 * beat
        root = chords[bar % len(chords)]
        # drums
        for b in range(4):
            place(buf, kick, t0 + b * beat, 0.9 if b % 2 == 0 else 0.0)
            if b % 2 == 1:
                place(buf, snare, t0 + b * beat, 0.7)
        if bar % 4 == 3:
            place(buf, kick, t0 + 3.5 * beat, 0.6)
        steps = 16 if hat16 else 8
        for h in range(steps):
            place(buf, ohat if (h % (steps // 4) == steps // 8) else hat, t0 + h * 4 * beat / steps, 0.35)
        # bass
        for s in range(8):
            n = root - 24 + (12 if (bass_style and s % 2 == 1) else 0)
            d = beat / 2 * 0.9
            x = (tri(note(n), d) * 0.8 + square(note(n), d, 0.5) * 0.2) * env(d, 0.005, 0.05, 0.7, 0.03)
            place(buf, x, t0 + s * beat / 2, 0.55)
        # chord stabs (offbeats)
        triad = [root, root + (3 if root in (69, 64, 62) else 4), root + 7]
        for s in range(4):
            d = beat * 0.3
            x = sum(square(note(n), d, 0.25) for n in triad) * env(d, 0.003, 0.05, 0.5, 0.05) * 0.12
            place(buf, x, t0 + s * beat + beat / 2, 1.0)
        # lead
        pattern = lead[bar % len(lead)]
        for k, n in enumerate(pattern):
            if n is None:
                continue
            d = beat / 2 * 0.85
            x = (square(note(n), d, 0.25) * 0.5 + tri(note(n), d) * 0.5) * env(d, 0.004, 0.06, 0.55, 0.05)
            place(buf, x, t0 + k * beat / 2, 0.32)
    # wrap the tails around so the loop point is seamless
    out = buf[:loop_n].copy()
    tail = buf[loop_n:]
    out[:len(tail)] += tail[:loop_n]
    return np.tanh(out * 1.2)


def main():
    effects = {
        "click": fx_click, "buy": fx_buy, "nope": fx_nope, "beep": fx_beep, "go": fx_go, "tick": fx_tick,
        "drop": fx_drop, "land": fx_land, "bump": fx_bump, "bonk": fx_bonk, "boost": fx_boost, "fall": fx_fall,
        "pop": fx_pop, "win": fx_win, "lose": fx_lose, "coin": fx_coin, "quack": fx_quack, "unlock": fx_unlock,
        "whoosh": fx_whoosh, "honk": fx_honk,
    }
    for name, fn in effects.items():
        save(name, fn())
        print("sfx", name)

    C, G, A, F, D, E = 72, 67, 69, 65, 62, 64
    menu_lead = [
        [84, None, 79, 76, 79, None, 84, 86],
        [83, None, 79, 74, 79, None, 83, 86],
        [84, None, 81, 76, 81, None, 84, 88],
        [81, None, 77, 72, 77, 79, 81, None],
    ]
    save("music_menu", song(118, 16, [C, G, A, F], menu_lead, True, False, 1), 0.8)
    print("music menu")
    game_lead = [
        [84, 86, 88, 84, 91, None, 88, None],
        [81, 84, 88, 81, 86, None, 84, None],
        [77, 81, 84, 77, 86, 84, 81, 79],
        [79, 83, 86, 79, 91, None, 89, 88],
    ]
    save("music_game", song(146, 16, [C, A, F, G], game_lead, True, True, 2), 0.8)
    print("music game")


if __name__ == "__main__":
    main()
