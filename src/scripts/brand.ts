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
