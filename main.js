// Dotilda landing page. Three small things, none of them animated:
// the month line on the device, "Try one", the galaxy, plus the latest release from GitHub.
// Every visible string comes from the HTML (data-* attributes), so a translated page needs no JS change.
(function () {
  const $ = id => document.getElementById(id);
  const css = name => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Month line on the device: this month up to today, a few quiet days, today outlined.
  const quiet = new Set([5, 6, 13, 21, 22]);
  const line = $('monthLine');
  for (let d = 1; d <= today.getDate(); d++) {
    const i = document.createElement('i');
    if (d === today.getDate()) i.className = 't';
    else if (quiet.has(d)) i.className = 'e';
    line.appendChild(i);
  }

  // Try one: three changes, then only the inner weather is offered, as in the app.
  const questions = JSON.parse($('questions').textContent);
  const q = $('tryQ'), btn = $('tryBtn'), meta = $('tryMeta');
  const seen = new Set([q.textContent]);
  let left = 3;
  btn.addEventListener('click', () => {
    const fresh = questions.filter(x => !seen.has(x));
    const next = fresh[Math.floor(Math.random() * fresh.length)];
    seen.add(next);
    q.textContent = next;
    left--;
    if (left > 0) {
      btn.textContent = btn.dataset.left.replace('{n}', left);
    } else {
      btn.hidden = true;
      meta.textContent = meta.dataset.done;
    }
  });

  // The galaxy: a sample year up to today. Drawn once per resize or theme change, never animated.
  const year = today.getFullYear();
  $('skyYear').textContent = year;
  let seed = 7;
  const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
  const journey = [9, 10, 11, 12, 13, 14, 15]; // March, "7 days to slow down"
  const days = [];
  for (let m = 0; m < 12; m++) {
    const length = new Date(year, m + 1, 0).getDate();
    days[m] = [];
    for (let d = 1; d <= 31; d++) {
      const date = new Date(year, m, d);
      const r = rnd(), w = Math.floor(rnd() * 5);
      if (d > length || date > today) days[m].push(null);
      else if (+date === +today) days[m].push('today');
      else if (m === 2 && journey.includes(d)) days[m].push('full');
      else days[m].push(r < .13 ? 'quiet' : r < .25 ? 'weather' + w : r < .29 ? 'photo' : r < .31 ? 'mirror' : r < .32 ? 'letter' : 'full');
    }
  }

  const canvas = $('galaxy'), months = document.querySelector('.galaxy .months');
  function draw() {
    const w = canvas.clientWidth, cell = w / 31, h = cell * 12, dpr = window.devicePixelRatio || 1;
    canvas.style.height = h + 'px';
    months.style.height = h + 'px';
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    const g = canvas.getContext('2d');
    g.setTransform(dpr, 0, 0, dpr, 0, 0);
    const ink = css('--ink'), ghost = css('--ghost'), todayColor = css('--today');
    const R = Math.min(cell * .32, 7);
    const at = (m, d) => [d * cell + cell / 2, m * cell + cell / 2];

    if (today >= new Date(year, 2, 16)) {
      g.strokeStyle = ink;
      g.lineWidth = 1;
      g.beginPath();
      journey.forEach((d, i) => { const [x, y] = at(2, d - 1); i ? g.lineTo(x, y - R * 1.9) : g.moveTo(x, y - R * 1.9); });
      g.stroke();
    }

    days.forEach((row, m) => row.forEach((t, d) => {
      if (!t) return;
      const [x, y] = at(m, d);
      g.fillStyle = ink;
      g.strokeStyle = ink;
      g.lineWidth = Math.max(1, R * .28);
      const dot = (r, fill) => { g.beginPath(); g.arc(x, y, r, 0, Math.PI * 2); fill ? g.fill() : g.stroke(); };
      if (t === 'quiet') { g.fillStyle = ghost; dot(R * .35, true); }
      else if (t === 'today') { g.fillStyle = todayColor; dot(R, true); }
      else if (t === 'full') dot(R, true);
      else if (t === 'photo') { dot(R * .55, true); dot(R * 1.05, false); }
      else if (t === 'mirror') { dot(R * .55, false); dot(R * 1.05, false); }
      else if (t === 'letter') {
        g.beginPath();
        g.moveTo(x, y - R * 1.1); g.lineTo(x + R, y); g.lineTo(x, y + R * 1.1); g.lineTo(x - R, y);
        g.closePath();
        g.fill();
      } else {
        // Inner weather as a moon phase: 0 sunny (full) .. 4 stormy (ring).
        const p = +t.slice(7), r = R * .85;
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
    }));
  }
  draw();
  let timer;
  addEventListener('resize', () => { clearTimeout(timer); timer = setTimeout(draw, 150); });
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', draw);

  // Latest release, live from GitHub. Without it, the button still opens the releases page.
  const dl = $('dl'), release = $('release');
  fetch('https://api.github.com/repos/kxrz/dotilda-app/releases/latest', { headers: { Accept: 'application/vnd.github+json' } })
    .then(r => r.ok ? r.json() : Promise.reject(r.status))
    .then(rel => {
      const apk = (rel.assets || []).find(a => a.name.endsWith('.apk'));
      const v = rel.tag_name.replace(/^v/i, '');
      dl.textContent = dl.dataset.label.replace('{v}', v);
      if (apk) dl.href = apk.browser_download_url;
      release.textContent = release.dataset.line
        .replace('{v}', v)
        .replace('{date}', new Date(rel.published_at).toLocaleDateString(release.dataset.locale, { day: 'numeric', month: 'long', year: 'numeric' }))
        .replace('{size}', apk ? (apk.size / 1048576).toFixed(1) : '–');
    })
    .catch(() => {});
})();
