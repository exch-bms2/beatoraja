#!/usr/bin/env python3
"""Summarize a JFR recording: CPU samples and allocation by luaj vs. beatoraja vs. other.

Usage: python3 scripts/jfr-hot.py recording.jfr [--thread main] [--top 30]
Needs the JDK `jfr` tool (JAVA_HOME/bin/jfr or on PATH).
"""
import argparse, collections, json, os, shutil, subprocess, sys

def jfr_tool():
    home = os.environ.get("JAVA_HOME")
    cand = os.path.join(home, "bin", "jfr") if home else None
    return cand if cand and os.path.exists(cand) else (shutil.which("jfr") or sys.exit("jfr tool not found"))

def events(path, name):
    out = subprocess.run([jfr_tool(), "print", "--json", "--stack-depth", "64", "--events", name, path],
                         check=True, capture_output=True, text=True).stdout
    return json.loads(out)["recording"]["events"]

def frames(ev):
    st = ev["values"].get("stackTrace") or {}
    for f in st.get("frames") or []:
        m = f["method"]
        yield m["type"]["name"].replace("/", "."), m["name"]

def area(cls):
    if cls.startswith("org.luaj."): return "luaj"
    if cls.startswith("bms.player.beatoraja.skin.lua."): return "beatoraja-lua-api"
    if cls.startswith("bms."): return "beatoraja"
    if cls.startswith("com.badlogic.") or cls.startswith("org.lwjgl."): return "gdx/lwjgl"
    return "jdk/other"

def thread_name(ev):
    t = ev["values"].get("sampledThread") or ev["values"].get("eventThread") or {}
    return t.get("javaName") or t.get("osName") or "?"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jfr"); ap.add_argument("--thread", default=None, help="only this Java thread name (default: busiest)")
    ap.add_argument("--top", type=int, default=30)
    a = ap.parse_args()
    cpu = events(a.jfr, "jdk.ExecutionSample")
    by_thread = collections.Counter(thread_name(e) for e in cpu)
    print("CPU samples by thread:", ", ".join(f"{t}={n}" for t, n in by_thread.most_common(6)))
    thread = a.thread or (by_thread.most_common(1)[0][0] if by_thread else None)
    cpu = [e for e in cpu if thread_name(e) == thread]
    print(f"\n== CPU on thread '{thread}': {len(cpu)} samples ==")
    top_self, top_incl, incl_area = collections.Counter(), collections.Counter(), collections.Counter()
    for e in cpu:
        fs = list(frames(e))
        if not fs: continue
        top_self[f"{fs[0][0]}.{fs[0][1]}"] += 1
        seen = set()
        for c, m in fs:
            k = f"{c}.{m}"
            if k not in seen: seen.add(k); top_incl[k] += 1
        areas = {area(c) for c, _ in fs}
        for ar in areas: incl_area[ar] += 1
    n = max(1, len(cpu))
    print("inclusive share by area (a sample counts once per area on its stack):")
    for ar, c in incl_area.most_common(): print(f"  {ar:18s} {100*c/n:6.2f} %")
    print(f"\ntop {a.top} self (leaf) methods:")
    for k, c in top_self.most_common(a.top): print(f"  {100*c/n:6.2f} %  {k}")
    print(f"\ntop {a.top} inclusive org.luaj / skin.lua methods:")
    for k, c in [x for x in top_incl.most_common() if x[0].startswith(("org.luaj.", "bms.player.beatoraja.skin.lua."))][:a.top]:
        print(f"  {100*c/n:6.2f} %  {k}")

    alloc = [e for e in events(a.jfr, "jdk.ObjectAllocationSample") if thread_name(e) == thread]
    w_cls, w_site, w_area = collections.Counter(), collections.Counter(), collections.Counter()
    for e in alloc:
        w = e["values"].get("weight", 0)
        w_cls[e["values"]["objectClass"]["name"].replace("/", ".")] += w
        fs = list(frames(e))
        if fs: w_site[f"{fs[0][0]}.{fs[0][1]}"] += w
        for ar in {area(c) for c, _ in fs}: w_area[ar] += w
    total = max(1, sum(w_cls.values()))
    print(f"\n== allocation (sampled weight) on '{thread}': {total/1e6:.1f} MB ==")
    print("inclusive by area:")
    for ar, w in w_area.most_common(): print(f"  {ar:18s} {100*w/total:6.2f} %")
    print("top classes:")
    for k, w in w_cls.most_common(15): print(f"  {100*w/total:6.2f} %  {k}")
    print("top allocation sites:")
    for k, w in w_site.most_common(20): print(f"  {100*w/total:6.2f} %  {k}")

if __name__ == "__main__":
    main()
