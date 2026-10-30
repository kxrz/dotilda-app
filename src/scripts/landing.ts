// The page's behaviour: the device's month line, "Try one", the year of dots, the latest release,
// and the motion. Motion is slow, short and only for people who accept it
// (prefers-reduced-motion); without it every element is simply there.
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

type PageData = {
  dateLocale: string;
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
addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = window.setTimeout(drawSky, 150); });
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', drawSky);

// Motion.
const mm = gsap.matchMedia();
mm.add('(prefers-reduced-motion: no-preference)', () => {
  root.classList.add('motion', 'motion-ready');
  const calm = { ease: 'power2.out', duration: 1.1 };

  // Hero: the words settle, then the Kompakt does a full e-ink refresh before showing its page.
  const hero = gsap.timeline({ defaults: calm });
  hero
    .from('[data-hero-title] .w', { autoAlpha: 0, y: '0.35em', stagger: .06 })
    .from('[data-reveal="hero"]', { autoAlpha: 0, y: 14, stagger: .12 }, '-=.7')
    .from('[data-reveal="device"]', { autoAlpha: 0, y: 24, duration: 1.4 }, '-=1.1')
    .set('.screen > :not(.flash)', { autoAlpha: 0 }, '<')
    .set('.flash', { autoAlpha: 1, backgroundColor: '#111' }, '-=.2')
    .set('.flash', { backgroundColor: '#F4F3EE' }, '+=.09')
    .set('.flash', { autoAlpha: 0 }, '+=.09')
    .set('.screen > :not(.flash)', { autoAlpha: 1 })
    .from('#monthLine i', { autoAlpha: 0, duration: .01, stagger: .035 }, '+=.1');

  // Sections: a gentle rise as they come into view, once.
  const rise = (targets: string | HTMLElement[], vars: gsap.TweenVars = {}) =>
    gsap.utils.toArray<HTMLElement>(targets).forEach(el =>
      gsap.from(el, { autoAlpha: 0, y: 18, ...calm, ...vars, scrollTrigger: { trigger: el, start: 'top 88%', once: true } }));

  rise('section [data-reveal]:not([data-reveal="step"]):not([data-reveal="fmt"]):not([data-reveal="quote"]):not([data-reveal="promise"]):not([data-reveal="how"])');

  for (const [selector, stagger] of [['step', .18], ['fmt', .07], ['quote', .25], ['promise', .08], ['how', .15]] as const) {
    const items = $$(`[data-reveal="${selector}"]`);
    if (!items.length) continue;
    gsap.from(items, {
      autoAlpha: 0, y: 16, ...calm, stagger,
      scrollTrigger: { trigger: items[0], start: 'top 88%', once: true },
    });
  }
  gsap.from('.step .rule', {
    scaleX: 0, transformOrigin: 'left center', duration: 1.4, ease: 'power3.inOut', stagger: .18,
    scrollTrigger: { trigger: '.cols', start: 'top 88%', once: true },
  });

  // The year inks in with the scroll, day after day.
  sky.shown = 0;
  gsap.to(sky, {
    shown: days.length, ease: 'none',
    onUpdate: drawSky,
    scrollTrigger: { trigger: '.galaxy', start: 'top 85%', end: 'bottom 55%', scrub: .8 },
  });

  return () => { root.classList.remove('motion'); sky.shown = days.length; drawSky(); };
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
