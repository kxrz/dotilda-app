// Draws the brand files served by /brand into public/brand/:
// Dotilda's four poses as SVG, in ink (for light backgrounds) and paper (for dark ones),
// the app's line icon, the mark + name lockup as PNG, and the wallpapers (desktop and mobile,
// light and dark) with their thumbnails in public/brand/thumbs/.
// Geometry follows the app (ui/DotildaMark.kt) on a 100-unit grid. Run: npm run brand
import { chromium } from 'playwright';
import { writeFileSync, mkdirSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const root = resolve(import.meta.dirname, '..');
const out = join(root, 'public/brand');
mkdirSync(out, { recursive: true });

const INK = '#141412', PAPER = '#FBFAF6', DARK = '#151513', LIGHT_INK = '#ECEAE2';

// Lens centres per pose: peering slides them down her nose, curious glances aside (not tilted).
const lenses = { neutral: [0, 0], peering: [0, 8], curious: [6, -3], pleased: [0, 0] };
// The app's smile: an arc of an ellipse 18 x 12.6 at (41, 66), from 20° to 160°.
const smile = '<path d="M58.46 74.45A9 6.3 0 0 1 41.54 74.45"/>';

// The glasses and smile are holes (a mask), so the file works on any background.
const mark = (pose, fill) => {
  const [dx, dy] = lenses[pose];
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<title>Dotilda, ${pose}</title>
<mask id="m"><rect width="100" height="100" fill="#fff"/><g fill="none" stroke="#000" stroke-width="4" stroke-linecap="round"><g transform="translate(${dx} ${dy})"><circle cx="37.5" cy="60" r="10"/><circle cx="62.5" cy="60" r="10"/><path d="M47.5 60h5"/></g>${pose === 'pleased' ? smile : ''}</g></mask>
<g fill="${fill}" mask="url(#m)"><circle cx="50" cy="14" r="13"/><circle cx="50" cy="60" r="36"/></g>
</svg>
`;
};

const files = [];
for (const pose of Object.keys(lenses)) {
  for (const [tone, fill] of [['ink', INK], ['paper', PAPER]]) {
    const name = `dotilda-${pose}-${tone}.svg`;
    writeFileSync(join(out, name), mark(pose, fill));
    files.push(name);
  }
}

// The launcher icon, as in the app (res/drawable/ic_launcher_foreground.xml): a line drawing.
writeFileSync(join(out, 'dotilda-icon.svg'), `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="432" height="432">
<title>Dotilda app icon</title>
<rect width="108" height="108" fill="#fff"/>
<g fill="none" stroke="#000"><circle cx="54" cy="61" r="17" stroke-width="3"/><circle cx="54" cy="38" r="5.5" stroke-width="3"/>
<g stroke-width="2.5"><circle cx="47.5" cy="61" r="4.5"/><circle cx="60.5" cy="61" r="4.5"/><path d="M52 61h4"/></g></g>
</svg>
`);
files.push('dotilda-icon.svg');

// Lockup: the mark and the name in Instrument Serif, drawn by the browser into PNG.
const font = pathToFileURL(join(root, 'public/fonts/instrument-serif-latin-400-normal.woff2')).href;
const lockup = (fill, bg) => `<!doctype html><meta charset="utf-8"><style>
@font-face{font-family:"Instrument Serif";src:url(${font})}
html,body{margin:0;background:${bg}}
.l{display:inline-flex;align-items:center;gap:56px;padding:120px;font:400 260px/1 "Instrument Serif";color:${fill};letter-spacing:-.01em}
.l img{width:240px;height:240px}
</style><div class="l"><img src="data:image/svg+xml,${encodeURIComponent(mark('neutral', fill))}">Dotilda</div>`;

const browser = await chromium.launch();
const page = await browser.newPage({ deviceScaleFactor: 1 });
for (const [name, fill, bg] of [['light', INK, PAPER], ['dark', LIGHT_INK, DARK]]) {
  await page.setContent(lockup(fill, bg));
  await page.evaluate(() => document.fonts.ready);
  await page.locator('.l').screenshot({ path: join(out, `dotilda-lockup-${name}.png`) });
  files.push(`dotilda-lockup-${name}.png`);
}

// Wallpapers. Every size is a share of the short side (u), so a thumbnail is the same drawing.
// Moon: an almost empty page; Dotilda, small, glances up at one warm dot.
// Year: a whole year as a sky of fine dots (12 months, real month lengths), the last day warm.
// On phones the drawing keeps clear of the clock (top) and the dock (bottom).
const tones = {
  light: { bg: PAPER, ink: INK, ghost: '#C9C5BA', warm: '#D1613E' },
  dark: { bg: DARK, ink: LIGHT_INK, ghost: '#4A4842', warm: '#E2795A' },
};
const dotilda = (pose, fill, cx, cy, size) =>
  `<image href="data:image/svg+xml,${encodeURIComponent(mark(pose, fill))}" x="${cx - size / 2}" y="${cy - size / 2}" width="${size}" height="${size}"/>`;
const circle = (x, y, r, fill) => `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${r.toFixed(2)}" fill="${fill}"/>`;

function moon(W, H, t) {
  const u = Math.min(W, H), wide = W > H;
  const [mx, my, ms, dx, dy, dr] = wide
    ? [0.3 * W, 0.68 * H, 0.11 * u, 0.7 * W, 0.3 * H, 0.012 * u]
    : [0.36 * W, 0.68 * H, 0.2 * u, 0.66 * W, 0.34 * H, 0.022 * u];
  return dotilda('curious', t.ink, mx, my, ms) + circle(dx, dy, dr, t.warm);
}

function year(W, H, t) {
  const u = Math.min(W, H), wide = W > H;
  const days = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  let seed = 7;
  const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  // Wide: a row per month. Tall: a column per month.
  const c = wide ? 0.016 * W : 0.034 * W;
  const cols = wide ? 31 : 12, rows = wide ? 12 : 31;
  const x0 = (W - cols * c) / 2, y0 = (wide ? 0.42 * H : 0.48 * H) - (rows * c) / 2;
  let dots = '';
  days.forEach((n, m) => {
    for (let d = 0; d < n; d++) {
      const [col, row] = wide ? [d, m] : [m, d];
      const x = x0 + (col + 0.5) * c, y = y0 + (row + 0.5) * c;
      if (m === 11 && d === n - 1) dots += circle(x, y, 0.2 * c, t.warm);
      else if (rnd() < 0.62) dots += circle(x, y, 0.15 * c, t.ink);
      else dots += circle(x, y, 0.06 * c, t.ghost);
    }
  });
  const ms = wide ? 0.07 * u : 0.11 * u;
  const my = y0 + rows * c + (wide ? 0.13 * H : 0.06 * H);
  return dots + dotilda('pleased', t.ink, W / 2, my, ms);
}

const wallpaper = (draw, W, H, t) =>
  `<!doctype html><meta charset="utf-8"><style>html,body{margin:0;background:${t.bg}}svg{display:block}</style>` +
  `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}"><rect width="${W}" height="${H}" fill="${t.bg}"/>${draw(W, H, t)}</svg>`;

mkdirSync(join(out, 'thumbs'), { recursive: true });
for (const [design, draw] of [['moon', moon], ['year', year]]) {
  for (const [device, W, H] of [['desktop', 3840, 2160], ['mobile', 1290, 2796]]) {
    for (const tone of ['light', 'dark']) {
      const name = `dotilda-wallpaper-${design}-${device}-${tone}.png`;
      for (const [scale, dir] of [[1, out], [W > H ? 640 / W : 300 / W, join(out, 'thumbs')]]) {
        const w = Math.round(W * scale), h = Math.round(H * scale);
        await page.setViewportSize({ width: w, height: h });
        await page.setContent(wallpaper(draw, w, h, tones[tone]));
        await page.waitForFunction(() => [...document.images].every(i => i.complete));
        await page.screenshot({ path: join(dir, name) });
      }
      files.push(name, `thumbs/${name}`);
    }
  }
}
await browser.close();
console.log(files.map(f => `public/brand/${f}`).join('\n'));
