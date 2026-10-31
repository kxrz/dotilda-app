// /brand: the light / dark switch (as on the landing page) and tap-to-copy colour codes.
import { inject } from '@vercel/analytics';

inject();

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

const said = document.getElementById('copied')!;
document.querySelectorAll<HTMLButtonElement>('.swatch').forEach(b => b.addEventListener('click', async () => {
  const hex = b.dataset.hex!;
  try { await navigator.clipboard.writeText(hex); } catch { return; }
  const code = b.querySelector('code')!;
  code.textContent = 'Copied';
  said.textContent = `${hex} copied`;
  setTimeout(() => { code.textContent = hex; }, 1200);
}));
