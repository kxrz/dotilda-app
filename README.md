# dotilda.com

The landing page for [Dotilda](https://github.com/kxrz/dotilda-app), the gentle daily journaling
app for e-ink. An app by readme.club, made and maintained by LesWWW.

This `website` branch holds only the site; the app lives on `main`. Vercel builds and deploys this
branch to production on every push.

## Stack

[Astro](https://astro.build) (static output) and [GSAP](https://gsap.com) with ScrollTrigger.
No database, no cookies, no outside fonts. Visits are counted with Vercel Web Analytics
(anonymous page views, no cookies), served from this domain.

```bash
npm install
npm run dev      # http://localhost:4321
npm run build    # static site in dist/
npm run preview  # serve dist/, then in another terminal:
npm run check:fit  # every language × 13 screens, Kompakt included: no clipped text, hero fits
```

Run `check:fit` after any change to copy, type sizes or layout (it needs Playwright's Chromium:
`npx playwright install chromium`). Big type is sized by width *and* height (`svh`), so the hero
question and its field share the first screen, even on a phone in landscape.

| Path | Role |
| --- | --- |
| `src/i18n/<code>.json` | Every word on the page, one file per language, including the "Try one" questions. |
| `src/i18n/index.ts` | Finds the language files, builds the URLs, falls back to English for missing keys. |
| `src/pages/[...lang].astro` | One page per language: English at `/`, the others at `/<code>`. |
| `src/components/Landing.astro` | The page itself (AIDA: hero, ritual, sky, get it). |
| `src/components/DotildaMark.astro` | Dotilda as drawn in the app, with her four poses. |
| `src/scripts/landing.ts` | Month line, "Try one", the year of dots, latest release, motion. |
| `src/styles/global.css` | Tokens, self-hosted fonts, layout. |
| `public/` | Fonts (SIL OFL, see `public/fonts/README.md`), icons, `og.png`, `motion.js`. |
| `vercel.json` | Build settings, strict CSP, caching. |
| `src/pages/sitemap.xml.ts`, `llms.txt.ts` | Sitemap, and a plain-text summary for AI answer engines. |

The only outside request is to `api.github.com` for the latest release (version, date, size,
release notes link). The download button points to `releases/latest/download/dotilda.apk`,
published by the app's release workflow, so it works even without that request.

## Languages

The site follows the app's languages: when the app gets `res/values-<code>/strings.xml`,
the site gets `src/i18n/<code>.json`.

1. Copy `src/i18n/en.json` to `src/i18n/<code>.json` (e.g. `fr.json`) and translate it.
   Set `language` to the language's own name and `dateLocale` (e.g. `fr-FR`).
   Take the `questions` from the app's translated bank.
2. That's it: the build adds `/<code>`, the language links in the header, the `hreflang` tags
   and the sitemap entry. Keys you leave out show in English until translated.

Languages today: English (`/`), French (`/fr`), Spanish (`/es`), Portuguese (`/pt`, Brazilian
Portuguese) and Polish (`/pl`).

While the app itself is in English only, each translated file sets `appLanguage` (a short notice
shown in the hero and next to the download steps, with an `EN` tag); English leaves it empty.
The Kompakt mockup shows the app's real screen, so its texts (`get.device`) stay in English:
translate only `get.device.aria`. When the app gets a language, empty that language's
`appLanguage` and translate `get.device`.

Placeholders such as `{n}`, `{v}`, `{date}` and `{size}` must be kept as they are.
Fonts ship with the Latin and Latin Extended subsets (Polish, Czech, Romanian...); the browser
fetches Latin Extended only on pages that need it. A language in another script (Greek, Cyrillic)
needs its subset added in `public/fonts` and `src/styles/global.css`.

## Motion

Slow and varied, each gesture borrowed from e-ink or from the app, never a plain fade everywhere:

| Where | What moves |
| --- | --- |
| Hero | The year comes up as a faint sky of dots; Dotilda types her question; your answer (or her example) becomes a drop of ink that lands on today's dot. Nothing is sent or saved. |
| Pause | One huge sentence, pinned while you scroll, inks in word by word; Dotilda smiles at the end. |
| Labels | Typed one character at a time, in their monospace. |
| Headings | Rise word by word out of a mask (`components/Words.astro`). |
| Ritual | Each rule is drawn, then its step is refreshed top to bottom. |
| Try one | The box arrives with an e-ink blink; Dotilda changes pose as you ask. |
| Formats | Each dot is stamped, then named. |
| Your sky | The year inks in day by day with the scroll; Dotilda's lines are written out. |
| Get Dotilda | Steps slide in from their numbers; the Kompakt (drawn after the real phone) does a full e-ink refresh. |

Dotilda changes pose in one step, as in the app, never with a tween.

People who ask for reduced motion get the complete page, still. `public/motion.js` hides the
animated elements before first paint only for everyone else, and shows them again after two
seconds if the script never runs.

## Rules

Pure ink on paper. `#D1613E` (readme.club terracotta) appears once, for today's dot.
LesWWW lime appears only under the LesWWW logo. No inline scripts or styles (the CSP forbids them):
pass data to the browser through `#page-data`.

## Vercel setup

- Production branch: `website`
- Framework preset: Astro (read from `vercel.json`: `npm run build`, output `dist`)
- Domains: `www.dotilda.com` is the main address (canonical, sitemap, share image);
  `dotilda.com` redirects to it. Changing this means changing `site` in `astro.config.mjs`.
