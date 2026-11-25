#!/usr/bin/env python3
"""Summarize skin loads in a JFR recording made with -PskinLoadJfr=<dir>.

Usage: python3 scripts/skin-load-jfr.py recording.jfr [--top 10] [--samples 15]
Needs the JDK `jfr` tool (JAVA_HOME/bin/jfr or on PATH).

For every beatoraja.SkinLoad event, it prints:
- the time in each phase (header, lua-exec, lua-convert, json-parse, pixmap-load, texture-upload, dispose-old)
  and "objects/other", the rest of the load (building skin objects, fonts, ...)
- how pixmap-load splits into pool hits, .cim reads, image decodes and .cim writes
- the slowest images
- CPU samples (Java + native) on the loading thread during "objects/other", by method
"""
import argparse, collections, datetime, json, os, re, shutil, subprocess, sys

TOP_LEVEL = ["header", "lua-exec", "lua-convert", "json-parse", "prefetch-start", "prefetch-wait", "pixmap-load",
             "texture-upload", "prefetch-wait-all", "dispose-old"]
IMAGE_SUB = ["pixmap-pool-hit", "cim-read", "image-decode", "cim-write"]


def jfr_tool():
    home = os.environ.get("JAVA_HOME")
    cand = os.path.join(home, "bin", "jfr") if home else None
    return cand if cand and os.path.exists(cand) else (shutil.which("jfr") or sys.exit("jfr tool not found"))


def events(path, names, depth=64):
    out = subprocess.run([jfr_tool(), "print", "--json", "--stack-depth", str(depth), "--events", ",".join(names), path],
                         check=True, capture_output=True, text=True).stdout
    return json.loads(out)["recording"]["events"]


def ts(s):
    # 2026-11-25T17:41:18.848717966+09:00 -> seconds (keep 9 fractional digits)
    m = re.match(r"(.*T\d\d:\d\d:\d\d)(?:\.(\d+))?(.*)$", s)
    base = datetime.datetime.fromisoformat(m.group(1) + (m.group(3) or ""))
    frac = int((m.group(2) or "0").ljust(9, "0")[:9]) / 1e9
    return base.timestamp() + frac


def dur(s):
    # ISO-8601 duration such as PT0.001234567S or PT1M2.5S; also accept plain numbers of ns
    if isinstance(s, (int, float)):
        return s / 1e9
    m = re.match(r"P(?:T(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?)?$", s)
    if not m:
        return 0.0
    h, mi, sec = (float(x) if x else 0.0 for x in m.groups())
    return h * 3600 + mi * 60 + sec


def thread_of(ev):
    v = ev["values"]
    t = v.get("sampledThread") or v.get("eventThread") or {}
    return t.get("javaName") or t.get("osName") or "?"


def frames(ev):
    st = ev["values"].get("stackTrace") or {}
    for f in st.get("frames") or []:
        m = f["method"]
        yield f'{m["type"]["name"].replace("/", ".")}.{m["name"]}'


def ms(x):
    return f"{x * 1000:8.1f} ms"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jfr")
    ap.add_argument("--top", type=int, default=10, help="slowest images per load")
    ap.add_argument("--samples", type=int, default=15, help="methods listed for objects/other")
    ap.add_argument("--no-samples", action="store_true", help="skip CPU samples (much faster on long recordings)")
    a = ap.parse_args()

    loads = sorted(events(a.jfr, ["beatoraja.SkinLoad"]), key=lambda e: ts(e["values"]["startTime"]))
    if not loads:
        sys.exit("no beatoraja.SkinLoad events (was the game started with -PskinLoadJfr=<dir>?)")
    phases = events(a.jfr, ["beatoraja.SkinLoadPhase"])
    samples = [] if a.no_samples else events(a.jfr, ["jdk.ExecutionSample", "jdk.NativeMethodSample"])

    def window(ev):
        s = ts(ev["values"]["startTime"])
        return s, s + dur(ev["values"]["duration"])

    ph = [(window(e), thread_of(e), e["values"]) for e in phases]
    sm = [(ts(e["values"]["startTime"]), thread_of(e), e) for e in samples]

    grand = collections.Counter()
    for i, load in enumerate(loads, 1):
        v = load["values"]
        (s, e), thread = window(load), thread_of(load)
        total = e - s
        mine = [(w, p) for w, t, p in ph if t == thread and w[0] >= s and w[1] <= e]
        by = collections.Counter(); cnt = collections.Counter(); mb = collections.Counter()
        per_image = collections.Counter()
        top_windows = []
        tops = [w for w, p in mine if p["phase"] in TOP_LEVEL]
        def nested(w):
            # e.g. lua-exec inside header (Lua loadHeader runs the file too): counted by the outer phase
            return any(o != w and o[0] <= w[0] and w[1] <= o[1] for o in tops)
        for (ps, pe), p in mine:
            d = pe - ps
            if p["phase"] in TOP_LEVEL and nested((ps, pe)):
                by["(in header) " + p["phase"]] += d; cnt["(in header) " + p["phase"]] += 1
                continue
            by[p["phase"]] += d; cnt[p["phase"]] += 1
            if p["phase"] in ("pixmap-load", "texture-upload"):
                per_image[p.get("detail") or "?"] += d
                mb[p["phase"]] += (p.get("bytes") or 0) / 1e6
            if p["phase"] in TOP_LEVEL:
                top_windows.append((ps, pe))
        other = total - sum(by[k] for k in TOP_LEVEL)
        print(f"\n=== load #{i}: {v.get('skinType')} [{v.get('format')}] {ms(total)}  thread={thread}  ok={v.get('success')}")
        print(f"    {v.get('path')}")
        for k in TOP_LEVEL:
            if cnt[k]:
                extra = f"  {mb[k]:7.1f} MB" if mb[k] else ""
                print(f"  {k:16s} {ms(by[k])} {100 * by[k] / total:5.1f} %  n={cnt[k]}{extra}")
            if k == "header":
                for kk in ("lua-exec", "lua-convert"):
                    if cnt["(in header) " + kk]:
                        d = by["(in header) " + kk]
                        print(f"    {'of which ' + kk:14s} {ms(d)}")
        print(f"  {'objects/other':16s} {ms(other)} {100 * other / total:5.1f} %")
        if cnt["pixmap-load"]:
            print("  pixmap-load breakdown: " + ", ".join(
                f"{k}={by[k] * 1000:.1f}ms/n={cnt[k]}" for k in IMAGE_SUB if cnt[k]))
        workers = [(w, p) for w, t, p in ph if t != thread and p["phase"] == "image-decode" and w[0] >= s and w[0] <= e]
        if workers:
            wsum = sum(w[1] - w[0] for w, _ in workers)
            span = max(w[1] for w, _ in workers) - min(w[0] for w, _ in workers)
            print(f"  worker image-decode: {wsum * 1000:.1f} ms CPU in {span * 1000:.1f} ms wall, n={len(workers)}, "
                  f"threads={len({t for w, t, p in ph if t != thread and p['phase'] == 'image-decode' and s <= w[0] <= e})}")
        if per_image:
            print(f"  slowest images (pixmap-load + texture-upload):")
            for path, d in per_image.most_common(a.top):
                print(f"    {ms(d)}  {path}")

        # samples in this load, outside the timed phases, on the loading thread
        top_windows.sort()
        def in_phase(t):
            return any(ps <= t <= pe for ps, pe in top_windows)
        rest = [ev for t, th, ev in sm if th == thread and s <= t <= e and not in_phase(t)]
        if rest:
            leaf, incl = collections.Counter(), collections.Counter()
            for ev in rest:
                fs = list(frames(ev))
                if not fs:
                    continue
                leaf[fs[0]] += 1
                for f in set(fs):
                    if f.startswith("bms."):
                        incl[f] += 1
            n = len(rest)
            print(f"  objects/other CPU samples: {n}")
            print("    inclusive bms.* methods:")
            for k, c in incl.most_common(a.samples):
                print(f"      {100 * c / n:5.1f} %  {k}")
            print("    leaf methods:")
            for k, c in leaf.most_common(a.samples):
                print(f"      {100 * c / n:5.1f} %  {k}")
        grand[v.get("skinType")] += total

    print("\n=== total by skin type")
    for k, d in grand.most_common():
        print(f"  {k:24s} {ms(d)}")


if __name__ == "__main__":
    main()
