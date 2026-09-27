"""Dump.java çıktısında bütünlük kontrolleri: geçersiz/ters saat, boş program, tekrar eden satır, kesik (sadece ilk saat) şüphesi."""
import re, sys, collections
scheds, cur = [], None
for l in open(sys.argv[1], encoding="utf-8"):
    l = l.rstrip("\n")
    if l.startswith("## "):
        src, fac, dep, yr, url = [x.strip() for x in l[3:].split(" | ")]
        cur = dict(src=src, fac=fac, dep=dep, yr=yr, url=url, e=[]); scheds.append(cur)
    else:
        cur["e"].append((l.split("\t") + [""] * 9)[:9])
def m(t):
    x = re.match(r"^(\d{2}):(\d{2})$", t); return int(x[1]) * 60 + int(x[2]) if x else None
issues = collections.defaultdict(list)
for s in scheds:
    name = f"{s['src']}: {s['fac']} / {s['dep']} / {s['yr']}"
    E = s["e"]
    if not E: issues["boş program"].append(name); continue
    for e in E:
        a, b = m(e[1]), m(e[2])
        if a is None or b is None: issues["geçersiz saat"].append(f"{name}: {e[1]}-{e[2]} {e[4]}"); continue
        if b <= a: issues["bitiş <= başlangıç"].append(f"{name}: {e[0]} {e[1]}-{e[2]} {e[4]}")
        if not e[4].strip(): issues["ders adı boş"].append(f"{name}: {e[0]} {e[1]}")
    for k, v in collections.Counter(tuple(x[:7]) for x in E).items():
        if v > 1: issues["birebir tekrar"].append(f"{name}: {k[0]} {k[1]} {k[4]}")
    durs = [m(e[2]) - m(e[1]) for e in E if m(e[1]) is not None and m(e[2]) is not None]
    if len(durs) >= 6 and all(d <= 50 for d in durs):
        keys = {(e[0], m(e[1]), e[3], e[4], e[8]) for e in E}
        cont = sum(1 for e in E if (e[0], m(e[2]) + 10, e[3], e[4], e[8]) in keys or (e[0], m(e[1]) - 60, e[3], e[4], e[8]) in keys)
        if cont / len(E) < 0.5: issues["kesik şüphesi (tüm dersler 50 dk, ardışık saat yok)"].append(name)
print("program:", len(scheds), "satır:", sum(len(s["e"]) for s in scheds))
for k, v in issues.items():
    print(f"\n### {k}: {len(v)}")
    for x in v: print("  -", x)
