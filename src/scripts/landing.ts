// The page's behaviour: the device's month line, "Try one", the year of dots, the latest release,
// and the motion. Motion is slow, short and only for people who accept it
// (prefers-reduced-motion); without it every element is simply there.
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { inject } from '@vercel/analytics';

// Vercel Web Analytics: page views only, no cookies, nothing that identifies a visitor.
// Served from this domain (/_vercel/insights), so the CSP stays 'self'.
inject();

gsap.registerPlugin(ScrollTrigger);

type PageData = {
  dateLocale: string;
  hero: { sample: string; noted: string; after: string };
  questions: string[];
  try: { another: string; done: string };
  get: { downloadVersion: string; line: string };
};

const data: PageData = JSON.parse(document.getElementById('page-data')!.textContent!);
const $ = <T extends Element = HTMLElement>(sel: string) => document.querySelector(sel) as T;
const $$ = (sel: string) => gsap.utils.toArray<HTMLElement>(sel);
const css = (name: string) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
const root = document.documentElement;

const today = new Date();
today.setHours(0, 0, 0, 0);

// Month line on the device: this month up to today, a few quiet days, today outlined.
const quietDays = new Set([5, 6, 13, 21, 22]);
const monthLine = $('#monthLine');
for (let d = 1; d <= today.getDate(); d++) {
  const i = document.createElement('i');
  if (d === today.getDate()) i.className = 't';
  else if (quietDays.has(d)) i.className = 'e';
  monthLine.appendChild(i);
}

// Try one: three changes, then only the inner weather, as in the app.
// Dotilda changes pose in one step, like on the Kompakt: curious while you browse, pleased at the end.
const tryQ = $('#tryQ'), tryBtn = $<HTMLButtonElement>('#tryBtn'), tryMeta = $('#tryMeta'), tryMark = $('.try-mark');
const seen = new Set([tryQ.textContent]);
let left = 3;
tryBtn.addEventListener('click', () => {
  const fresh = data.questions.filter(q => !seen.has(q));
  const next = fresh[Math.floor(Math.random() * fresh.length)];
  seen.add(next);
  left--;
  const show = () => {
    tryQ.textContent = next;
    tryMark.dataset.pose = left > 0 ? 'curious' : 'pleased';
    if (left > 0) tryBtn.textContent = data.try.another.replace('{n}', String(left));
    else { tryBtn.hidden = true; tryMeta.textContent = data.try.done; }
  };
  if (root.classList.contains('motion')) {
    gsap.timeline()
      .to(tryQ, { autoAlpha: 0, y: -6, duration: .25, ease: 'power1.in' })
      .call(show)
      .fromTo(tryQ, { y: 8 }, { autoAlpha: 1, y: 0, duration: .5, ease: 'power2.out' });
  } else show();
});

// Hero: the year as a faint sky of dots behind Dotilda's question. Past days are pale ink,
// today is an empty terracotta ring, waiting. Answer, and your words become a drop of ink
// that lands in today's place. Nothing is sent or saved.
const heroEl = $('.hero'), heroCanvas = $<HTMLCanvasElement>('#heroSky');
const heroCells: { m: number; d: number; past: boolean; today: boolean }[] = [];
for (let m = 0; m < 12; m++) {
  for (let d = 1; d <= new Date(today.getFullYear(), m + 1, 0).getDate(); d++) {
    const date = new Date(today.getFullYear(), m, d);
    heroCells.push({ m, d, past: date < today, today: +date === +today });
  }
}
const heroSky = { shown: heroCells.length, inked: false, ring: 0 };

function heroGrid() {
  const w = heroCanvas.clientWidth, h = heroCanvas.clientHeight;
  const cell = Math.min(w / 31, (h * .9) / 12);
  const left = w - cell * 31, top = (h - cell * 12) / 2;
  return { w, h, cell, R: Math.min(cell * .13, 4.5), at: (m: number, d: number) => [left + (d - .5) * cell, top + (m + .5) * cell] };
}

function drawHeroSky() {
  const { w, h, R, at } = heroGrid(), dpr = window.devicePixelRatio || 1;
  heroCanvas.width = w * dpr;
  heroCanvas.height = h * dpr;
  const g = heroCanvas.getContext('2d')!;
  g.setTransform(dpr, 0, 0, dpr, 0, 0);
  const ink = css('--ink'), ghost = css('--ghost'), todayColor = css('--today');
  heroCells.slice(0, Math.round(heroSky.shown)).forEach(({ m, d, past, today: isToday }) => {
    const [x, y] = at(m, d);
    g.beginPath();
    if (isToday) {
      g.arc(x, y, R * 1.5, 0, Math.PI * 2);
      if (heroSky.inked) { g.fillStyle = todayColor; g.fill(); }
      else { g.strokeStyle = todayColor; g.lineWidth = 1.5; g.stroke(); }
    } else {
      g.globalAlpha = past ? .11 : .45;
      g.fillStyle = past ? ink : ghost;
      g.arc(x, y, past ? R : R * .45, 0, Math.PI * 2);
      g.fill();
      g.globalAlpha = 1;
    }
  });
  if (heroSky.ring > 0 && heroSky.ring < 1) {
    const t = heroCells.find(c => c.today)!, [x, y] = at(t.m, t.d);
    g.globalAlpha = 1 - heroSky.ring;
    g.strokeStyle = todayColor;
    g.lineWidth = 1.5;
    g.beginPath();
    g.arc(x, y, R * (1.5 + heroSky.ring * 5), 0, Math.PI * 2);
    g.stroke();
    g.globalAlpha = 1;
  }
}

const ask = $<HTMLFormElement>('#ask'), askInput = $<HTMLInputElement>('#askInput');
const askQ = $('#askQ'), askNote = $('#askNote'), askMark = $('.ask-mark');
let answered = false;

function noted() {
  heroSky.inked = true;
  drawHeroSky();
  askMark.dataset.pose = 'pleased';
  ask.classList.add('done');
  $('#heroGet').classList.add('solid');
  askNote.textContent = data.hero.after;
  if (root.classList.contains('motion')) {
    gsap.timeline()
      .to(askQ, { autoAlpha: 0, y: -10, duration: .3, ease: 'power1.in' })
      .call(() => { askQ.textContent = data.hero.noted; })
      .fromTo(askQ, { y: 14 }, { autoAlpha: 1, y: 0, duration: .9, ease: 'power3.out' })
      .from(askNote, { autoAlpha: 0, duration: .6 }, '-=.4');
    gsap.to(heroSky, { ring: 1, duration: 1.4, ease: 'power2.out', onUpdate: drawHeroSky, onComplete: () => { heroSky.ring = 0; drawHeroSky(); } });
  } else {
    askQ.textContent = data.hero.noted;
  }
}

function flyToToday(text: string) {
  const hr = heroEl.getBoundingClientRect(), ir = askInput.getBoundingClientRect();
  const word = document.createElement('span');
  word.className = 'ink-word';
  word.textContent = text;
  heroEl.appendChild(word);
  const x0 = ir.left - hr.left + parseFloat(getComputedStyle(askInput).paddingLeft);
  const y0 = ir.top - hr.top + (ir.height - word.offsetHeight) / 2;
  gsap.set(word, { x: x0, y: y0, transformOrigin: '50% 50%' });
  askInput.value = '';

  const drop = document.createElement('span');
  drop.className = 'ink-drop';
  heroEl.appendChild(drop);
  const t = heroCells.find(c => c.today)!;
  const { R, at } = heroGrid(), cr = heroCanvas.getBoundingClientRect();
  const [tx, ty] = at(t.m, t.d);

  gsap.set(drop, { x: x0 + Math.min(word.offsetWidth, ir.width) / 2, y: y0 + word.offsetHeight / 2, scale: 0 });
  gsap.timeline({ onComplete: () => { drop.remove(); word.remove(); noted(); } })
    .to(word, { scaleX: .04, scaleY: .3, autoAlpha: 0, duration: .75, ease: 'power3.in' })
    .to(drop, { scale: 1, duration: .4, ease: 'back.out(3)' }, '-=.3')
    .to(drop, { x: cr.left - hr.left + tx, duration: 1.2, ease: 'power1.inOut' }, '+=.1')
    .to(drop, { y: cr.top - hr.top + ty, duration: 1.2, ease: 'back.in(1.6)' }, '<')
    .to(drop, { scale: (R * 3) / 16, duration: .35, ease: 'power2.in' }, '-=.35');
}

ask.addEventListener('submit', e => {
  e.preventDefault();
  if (answered) return;
  answered = true;
  askInput.blur();
  const typed = askInput.value.trim();
  if (!root.classList.contains('motion')) {
    if (!typed) askInput.value = data.hero.sample;
    noted();
    return;
  }
  if (typed) { flyToToday(typed); return; }
  // Nothing typed: Dotilda's example answer writes itself, then flies.
  const sample = data.hero.sample, typing = { n: 0 };
  gsap.timeline({ onComplete: () => flyToToday(sample) })
    .to(typing, { n: sample.length, duration: sample.length * .045, ease: 'none', onUpdate: () => { askInput.value = sample.slice(0, Math.round(typing.n)); } })
    .to({}, { duration: .4 });
});

// The year of dots: a sample year up to today. It inks in as you scroll past it, one day after
// another, then stays still. Without motion it is drawn whole at once.
const year = today.getFullYear();
$('#skyYear').textContent = String(year);
let seed = 7;
const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
const journey = [9, 10, 11, 12, 13, 14, 15]; // March, "7 days to slow down"
type Day = { m: number; d: number; kind: string };
const days: Day[] = [];
for (let m = 0; m < 12; m++) {
  const length = new Date(year, m + 1, 0).getDate();
  for (let d = 1; d <= length; d++) {
    const date = new Date(year, m, d);
    const r = rnd(), w = Math.floor(rnd() * 5);
    if (date > today) continue;
    days.push({
      m, d,
      kind: +date === +today ? 'today'
        : m === 2 && journey.includes(d) ? 'full'
        : r < .13 ? 'quiet' : r < .25 ? 'weather' + w : r < .29 ? 'photo' : r < .31 ? 'mirror' : r < .32 ? 'letter' : 'full',
    });
  }
}

const canvas = $<HTMLCanvasElement>('#galaxy'), months = $('.galaxy .months');
const sky = { shown: days.length };

function drawSky() {
  const w = canvas.clientWidth, cell = w / 31, h = cell * 12, dpr = window.devicePixelRatio || 1;
  canvas.style.height = h + 'px';
  months.style.height = h + 'px';
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  const g = canvas.getContext('2d')!;
  g.setTransform(dpr, 0, 0, dpr, 0, 0);
  const ink = css('--ink'), ghost = css('--ghost'), todayColor = css('--today');
  const R = Math.min(cell * .32, 7);
  const at = (m: number, d: number) => [(d - 1) * cell + cell / 2, m * cell + cell / 2];
  const shown = Math.round(sky.shown);

  // The journey's constellation: a fine thread through its days, behind the dots,
  // reaching each day as it is inked.
  const linked = journey.filter(d => { const i = days.findIndex(x => x.m === 2 && x.d === d); return i >= 0 && i < shown; });
  if (linked.length > 1) {
    const [x0, y] = at(2, linked[0]), [x1] = at(2, linked[linked.length - 1]);
    g.strokeStyle = ink;
    g.lineWidth = Math.max(1, R * .3);
    g.beginPath();
    g.moveTo(x0, y);
    g.lineTo(x1, y);
    g.stroke();
  }

  days.slice(0, shown).forEach(({ m, d, kind }) => {
    const [x, y] = at(m, d);
    g.fillStyle = ink;
    g.strokeStyle = ink;
    g.lineWidth = Math.max(1, R * .28);
    const dot = (r: number, fill: boolean) => { g.beginPath(); g.arc(x, y, r, 0, Math.PI * 2); fill ? g.fill() : g.stroke(); };
    if (kind === 'quiet') { g.fillStyle = ghost; dot(R * .35, true); }
    else if (kind === 'today') { g.fillStyle = todayColor; dot(R, true); }
    else if (kind === 'full') dot(R, true);
    else if (kind === 'photo') { dot(R * .55, true); dot(R * 1.05, false); }
    else if (kind === 'mirror') { dot(R * .55, false); dot(R * 1.05, false); }
    else if (kind === 'letter') {
      g.beginPath();
      g.moveTo(x, y - R * 1.1); g.lineTo(x + R, y); g.lineTo(x, y + R * 1.1); g.lineTo(x - R, y);
      g.closePath();
      g.fill();
    } else {
      // Inner weather as a moon phase: 0 sunny (full) .. 4 stormy (ring).
      const p = +kind.slice(7), r = R * .85;
      if (p === 0) dot(R, true);
      else {
        dot(r, false);
        if (p < 4) {
          const k = [0, .5, 0, -.6][p];
          g.beginPath();
          g.arc(x, y, r, -Math.PI / 2, Math.PI / 2);
          g.ellipse(x, y, Math.abs(k) * r, r, 0, Math.PI / 2, -Math.PI / 2, k > 0);
          g.fill();
        }
      }
    }
  });
}

let resizeTimer: number;
addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = window.setTimeout(() => { drawSky(); drawHeroSky(); }, 150); });
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => { drawSky(); drawHeroSky(); });

// Motion.
const mm = gsap.matchMedia();
mm.add('(prefers-reduced-motion: no-preference)', () => {
  root.classList.add('motion', 'motion-ready');
  const calm = { ease: 'power2.out', duration: 1.1 };
  const onEnter = (trigger: gsap.DOMTarget, start = 'top 85%') => ({ trigger, start, once: true });

  // Hero: the sky of dots comes up row by row, the tagline settles, Dotilda appears and her
  // question is typed out letter by letter, then the field waits for your line.
  heroSky.shown = 0;
  gsap.timeline({ defaults: calm })
    .to(heroSky, { shown: heroCells.length, duration: 1.8, ease: 'power1.inOut', onUpdate: drawHeroSky })
    .from('.hero-in > .label', { autoAlpha: 0, y: 10 }, .2)
    .from('[data-hero-title] .w', { autoAlpha: 0, y: '0.35em', stagger: .05 }, .35)
    .from('.ask-who', { autoAlpha: 0, x: -12 }, '-=.6')
    .from('.ask-chars .c', { autoAlpha: 0, duration: .01, stagger: .05, ease: 'none' }, '-=.3')
    .from('.ask-field', { autoAlpha: 0, y: 16 }, '-=.2')
    .from('#askNote, .hero-foot', { autoAlpha: 0, y: 10, stagger: .15 }, '-=.7');

  // Get Dotilda: the Kompakt does a full e-ink refresh (ink, white, page) as it comes into view.
  gsap.timeline({ scrollTrigger: onEnter('.kompakt', 'top 80%') })
    .from('.kompakt', { autoAlpha: 0, y: 30, ...calm, duration: 1.3 })
    .set('.screen > :not(.flash)', { autoAlpha: 0 }, 0)
    .set('.flash', { autoAlpha: 1, backgroundColor: '#111' }, '-=.4')
    .set('.flash', { backgroundColor: '#F4F3EE' }, '+=.09')
    .set('.flash', { autoAlpha: 0 }, '+=.09')
    .set('.screen > :not(.flash)', { autoAlpha: 1 })
    .from('#monthLine i', { autoAlpha: 0, duration: .01, stagger: .035 }, '+=.1');

  // The pause: the sentence is pinned while you scroll, and inks in word by word, from pale
  // grey to black. When it is complete, Dotilda pushes her glasses up (one step, as in the app).
  const pauseMark = $('.pause-mark');
  gsap.timeline({
    scrollTrigger: {
      trigger: '.pause', start: 'top top', end: '+=140%', pin: true, anticipatePin: 1, scrub: 1,
      onUpdate: st => { pauseMark.dataset.pose = st.progress > .97 ? 'pleased' : 'peering'; },
    },
  })
    .from('.pause .wi', { opacity: .12, stagger: .5, duration: 1, ease: 'none' })
    .from(pauseMark, { opacity: .12, duration: 1, ease: 'none' });

  // Section labels are typed, in their monospace, one character at a time.
  $$('section .stack > .label').forEach(label => gsap.from(label, {
    clipPath: 'inset(0 100% 0 0)', duration: .9, ease: `steps(${label.textContent!.length})`,
    scrollTrigger: onEnter(label, 'top 90%'),
  }));

  // Headings rise word by word out of their mask; the text under them settles after.
  $$('[data-split]').forEach(h => {
    gsap.from(h.querySelectorAll('.wi'), { yPercent: 105, duration: 1.2, ease: 'power3.out', stagger: .05, scrollTrigger: onEnter(h) });
    const lede = h.parentElement!.querySelector('.lede');
    if (lede) gsap.from(lede, { autoAlpha: 0, y: 12, ...calm, delay: .5, scrollTrigger: onEnter(h) });
  });

  // Ritual: each rule is drawn, then its step is refreshed top to bottom, like an e-ink page.
  gsap.timeline({ scrollTrigger: onEnter('.cols') })
    .from('.step .rule', { scaleX: 0, transformOrigin: 'left center', duration: 1.2, ease: 'power3.inOut', stagger: .2 })
    .from('.step > :not(.rule)', { clipPath: 'inset(0 0 100% 0)', duration: .7, ease: 'power2.inOut', stagger: .08 }, '-=.9');

  // Try one: the box arrives with a full e-ink refresh, a blink of ink then the page.
  gsap.timeline({ scrollTrigger: onEnter('.try') })
    .from('.try', { autoAlpha: 0, duration: .01 })
    .set('.try', { filter: 'invert(1)' }, '+=.05')
    .set('.try', { filter: 'none' }, '+=.1');

  // Formats: each dot is stamped, then its name appears beside it.
  gsap.timeline({ scrollTrigger: onEnter('.formats') })
    .from('.fmt svg', { scale: 0, transformOrigin: '50% 50%', duration: .6, ease: 'back.out(2.2)', stagger: .09 })
    .from('.fmt b, .fmt span', { autoAlpha: 0, x: -8, ...calm, duration: .8, stagger: .04 }, '<+=.15');

  // Dotilda's lines are written out, left to right, one after the other.
  $$('[data-anim="quote"]').forEach((q, i) => gsap.timeline({ scrollTrigger: onEnter('.quotes', 'top 80%'), delay: i * 1.1 })
    .from(q.querySelector('.voice'), { clipPath: 'inset(0 100% 0 0)', duration: 1.4, ease: 'power1.inOut' })
    .from(q.querySelector('figcaption'), { autoAlpha: 0, duration: .6 }, '-=.3'));

  // Promises settle quietly, the only plain fade on the page.
  gsap.from('[data-anim="promise"]', { autoAlpha: 0, y: 14, ...calm, stagger: .08, scrollTrigger: onEnter('.promises') });

  // Get Dotilda: the steps slide in from their numbers, then the buttons.
  gsap.timeline({ scrollTrigger: onEnter('.how') })
    .from('[data-anim="how"]', { autoAlpha: 0, x: -16, ...calm, stagger: .15 })
    .from('[data-anim="actions"]', { autoAlpha: 0, y: 10, ...calm }, '-=.6');

  // The year inks in with the scroll, day after day.
  sky.shown = 0;
  gsap.to(sky, {
    shown: days.length, ease: 'none',
    onUpdate: drawSky,
    scrollTrigger: { trigger: '.galaxy', start: 'top 85%', end: 'bottom 55%', scrub: .8 },
  });

  // Positions are measured once; measure again when the fonts change the page's height.
  document.fonts.ready.then(() => ScrollTrigger.refresh());

  return () => { root.classList.remove('motion'); sky.shown = days.length; heroSky.shown = heroCells.length; drawSky(); drawHeroSky(); };
});
drawSky();
drawHeroSky();

// Latest release, live from GitHub. Without it the button still downloads the latest APK.
fetch('https://api.github.com/repos/kxrz/dotilda-app/releases/latest', { headers: { Accept: 'application/vnd.github+json' } })
  .then(r => (r.ok ? r.json() : Promise.reject(r.status)))
  .then(rel => {
    const apk = (rel.assets || []).find((a: { name: string }) => a.name === 'dotilda.apk');
    const v = String(rel.tag_name).replace(/^v/i, '');
    $('#dl').textContent = data.get.downloadVersion.replace('{v}', v);
    $<HTMLAnchorElement>('#changes').href = rel.html_url;
    $('#release').textContent = data.get.line
      .replace('{v}', v)
      .replace('{date}', new Date(rel.published_at).toLocaleDateString(data.dateLocale, { day: 'numeric', month: 'long', year: 'numeric' }))
      .replace('{size}', apk ? (apk.size / 1048576).toFixed(1) : '–');
  })
  .catch(() => {});
