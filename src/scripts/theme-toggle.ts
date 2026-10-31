// The header's light / dark switch on the inner pages (/brand, /stats). The choice wins over the
// system and is remembered; public/theme.js applies it before the first paint on the next visit.
const root = document.documentElement;
const systemDark = matchMedia('(prefers-color-scheme: dark)');
const toggle = document.getElementById('themeToggle') as HTMLButtonElement;
const isDark = () => root.dataset.theme ? root.dataset.theme === 'dark' : systemDark.matches;
const label = () => {
  toggle.setAttribute('aria-label', isDark() ? toggle.dataset.toLight! : toggle.dataset.toDark!);
  const paper = getComputedStyle(root).getPropertyValue('--paper').trim();
  document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]').forEach(m => { m.content = paper; });
};
toggle.addEventListener('click', () => {
  const theme = isDark() ? 'light' : 'dark';
  root.dataset.theme = theme;
  try { localStorage.setItem('dotilda-theme', theme); } catch { /* private mode: the choice lasts this visit */ }
  label();
});
systemDark.addEventListener('change', label);
label();
