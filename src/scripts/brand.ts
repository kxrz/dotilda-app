// /brand: the light / dark switch and tap-to-copy colour codes.
import { inject } from '@vercel/analytics';
import './theme-toggle';

inject();

const said = document.getElementById('copied')!;
document.querySelectorAll<HTMLButtonElement>('.swatch').forEach(b => b.addEventListener('click', async () => {
  const hex = b.dataset.hex!;
  try { await navigator.clipboard.writeText(hex); } catch { return; }
  const code = b.querySelector('code')!;
  code.textContent = 'Copied';
  said.textContent = `${hex} copied`;
  setTimeout(() => { code.textContent = hex; }, 1200);
}));

// The LinkedIn post: copy its text in one tap.
document.querySelectorAll<HTMLButtonElement>('.copy-post').forEach(b => b.addEventListener('click', async () => {
  const text = document.getElementById(b.dataset.target!)!.textContent!;
  try { await navigator.clipboard.writeText(text); } catch { return; }
  const label = b.textContent;
  b.textContent = b.dataset.done!;
  setTimeout(() => { b.textContent = label; }, 1500);
}));
