// Seven slides that explain Dotilda, one idea each, in the site's identity: for a talk (16:9,
// 1920x1080) and for LinkedIn (4:5, 1080x1350), as PNG and as one PDF per format (LinkedIn posts a
// PDF as a carousel). Written to public/brand/slides/, thumbnails in public/brand/slides/thumbs/.
// English only. Run: npm run slides
import { chromium } from 'playwright';
import { readFileSync, mkdirSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { mark, INK } from './mark.mjs';

const root = resolve(import.meta.dirname, '..');
const out = join(root, 'public/brand/slides');
mkdirSync(join(out, 'thumbs'), { recursive: true });

const b64 = path => readFileSync(join(root, path)).toString('base64');
const font = (family, file, weight = 400) =>
  `@font-face{font-family:"${family}";font-weight:${weight};src:url(data:font/woff2;base64,${b64(`public/fonts/${file}`)})}`;
const fonts = font('Instrument Serif', 'instrument-serif-latin-400-normal.woff2')
  + font('Geist', 'geist-latin-400-normal.woff2') + font('Geist', 'geist-latin-500-normal.woff2', 500)
  + font('Space Mono', 'space-mono-latin-400-normal.woff2');
const img = (pose, size) => `<img class="mk" style="width:${size}" src="data:image/svg+xml,${encodeURIComponent(mark(pose, INK))}" alt="">`;
const screen = name => `data:image/png;base64,${b64(`public/app/${name}.png`)}`;

// The inner weather as the app draws it: a moon, full (sunny) to new (stormy).
const moon = (f, r = 9) => {
  const rx = Math.abs(1 - 2 * f) * r;
  const lit = f === 0 ? '' : f === 1 ? `<circle cx="11" cy="11" r="${r}" fill="${INK}"/>`
    : `<path fill="${INK}" d="M11 ${11 - r}A${r} ${r} 0 0 1 11 ${11 + r}A${rx} ${r} 0 0 ${f > .5 ? 1 : 0} 11 ${11 - r}z"/>`;
  return `<svg viewBox="0 0 22 22"><circle cx="11" cy="11" r="${r}" fill="none" stroke="${INK}" stroke-width="1.6"/>${lit}</svg>`;
};
const dot = '<svg viewBox="0 0 22 22"><circle cx="11" cy="11" r="7" fill="#141412"/></svg>';

// A few months of dots: answered days in ink, quiet days small and grey, today warm.
const months = () => {
  let seed = 9;
  const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  const rows = [['Jul', 31], ['Aug', 31], ['Sep', 30], ['Oct', 24]];
  return `<div class="months">${rows.map(([m, n], r) => `<div class="mrow"><span class="ml">${m}</span><svg viewBox="0 0 310 12">${
    Array.from({ length: n }, (_, d) => {
      const x = d * 10 + 5, last = r === rows.length - 1 && d === n - 1;
      if (last) return `<circle cx="${x}" cy="6" r="3.6" fill="#D1613E"/>`;
      return rnd() < .24 ? `<circle cx="${x}" cy="6" r="1.2" fill="#C9C5BA"/>` : `<circle cx="${x}" cy="6" r="3" fill="${INK}"/>`;
    }).join('')}</svg></div>`).join('')}</div>`;
};

const slides = [
  { cover: true, label: 'Dotilda · by readme.club', title: 'A journal born on e‑ink. For any Android.',
    lede: 'One small question a day. Your answer becomes a dot in your year.',
    visual: `<div class="ask">${img('peering', '42%')}<div class="bubble">What made you smile today?</div></div>` },
  { label: 'Principle 01', title: 'One question a day.',
    lede: 'At the hour you choose, Dotilda asks one small question. Under two minutes, then the rest of the day is yours again.',
    visual: `<div class="ask">${img('peering', '34%')}<div class="bubble">Tea or coffee kind of day?</div><div class="chips"><span>Tea</span><span>Coffee</span></div></div>` },
  { label: 'Principle 02', title: 'A line is plenty.',
    lede: 'Write a line, pick a word, choose tea or coffee, or just tell her the weather inside. Every answer is a dot.',
    visual: `<ul class="kinds">
      <li>${dot}<span><b>A line</b>The smell of warm bread on the way home.</span></li>
      <li>${dot}<span><b>A word</b>Golden</span></li>
      <li>${dot}<span><b>This or that</b>Tea</span></li>
      <li>${moon(.75)}<span><b>The weather inside</b>Mostly fine, a cloud or two.</span></li></ul>` },
  { label: 'Principle 03', title: 'Miss a day? Nothing breaks.',
    lede: 'No streak, no red, no guilt. An empty day is a tiny grey dot, a breath in the grid.',
    visual: `${months()}<p class="quote">“There you are. Your sky kept your place.”</p>` },
  { label: 'Principle 04', title: 'Yours alone.',
    lede: 'Nothing about you leaves the phone. Even our usage numbers are GitHub’s public download counters.',
    visual: `<ul class="zeros"><li><b>0</b>accounts</li><li><b>0</b>trackers</li><li><b>0</b>ads</li></ul>` },
  { label: 'Principle 05', title: 'Calm on any screen.',
    lede: 'Born on e‑ink: pure black on white, large type, no scrolling, no animation. Restful on any Android 11+ phone, tablet or e‑reader.',
    visual: `<div class="phone"><img src="${screen('today')}" alt=""></div>` },
  { label: 'Principle 06', title: 'Free, for good.',
    lede: 'No purchase, no subscription, no ads. Open source under Apache 2.0. Made by LesWWW for readme.club.',
    visual: `<div class="end">${img('pleased', '46%')}<p class="url">Dotilda<span class="wd"></span>com</p></div>` },
];

const css = `${fonts}
*{box-sizing:border-box;margin:0;padding:0}
html,body{background:#FBFAF6;color:${INK}}
.s{width:100vw;height:100vh;padding:6vmin 7vmin;display:grid;grid-template-rows:auto 1fr auto;gap:4vmin;font-family:Geist,sans-serif;overflow:hidden;break-after:page}
.top,.foot{display:flex;justify-content:space-between;align-items:center;font:400 1.7vmin/1 "Space Mono",monospace;letter-spacing:.1em;text-transform:uppercase;color:#55534D}
.brand{display:flex;align-items:center;gap:1.4vmin;font:400 3.4vmin/1 "Instrument Serif",serif;letter-spacing:0;text-transform:none;color:${INK}}
.brand .mk{width:3.6vmin!important}
.body{display:grid;grid-template-columns:1.1fr .9fr;gap:6vmin;align-items:center;min-height:0}
.text{display:grid;gap:3vmin;align-content:center}
.label{font:400 1.8vmin/1 "Space Mono",monospace;letter-spacing:.1em;text-transform:uppercase;color:#55534D}
h1{font:400 10vmin/1 "Instrument Serif",serif;letter-spacing:-.015em;text-wrap:balance}
.cover h1{font-size:11.5vmin}
.lede{font-size:3vmin;line-height:1.4;color:#55534D;max-width:30em;text-wrap:pretty}
.visual{display:grid;place-items:center;min-height:0;height:100%}
.ask{display:grid;justify-items:center;gap:3vmin;width:100%}
.bubble{position:relative;border:.35vmin solid ${INK};border-radius:2.6vmin;padding:2.6vmin 3.4vmin;font:400 5.6vmin/1.05 "Instrument Serif",serif;text-align:center;max-width:100%}
.bubble::before{content:"";position:absolute;left:50%;top:-1.5vmin;width:2.6vmin;height:2.6vmin;background:#FBFAF6;border-left:.35vmin solid ${INK};border-top:.35vmin solid ${INK};transform:translateX(-50%) rotate(45deg)}
.chips{display:flex;gap:2vmin}.chips span{border:.3vmin solid ${INK};border-radius:1.2vmin;padding:1.6vmin 4vmin;font-size:3vmin}
.chips span:first-child{background:${INK};color:#FBFAF6}
.kinds{list-style:none;display:grid;gap:3.2vmin;width:100%}
.kinds li{display:grid;grid-template-columns:5vmin 1fr;gap:2.4vmin;align-items:start}
.kinds svg{width:5vmin;height:5vmin}
.kinds b{display:block;font:400 1.7vmin/1.4 "Space Mono",monospace;letter-spacing:.08em;text-transform:uppercase;color:#55534D}
.kinds span{font:400 4.4vmin/1.1 "Instrument Serif",serif}
.months{display:grid;gap:2.2vmin;width:100%}
.mrow{display:grid;grid-template-columns:7vmin 1fr;align-items:center}
.ml{font:400 1.8vmin/1 "Space Mono",monospace;letter-spacing:.08em;text-transform:uppercase;color:#55534D}
.mrow svg{width:100%;height:auto}
.quote{font:italic 400 4vmin/1.2 "Instrument Serif",serif;margin-top:4vmin;justify-self:start}
.visual:has(.months){display:grid;place-items:center start;align-content:center}
.zeros{list-style:none;display:grid;gap:1vmin}
.zeros li{display:flex;align-items:baseline;gap:3vmin;font:400 6vmin/1 "Instrument Serif",serif}
.zeros b{font-weight:400;font-size:17vmin;line-height:.9;width:9vmin}
.phone{height:100%;max-height:62vmin;aspect-ratio:480/800;background:#1a1a19;border-radius:4.5vmin;padding:3.2vmin 1.8vmin 5.5vmin;display:grid;grid-template-rows:minmax(0,1fr);min-height:0}
.phone img{width:100%;height:100%;min-height:0;object-fit:cover;border-radius:1vmin;background:#fff}
.end{display:grid;justify-items:center;gap:4vmin;width:100%}
.url{font:400 7.4vmin/1 "Instrument Serif",serif;display:flex;align-items:baseline}
.wd{display:inline-block;width:1.3vmin;height:1.3vmin;border-radius:50%;background:#D1613E;margin:0 .3vmin 0 .5vmin}
/* Portrait (LinkedIn): the idea on top, its picture below. */
@media (orientation:portrait){
  .s{padding:7vmin 7vmin 6vmin}
  .body{grid-template-columns:1fr;grid-template-rows:auto minmax(0,1fr);gap:5vmin;align-items:stretch}
  .text{align-content:start}
  h1{font-size:11vmin}.cover h1{font-size:12.5vmin}
  .lede{font-size:3.6vmin}
  .phone{max-height:none;height:100%}
  .visual{overflow:hidden}
  .ask .mk{width:24%!important}.end .mk{width:30%!important}
}`;

const slide = (sl, i) => `<section class="s${sl.cover ? ' cover' : ''}">
  <div class="top"><span class="brand">${img('neutral', '3.6vmin')}Dotilda</span><span>${String(i + 1).padStart(2, '0')} / ${String(slides.length).padStart(2, '0')}</span></div>
  <div class="body"><div class="text"><p class="label">${sl.label}</p><h1>${sl.title}</h1><p class="lede">${sl.lede}</p></div><div class="visual">${sl.visual}</div></div>
  <div class="foot"><span>dotilda.com</span><span>Free · open source · public beta</span></div>
</section>`;

const formats = [['16x9', 1920, 1080, 640], ['linkedin', 1080, 1350, 300]];
const browser = await chromium.launch();
for (const [name, W, H, thumbW] of formats) {
  const page = await browser.newPage({ viewport: { width: W, height: H } });
  for (let i = 0; i < slides.length; i++) {
    await page.setContent(`<!doctype html><meta charset="utf-8"><style>${css}</style>${slide(slides[i], i)}`);
    await page.evaluate(() => document.fonts.ready);
    await page.waitForFunction(() => [...document.images].every(im => im.complete));
    const file = `dotilda-slide-${String(i + 1).padStart(2, '0')}-${name}.png`;
    await page.screenshot({ path: join(out, file) });
    const th = await browser.newPage({ viewport: { width: thumbW, height: Math.round(thumbW * H / W) } });
    await th.setContent(`<img src="data:image/png;base64,${readFileSync(join(out, file)).toString('base64')}" style="width:100%;display:block">`);
    await th.waitForFunction(() => document.images[0].complete);
    await th.screenshot({ path: join(out, 'thumbs', file) });
    await th.close();
    console.log(`public/brand/slides/${file}`);
  }
  // One PDF per format, a page per slide: LinkedIn shows a PDF as a carousel.
  await page.setContent(`<!doctype html><meta charset="utf-8"><style>${css}@page{size:${W}px ${H}px;margin:0}</style>${slides.map(slide).join('')}`);
  await page.evaluate(() => document.fonts.ready);
  await page.waitForFunction(() => [...document.images].every(im => im.complete));
  await page.emulateMedia({ media: 'screen' });
  await page.pdf({ path: join(out, `dotilda-slides-${name}.pdf`), width: `${W}px`, height: `${H}px`, printBackground: true, pageRanges: `1-${slides.length}` });
  console.log(`public/brand/slides/dotilda-slides-${name}.pdf`);
  await page.close();
}
await browser.close();
