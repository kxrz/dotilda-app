// Draws one share image per language (1200x630) from src/i18n/<code>.json into public/og/<code>.png:
// Dotilda, the tagline in the language, the label, and a few rows of the year's dots.
// Run after changing a tagline or adding a language: npm run og
import { chromium } from 'playwright';
import { readdirSync, readFileSync, writeFileSync, mkdirSync, mkdtempSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { pathToFileURL } from 'node:url';

const root = resolve(import.meta.dirname, '..');
const fonts = pathToFileURL(join(root, 'public/fonts')).href;
const i18n = join(root, 'src/i18n');
const en = JSON.parse(readFileSync(join(i18n, 'en.json'), 'utf8'));
const locales = readdirSync(i18n).filter(f => f.endsWith('.json')).map(f => f.slice(0, -5));

const LATIN = 'U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+2000-206F,U+20AC,U+2122,U+FEFF,U+FFFD';
const EXT = 'U+0100-02BA,U+02BD-02C5,U+02C7-02CC,U+02CE-02D7,U+02DD-02FF,U+1E00-1E9F,U+1EF2-1EFF,U+2C60-2C7F,U+A720-A7FF';
const face = (family, file) => [['latin', LATIN], ['latin-ext', EXT]].map(([sub, range]) =>
  `@font-face{font-family:"${family}";src:url(${fonts}/${file.replace('SUB', sub)});unicode-range:${range}}`).join('');

const html = (s) => `<!doctype html><html lang="${s.code}"><meta charset="utf-8"><style>
${face('Instrument Serif', 'instrument-serif-SUB-400-normal.woff2')}
${face('Space Mono', 'space-mono-SUB-400-normal.woff2')}
html,body{margin:0;width:1200px;height:630px;background:#FBFAF6;color:#141412}
.og{box-sizing:border-box;width:1200px;height:630px;padding:72px 80px;display:grid;grid-template-rows:auto 1fr auto;font-family:"Instrument Serif",serif}
.brand{display:flex;align-items:center;gap:16px;font-size:44px}
h1{font-weight:400;font-size:96px;line-height:1;letter-spacing:-.01em;margin:0;align-self:center;max-width:820px;text-wrap:balance}
.foot{display:flex;justify-content:space-between;align-items:flex-end;gap:40px}
.label{font-family:"Space Mono",monospace;font-size:18px;letter-spacing:.08em;text-transform:uppercase;color:#55534D;max-width:620px;line-height:1.5}
</style><div class="og">
<div class="brand"><svg width="46" height="46" viewBox="0 0 100 100"><circle cx="50" cy="14" r="13" fill="#141412"/><circle cx="50" cy="60" r="36" fill="#141412"/><g fill="none" stroke="#FBFAF6" stroke-width="4" stroke-linecap="round" transform="translate(0 8)"><circle cx="37.5" cy="60" r="10"/><circle cx="62.5" cy="60" r="10"/><path d="M47.5 60h5"/></g></svg> Dotilda</div>
<h1 id="t">${s.title}</h1>
<div class="foot"><span class="label">${s.label}</span><canvas id="c" width="372" height="60"></canvas></div></div>
<script>
const g=c.getContext('2d');let s=5;const r=()=>(s=s*16807%2147483647)/2147483647;
for(let y=0;y<5;y++)for(let x=0;x<31;x++){const v=r(),cx=x*12+6,cy=y*12+6;g.beginPath();
 if(y===4&&x===30){g.fillStyle='#D1613E';g.arc(cx,cy,4,0,7);g.fill();continue}
 if(v<.13){g.fillStyle='#C9C5BA';g.arc(cx,cy,1.5,0,7)}else{g.fillStyle='#141412';g.arc(cx,cy,4,0,7)}g.fill()}
// Long taglines shrink until they fit their box.
const t=document.getElementById('t');let size=96;
while(size>56&&t.getBoundingClientRect().height>300){size-=4;t.style.fontSize=size+'px'}
</script></html>`;

const out = join(root, 'public/og');
mkdirSync(out, { recursive: true });
const tmp = mkdtempSync(join(tmpdir(), 'dotilda-og-'));
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });
for (const code of locales) {
  const s = code === 'en' ? en : { ...en, ...JSON.parse(readFileSync(join(i18n, `${code}.json`), 'utf8')) };
  const file = join(tmp, `${code}.html`);
  writeFileSync(file, html({ code, title: s.hero.title, label: s.hero.label }));
  await page.goto(pathToFileURL(file).href);
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(200);
  await page.screenshot({ path: join(out, `${code}.png`) });
  console.log(`public/og/${code}.png`);
}
await browser.close();
