// A plain-text summary for AI answer engines (llmstxt.org), built from the English page copy.
import type { APIRoute } from 'astro';
import { locales, pathFor, t } from '../i18n';

export const GET: APIRoute = ({ site }) => {
  const s = t('en');
  const repo = 'https://github.com/kxrz/dotilda-app';
  const text = `# Dotilda

> ${s.meta.appDescription}

Dotilda is an app by readme.club, made and maintained by LesWWW. "Dotilda" is also the guide
character who signs each question: a kind Victorian governess, an ink dot with a bun and round
glasses, a little mischievous, never guilt-tripping.

## How it works

${s.ritual.steps.map(st => `- ${st.title}: ${st.text}`).join('\n')}
- Another question: up to 3 times a day, then only the inner weather is offered.

## Eight ways to answer

${s.shapes.formats.map(f => `- ${f.name}: ${f.text}${'soon' in f && f.soon ? ' (coming soon)' : ''}`).join('\n')}

## Promises

${s.promises.map(p => `- ${p.title}: ${p.text}`).join('\n')}
- ${s.sky.title} ${s.sky.lede}

## Status

${s.get.beta}
${s.coming.label}: ${s.coming.text}

## Get it

- Download the latest APK: ${repo}/releases/latest/download/dotilda.apk
- Release notes: ${repo}/releases/latest
- Install with Mudita Center on the Mudita Kompakt, or Obtainium on any Android 11+ e-reader.
- Price: free, no purchase, no subscription, no ads. License: Apache 2.0.

## Links

- Website: ${locales.map(l => site!.origin + pathFor(l)).join(', ')}
- Source code: ${repo}
- readme.club: https://readme.club
- LesWWW: https://www.leswww.com
`;
  return new Response(text, { headers: { 'Content-Type': 'text/plain; charset=utf-8' } });
};
