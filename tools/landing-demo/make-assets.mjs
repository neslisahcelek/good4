// Renders the demo images (community logos/covers, event banners, Kampüs Dolabı item
// illustrations) to tools/landing-demo/assets with headless Chrome. Run once after editing:
//   node tools/landing-demo/make-assets.mjs
import { execFileSync } from "node:child_process";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { COMMUNITIES, EVENTS, LISTINGS } from "./demo-data.mjs";

const CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const OUT = join(dirname(fileURLToPath(import.meta.url)), "assets");
const TMP = mkdtempSync(join(tmpdir(), "good4-demo-assets-"));
const FONT = `"Avenir Next", "Helvetica Neue", sans-serif`;

// Simple flat glyphs (24-unit viewBox), drawn in currentColor.
const GLYPHS = {
  camera: `<rect x="2.5" y="6.5" width="19" height="13" rx="3.5" fill="none" stroke="currentColor" stroke-width="1.8"/>
    <path d="M8 6.5 9.6 4h4.8L16 6.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
    <circle cx="12" cy="13" r="3.6" fill="none" stroke="currentColor" stroke-width="1.8"/>`,
  mountain: `<path d="M2.5 19.5 9 8l3.6 6.2L15 11l6.5 8.5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
    <path d="m7.4 10.8 1.6 1.4 1.5-1.3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>`,
  code: `<path d="m8.5 7-5 5 5 5M15.5 7l5 5-5 5M13.5 5l-3 14" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/>`,
  rocket: `<path d="M12 2.8c3.3 2 5 5.4 4.6 10.2l-2 2.5h-5.2l-2-2.5C7 8.2 8.7 4.8 12 2.8z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
    <circle cx="12" cy="9" r="1.8" fill="currentColor"/>
    <path d="M9.6 15.5 8 20l2.4-1.4M14.4 15.5 16 20l-2.4-1.4M7.6 12.4 4.5 15l3.4.3M16.4 12.4l3.1 2.6-3.4.3" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>`,
  heart: `<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
    <path d="M8.6 11.6h2.2l1-1.8 1.6 3.4 1-1.6h1.6" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>`,
};

// Item illustrations for listings (240-unit viewBox), flat with a soft shadow.
const ITEMS = {
  books: `<ellipse cx="120" cy="206" rx="78" ry="9" fill="#00000014"/>
    <rect x="52" y="150" width="136" height="30" rx="5" fill="#2f6f9f"/><rect x="52" y="150" width="14" height="30" fill="#24577d"/>
    <rect x="62" y="120" width="120" height="30" rx="5" fill="#e0a43a"/><rect x="62" y="120" width="12" height="30" fill="#c08626"/>
    <rect x="46" y="180" width="148" height="26" rx="5" fill="#0d7a52"/><rect x="46" y="180" width="14" height="26" fill="#0a5f40"/>
    <rect x="80" y="130" width="70" height="4" rx="2" fill="#fff8"/><rect x="80" y="161" width="80" height="4" rx="2" fill="#fff8"/>
    <rect x="78" y="190" width="90" height="4" rx="2" fill="#fff8"/>
    <path d="M86 120V52a6 6 0 0 1 6-6h62a6 6 0 0 1 6 6v68z" fill="#f7f3ea" stroke="#d9d2c3" stroke-width="3"/>
    <path d="M104 70h38M104 84h38M104 98h26" stroke="#b9b0a0" stroke-width="5" stroke-linecap="round"/>`,
  lamp: `<ellipse cx="120" cy="208" rx="62" ry="9" fill="#00000014"/>
    <rect x="72" y="190" width="96" height="16" rx="8" fill="#3b4048"/>
    <path d="M118 192 96 112l40-46" fill="none" stroke="#4a5059" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="96" cy="112" r="9" fill="#2c3036"/><circle cx="136" cy="66" r="8" fill="#2c3036"/>
    <path d="M128 50 182 74l-26 34-40-38z" fill="#f2c94c"/><path d="m156 108 26-34" stroke="#d9ad2b" stroke-width="6" stroke-linecap="round"/>
    <path d="M168 104 196 132" stroke="#f2c94c88" stroke-width="10" stroke-linecap="round"/><path d="M152 116l14 34" stroke="#f2c94c66" stroke-width="10" stroke-linecap="round"/>`,
  fridge: `<ellipse cx="120" cy="210" rx="66" ry="9" fill="#00000014"/>
    <rect x="62" y="40" width="116" height="168" rx="14" fill="#e9eef2" stroke="#c7d0d8" stroke-width="3"/>
    <path d="M62 96h116" stroke="#c7d0d8" stroke-width="3"/><rect x="152" y="58" width="8" height="26" rx="4" fill="#9aa6b2"/>
    <rect x="152" y="110" width="8" height="40" rx="4" fill="#9aa6b2"/><rect x="74" y="196" width="18" height="10" rx="3" fill="#9aa6b2"/>
    <rect x="148" y="196" width="18" height="10" rx="3" fill="#9aa6b2"/><circle cx="84" cy="70" r="7" fill="#7cc4a3"/>`,
  helmet: `<ellipse cx="120" cy="196" rx="76" ry="9" fill="#00000014"/>
    <path d="M44 150c0-52 34-86 80-86s74 34 74 82c0 8-6 12-14 12H58c-8 0-14-2-14-8z" fill="#e2574c"/>
    <path d="M86 70c-8 22-8 50-4 88M120 64c-2 30 0 62 4 94M154 72c6 22 8 50 6 86" stroke="#b9403a" stroke-width="7" fill="none" stroke-linecap="round"/>
    <path d="M44 150h154" stroke="#2b2f36" stroke-width="10" stroke-linecap="round"/>
    <path d="M66 160c8 18 22 26 40 26" stroke="#2b2f36" stroke-width="5" fill="none" stroke-linecap="round"/>`,
  guitar: `<ellipse cx="120" cy="214" rx="56" ry="8" fill="#00000014"/>
    <rect x="113" y="20" width="14" height="110" rx="4" fill="#6b4a2b"/><rect x="108" y="10" width="24" height="24" rx="6" fill="#4d331d"/>
    <path d="M120 106c-26 0-38 16-34 34 2 10-12 16-12 36 0 22 20 36 46 36s46-14 46-36c0-20-14-26-12-36 4-18-8-34-34-34z" fill="#d79a52"/>
    <circle cx="120" cy="160" r="15" fill="#3a2a1a"/><rect x="104" y="190" width="32" height="7" rx="3" fill="#4d331d"/>
    <path d="M116 30v158M120 30v158M124 30v158" stroke="#f3e6cf" stroke-width="1.3"/>`,
  jacket: `<ellipse cx="120" cy="214" rx="74" ry="8" fill="#00000014"/>
    <path d="M92 38h56l40 26 14 58-24 8-6-28v104H68V102l-6 28-24-8 14-58z" fill="#2f5d8a"/>
    <path d="M92 38c4 16 14 24 28 24s24-8 28-24" fill="#27507a"/><path d="M120 62v144" stroke="#d8e2ec" stroke-width="4"/>
    <path d="M78 118h28M134 118h28" stroke="#27507a" stroke-width="6" stroke-linecap="round"/>
    <path d="M92 38c-6 10-6 20 0 30M148 38c6 10 6 20 0 30" stroke="#f1f4f7" stroke-width="10" stroke-linecap="round" fill="none"/>`,
  notes: `<ellipse cx="120" cy="206" rx="70" ry="8" fill="#00000014"/>
    <rect x="66" y="44" width="112" height="152" rx="8" fill="#ffffff" stroke="#d9dde3" stroke-width="3" transform="rotate(-6 122 120)"/>
    <rect x="60" y="40" width="112" height="152" rx="8" fill="#fffdf6" stroke="#d9dde3" stroke-width="3"/>
    <path d="M78 70h76M78 88h76M78 106h60M78 124h76M78 142h48" stroke="#9fb4c8" stroke-width="5" stroke-linecap="round"/>
    <path d="M70 52v130" stroke="#e98f8f" stroke-width="3"/>
    <path d="m150 150 28-28 10 10-28 28-14 4z" fill="#f2c94c"/><path d="m178 122 10 10" stroke="#3b4048" stroke-width="4"/>`,
};

function page(width, height, body) {
  return `<!doctype html><html><head><meta charset="utf-8"><style>
    html,body{margin:0;width:${width}px;height:${height}px;overflow:hidden}
    body{font-family:${FONT};-webkit-font-smoothing:antialiased}
  </style></head><body>${body}</body></html>`;
}

function glyph(name, size, color) {
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" style="color:${color}">${GLYPHS[name]}</svg>`;
}

function logo(c) {
  return page(400, 400, `<div style="width:400px;height:400px;display:grid;place-items:center;
    background:linear-gradient(145deg,${c.colors[0]},${c.colors[1]})">${glyph(c.glyph, 230, "#fff")}</div>`);
}

function cover(c) {
  return page(1200, 500, `<div style="position:relative;width:1200px;height:500px;overflow:hidden;
    background:linear-gradient(120deg,${c.colors[0]},${c.colors[1]})">
    <div style="position:absolute;right:-60px;top:-80px;opacity:.16">${glyph(c.glyph, 640, "#fff")}</div>
    <div style="position:absolute;left:-120px;bottom:-220px;width:520px;height:520px;border-radius:50%;background:#ffffff14"></div>
  </div>`);
}

function banner(e, c) {
  return page(1200, 500, `<div style="position:relative;width:1200px;height:500px;overflow:hidden;color:#fff;
    background:linear-gradient(115deg,${c.colors[0]} 0%,${c.colors[1]} 100%)">
    <div style="position:absolute;right:-40px;top:50%;transform:translateY(-50%);opacity:.22">${glyph(c.glyph, 520, "#fff")}</div>
    <div style="position:absolute;left:72px;top:64px;right:360px">
      <div style="display:inline-block;padding:10px 22px;border-radius:999px;background:#ffffff26;font-size:26px;font-weight:600;letter-spacing:.02em">${e.chip}</div>
      <div style="margin-top:28px;font-size:${e.title.length > 34 ? 62 : 70}px;line-height:1.04;font-weight:700;letter-spacing:-.02em">${e.title}</div>
      <div style="margin-top:28px;font-size:30px;font-weight:500;opacity:.92">${e.when} · ${e.place}</div>
    </div>
    <div style="position:absolute;left:72px;bottom:52px;font-size:24px;font-weight:600;opacity:.85">${c.name}</div>
  </div>`);
}

function item(l) {
  return page(1000, 1000, `<div style="width:1000px;height:1000px;display:grid;place-items:center;
    background:radial-gradient(circle at 50% 40%,#ffffff 0%,${l.tint} 70%)">
    <svg viewBox="0 0 240 240" width="760" height="760">${ITEMS[l.art]}</svg></div>`);
}

function render(name, width, height, html) {
  const file = join(TMP, `${name}.html`);
  writeFileSync(file, html);
  execFileSync(CHROME, [
    "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
    `--window-size=${width},${height}`, `--screenshot=${join(OUT, `${name}.png`)}`, `file://${file}`,
  ], { stdio: "ignore" });
  console.log(`assets/${name}.png`);
}

mkdirSync(OUT, { recursive: true });
for (const c of COMMUNITIES) {
  render(`logo-${c.id}`, 400, 400, logo(c));
  render(`cover-${c.id}`, 1200, 500, cover(c));
}
for (const e of EVENTS) render(`event-${e.id}`, 1200, 500, banner(e, COMMUNITIES.find((c) => c.id === e.community)));
for (const l of LISTINGS) render(`item-${l.id}`, 1000, 1000, item(l));
