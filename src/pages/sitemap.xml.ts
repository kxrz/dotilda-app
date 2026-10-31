import type { APIRoute } from 'astro';
import { locales, pathFor } from '../i18n';

export const GET: APIRoute = ({ site }) => {
  const urls = [...locales.map(pathFor), '/brand', '/stats'].map(path => `  <url><loc>${site!.origin}${path}</loc><lastmod>${new Date().toISOString().slice(0, 10)}</lastmod></url>`).join('\n');
  return new Response(
    `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`,
    { headers: { 'Content-Type': 'application/xml' } },
  );
};
