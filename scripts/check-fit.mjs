// Checks that the page fits every screen: no clipped or overflowing text, no sideways scroll,
// the pinned sentence fits in one screen, the Kompakt mockup holds its content, and Dotilda's
// question and the answer field are both visible on the first screen.
// Every page in the sitemap is checked, so each language is covered.
// Usage: npm run build && npm run preview, then in another terminal: npm run check:fit [url]
// Needs Playwright with Chromium (npx playwright install chromium).
import { chromium } from 'playwright';

const url = process.argv[2] || 'http://localhost:4321/';
const screens = [
  ['Mudita Kompakt', 360, 564], ['small phone', 320, 568], ['Android', 360, 640],
  ['iPhone 8', 375, 667], ['iPhone', 390, 844], ['iPhone Max', 430, 932],
  ['phone landscape', 844, 390], ['small landscape', 667, 375], ['iPad', 768, 1024],
  ['iPad landscape', 1024, 768], ['laptop', 1280, 720], ['desktop', 1440, 900], ['full HD', 1920, 1080],
];

const origin = new URL(url).origin;
const sitemap = await (await fetch(new URL('/sitemap.xml', origin))).text();
const pages = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map(m => origin + new URL(m[1]).pathname);

const browser = await chromium.launch();
let failed = 0;
for (const pageUrl of pages) {
console.log(`\n${new URL(pageUrl).pathname}`);
for (const [name, width, height] of screens) {
  const page = await browser.newPage({ viewport: { width, height }, reducedMotion: 'reduce' });
  await page.route('https://api.github.com/**', r => r.fulfill({ status: 404, body: '{}' }));
  await page.route('**/_vercel/**', r => r.fulfill({ status: 200, body: '' }));
  await page.goto(pageUrl);
  await page.evaluate(() => document.fonts.ready);
  const issues = await page.evaluate(() => {
    const out = [], vw = innerWidth, vh = innerHeight;
    if (document.documentElement.scrollWidth > vw) out.push(`scrolls sideways (${document.documentElement.scrollWidth}px)`);
    const texts = [...document.querySelectorAll('body *')].filter(e => e.offsetParent !== null && !e.closest('.sr-only')
      && [...e.childNodes].some(c => c.nodeType === 3 && c.textContent.trim()));
    for (const e of texts) {
      const r = e.getBoundingClientRect(), label = `"${e.textContent.trim().slice(0, 32)}"`;
      if (r.right > vw + 1) out.push(`${label} goes off the right edge`);
      for (let a = e.parentElement; a && a !== document.body; a = a.parentElement) {
        const cs = getComputedStyle(a);
        if (cs.overflowX === 'visible' && cs.overflowY === 'visible') continue;
        const ar = a.getBoundingClientRect();
        if (r.left < ar.left - 1 || r.right > ar.right + 1 || r.top < ar.top - 1 || r.bottom > ar.bottom + 1) { out.push(`${label} is clipped by .${a.className}`); break; }
      }
    }
    const screen = document.querySelector('.screen').getBoundingClientRect();
    for (const c of document.querySelectorAll('.screen > :not(.flash)')) {
      if (c.getBoundingClientRect().bottom > screen.bottom + 1) out.push(`Kompakt screen overflows (.${c.className})`);
    }
    if (document.querySelector('.pause-text').getBoundingClientRect().height > vh) out.push('pause sentence taller than the screen');
    if (document.querySelector('.ask-field').getBoundingClientRect().bottom + scrollY > vh) out.push('answer field below the first screen');
    return [...new Set(out)];
  });
  console.log(`${issues.length ? '✗' : '✓'} ${name} ${width}×${height}${issues.length ? '\n    ' + issues.join('\n    ') : ''}`);
  failed += issues.length ? 1 : 0;
  await page.close();
}
}
await browser.close();
process.exit(failed ? 1 : 0);
