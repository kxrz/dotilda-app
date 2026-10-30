# dotilda.com

The landing page for [Dotilda](https://github.com/kxrz/dotilda-app), the gentle daily journaling
app for e-ink. An app by readme.club, made and maintained by LesWWW.

This `website` branch holds only the site; the app lives on `main`. Vercel builds and deploys this
branch to production on every push.

## Stack

[Astro](https://astro.build) (static output) and [GSAP](https://gsap.com) with ScrollTrigger.
No database, no cookies, no analytics, no outside fonts.

```bash
npm install
npm run dev      # http://localhost:4321
npm run build    # static site in dist/
```

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

Placeholders such as `{n}`, `{v}`, `{date}` and `{size}` must be kept as they are.
Fonts ship with the Latin subset only; a language in another script needs its subset added in
`public/fonts` and `src/styles/global.css`.

## Motion

Slow, short and rare, in the spirit of e-ink: the hero settles in, the Kompakt does a full
refresh before showing its page, sections rise as they appear, and the year of dots inks in
day by day as you scroll. Dotilda changes pose in one step, as in the app, never with a tween.

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
- Domain: `dotilda.com`, with `www.dotilda.com` redirecting to it
