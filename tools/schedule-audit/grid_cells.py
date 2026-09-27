"""Reads merged-cell timetables rendered from spreadsheets (e.g. via pdfcrop.swift at scale 2).

Row boundaries come from the time column, cell boundaries from the horizontal borders drawn inside
each course column, and cell text from pdflines.swift output. Used for the EEM XLSX schedule.
Config (JSON): {"lines": "...tsv", "pages": {"1": {"image": "...", "time_x": [a,b], "times": [...],
"columns": [[a,b,label,mid_or_null], ...]}, ...}}
"""
import json, re, sys
from PIL import Image

def dark_fraction(px, y, a, b):
    xs = range(a + 4, b - 4, 2)
    return sum(1 for x in xs if px[x, y] < 120) / max(1, len(xs))

def borders(px, a, b, y0, y1, threshold=0.85):
    ys = [y for y in range(y0, y1) if dark_fraction(px, y, a, b) >= threshold]
    groups = []
    for y in ys:
        if groups and y - groups[-1][-1] <= 2: groups[-1].append(y)
        else: groups.append([y])
    return [(g[0] + g[-1]) / 2 for g in groups]

def vertical_line(px, x, y0, y1, window=12):
    """True when a vertical border runs through the row near x (split cells rarely align exactly)."""
    ys = range(int(y0) + 3, int(y1) - 3)
    for cx in range(x - window, x + window + 1):
        if sum(1 for y in ys if min(px[cx + d, y] for d in (-1, 0, 1)) < 120) / max(1, len(ys)) > 0.8:
            return True
    return False

def main(config_path):
    cfg = json.load(open(config_path))
    lines = [l.rstrip("\n").split("\t") for l in open(cfg["lines"], encoding="utf-8")]
    for page, pc in cfg["pages"].items():
        im = Image.open(pc["image"]).convert("L"); px = im.load()
        # Pages without their own time column borrow the row borders of the page that has one.
        tim = Image.open(pc.get("time_image", pc["image"])).convert("L")
        ta, tb = pc["time_x"]
        if "row_labels" in pc:
            # Rows from the time labels themselves: robust when the time column's borders are faint.
            pattern = re.compile(pc["row_labels"])
            labels = sorted(((int(l[2]) + int(l[4])) / 2, pattern.search(l[5]).group(0))
                            for l in lines if l[0] == pc.get("label_page", page)
                            and int(l[1]) < tb and pattern.search(l[5]))
            centers = [c for c, _ in labels]
            edges = [centers[0] - (centers[1] - centers[0]) / 2]
            edges += [(a + b) / 2 for a, b in zip(centers, centers[1:])]
            edges.append(centers[-1] + (centers[-1] - centers[-2]) / 2)
            rows = list(zip(edges, edges[1:]))
            times = [t for _, t in labels]
        else:
            rows_b = borders(tim.load(), ta, tb, pc.get("y_min", 405), pc.get("y_max", tim.size[1] - 1))
            rows = list(zip(rows_b, rows_b[1:]))
            times = pc["times"]
        if len(rows) != len(times):
            print(f"# page {page}: {len(rows)} rows detected, {len(times)} times given", file=sys.stderr)
        page_lines = [l for l in lines if l[0] == page]
        for a, b, label, mid in pc["columns"]:
            # Per row: one full segment, or two halves when the middle border is drawn.
            segs = []
            for (y0, y1) in rows:
                if mid and vertical_line(px, mid, y0, y1): segs.append([(a, mid, "L"), (mid, b, "R")])
                else: segs.append([(a, b, "")])
            for side in ("", "L", "R"):
                start = None
                for i, (y0, y1) in enumerate(rows):
                    seg = next((s for s in segs[i] if s[2] == side), None)
                    if seg is None:
                        if start is not None: yield_cell(page, label, side, start, i - 1, rows, times, page_lines, prev); start = None
                        continue
                    if start is None: start, prev = i, seg
                    nxt = i + 1 < len(rows) and next((s for s in segs[i + 1] if s[2] == side), None)
                    # Look for a border anywhere between the two row centres, so unlabeled rows in
                    # between (e.g. a lunch break) still separate the cells.
                    c0 = (rows[i][0] + rows[i][1]) / 2
                    c1 = (rows[i + 1][0] + rows[i + 1][1]) / 2 if i + 1 < len(rows) else c0
                    closed = not nxt or any(dark_fraction(px, int(y), seg[0], seg[1]) >= 0.85 for y in range(int(c0) + 5, int(c1) - 4))
                    if closed:
                        yield_cell(page, label, side, start, i, rows, times, page_lines, seg); start = None

def yield_cell(page, label, side, i0, i1, rows, times, page_lines, seg):
    a, b, _ = seg
    top, bottom = rows[i0][0], rows[i1][1]
    text = [l[5] for l in page_lines
            if a < (int(l[1]) + int(l[3])) / 2 < b and top < (int(l[2]) + int(l[4])) / 2 < bottom]
    text = [re.sub(r"^\d{1,2}:\d{2}\s+", "", t) for t in text]
    text = [t for t in text if not re.fullmatch(r"\d{1,2}:\d{2}", t)]
    if not text: return
    start = times[i0] if i0 < len(times) else "?"
    end_start = times[i1] if i1 < len(times) else "?"
    print(f"{label}{side}\t{start}\t{end_start}\t" + " | ".join(text))

if __name__ == "__main__":
    main(sys.argv[1])
