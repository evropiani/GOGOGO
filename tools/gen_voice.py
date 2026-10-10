#!/usr/bin/env python3
"""Builds the announcer shouts for the start lights: voice_go1/2/3.ogg in app/src/main/assets/sfx.

A plain spoken "Go!" from an offline speech synthesizer is re-shaped into a shout with the WORLD
vocoder: new pitch contour (higher, excited, vibrato on the long one), stretched vowel, brighter
"shouted" spectral envelope, then a stadium double, saturation, compression and a short slap echo.

The shipped files start from CMU Flite's "awb" voice (male, a clean monophthong "o" that stretches
well); the other sources are there for comparison.

Run:   python3 tools/gen_voice.py                              (writes the three OGG files)
       python3 tools/gen_voice.py --source hts --out /tmp/x    (try another synthesizer voice)
Needs: apt install flite festival festvox-us-slt-hts espeak-ng
       pip install pyworld numpy scipy soundfile
"""
import argparse
import os
import subprocess
import tempfile

import numpy as np
import pyworld as pw
import soundfile as sf
from scipy import signal

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/assets/sfx")
SR = 44100
FP = 5.0  # WORLD frame period (ms)
FFT = 2048  # envelope FFT size at SR

# synthesizer voices to start from: engine, voice, formant scale (to bring a voice into a male range)
SOURCES = {
    "rms": ("flite", "rms", 1.0),
    "awb": ("flite", "awb", 1.0),
    "kal": ("flite", "kal16", 1.0),
    "slt": ("flite", "slt", 0.86),
    "hts": ("festival", "cmu_us_slt_arctic_hts", 0.86),
    "diphone": ("festival", "kal_diphone", 1.0),
}

# the three shouts: vowel length (s), pitch shape (Hz), loudness
SHOUTS = [
    {"name": "voice_go1", "vowel": 0.34, "start": 185, "peak": 228, "end": 168, "vib": 0.0, "gain": 0.9, "seed": 1},
    {"name": "voice_go2", "vowel": 0.36, "start": 205, "peak": 258, "end": 186, "vib": 0.0, "gain": 0.95, "seed": 2},
    {"name": "voice_go3", "vowel": 1.30, "start": 210, "peak": 292, "end": 175, "vib": 0.5, "gain": 1.0, "seed": 3},
]


# ------------------------------------------------------------------ source speech

def speak(engine, voice, text):
    """Runs an offline synthesizer and returns (samples, rate)."""
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "say.wav")
        if engine == "flite":
            subprocess.run(["flite", "-voice", voice, "-t", text, "-o", wav], check=True)
        else:
            txt = os.path.join(tmp, "say.txt")
            with open(txt, "w") as f:
                f.write(text + "\n")
            subprocess.run(["text2wave", "-eval", "(voice_%s)" % voice, "-o", wav, txt], check=True)
        x, sr = sf.read(wav)
    if x.ndim > 1:
        x = x.mean(axis=1)
    return x.astype(np.float64), sr


def analyze(x, sr):
    f0, t = pw.harvest(x, sr, f0_floor=55, f0_ceil=500, frame_period=FP)
    f0 = pw.stonemask(x, f0, t, sr)
    sp = pw.cheaptrick(x, f0, t, sr)
    ap = pw.d4c(x, f0, t, sr)
    return f0, sp, ap


def segment(f0, sp):
    """Frame indices: sound start, voicing start, voicing end (the vowel), sound end."""
    e = 10 * np.log10(np.sum(sp, axis=1) + 1e-12)
    top = e.max()
    loud = np.where(e > top - 38)[0]
    s0, s1 = loud[0], loud[-1]
    # the longest run of loud voicing is the vowel (ignore stray voiced blips)
    on = (f0 > 0) & (e > top - 24)
    best, v0, v1, i = 0, 0, 0, 0
    while i < len(on):
        if on[i]:
            j = i
            while j + 1 < len(on) and on[j + 1]:
                j += 1
            if j - i + 1 > best:
                best, v0, v1 = j - i + 1, i, j
            i = j + 1
        else:
            i += 1
    return s0, max(s0, v0), max(v0 + 4, v1), s1


# ------------------------------------------------------------------ envelope reshaping

def to_grid(sp, ap, sr, fscale):
    """Moves envelope and aperiodicity onto the output FFT grid, warping formants for a shout.

    Shouting opens the jaw (first formant up) and adds effort; the source formants are also scaled by
    fscale to put the voice in a male range."""
    n_src = sp.shape[1]
    f_src = np.linspace(0, sr / 2, n_src)
    n_out = FFT // 2 + 1
    f_out = np.linspace(0, SR / 2, n_out)
    # warp: output frequency f reads the source at f / w(f); w is larger in the F1 region
    w = fscale * (1.04 + 0.12 * np.exp(-f_out / 1400.0))
    f_read = f_out / w
    lim = 0.9 * sr / 2
    lsp = np.log(sp + 1e-16)
    out_sp = np.empty((sp.shape[0], n_out))
    out_ap = np.empty((sp.shape[0], n_out))
    inside = f_read <= lim
    k_lim = np.searchsorted(f_src, lim)
    for i in range(sp.shape[0]):
        row = np.interp(f_read[inside], f_src, lsp[i])
        edge = np.mean(lsp[i, k_lim - 6:k_lim])
        octs = np.log2(f_read[~inside] / lim)
        out_sp[i, inside] = row
        out_sp[i, ~inside] = edge - (6.0 + octs * 24.0) / 10.0 * np.log(10)  # a little "air" above the source band
        out_ap[i, inside] = np.interp(f_read[inside], f_src, ap[i])
        out_ap[i, ~inside] = 0.999
    # shouted tilt: flatter spectrum (+4 dB/oct from 500 Hz to 4 kHz), a presence lift, less rumble
    g = np.clip(4.0 * np.log2(np.maximum(f_out, 500.0) / 500.0), 0, 12.0)
    g -= np.clip(6.0 * np.log2(np.maximum(f_out, 4500.0) / 4500.0), 0, 12.0)
    g += 3.0 * np.exp(-((f_out - 3000.0) / 900.0) ** 2)
    g -= 9.0 * np.clip(1.0 - f_out / 160.0, 0, 1)
    out_sp += g[None, :] / 10.0 * np.log(10)
    return np.exp(out_sp), out_ap


def press(ap):
    """Pressed, effortful phonation: harmonics stay periodic higher up the spectrum."""
    f = np.linspace(0, SR / 2, ap.shape[1])
    k = np.clip((f - 2500.0) / 4000.0, 0, 1)
    return np.clip(ap * (0.35 + 0.65 * k)[None, :], 0.001, 0.999)


# ------------------------------------------------------------------ prosody

def time_map(n_out_cons, v0, v1, vowel_s):
    """Source frame position for every output frame: consonant 1:1, vowel nucleus stretched."""
    on = min(6, max(1, (v1 - v0) // 5))           # g->o transition, kept at speed
    hold_end = v0 + on + max(2, int((v1 - v0 - on) * 0.45))  # end of the steady o, before the off-glide
    n_vowel = int(vowel_s * 1000 / FP)
    n_off = max(4, min(int(0.32 * n_vowel), int((v1 - hold_end) * 1.2)))
    n_hold = max(1, n_vowel - on - n_off)
    src = list(np.arange(v0 - n_out_cons, v0, dtype=np.float64))
    src += list(v0 + np.arange(on, dtype=np.float64))
    src += list(np.linspace(v0 + on, hold_end, n_hold, endpoint=False))
    src += list(np.linspace(hold_end, v1, n_off))
    return np.array(src)


def smooth_noise(rng, n, cutoff_frames):
    x = rng.standard_normal(n + 64)
    b, a = signal.butter(2, 1.0 / max(2.0, cutoff_frames))
    y = signal.filtfilt(b, a, x)[32:32 + n]
    return y / (np.std(y) + 1e-9)


def pitch(n, spec, rng):
    """Excited shout contour: scoop up to the peak, then fall; the long one gets a growing vibrato."""
    t = np.arange(n) * FP / 1000.0
    dur = max(t[-1], 1e-3)
    u = t / dur
    st0 = 12 * np.log2(spec["start"] / spec["peak"])
    st1 = 12 * np.log2(spec["end"] / spec["peak"])
    if spec["vib"] > 0:
        # long call: rise for 0.3 s, hold near the top (slight climb), fall in the last 0.28 s
        rise = np.clip(t / 0.30, 0, 1)
        fall = np.clip((t - (dur - 0.28)) / 0.28, 0, 1)
        st = st0 * (1 - rise) ** 2 - 0.6 + 0.6 * np.clip(t / (dur - 0.28), 0, 1)
        st = st * (1 - fall) + st1 * fall ** 1.6
        depth = spec["vib"] * np.clip((t - 0.32) / 0.5, 0, 1) * (1 - fall)
        vib_rate = 5.6 + 0.4 * u
        st += depth * np.sin(2 * np.pi * np.cumsum(vib_rate) * FP / 1000.0)
    else:
        rise = np.clip(t / 0.07, 0, 1)
        st = st0 * (1 - rise) ** 2
        st += st1 * np.clip((u - 0.35) / 0.65, 0, 1) ** 1.5
    st += 0.1 * smooth_noise(rng, n, 6)  # micro-jitter so it does not sound machine-flat
    return spec["peak"] * 2 ** (st / 12.0)


def loudness(n_cons, n_vowel, spec, rng):
    """Per-frame amplitude: punchy attack; the long one swells and then trails off."""
    a = np.ones(n_cons + n_vowel)
    t = np.arange(n_vowel) * FP / 1000.0
    dur = max(t[-1], 1e-3)
    att = np.clip(t / 0.025, 0, 1)
    if spec["vib"] > 0:
        body = 0.82 + 0.18 * np.clip(t / 0.5, 0, 1)
        rel = np.clip((dur - t) / 0.32, 0, 1) ** 1.3
    else:
        body = 1.0 - 0.18 * np.clip(t / dur, 0, 1)
        rel = np.clip((dur - t) / 0.12, 0, 1)
    v = att * body * rel
    v *= 10 ** (0.6 * smooth_noise(rng, n_vowel, 8) / 20)
    a[n_cons:] = v
    return a


# ------------------------------------------------------------------ building one shout

def interp_frames(m, pos):
    i = np.clip(np.floor(pos).astype(int), 0, len(m) - 1)
    j = np.clip(i + 1, 0, len(m) - 1)
    f = (pos - np.floor(pos))[:, None]
    return m[i] * (1 - f) + m[j] * f


def render(src, spec, detune=0.0, warp=1.0, seed_add=0):
    x, sr, fscale = src
    f0, sp, ap = analyze(x, sr)
    s0, v0, v1, s1 = segment(f0, sp)
    lsp, gap = to_grid(sp, ap, sr, fscale * warp)
    lsp = np.log(lsp)
    # level the vowel: every frame at the vowel's peak level (the shout's own loudness curve comes later)
    e = np.log(np.sum(sp, axis=1) + 1e-16)
    e = np.convolve(np.pad(e, 2, mode="edge"), np.ones(5) / 5, "valid")
    ref = e[v0:v1 + 1].max()
    lsp[v0:v1 + 1] += np.minimum(ref - e[v0:v1 + 1], 12 / 10 * np.log(10))[:, None]
    n_cons = max(1, v0 - s0)
    pos = time_map(n_cons, v0, v1, spec["vowel"])
    n_vowel = len(pos) - n_cons
    rng = np.random.default_rng(spec["seed"] * 101 + seed_add)
    out_sp = np.exp(interp_frames(lsp, pos))
    out_ap = interp_frames(gap, pos)
    f0o = np.zeros(len(pos))
    f0o[n_cons:] = pitch(n_vowel, spec, rng) * 2 ** (detune / 1200.0)
    out_ap[n_cons:] = press(out_ap[n_cons:])
    amp = loudness(n_cons, n_vowel, spec, rng)
    out_sp *= (np.maximum(amp, 1e-3) ** 2)[:, None]  # WORLD needs a non-zero envelope
    # a crisp but not spitty /g/ release; soften any long closure murmur before it
    out_sp[:n_cons] *= 0.5
    if n_cons > 8:
        out_sp[:n_cons - 8] *= 0.25
    y = pw.synthesize(np.ascontiguousarray(f0o), np.ascontiguousarray(out_sp), np.ascontiguousarray(out_ap), SR, FP)
    return y


# ------------------------------------------------------------------ finishing

def compress(x, thresh_db=-14.0, ratio=3.0, att=0.004, rel=0.08):
    """Peak compressor (keeps the long call's sustain as loud as its attack)."""
    env = np.abs(x)
    a_att = np.exp(-1.0 / (SR * att))
    a_rel = np.exp(-1.0 / (SR * rel))
    e = 0.0
    out = np.empty_like(env)
    for i, v in enumerate(env):
        c = a_att if v > e else a_rel
        e = c * e + (1 - c) * v
        out[i] = e
    db = 20 * np.log10(out + 1e-9)
    over = np.maximum(0.0, db - thresh_db)
    gain = 10 ** (-over * (1 - 1.0 / ratio) / 20)
    return x * gain


def room(x, rng, long_call):
    """Short slap echo off the stands plus a small, dark room."""
    n_tail = int(SR * (0.42 if long_call else 0.3))
    y = np.concatenate([x, np.zeros(n_tail)])
    out = y.copy()
    for d_ms, g in ((110, 0.17), (220, 0.07)) if long_call else ((95, 0.13),):
        d = int(SR * d_ms / 1000)
        out[d:] += g * y[:len(y) - d]
    ir_len = int(SR * 0.35)
    tt = np.arange(ir_len) / SR
    ir = rng.standard_normal(ir_len) * np.exp(-tt / 0.07)
    ir[:int(SR * 0.012)] = 0
    b, a = signal.butter(2, 4500 / (SR / 2))
    ir = signal.lfilter(b, a, ir)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    wet = signal.fftconvolve(y, ir)[:len(y)]
    out += 0.1 * wet
    return out


def trim(x, floor_db=-50.0):
    e = np.abs(x)
    th = e.max() * 10 ** (floor_db / 20)
    idx = np.where(e > th)[0]
    return x[max(0, idx[0] - int(SR * 0.004)): idx[-1] + 1]


def finish(main, double, spec):
    lag = int(SR * 0.012)
    n = max(len(main), len(double) + lag)
    x = np.zeros(n)
    x[:len(main)] += main
    x[lag:lag + len(double)] += 0.3 * double
    x = trim(x)
    b, a = signal.butter(2, 90 / (SR / 2), "highpass")
    x = signal.lfilter(b, a, x)
    x /= np.max(np.abs(x)) + 1e-9
    x = compress(x)
    x /= np.max(np.abs(x)) + 1e-9
    x = np.tanh(1.5 * x) / np.tanh(1.5)  # a bit of vocal grit
    rng = np.random.default_rng(spec["seed"] + 77)
    x = room(x, rng, spec["vib"] > 0)
    x = trim(x, -60.0)
    fade_in = int(SR * 0.003)
    fade_out = int(SR * 0.08)
    x[:fade_in] *= np.linspace(0, 1, fade_in)
    x[-fade_out:] *= np.linspace(1, 0, fade_out) ** 2
    x = x / (np.max(np.abs(x)) + 1e-9) * 10 ** (-1.0 / 20) * spec["gain"]
    return x.astype(np.float32)


def build(source, out_dir):
    engine, voice, fscale = SOURCES[source]
    x, sr = speak(engine, voice, "Go!")
    src = (x, sr, fscale)
    os.makedirs(out_dir, exist_ok=True)
    for spec in SHOUTS:
        main = render(src, spec)
        double = render(src, spec, detune=10.0, warp=1.02, seed_add=5)
        y = finish(main, double, spec)
        path = os.path.join(out_dir, spec["name"] + ".ogg")
        sf.write(path, y, SR, format="OGG", subtype="VORBIS")
        print("%s  %.2f s" % (path, len(y) / SR))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", default="awb", choices=sorted(SOURCES))
    ap.add_argument("--out", default=OUT)
    a = ap.parse_args()
    build(a.source, a.out)


if __name__ == "__main__":
    main()
