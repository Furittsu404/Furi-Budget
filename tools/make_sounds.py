#!/usr/bin/env python3
"""Generates the app's built-in sound styles as small 16-bit mono WAV files in app/src/main/res/raw.

Everything is plain synthesis (sines, squares, decaying partials), so the sounds are original and carry no licence.
Run from the project root: python3 tools/make_sounds.py
Also writes app/src/main/kotlin/app/furibudget/SoundCatalog.kt so the shrinker keeps every file.
"""
import math, struct, wave, os, random

RATE = 22050
OUT = "app/src/main/res/raw"
KT = "app/src/main/kotlin/app/furibudget/SoundCatalog.kt"
EVENTS = ["keypad", "saved", "paid", "deleted", "goal"]
STYLES = ["soft", "pop", "coins", "water", "retro"]

# Notes in Hz
C5, D5, E5, G5, A5, C6, E6, G4, E4, C4 = 523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1318.5, 392.0, 329.63, 261.63


def env(t, dur, attack=0.005, release=0.06):
    if t < attack:
        return t / attack
    if t > dur - release:
        return max(0.0, (dur - t) / release)
    return 1.0


def tone(freq, dur, kind="sine", decay=None, sweep=None, vol=0.5, attack=0.004, release=0.05):
    n = int(RATE * dur)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = freq if sweep is None else freq + (sweep - freq) * (t / dur)
        phase += 2 * math.pi * f / RATE
        if kind == "sine":
            s = math.sin(phase)
        elif kind == "square":
            s = 1.0 if math.sin(phase) >= 0 else -1.0
            s *= 0.6
        elif kind == "tri":
            s = 2 / math.pi * math.asin(math.sin(phase))
        else:
            s = math.sin(phase)
        a = env(t, dur, attack, release)
        if decay:
            a *= math.exp(-t * decay)
        out.append(s * a * vol)
    return out


def metal(freq, dur, vol=0.45):
    """Coin-like ring: a few inharmonic partials that die away quickly."""
    n = int(RATE * dur)
    parts = [(1.0, 1.0, 9.0), (2.76, 0.6, 12.0), (5.4, 0.35, 18.0), (8.93, 0.2, 26.0)]
    out = []
    for i in range(n):
        t = i / RATE
        s = sum(g * math.sin(2 * math.pi * freq * r * t) * math.exp(-t * d) for r, g, d in parts)
        out.append(s * vol * min(1.0, t / 0.002) / 2.2)
    return out


def seq(*chunks, gap=0.0):
    out = []
    for c in chunks:
        out += c
        out += [0.0] * int(RATE * gap)
    return out


def mix_at(base, add, at):
    start = int(RATE * at)
    need = start + len(add)
    if len(base) < need:
        base += [0.0] * (need - len(base))
    for i, v in enumerate(add):
        base[start + i] += v
    return base


def write(path, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = min(1.0, 0.9 / peak)
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * scale)) * 32767)) for s in samples))


def notes(freqs, each, kind="sine", gap=0.0, decay=None, vol=0.5, release=0.06):
    return seq(*[tone(f, each, kind, decay=decay, vol=vol, release=release) for f in freqs], gap=gap)


def make(style, event):
    if style == "soft":
        return {
            "keypad": tone(1500, 0.03, "sine", decay=60, vol=0.35, release=0.02),
            "saved": notes([E5, A5], 0.11, decay=6),
            "paid": notes([C5, E5, G5], 0.1, decay=6),
            "deleted": notes([E5, C5], 0.1, decay=7, vol=0.4),
            "goal": notes([C5, E5, G5, C6], 0.12, decay=4, release=0.1),
        }[event]
    if style == "pop":
        def bubble(f0, f1, d=0.09, v=0.55):
            return tone(f0, d, "sine", sweep=f1, decay=14, vol=v, release=0.02)
        return {
            "keypad": bubble(900, 1500, 0.035, 0.4),
            "saved": seq(bubble(500, 1100), bubble(700, 1500), gap=0.02),
            "paid": seq(bubble(500, 1000), bubble(650, 1300), bubble(800, 1700), gap=0.02),
            "deleted": bubble(1100, 350, 0.14),
            "goal": seq(bubble(500, 1000), bubble(650, 1300), bubble(800, 1700), bubble(1000, 2100, 0.16), gap=0.03),
        }[event]
    if style == "coins":
        return {
            "keypad": metal(3200, 0.05, 0.3),
            "saved": mix_at(metal(2400, 0.28), metal(3100, 0.3), 0.07),
            "paid": mix_at(mix_at(metal(2300, 0.3), metal(2900, 0.3), 0.07), metal(3500, 0.34), 0.14),
            "deleted": metal(1500, 0.26, 0.4),
            "goal": mix_at(mix_at(mix_at(metal(2300, 0.3), metal(2900, 0.3), 0.08), metal(3500, 0.3), 0.16), metal(4200, 0.5), 0.24),
        }[event]
    if style == "water":
        def drip(f0, f1, d=0.16, v=0.5):
            return tone(f0, d, "sine", sweep=f1, decay=11, vol=v, release=0.04)
        return {
            "keypad": drip(1300, 1700, 0.04, 0.35),
            "saved": drip(600, 1300),
            "paid": seq(drip(600, 1300), drip(700, 1500), gap=0.05),
            "deleted": drip(1200, 500, 0.2),
            "goal": seq(drip(600, 1300), drip(700, 1500), drip(800, 1800), drip(900, 2000, 0.3), gap=0.06),
        }[event]
    if style == "retro":
        return {
            "keypad": tone(1800, 0.025, "square", vol=0.25, release=0.01),
            "saved": notes([C5, G5], 0.07, "square", vol=0.3, release=0.01),
            "paid": notes([C5, E5, G5], 0.07, "square", vol=0.3, release=0.01),
            "deleted": notes([G4, E4, C4], 0.07, "square", vol=0.3, release=0.01),
            "goal": notes([C5, E5, G5, C6, E6], 0.08, "square", vol=0.3, release=0.01),
        }[event]
    raise ValueError(style)


def main():
    os.makedirs(OUT, exist_ok=True)
    random.seed(1)
    entries = []
    for style in STYLES:
        for event in EVENTS:
            name = f"snd_{style}_{event}"
            write(f"{OUT}/{name}.wav", make(style, event))
            entries.append((style, event, name))
    lines = [
        "package app.furibudget",
        "",
        "/** Generated by tools/make_sounds.py. Lists every built-in sound so the shrinker keeps the raw files. */",
        "object SoundCatalog {",
        "    val styles = listOf(" + ", ".join(f'"{s}"' for s in STYLES) + ")",
        "    val events = listOf(" + ", ".join(f'"{e}"' for e in EVENTS) + ")",
        "",
        "    /** Resource id of the built-in sound for [style] and [event], or null for \"silent\" and unknown names. */",
        "    fun res(style: String, event: String): Int? = when (style to event) {",
    ]
    for style, event, name in entries:
        lines.append(f'        "{style}" to "{event}" -> R.raw.{name}')
    lines += ["        else -> null", "    }", "}", ""]
    open(KT, "w").write("\n".join(lines))
    total = sum(os.path.getsize(f"{OUT}/{n}.wav") for _, _, n in entries)
    print(f"wrote {len(entries)} files, {total/1024:.0f} KB")


main()
