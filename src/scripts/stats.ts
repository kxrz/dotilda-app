// /stats: reads GitHub Releases' public counters and draws the totals, the bars and the table.
// No analytics here. GitHub allows 60 unauthenticated reads an hour per address.
import './theme-toggle';

type Asset = { name: string; download_count: number };
type Release = { tag_name: string; published_at: string; prerelease: boolean; draft: boolean; assets: Asset[] };
type Row = { version: string; date: Date; apk: number; checks: number };

const API = 'https://api.github.com/repos/kxrz/dotilda-app/releases?per_page=100';
const $ = (id: string) => document.getElementById(id)!;
const num = new Intl.NumberFormat('en-GB');
const day = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
const stamp = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZoneName: 'short' });

// dotilda.apk (people and the in-app updater) + dotilda-x.y.z.apk (manual downloads); manifest.json.
const isApk = (n: string) => n === 'dotilda.apk' || /^dotilda-.+\.apk$/.test(n);
const sum = (assets: Asset[], keep: (n: string) => boolean) =>
  assets.filter(a => keep(a.name)).reduce((total, a) => total + a.download_count, 0);

function toRows(releases: Release[]): Row[] {
  return releases
    .filter(r => !r.draft && !r.prerelease && !r.tag_name.includes('-'))
    .map(r => ({
      version: r.tag_name,
      date: new Date(r.published_at),
      apk: sum(r.assets, isApk),
      checks: sum(r.assets, n => n === 'manifest.json'),
    }))
    .sort((a, b) => b.date.getTime() - a.date.getTime());
}

function el<K extends keyof HTMLElementTagNameMap>(tag: K, text = '', cls = '') {
  const e = document.createElement(tag);
  if (text) e.textContent = text;
  if (cls) e.className = cls;
  return e;
}

function render(rows: Row[]) {
  $('totalApk').textContent = num.format(rows.reduce((t, r) => t + r.apk, 0));
  $('totalChecks').textContent = num.format(rows.reduce((t, r) => t + r.checks, 0));
  $('versions').textContent = num.format(rows.length);
  $('latest').textContent = rows.length ? `Latest: ${rows[0].version}, ${day.format(rows[0].date)}.` : 'None published yet.';

  // One bar per version, newest first, as in the table: a thin ink line ending in a dot.
  const max = Math.max(1, ...rows.map(r => r.apk));
  const svgNS = 'http://www.w3.org/2000/svg';
  $('bars').replaceChildren(...rows.map(r => {
    const li = el('li');
    li.append(el('span', r.version, 'st-v'));
    const svg = document.createElementNS(svgNS, 'svg');
    svg.setAttribute('viewBox', '0 0 100 10');
    svg.setAttribute('preserveAspectRatio', 'none');
    svg.setAttribute('aria-hidden', 'true');
    const w = (r.apk / max) * 100;
    const track = document.createElementNS(svgNS, 'rect');
    track.setAttribute('class', 'track');
    track.setAttribute('x', '0'); track.setAttribute('y', '4.5'); track.setAttribute('width', '100'); track.setAttribute('height', '1');
    const bar = document.createElementNS(svgNS, 'rect');
    bar.setAttribute('class', 'bar');
    bar.setAttribute('x', '0'); bar.setAttribute('y', '2'); bar.setAttribute('width', String(w)); bar.setAttribute('height', '6');
    svg.append(track, bar);
    li.append(svg, el('span', num.format(r.apk), 'st-n'));
    li.setAttribute('aria-label', `${r.version}: ${num.format(r.apk)} APK downloads`);
    return li;
  }));

  $('rows').replaceChildren(...rows.map(r => {
    const tr = el('tr');
    const th = el('th', r.version);
    th.setAttribute('scope', 'row');
    const time = el('time', day.format(r.date));
    time.setAttribute('datetime', r.date.toISOString());
    const td = el('td');
    td.append(time);
    tr.append(th, td, el('td', num.format(r.apk), 'num'), el('td', num.format(r.checks), 'num'));
    return tr;
  }));
}

function fail(message: string) {
  const status = $('status');
  status.textContent = message + ' ';
  const again = el('button', 'Try again', 'btn');
  again.type = 'button';
  again.addEventListener('click', load);
  status.append(again);
  status.classList.add('st-error');
}

async function load() {
  const status = $('status');
  status.classList.remove('st-error');
  status.textContent = 'Reading GitHub…';
  let res: Response;
  try {
    res = await fetch(API, { headers: { Accept: 'application/vnd.github+json' } });
  } catch {
    fail('GitHub could not be reached. Check the connection.');
    return;
  }
  if (!res.ok) {
    const reset = Number(res.headers.get('x-ratelimit-reset'));
    const limited = res.status === 403 || res.status === 429 || res.headers.get('x-ratelimit-remaining') === '0';
    fail(limited
      ? `GitHub allows 60 reads an hour from one address, and they are used up.${reset ? ` Try again after ${new Date(reset * 1000).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}.` : ''}`
      : `GitHub did not answer (error ${res.status}).`);
    return;
  }
  render(toRows(await res.json()));
  status.hidden = true;
  $('data').hidden = false;
  $('readAt').textContent = `Read from GitHub on ${stamp.format(new Date())}.`;
}

load();
