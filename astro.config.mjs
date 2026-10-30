import { defineConfig } from 'astro/config';

export default defineConfig({
  site: 'https://dotilda.com',
  trailingSlash: 'never',
  build: {
    format: 'directory',
    // Everything ships as files: the CSP allows no inline style or script.
    inlineStylesheets: 'never',
  },
  vite: { build: { assetsInlineLimit: 0 } },
});
