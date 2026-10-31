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
  hero: { sample: string; noted: string; after: string; privacy: string; asks: string; quips: string[] };
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

// Hero: Dotilda asks in her bubble; this month's dots wait below, today empty under its caret.
// Answer, and your words become a drop of ink that lands in today's place. Nothing is sent or
// saved. With motion on, the scene first plays once by itself (unless you start typing), then
// "Your turn" sets it up again with a new question. Dotilda changes face in one step, as in the app.
const heroEl = $('.hero'), monthDots = $('#monthDots');
const ask = $<HTMLFormElement>('#ask'), askInput = $<HTMLInputElement>('#askInput'), askBtn = $<HTMLButtonElement>('#askBtn');
const askQ = $('#askQ'), askNote = $('#askNote'), askWho = $('#askWho'), heroMark = $('.hero-mark');
const poke = $<HTMLButtonElement>('#poke'), yourTurn = $<HTMLButtonElement>('#yourTurn');
const motion = () => root.classList.contains('motion');

// This month, days 1 to today. Answered and quiet days are drawn from a fixed seed, so the row
// looks lived-in and stays the same on every visit.
const dayNow = today.getDate();
$('#monthName').textContent = today.toLocaleDateString(data.dateLocale, { month: 'long' });
{
  let seed = 11;
  const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  const ns = 'http://www.w3.org/2000/svg', step = 10;
  monthDots.setAttribute('viewBox', `0 0 ${31 * step} 20`);
  for (let d = 1; d <= dayNow; d++) {
    const c = document.createElementNS(ns, 'circle');
    const quiet = d < dayNow && rnd() < .2;
    c.setAttribute('cx', String((d - .5) * step));
    c.setAttribute('cy', '6');
    c.setAttribute('r', d === dayNow ? '3.4' : quiet ? '1.2' : '3');
    c.setAttribute('class', d === dayNow ? 'd-today' : quiet ? 'd-quiet' : 'd-ink');
    if (d === dayNow) c.id = 'todayDot';
    monthDots.append(c);
  }
  const caret = document.createElementNS(ns, 'path');
  caret.setAttribute('d', `M${(dayNow - .5) * step} 13l3 4h-6z`);
  caret.setAttribute('class', 'caret');
  monthDots.append(caret);
}
const todayDot = $('#todayDot');

// The question, set letter by letter (screen readers get it whole).
function setQuestion(text: string) {
  askQ.innerHTML = '';
  const sr = document.createElement('span');
  sr.className = 'sr-only';
  sr.textContent = text;
  const chars = document.createElement('span');
  chars.className = 'ask-chars';
  chars.setAttribute('aria-hidden', 'true');
  text.split(' ').forEach((w, i) => {
    const cw = document.createElement('span');
    cw.className = 'cw';
    [...w].forEach(ch => { const c = document.createElement('span'); c.className = 'c'; c.textContent = ch; cw.append(c); });
    chars.append(cw, i < text.split(' ').length - 1 ? ' ' : '');
  });
  askQ.append(sr, chars);
  if (motion()) gsap.from(chars.querySelectorAll('.c'), { autoAlpha: 0, duration: .01, stagger: .04, ease: 'none' });
}

// Poke her: each tap shows another face, in one step; poke on, and she has a word with you.
const pokePoses = ['wink', 'surprised', 'curious', 'sleepy', 'pleased'];
let pokes = 0;
poke.addEventListener('click', () => {
  pokes++;
  heroMark.dataset.pose = pokePoses[(pokes - 1) % pokePoses.length];
  if (pokes >= 3) askWho.textContent = data.hero.quips[(pokes - 3) % data.hero.quips.length];
});

type Phase = 'waiting' | 'demo' | 'flying' | 'shown' | 'yours' | 'done';
let phase: Phase = 'waiting', demo: gsap.core.Timeline | null = null, demoTimer = 0;

function noted() {
  todayDot.classList.add('inked');
  heroMark.dataset.pose = 'pleased';
  ask.classList.add('done');
  const mine = phase !== 'demo';
  phase = mine ? 'done' : 'shown';
  if (mine) { $('#heroGet').classList.add('solid'); askNote.textContent = data.hero.after; }
  else { askNote.textContent = ''; yourTurn.hidden = false; }
  if (motion()) {
    gsap.timeline()
      .to(askQ, { autoAlpha: 0, y: -8, duration: .25, ease: 'power1.in' })
      .call(() => { askQ.textContent = data.hero.noted; ScrollTrigger.refresh(); })
      .fromTo(askQ, { y: 10 }, { autoAlpha: 1, y: 0, duration: .7, ease: 'power3.out' })
      .from([askNote, yourTurn], { autoAlpha: 0, y: 6, duration: .5 }, '-=.3');
    gsap.fromTo(todayDot, { attr: { r: 6 } }, { attr: { r: 3.4 }, duration: .6, ease: 'back.out(3)' });
  } else {
    askQ.textContent = data.hero.noted;
    ScrollTrigger.refresh();
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
  const tr = todayDot.getBoundingClientRect();
  gsap.set(drop, { x: x0 + Math.min(word.offsetWidth, ir.width) / 2, y: y0 + word.offsetHeight / 2, scale: 0 });
  heroMark.dataset.pose = 'neutral'; // she pushes her glasses up to watch it land
  gsap.timeline({ onComplete: () => { drop.remove(); word.remove(); noted(); } })
    .to(word, { scaleX: .04, scaleY: .3, autoAlpha: 0, duration: .65, ease: 'power3.in' })
    .to(drop, { scale: 1, duration: .35, ease: 'back.out(3)' }, '-=.3')
    .to(drop, { x: tr.left - hr.left + tr.width / 2, duration: 1.1, ease: 'power1.inOut' }, '+=.1')
    .to(drop, { y: tr.top - hr.top + tr.height / 2, duration: 1.1, ease: 'back.in(1.4)' }, '<')
    .to(drop, { scale: Math.max(tr.width, 6) / 16, duration: .3, ease: 'power2.in' }, '-=.3');
}

// The scene plays once by itself: a sample answer writes itself, the button is pressed, the drop flies.
function playDemo() {
  if (phase !== 'waiting' || document.activeElement === askInput || askInput.value) return;
  phase = 'demo';
  const sample = data.hero.sample, typing = { n: 0 };
  heroMark.dataset.pose = 'curious';
  demo = gsap.timeline({ onComplete: () => { askBtn.classList.remove('pressed'); flyToToday(sample); } })
    .to(typing, { n: sample.length, duration: sample.length * .04, ease: 'none', onUpdate: () => { askInput.value = sample.slice(0, Math.round(typing.n)); } })
    .call(() => askBtn.classList.add('pressed'), [], '+=.35')
    .to({}, { duration: .3 });
}
// Typing during the scene takes over: it stops, and the field is yours.
const takeOver = () => {
  clearTimeout(demoTimer);
  if (phase === 'demo' && demo) { demo.kill(); demo = null; askInput.value = ''; askBtn.classList.remove('pressed'); heroMark.dataset.pose = 'peering'; phase = 'yours'; }
  else if (phase === 'waiting') phase = 'yours';
};
askInput.addEventListener('pointerdown', takeOver);
askInput.addEventListener('focus', takeOver);

// "Your turn": today is empty again (one step, like an e-ink redraw), a new question, the field.
yourTurn.addEventListener('click', () => {
  phase = 'yours';
  todayDot.classList.remove('inked');
  yourTurn.hidden = true;
  ask.classList.remove('done');
  heroMark.dataset.pose = 'peering';
  askWho.textContent = data.hero.asks;
  askNote.textContent = data.hero.privacy;
  setQuestion(data.questions[1] ?? data.questions[0]);
  askInput.focus();
  ScrollTrigger.refresh();
});

ask.addEventListener('submit', e => {
  e.preventDefault();
  if (phase === 'flying' || phase === 'done' || phase === 'shown' || phase === 'demo') return;
  takeOver();
  askInput.blur();
  const typed = askInput.value.trim();
  phase = 'flying';
  if (!motion()) {
    phase = 'yours';
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
    .from('#poke', { autoAlpha: 0, scale: .5, y: 20, duration: .8, ease: 'back.out(2.2)' }, '-=.5')
    .from('.ask-who', { autoAlpha: 0, x: -10, duration: .6 }, '-=.3')
    .from('.bubble', { autoAlpha: 0, scale: .9, transformOrigin: '0% 30%', duration: .5, ease: 'back.out(1.8)' }, '-=.4')
    .from('.ask-chars .c', { autoAlpha: 0, duration: .01, stagger: .045, ease: 'none' }, '-=.1')
    .from('.ask-field', { autoAlpha: 0, y: 14 }, '-=.2')
    .from('.month', { autoAlpha: 0, y: 10 }, '-=.8')
    .from('#askNote, .hero-foot', { autoAlpha: 0, y: 10, stagger: .15 }, '-=.8');

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
