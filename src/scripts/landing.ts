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
  hero: {
    sample: string; noted: string; after: string; privacy: string; asks: string; quips: string[];
    demo: { type: 'line' | 'word' | 'choice' | 'weather'; q: string; options?: string[] }[];
    placeholder: string; word: string; justWeather: string; yourTurn: string; nextDay: string;
    weatherLabels: string[]; weatherPhrases: string[];
  };
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

// Hero: Dotilda's playground. She asks in her bubble, as in the app; answer in a line, a word,
// a choice or a moon, change the question (3 times, then only the weather), go on to the next
// day and watch this month's row fill up. Each answer fills its day's dot, which swells and settles.
// With motion on, the scene first plays once by itself, unless you start first. Poke her: she
// pulls a face in one step (as in the app), hops, and poked on, has a word with you.
// Nothing is sent or saved.
type Kind = 'line' | 'word' | 'choice' | 'weather';
type DemoQ = { type: Kind; q: string; options?: string[] };
const monthDots = $('#monthDots'), monthName = $('#monthName');
const ask = $<HTMLFormElement>('#ask'), askInput = $<HTMLTextAreaElement>('#askInput'), askBtn = $<HTMLButtonElement>('#askBtn');
const askQ = $('#askQ'), askNote = $('#askNote'), askWho = $('#askWho'), askLabel = $('#askLabel'), heroMark = $('.hero-mark');
const choicesEl = $('#choices'), moonsEl = $('#moons'), another = $<HTMLButtonElement>('#another'), nextDayBtn = $<HTMLButtonElement>('#nextDay');
const poke = $<HTMLButtonElement>('#poke'), pokeHint = $('#pokeHint');
const motion = () => root.classList.contains('motion');
const ns = 'http://www.w3.org/2000/svg';
const svgEl = (tag: string, attrs: Record<string, string | number>) => {
  const e = document.createElementNS(ns, tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, String(v));
  return e;
};

// The inner weather as the app draws it: a moon, full (sunny) to new (stormy).
const moonLit = [1, .75, .5, .25, 0];
function moon(cx: number, cy: number, r: number, i: number, cls = '') {
  const g = svgEl('g', { class: cls });
  g.append(svgEl('circle', { cx, cy, r, class: 'moon-o m-o' }));
  const f = moonLit[i];
  if (f > 0) {
    const rx = Math.abs(1 - 2 * f) * r;
    g.append(svgEl('path', { class: 'moon-l m-l', d: f === 1
      ? `M${cx} ${cy - r}a${r} ${r} 0 1 1 0 ${2 * r}a${r} ${r} 0 1 1 0 ${-2 * r}z`
      : `M${cx} ${cy - r}A${r} ${r} 0 0 1 ${cx} ${cy + r}A${rx} ${r} 0 0 ${f > .5 ? 1 : 0} ${cx} ${cy - r}z` }));
  }
  return g;
}

// The month row. Days before the real today are drawn from a fixed seed (answered or quiet);
// the days you answer here keep the shape of your answer.
const answers = new Map<string, number>(); // day key -> -1 for a dot, 0..4 for a moon
const keyOf = (d: Date) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
const view = new Date(today);
function drawMonth(pulse = false) {
  monthName.textContent = view.toLocaleDateString(data.dateLocale, { month: 'long' });
  monthDots.replaceChildren();
  monthDots.setAttribute('viewBox', `0 0 310 20`);
  let seed = 11 + view.getMonth();
  const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  for (let d = 1; d <= view.getDate(); d++) {
    const date = new Date(view.getFullYear(), view.getMonth(), d), x = (d - .5) * 10, isView = d === view.getDate();
    const quiet = rnd() < .2, mine = answers.get(keyOf(date));
    let el: Element;
    if (mine !== undefined) el = mine >= 0 ? moon(x, 6, 3.4, mine, isView ? 'warm' : '') : svgEl('circle', { cx: x, cy: 6, r: 3, class: `d-ink${isView ? ' warm' : ''}` });
    else if (isView) el = svgEl('circle', { cx: x, cy: 6, r: 3.4, class: 'd-today' });
    else if (date < today) el = svgEl('circle', { cx: x, cy: 6, r: quiet ? 1.2 : 3, class: quiet ? 'd-quiet' : 'd-ink' });
    else continue;
    if (isView) el.id = 'todayDot';
    monthDots.append(el);
  }
  monthDots.append(svgEl('path', { d: `M${(view.getDate() - .5) * 10} 13l3 4h-6z`, class: 'caret' }));
  if (pulse && motion()) gsap.timeline()
    .to('#todayDot', { scale: 3.2, transformOrigin: '50% 50%', duration: .45, ease: 'power2.out' })
    .to('#todayDot', { scale: 1, duration: .7, ease: 'back.out(2.2)' }, '+=.15');
}
drawMonth();

// The question, set letter by letter (screen readers get it whole).
function setQuestion(text: string) {
  askQ.replaceChildren();
  const sr = document.createElement('span');
  sr.className = 'sr-only';
  sr.textContent = text;
  const chars = document.createElement('span');
  chars.className = 'ask-chars';
  chars.setAttribute('aria-hidden', 'true');
  const words = text.split(' ');
  words.forEach((w, i) => {
    const cw = document.createElement('span');
    cw.className = 'cw';
    [...w].forEach(ch => { const c = document.createElement('span'); c.className = 'c'; c.textContent = ch; cw.append(c); });
    chars.append(cw, i < words.length - 1 ? ' ' : '');
  });
  askQ.append(sr, chars);
  askLabel.textContent = text;
  if (motion()) gsap.from(chars.querySelectorAll('.c'), { autoAlpha: 0, duration: .01, stagger: .035, ease: 'none' });
}
function say(text: string) {
  if (!motion()) { askQ.textContent = text; return; }
  gsap.timeline()
    .to(askQ, { autoAlpha: 0, y: -8, duration: .2, ease: 'power1.in' })
    .call(() => { askQ.textContent = text; })
    .fromTo(askQ, { y: 10 }, { autoAlpha: 1, y: 0, duration: .6, ease: 'power3.out' });
}

// The answer controls for one kind of question.
for (let i = 0; i < 5; i++) {
  const b = document.createElement('button');
  b.type = 'button';
  b.dataset.w = String(i);
  const svg = svgEl('svg', { viewBox: '0 0 22 22', 'aria-hidden': 'true' });
  svg.append(moon(11, 11, 8, i));
  const label = document.createElement('span');
  label.textContent = data.hero.weatherLabels[i];
  b.append(svg, label);
  moonsEl.append(b);
}
let qIndex = 0, changes = 3, onlyWeather = false;
let currentQ: DemoQ = data.hero.demo[0];
function showQuestion(q: DemoQ) {
  currentQ = q;
  ask.dataset.kind = q.type;
  ask.classList.remove('done');
  askInput.value = ''; fitAnswer();
  askInput.placeholder = q.type === 'word' ? data.hero.word : data.hero.placeholder;
  askInput.maxLength = q.type === 'word' ? 24 : 140;
  choicesEl.replaceChildren(...(q.options ?? []).map(o => {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'btn';
    b.textContent = o;
    return b;
  }));
  setQuestion(q.q);
}
const anotherLabel = () => {
  another.hidden = onlyWeather;
  another.textContent = changes > 0 ? data.try.another.replace('{n}', String(changes)) : data.hero.justWeather;
};

// Poke her: one face per tap (as in the app), a little hop on the page, then a word or two.
const pokePoses = ['wink', 'surprised', 'curious', 'sleepy', 'pleased'];
let pokes = 0;
poke.addEventListener('click', () => {
  pokes++;
  pokeHint.hidden = true;
  heroMark.dataset.pose = pokePoses[(pokes - 1) % pokePoses.length];
  if (pokes >= 3) askWho.textContent = data.hero.quips[(pokes - 3) % data.hero.quips.length];
  if (motion()) gsap.fromTo(poke, { y: 0, rotation: 0 }, { y: -12, rotation: pokes % 2 ? -6 : 6, duration: .14, ease: 'power2.out', yoyo: true, repeat: 1 });
});

// In the ink card, Dotilda watches the main button and smiles when you reach for it (one step).
const ctaMark = $('.cta-mark'), ctaMain = $('.cta-main');
const ctaSmile = (on: boolean) => () => { ctaMark.dataset.pose = on ? 'pleased' : 'peering'; };
ctaMain.addEventListener('pointerenter', ctaSmile(true));
ctaMain.addEventListener('pointerleave', ctaSmile(false));
ctaMain.addEventListener('focus', ctaSmile(true));
ctaMain.addEventListener('blur', ctaSmile(false));

type Phase = 'waiting' | 'demo' | 'flying' | 'answered' | 'yours';
let phase: Phase = 'waiting', demo: gsap.core.Timeline | null = null, demoTimer = 0, demoDone = false;

function landed(value: number) {
  answers.set(keyOf(view), value);
  drawMonth(true);
  heroMark.dataset.pose = 'pleased';
  const wasDemo = phase === 'demo';
  phase = 'answered';
  say(value >= 0 ? data.hero.weatherPhrases[value] : data.hero.noted);
  askNote.textContent = wasDemo ? '' : data.hero.after;
  nextDayBtn.textContent = wasDemo ? data.hero.yourTurn : data.hero.nextDay;
  nextDayBtn.hidden = false;
  if (wasDemo) { demoDone = true; pokeHint.hidden = pokes > 0; }
  if (motion()) gsap.from([nextDayBtn, askNote, pokeHint], { autoAlpha: 0, y: 6, duration: .5, delay: .4 });
  ScrollTrigger.refresh();
}

// Your answer fills today's dot, in place: it swells, then settles, so you see it is yours.
function answer(value: number) {
  if (phase === 'flying' || phase === 'answered') return;
  const wasDemo = phase === 'demo';
  phase = wasDemo ? 'demo' : 'flying';
  ask.classList.add('done');
  heroMark.dataset.pose = 'neutral'; // she pushes her glasses up to watch her dot
  const finish = () => { phase = wasDemo ? 'demo' : 'yours'; landed(value); };
  if (motion()) gsap.delayedCall(.2, finish); else finish();
}

ask.addEventListener('submit', e => {
  e.preventDefault();
  const typed = askInput.value.trim();
  if (phase !== 'demo') takeOver();
  if (!typed) { askInput.focus(); return; }
  askInput.blur();
  answer(-1);
  askInput.value = ''; fitAnswer();
});
choicesEl.addEventListener('click', e => {
  const b = (e.target as Element).closest('button');
  if (!b) return;
  takeOver();
  answer(-1);
});
moonsEl.addEventListener('click', e => {
  const b = (e.target as Element).closest<HTMLButtonElement>('button');
  if (!b) return;
  takeOver();
  answer(Number(b.dataset.w));
});

// Another question: three times, then only the weather, as in the app. She glances aside.
another.addEventListener('click', () => {
  takeOver();
  if (changes > 0) {
    changes--;
    qIndex++;
    showQuestion(data.hero.demo[qIndex % data.hero.demo.length]);
    heroMark.dataset.pose = 'curious';
  } else {
    onlyWeather = true;
    showQuestion(data.hero.demo.find(q => q.type === 'weather')!);
    heroMark.dataset.pose = 'peering';
  }
  askWho.textContent = data.hero.asks;
  anotherLabel();
});

// The next day: the caret moves on (to next month after the last day), a new question.
nextDayBtn.addEventListener('click', () => {
  phase = 'yours';
  view.setDate(view.getDate() + 1);
  drawMonth();
  nextDayBtn.hidden = true;
  changes = 3;
  onlyWeather = false;
  qIndex++;
  showQuestion(data.hero.demo[qIndex % data.hero.demo.length]);
  anotherLabel();
  heroMark.dataset.pose = 'peering';
  askWho.textContent = data.hero.asks;
  askNote.textContent = '';
  if (currentQ.type === 'line' || currentQ.type === 'word') askInput.focus({ preventScroll: true });
  ScrollTrigger.refresh();
});

// The scene plays once by itself: a sample answer writes itself, the button is pressed, the drop flies.
function playDemo() {
  if (phase !== 'waiting' || demoDone || document.activeElement === askInput || askInput.value) return;
  phase = 'demo';
  const sample = data.hero.sample, typing = { n: 0 };
  heroMark.dataset.pose = 'curious';
  demo = gsap.timeline({ onComplete: () => { askBtn.classList.remove('pressed'); demo = null; answer(-1); askInput.value = ''; fitAnswer(); } })
    .to(typing, { n: sample.length, duration: sample.length * .04, ease: 'none', onUpdate: () => { askInput.value = sample.slice(0, Math.round(typing.n)); fitAnswer(); } })
    .call(() => askBtn.classList.add('pressed'), [], '+=.35')
    .to({}, { duration: .3 });
}
// Starting yourself stops the scene (or keeps it from playing): the field is yours.
function takeOver() {
  clearTimeout(demoTimer);
  if (phase === 'demo' && demo) { demo.kill(); demo = null; askInput.value = ''; fitAnswer(); askBtn.classList.remove('pressed'); heroMark.dataset.pose = 'peering'; phase = 'yours'; }
  else if (phase === 'waiting') phase = 'yours';
}
// The answer wraps and grows with its words, instead of being cut off; Enter adds the dot.
function fitAnswer() { askInput.style.height = 'auto'; askInput.style.height = `${askInput.scrollHeight + 2}px`; }
askInput.addEventListener('input', fitAnswer);
askInput.addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); ask.requestSubmit(); } });
askInput.addEventListener('pointerdown', takeOver);
askInput.addEventListener('focus', () => { if (phase !== 'demo' || demo) takeOver(); });

// Inside the app: a strip of real screens. Swipe, drag or use the page keys; the screen nearest
// the middle stands straight and gets its caption.
const track = $('#track');
const slides = [...track.querySelectorAll<HTMLElement>('.slide')];
const prevShot = $<HTMLButtonElement>('#prevShot'), nextShot = $<HTMLButtonElement>('#nextShot'), shotCount = $('#shotCount');
let current = 0;
const setActive = (i: number) => {
  if (i === current && slides[i].classList.contains('active')) return;
  slides[current].classList.remove('active');
  current = i;
  slides[i].classList.add('active');
  shotCount.textContent = `${i + 1} / ${slides.length}`;
  prevShot.disabled = i === 0;
  nextShot.disabled = i === slides.length - 1;
};
const nearest = () => {
  const middle = track.scrollLeft + track.clientWidth / 2;
  let best = 0;
  slides.forEach((sl, i) => {
    if (Math.abs(sl.offsetLeft + sl.offsetWidth / 2 - middle) < Math.abs(slides[best].offsetLeft + slides[best].offsetWidth / 2 - middle)) best = i;
  });
  return best;
};
const goTo = (i: number) => {
  const sl = slides[Math.max(0, Math.min(slides.length - 1, i))];
  track.scrollTo({ left: sl.offsetLeft - (track.clientWidth - sl.offsetWidth) / 2, behavior: root.classList.contains('motion') ? 'smooth' : 'auto' });
};
let scrollTick = 0;
track.addEventListener('scroll', () => {
  cancelAnimationFrame(scrollTick);
  scrollTick = requestAnimationFrame(() => setActive(nearest()));
}, { passive: true });
prevShot.addEventListener('click', () => goTo(current - 1));
nextShot.addEventListener('click', () => goTo(current + 1));
track.addEventListener('keydown', e => {
  if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') { e.preventDefault(); goTo(current + (e.key === 'ArrowRight' ? 1 : -1)); }
});
// Mouse drag on desktop (touch and trackpads scroll natively).
let drag: { x: number; left: number } | null = null;
track.addEventListener('pointerdown', e => {
  if (e.pointerType !== 'mouse') return;
  drag = { x: e.clientX, left: track.scrollLeft };
  track.classList.add('dragging');
  track.setPointerCapture(e.pointerId);
});
track.addEventListener('pointermove', e => { if (drag) track.scrollLeft = drag.left - (e.clientX - drag.x); });
const endDrag = () => { if (!drag) return; drag = null; track.classList.remove('dragging'); goTo(nearest()); };
track.addEventListener('pointerup', endDrag);
track.addEventListener('pointercancel', endDrag);
track.addEventListener('dragstart', e => e.preventDefault());
prevShot.disabled = true;

// The year of dots: a sample year up to today. It inks in as you scroll past it, one day after
// another, then stays still. Without motion it is drawn whole at once.
const year = today.getFullYear();
$('#skyYear').textContent = String(year);
let seed = 7;
const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
const journey = [9, 10, 11, 12, 13, 14, 15]; // March: "7 days to slow down", a finished journey
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
      // Only what the released app draws: answers, inner weather, Mirror, photos (squares),
      // letters (diamonds), quiet days, and a journey's constellation.
      kind: +date === +today ? 'today'
        : m === 2 && journey.includes(d) ? 'full'
        : r < .13 ? 'quiet' : r < .25 ? 'weather' + w : r < .28 ? 'mirror' : r < .31 ? 'photo' : r < .315 ? 'letter' : 'full',
    });
  }
}

const canvas = $<HTMLCanvasElement>('#galaxy'), months = $('.galaxy .months');
const sky = { shown: days.length };

function drawSky() {
  const w = canvas.clientWidth, cell = w / 31, h = cell * 12, dpr = window.devicePixelRatio || 1;
  canvas.style.height = h + 'px';
  months.style.height = h + 'px';
  // Each label fits its row: smaller type on narrow screens, and only the initial when a row is
  // too short for three letters (as in the app's Galaxy).
  months.style.fontSize = Math.min(11.2, cell * .78) + 'px';
  months.querySelectorAll<HTMLElement>('span').forEach(m => {
    m.dataset.full ??= m.textContent!;
    m.textContent = cell < 16 ? m.dataset.full[0] : m.dataset.full;
  });
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
    else if (kind === 'mirror') { dot(R * .55, false); dot(R * 1.05, false); }
    else if (kind === 'photo') g.fillRect(x - R * .8, y - R * .8, R * 1.6, R * 1.6);
    else if (kind === 'letter') {
      g.beginPath();
      g.moveTo(x, y - R * 1.1); g.lineTo(x + R * .9, y); g.lineTo(x, y + R * 1.1); g.lineTo(x - R * .9, y);
      g.closePath();
      g.fill();
    }
    else {
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
addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = window.setTimeout(drawSky, 150); });
const systemDark = matchMedia('(prefers-color-scheme: dark)');
systemDark.addEventListener('change', drawSky);

// Light / dark switch in the header: the choice wins over the system and is remembered
// (public/theme.js applies it before the first paint on the next visit).
const themeToggle = $<HTMLButtonElement>('#themeToggle');
const isDark = () => root.dataset.theme ? root.dataset.theme === 'dark' : systemDark.matches;
const labelToggle = () => {
  themeToggle.setAttribute('aria-label', isDark() ? themeToggle.dataset.toLight! : themeToggle.dataset.toDark!);
  document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]').forEach(m => { m.content = css('--paper'); });
};
themeToggle.addEventListener('click', () => {
  const theme = isDark() ? 'light' : 'dark';
  root.dataset.theme = theme;
  try { localStorage.setItem('dotilda-theme', theme); } catch { /* private mode: the choice lasts this visit */ }
  labelToggle();
  drawSky();
});
systemDark.addEventListener('change', labelToggle);
labelToggle();

// Motion.
const mm = gsap.matchMedia();
mm.add('(prefers-reduced-motion: no-preference)', () => {
  root.classList.add('motion', 'motion-ready');
  const calm = { ease: 'power2.out', duration: 1.1 };
  const onEnter = (trigger: gsap.DOMTarget, start = 'top 85%') => ({ trigger, start, once: true });

  // Hero: the title settles word by word, Dotilda pops up, her bubble opens and the question is
  // typed out letter by letter; the field and this month's dots come up. Then, if nobody has
  // started typing, the scene plays once by itself.
  gsap.timeline({ defaults: calm, onComplete: () => { demoTimer = window.setTimeout(playDemo, 900); } })
    .from('.hero-in > .label', { autoAlpha: 0, y: 10 }, .1)
    .from('[data-hero-title] .w', { autoAlpha: 0, y: '0.35em', stagger: .05 }, .2)
    .from('.hero-lede', { autoAlpha: 0, y: 10 }, '-=.6')
    .from('.dotilda', { autoAlpha: 0, scale: .5, y: 20, duration: .8, ease: 'back.out(2.2)' }, '-=.5')
    .from('.ask-who', { autoAlpha: 0, x: -10, duration: .6 }, '-=.3')
    .from('.bubble', { autoAlpha: 0, scale: .9, transformOrigin: '0% 30%', duration: .5, ease: 'back.out(1.8)' }, '-=.4')
    .from('.ask-chars .c', { autoAlpha: 0, duration: .01, stagger: .045, ease: 'none' }, '-=.1')
    .from('.ask-field', { autoAlpha: 0, y: 14 }, '-=.2')
    .from('.month', { autoAlpha: 0, y: 10 }, '-=.8')
    .from('#askNote, .ask-more', { autoAlpha: 0, y: 10, stagger: .15 }, '-=.8');

  // ScrollTriggers are created in page order, the pinned pause first, so that every trigger
  // below it counts the pin's extra scroll.

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

  // Get Dotilda: the Kompakt comes up, then its real screens fade one into the next every few
  // seconds (CSS transition on .on), while it is in view.
  const shots = $$('.screen .shots img');
  let shot = 0;
  const next = () => {
    shots[shot].classList.remove('on');
    shot = (shot + 1) % shots.length;
    shots[shot].classList.add('on');
    turn.restart(true);
  };
  const turn = gsap.delayedCall(4.5, next).pause();
  gsap.from('.kompakt', { autoAlpha: 0, y: 30, ...calm, duration: 1.3, scrollTrigger: onEnter('.kompakt', 'top 80%') });
  ScrollTrigger.create({
    trigger: '.kompakt', start: 'top bottom', end: 'bottom top',
    onToggle: st => (st.isActive ? turn.play() : turn.pause()),
  });

  // Inside the app: the strip of screens slides in from the right.
  gsap.from('[data-anim="page"]', {
    autoAlpha: 0, x: 80, ...calm, duration: 1.2, stagger: .08,
    clearProps: 'transform,opacity,visibility', scrollTrigger: onEnter('.carousel'),
  });

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

  // Try one: the box settles in gently.
  gsap.from('.try', { autoAlpha: 0, y: 16, ...calm, scrollTrigger: onEnter('.try') });

  // Formats: each dot is stamped, then its name appears beside it.
  gsap.timeline({ scrollTrigger: onEnter('.formats') })
    .from('.fmt svg', { scale: 0, transformOrigin: '50% 50%', duration: .6, ease: 'back.out(2.2)', stagger: .09 })
    .from('.fmt b, .fmt span', { autoAlpha: 0, x: -8, ...calm, duration: .8, stagger: .04 }, '<+=.15');

  // Dotilda's lines are written out, left to right, one after the other.
  $$('[data-anim="quote"]').forEach((q, i) => gsap.timeline({ scrollTrigger: onEnter('.quotes', 'top 80%'), delay: i * 1.1 })
    .from(q.querySelector('.voice'), { clipPath: 'inset(0 100% 0 0)', duration: 1.4, ease: 'power1.inOut' })
    .from(q.querySelector('figcaption'), { autoAlpha: 0, duration: .6 }, '-=.3'));

  // The ink card after the hero unfolds as it comes up (its corners open to full width, scrubbed
  // by the scroll); then its three facts arrive one after the other, and the buttons last.
  gsap.fromTo('[data-anim="cta-card"]', { clipPath: 'inset(6% 7% 6% 7% round 3rem)', scale: .97 },
    { clipPath: 'inset(0% 0% 0% 0% round 1.5rem)', scale: 1, ease: 'none', scrollTrigger: { trigger: '.cta', start: 'top 95%', end: 'top 35%', scrub: .6 } });
  gsap.timeline({ scrollTrigger: onEnter('.cta-facts', 'top 85%') })
    .from('[data-anim="cta-fact"]', { autoAlpha: 0, y: 28, duration: .9, ease: 'power3.out', stagger: .18 })
    .from('.cta-n', { autoAlpha: 0, x: -10, duration: .5, stagger: .18 }, '<.1')
    .from('[data-anim="cta-actions"]', { autoAlpha: 0, y: 14, duration: .8, ease: 'power2.out', stagger: .12 }, '-=.4')
    .from('.cta-main', { scale: .9, duration: .6, ease: 'back.out(2.4)' }, '<');

  // Promises settle quietly, the only plain fade on the page.
  gsap.from('[data-anim="promise"]', { autoAlpha: 0, y: 14, ...calm, stagger: .08, scrollTrigger: onEnter('.promises') });

  // FAQ: the questions settle one after the other; feedback buttons follow.
  gsap.from('[data-anim="faq"]', { autoAlpha: 0, y: 12, ...calm, stagger: .07, scrollTrigger: onEnter('.faq') });
  gsap.from('[data-anim="feedback"]', { autoAlpha: 0, y: 12, ...calm, scrollTrigger: onEnter('#feedback .actions') });

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

  return () => { root.classList.remove('motion'); clearTimeout(demoTimer); sky.shown = days.length; drawSky(); };
});
drawSky();

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
