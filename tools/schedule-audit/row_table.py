"""Reads "one row per lesson hour" timetables (GÜN | SAAT | DERS KODU | DERS ADI | ... columns).

Input: pdflines.swift output and page images rendered at the same scale. Row borders come from the
horizontal lines in a reference column, column borders from the vertical lines, and the weekday
advances whenever the hour goes back to an earlier value. Hours with identical course text are then
merged into blocks. Usage: row_table.py lines.tsv "img_pattern_with_{page}" "class:pages,..."
e.g. row_table.py lines.tsv /tmp/p{page}.jpg "1:1,2 2:3,4"
"""
import re, sys
from collections import defaultdict
from PIL import Image

CENTERED = False
DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"]
TIME = re.compile(r"^(\d{1,2})[.:](\d{2})$")

def dark(px, x, y): return px[x, y] < 130

def vertical_lines(px, w, h):
    # A column border is a continuous dark run of at least ~120 px (letters are never that tall).
    def longest_run(x):
        best = run = 0
        for y in range(h):
            run = run + 1 if dark(px, x, y) else 0
            best = max(best, run)
        return best
    xs = [x for x in range(w) if longest_run(x) >= 120]
    out = []
    for x in xs:
        if out and x - out[-1][-1] <= 3: out[-1].append(x)
        else: out.append([x])
    return [sum(g) / len(g) for g in out]

def horizontal_lines(px, a, b, h):
    # Thin borders are often rendered as two light-grey pixel rows, hence the lenient threshold.
    ys = [y for y in range(h) if sum(1 for x in range(int(a) + 3, int(b) - 3) if px[x, y] < 180) > (b - a - 6) * 0.9]
    out = []
    for y in ys:
        if out and y - out[-1][-1] <= 2: out[-1].append(y)
        else: out.append([y])
    return [sum(g) / len(g) for g in out]

def page_rows(lines, image):
    """Rows keyed by the hour labels: each label sits at the bottom of its row, so every other text
    line belongs to the first label at or below it. Row borders are not needed (they are often faint)."""
    im = Image.open(image).convert("L"); px = im.load(); w, h = im.size
    cols = vertical_lines(px, w, h)
    spans = list(zip(cols, cols[1:]))
    def col_of(l):
        cx = (int(l[1]) + int(l[3])) / 2
        return next((i for i, (a, b) in enumerate(spans) if a < cx < b), None)
    counts = defaultdict(int)
    for l in lines:
        if TIME.match(l[5].strip()) and col_of(l) is not None: counts[col_of(l)] += 1
    if not counts: return []
    ti = max(counts, key=counts.get)
    labels = sorted(((int(l[2]) + int(l[4])) / 2, l[5].strip()) for l in lines
                    if col_of(l) == ti and TIME.match(l[5].strip()))
    rows = [[""] * len(spans) for _ in labels]
    for i, (_, t) in enumerate(labels): rows[i][ti] = t
    # A day ends where the time column shows an empty separator row: two or more borders
    # between consecutive hour labels instead of one.
    # The GÜN column is one merged cell per day, so its horizontal borders are exactly the day
    # boundaries; a label's day is the number of those borders above it.
    if ti > 0:
        gun = next((i for i, (a, b) in enumerate(spans) if i < ti and any(
            "GÜN" in l[5] and a < (int(l[1]) + int(l[3])) / 2 < b for l in lines)), ti - 1)
        day_borders, previous = [], None
        for y in horizontal_lines(px, spans[gun][0], spans[gun][1], h):
            # Separator rows between days are drawn as two or three close borders: chain them into one.
            if previous is None or y - previous >= 35: day_borders.append(y)
            previous = y
        day_of = [sum(1 for y in day_borders if y < ly) for ly, _ in labels]
    else:
        # No GÜN column on this page (continuation page without its left border): a day ends
        # where the time column shows an empty separator row, i.e. two borders between labels.
        borders = horizontal_lines(px, spans[ti][0], spans[ti][1], h)
        day_of, d = [], 0
        for i in range(len(labels)):
            if i > 0 and sum(1 for y in borders if labels[i - 1][0] < y < labels[i][0]) >= 2: d += 1
            day_of.append(d)
    first = day_of[0] if day_of else 0
    breaks = [d - first for d in day_of]
    cells = defaultdict(list)
    for l in lines:
        ci = col_of(l)
        if ci is None or ci == ti: continue
        cy = (int(l[2]) + int(l[4])) / 2
        if CENTERED:
            # Hour labels vertically centred in their row: attach to the nearest one.
            ri = min(range(len(labels)), key=lambda i: abs(labels[i][0] - cy))
        else:
            ri = next((i for i, (ly, _) in enumerate(labels) if ly >= cy - 4), None)
            if ri is None or labels[ri][0] - cy > 70: continue
        cells[(ri, ci)].append((cy, int(l[1]), l[5]))
    for (ri, ci), items in cells.items():
        rows[ri][ci] = " ".join(t for _, _, t in sorted(items))
    for r, b in zip(rows, breaks): r.append(b)
    return [["SAAT" if i == ti else "" for i in range(len(spans))] + [""]] + rows

def main(lines_path, image_pattern, spec):
    lines = [l.rstrip("\n").split("\t") for l in open(lines_path, encoding="utf-8")]
    for group in spec.split():
        label, pages = group.split(":")
        day, last, hours = -1, None, []
        for p in pages.split(","):
            rows = page_rows([l for l in lines if l[0] == p], image_pattern.format(page=p))
            if not rows: continue
            ti = rows[0].index("SAAT")
            base = None
            for k, r in enumerate(rows[1:]):
                m = TIME.match(r[ti].strip())
                if not m: continue
                t = int(m.group(1)) * 60 + int(m.group(2))
                if base is None:
                    # A page continues the previous day unless its first hour goes back.
                    base = day + 1 if (last is None or t < last) else day
                day = base + r[-1]
                last = t
                rest = [c.strip() for i, c in enumerate(r[:-1]) if i > ti]
                if any("DERS KODU" in c or "DERS ADI" in c for c in rest): continue
                hours.append((DAYS[day], t, rest))
        blocks = []
        for d, t, rest in hours:
            key = " | ".join(rest)
            if not key.replace("|", "").strip(): continue
            if blocks and blocks[-1][0] == d and blocks[-1][3] == key and blocks[-1][2] == t:
                blocks[-1][2] = t + 60
            else:
                blocks.append([d, t, t + 60, key])
        for d, a, b, key in blocks:
            print(f"{label}\t{d}\t{a // 60:02d}:{a % 60:02d}\t{(b - 10) // 60:02d}:{(b - 10) % 60:02d}\t{key}")

if __name__ == "__main__":
    # Optional 4th argument "center": hour labels sit in the middle of their rows.
    CENTERED = len(sys.argv) > 4 and sys.argv[4] == "center"
    main(*sys.argv[1:4])
