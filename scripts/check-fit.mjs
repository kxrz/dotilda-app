// Checks that the page fits every screen: no clipped or overflowing text, no sideways scroll,
// the pinned sentence fits in one screen, the Kompakt mockup holds its content, and Dotilda's
// question and the answer field are both visible on the first screen, and the whole hero (Dotilda's
// playground) fits in it on any screen at least 560 px tall.
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

// /stats gets a long, busy release list (40 versions, six-figure counters) to test its widths.
const releases = JSON.stringify(Array.from({ length: 40 }, (_, i) => ({
  tag_name: `v1.${39 - i}.10`, published_at: new Date(Date.UTC(2027, 0, 40 - i)).toISOString(), prerelease: false, draft: false,
  assets: [{ name: 'dotilda.apk', download_count: 123456 * (i + 1) }, { name: `dotilda-1.${39 - i}.10.apk`, download_count: 999 },
    { name: 'manifest.json', download_count: 987654 }],
})));
const browser = await chromium.launch();
let failed = 0;
for (const pageUrl of pages) {
console.log(`\n${new URL(pageUrl).pathname}`);
for (const [name, width, height] of screens) {
  const page = await browser.newPage({ viewport: { width, height }, reducedMotion: 'reduce' });
  await page.route('https://api.github.com/**', r => r.request().url().includes('/releases?')
    ? r.fulfill({ status: 200, contentType: 'application/json', body: releases })
    : r.fulfill({ status: 404, body: '{}' }));
  await page.route('**/_vercel/**', r => r.fulfill({ status: 200, body: '' }));
  await page.goto(pageUrl);
  await page.evaluate(() => document.fonts.ready);
  const issues = await page.evaluate(() => {
    const out = [], vw = innerWidth, vh = innerHeight;
    if (document.documentElement.scrollWidth > vw) out.push(`scrolls sideways (${document.documentElement.scrollWidth}px)`);
    const texts = [...document.querySelectorAll('body *')].filter(e => e.offsetParent !== null && !e.closest('.sr-only')
      && [...e.childNodes].some(c => c.nodeType === 3 && c.textContent.trim()));
    // A horizontal scroller (the screens carousel) hides nothing: its content is swiped into view.
    const scrollsX = el => ['auto', 'scroll'].includes(getComputedStyle(el).overflowX);
    for (const e of texts) {
      const r = e.getBoundingClientRect(), label = `"${e.textContent.trim().slice(0, 32)}"`;
      let inScroller = false;
      for (let a = e.parentElement; a && a !== document.body; a = a.parentElement) {
        const cs = getComputedStyle(a);
        if (cs.overflowX === 'visible' && cs.overflowY === 'visible') continue;
        const ar = a.getBoundingClientRect();
        if (scrollsX(a)) { inScroller = true; if (r.top < ar.top - 1 || r.bottom > ar.bottom + 1) { out.push(`${label} is clipped by .${a.className}`); break; } continue; }
        if (r.left < ar.left - 1 || r.right > ar.right + 1 || r.top < ar.top - 1 || r.bottom > ar.bottom + 1) { out.push(`${label} is clipped by .${a.className}`); break; }
      }
      if (!inScroller && r.right > vw + 1) out.push(`${label} goes off the right edge`);
    }
    // The landing page's own checks (other pages, like /brand, have none of these).
    const screen = document.querySelector('.screen')?.getBoundingClientRect();
    if (screen) for (const c of document.querySelectorAll('.screen > :not(.flash)')) {
      if (c.getBoundingClientRect().bottom > screen.bottom + 1) out.push(`Kompakt screen overflows (.${c.className})`);
    }
    const pause = document.querySelector('.pause-text'), field = document.querySelector('.ask-field');
    if (pause && pause.getBoundingClientRect().height > vh) out.push('pause sentence taller than the screen');
    if (field && field.getBoundingClientRect().bottom + scrollY > vh) out.push('answer field below the first screen');
    // Dotilda's playground fits in one screen on anything taller than a phone in landscape.
    const heroIn = document.querySelector('.hero-in');
    if (heroIn && vh >= 560 && heroIn.getBoundingClientRect().bottom + scrollY > vh + 1) out.push(`hero taller than the screen (${Math.round(heroIn.getBoundingClientRect().bottom + scrollY)}px)`);
    return [...new Set(out)];
  });
  // The playground changes shape (choices, moons, Dotilda's reply): every state must still fit.
  if (await page.$('#another')) {
    const state = async name => {
      const r = await page.evaluate(() => ({ bottom: document.querySelector('.hero-in').getBoundingClientRect().bottom + scrollY, sw: document.documentElement.scrollWidth }));
      if (height >= 560 && r.bottom > height + 1) issues.push(`hero taller than the screen with ${name} (${Math.round(r.bottom)}px)`);
      if (r.sw > width) issues.push(`scrolls sideways with ${name} (${r.sw}px)`);
    };
    for (const name of ['a choice', 'one word', 'the moons']) { await page.click('#another'); await state(name); }
    await page.click('#moons button[data-w="4"]'); await state("Dotilda's reply");
  }
  console.log(`${issues.length ? '✗' : '✓'} ${name} ${width}×${height}${issues.length ? '\n    ' + issues.join('\n    ') : ''}`);
  failed += issues.length ? 1 : 0;
  await page.close();
}
}
await browser.close();
process.exit(failed ? 1 : 0);
