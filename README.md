# dotilda.com

The website of [Dotilda](https://github.com/kxrz/dotilda-app), a gentle daily journaling app for
the Mudita Kompakt and Android 11+ e-readers. An app by [readme.club](https://readme.club), made
and maintained by [LesWWW](https://www.leswww.com).

One page, in five languages, that lets visitors live the app's ritual for a few seconds (Dotilda
asks, you answer, a dot lands in your sky) and then sends them to the APK.

This `website` branch holds only the site and shares no history with the app on `main`.

## Quick start

```bash
npm install
npm run dev        # http://localhost:4321
npm run build      # static site in dist/
npm run preview    # serve dist/ on http://localhost:4321
npm run check:fit  # with preview running: every language on 13 screens (see Checks)
npm run og         # redraw the share images (see Share images)
```

`check:fit` and `og` use Playwright's Chromium: run `npx playwright install chromium` once.

## Stack

- [Astro 5](https://astro.build), static output. No framework on the client, no build-time data.
- [GSAP](https://gsap.com) with ScrollTrigger for the motion.
- [Vercel Web Analytics](https://vercel.com/docs/analytics) for anonymous page views.
- Self-hosted fonts. No database, no cookies, no outside fonts or scripts.

## What is where

| Path | Role |
| --- | --- |
| `src/i18n/<code>.json` | Every word on the page, one file per language, "Try one" questions included. |
| `src/i18n/index.ts` | Finds the language files, builds the URLs, falls back to English for missing keys. |
| `src/pages/[...lang].astro` | One page per language: English at `/`, the others at `/<code>`. |
| `src/layouts/Layout.astro` | `<head>`: title, description, canonical, `hreflang`, share tags, JSON-LD. |
| `src/components/Landing.astro` | The page itself, top to bottom (see below). |
| `src/components/DotildaMark.astro` | Dotilda as drawn in the app (`ui/DotildaMark.kt`), with her four poses. |
| `src/components/Words.astro` | Splits a heading into words for the motion; screen readers get the sentence whole. |
| `src/scripts/landing.ts` | The hero sky and ink drop, the month line, "Try one", the year of dots, the latest release, the motion, analytics. |
| `src/styles/global.css` | Tokens, `@font-face`, layout. |
| `src/pages/sitemap.xml.ts` | Sitemap, one entry per language. |
| `src/pages/llms.txt.ts` | `/llms.txt`, a plain-text summary for AI answer engines, built from `en.json`. |
| `src/assets/leswww-logo.svg` | LesWWW wordmark, from the LesWWW brand repo. |
| `public/fonts/` | WOFF2 files, SIL OFL (see `public/fonts/README.md`). |
| `public/app/*.png` | Real screens of the released app, shown in the Kompakt and in "Inside the app". |
| `public/og/<code>.png` | Share image of each language, drawn by `scripts/og.mjs`. |
| `public/theme.js` | Runs before first paint: applies the light or dark theme chosen in the header. |
| `public/motion.js` | Runs before first paint; see Motion. |
| `scripts/check-fit.mjs` | The fit check (`npm run check:fit`). |
| `scripts/og.mjs` | The share image generator (`npm run og`). |
| `vercel.json` | Build settings, security headers (strict CSP), caching. |

The page follows AIDA, in this order:

| | Section | What it does |
| --- | --- | --- |
| Attention | hero | Dotilda asks; the visitor answers and watches a dot land in the year. |
| Interest | `#ritual` | The ritual in three steps. |
| Interest | `#inside` | Eight real screens of the released app. |
| Interest | `#shapes` | The eight ways to answer, and "Try one" for a few more questions. |
| Desire | `.pause` | One huge sentence, to slow down. |
| Desire | `#sky` | The year of dots, Dotilda's voice, six promises, what comes next. |
| Action | `#get` | Beta status, three ways to install (Kompakt, other e-readers, Obtainium), download, the Kompakt. |
| Action | `#faq` | The questions people ask before installing (also in JSON-LD as `FAQPage`). |
| Action | `#feedback` | Where beta testers talk back: GitHub issues, or a message on Reddit. |

## Content and languages

All copy lives in `src/i18n/`. Change a word there, never in the components.

**Show only what the released app does.** The site recruits beta testers: what they read must be
what they install. Features still in development are tagged `soon` (`shapes.formats[].soon`) or
listed under `coming`; Dotilda's quotes are real lines from the app's `strings.xml`. When a
release ships a feature, remove its `soon` tag, update `coming`, and refresh the screens.
Languages today: English (`/`), French (`/fr`), Spanish (`/es`), Portuguese (`/pt`, Brazilian)
and Polish (`/pl`).

The site follows the app's languages: when the app gets `res/values-<code>/strings.xml`, the site
gets `src/i18n/<code>.json`. To add one:

1. Copy `src/i18n/en.json` to `src/i18n/<code>.json` and translate it. Set `language` to the
   language's own name, `dateLocale` (e.g. `fr-FR`) and `meta.ogLocale` (e.g. `fr_FR`).
   Take the `questions` from the app's translated bank.
2. Run `npm run og` and update `meta.ogImageAlt` to describe the new image.
3. Run `npm run build`, `npm run preview` and `npm run check:fit`.

The build adds `/<code>`, the language links in the header, the `hreflang` tags and the sitemap
entry. Keys you leave out show in English until translated.

- Keep placeholders such as `{n}`, `{v}`, `{date}` and `{size}` as they are.
- Feedback links live in `Landing.astro` (`feedback`): GitHub issues and a Reddit profile to
  message (a link is hidden while its URL is empty). The Obtainium button opens `obtainium://add/<repo>` through Obtainium's
  redirect page.
- **The app is in English for now.** Each translated file sets `appLanguage`, a short notice shown
  with an `EN` tag in the hero and next to the download steps; English leaves it empty.
  The Kompakt mockup shows the app's real screen, so `get.device` stays in English (translate
  only `get.device.aria`). When the app speaks a language, empty its `appLanguage` and translate
  its `get.device`.
- Voice: Dotilda is a kind Victorian governess, a little mischievous, never guilt-tripping.
  Short sentences, no exclamation marks in a row. French uses "vous"; Spanish "tú"; Portuguese
  "você"; Polish the informal "Ty", without past-tense forms that would assume a gender.
- Fonts ship with the Latin and Latin Extended subsets; the browser fetches Latin Extended only
  on pages that need it (Polish). A language in another script (Greek, Cyrillic) needs its
  subset added in `public/fonts/`, `src/styles/global.css` and `scripts/og.mjs`.

## Real screens

`public/app/` holds screenshots of the released app, made by the app repository's Screenshots
workflow (branch `screenshots/main`, 479×750, the Kompakt's app area). They are converted to 16
greys, as on the e-ink screen. To refresh them after a release:

```bash
git fetch origin screenshots/main
for f in 03-today:today 17-journeys:journeys 25-done-constellation:constellation 12-galaxy:galaxy \
         18-review-weather:review 20-answer-letter:letter 21-answer-photo:photo 13-spira-breathe:spira; do
  git show origin/screenshots/main:${f%%:*}.png > /tmp/${f##*:}.png
done
python3 -c "from PIL import Image; import sys
for n in sys.argv[1:]: Image.open(f'/tmp/{n}.png').convert('L').quantize(colors=16).save(f'public/app/{n}.png', optimize=True)" today journeys constellation galaxy review letter photo spira
```

Use only screens from `screenshots/main` (the released app), never from a feature branch.

## Design

The page is a sheet of e-ink paper: ink on a warm off-white, one reading column, the year of dots
as the only image.

- Colours are tokens at the top of `global.css`, with a dark theme (night ink). The page follows
  the system until the header's moon / sun button picks light or dark; the choice is kept in
  `localStorage` (`dotilda-theme`) and applied before the first paint by `public/theme.js`.
  `#D1613E`, readme.club's terracotta, appears once: today's dot. LesWWW's lime appears only
  under the LesWWW logo, following the LesWWW brand rule (lime = action).
- Type: Instrument Serif for headings and Dotilda's voice, Geist for text, Space Mono for labels,
  IBM Plex Mono and the LesWWW wordmark for the LesWWW credit, Space Grotesk for "readme.club".
- Big type is sized by width *and* height (`min(vw, svh)`), so Dotilda's question and the answer
  field share the first screen everywhere, a phone in landscape and the Kompakt included.
- The Kompakt is drawn after the real phone: black slab, speaker and camera, side buttons,
  status bar, white back / home / menu keys, with real screens of the app inside. It keeps its
  own colours in both themes.

## Motion

Slow and varied, each gesture borrowed from e-ink or from the app:

| Where | What moves |
| --- | --- |
| Hero | The year comes up as a faint sky of dots; Dotilda types her question; your answer (or her example) becomes a drop of ink that lands on today's dot. Nothing is sent or saved. |
| Pause | One huge sentence, pinned while you scroll, inks in word by word; Dotilda smiles at the end. |
| Labels | Typed one character at a time, in their monospace. |
| Headings | Rise word by word out of a mask (`Words.astro`). |
| Ritual | Each rule is drawn, then its step is refreshed top to bottom. |
| Try one | The box arrives with an e-ink blink; Dotilda changes pose as you ask. |
| Formats | Each dot is stamped, then named. |
| Your sky | The year inks in day by day with the scroll; Dotilda's lines are written out. |
| Inside the app | Each real screen arrives with its own e-ink blink. |
| FAQ | Questions settle one after the other. |
| Get Dotilda | Install ways slide in from their numbers; the Kompakt turns through real screens (Today, a finished journey, Galaxy, month review) every 4.5 s, each with a full e-ink refresh, only while it is in view. |

- Dotilda changes pose in one step, as in the app, never with a tween.
- Reduced motion: people who ask for it get the complete page, still, and the hero still works.
- `public/motion.js` hides what GSAP reveals before first paint, only for people who accept
  motion, and shows it again after two seconds if the script never runs.
- ScrollTriggers are created in page order, the pinned pause first, so every trigger below it
  counts the pin's extra scroll.
- The pause is pinned with `anticipatePin`, and ScrollTrigger measures again once the fonts are
  in and when the hero text changes, so the pin never jumps.

## Privacy

The page sets no cookies and stores nothing. What it asks of the outside world:

- `api.github.com`, once per visit, for the latest release: version, date, APK size, release
  notes link. If it fails, the download button still points to
  `releases/latest/download/dotilda.apk`, which the app's release workflow publishes.
- Vercel Web Analytics: anonymous page views, no cookies, served from this domain
  (`/_vercel/insights`).

What a visitor types in the hero never leaves the page. Keep the footer line ("counts visits
anonymously, and that is all") true: any new tracking needs that line, and this section, changed.

## Search, AI engines and sharing

- `Layout.astro` sets, per language: title, description, canonical, `hreflang` (plus
  `x-default`), Open Graph and Twitter tags, and JSON-LD (the site, readme.club, LesWWW, and
  the app as a free `MobileApplication` in English with its download URL and licence).
- `/sitemap.xml` lists every language; `/robots.txt` allows everything and points to it.
- `/llms.txt` summarises the app for AI answer engines, from `en.json`.
- `www.dotilda.com` is the only address the site declares; `dotilda.com` redirects to it.

### Share images

`npm run og` draws `public/og/<code>.png` (1200×630) for every language file: Dotilda, the
tagline (`hero.title`), the label and a few rows of dots. Long taglines shrink to fit.
Run it after changing a tagline or adding a language, and commit the images. Facebook and
LinkedIn cache share cards: pass the URL through their debuggers to refresh them.

## Rules for the code

- No inline scripts or styles: the CSP allows `'self'` only. Pass data to the browser through
  the `#page-data` JSON block, and set styles from CSS or through the DOM.
- Every visible string comes from `src/i18n/`, including those the script writes.
- The only outside requests are the two in Privacy. Adding one means changing `connect-src` in
  `vercel.json` and the Privacy section.

## Checks before you push

1. `npm run build` passes.
2. `npm run preview`, then `npm run check:fit`. It opens every page of the sitemap on 13 screens,
   from the Mudita Kompakt (360×564) to full HD, phones in landscape included, and fails if any
   text is clipped or off the screen, the page scrolls sideways, the pinned sentence is taller
   than the screen, the Kompakt mockup overflows, or the answer field is below the first screen.
3. After a change to the motion, scroll the page once with motion on and once with reduced motion.
4. After a change to a tagline or a language, `npm run og`.

## Deployment

Vercel builds and deploys the `website` branch on every push, with the settings in `vercel.json`
(Astro, `npm run build`, `dist`). `www.dotilda.com` is the main address; `dotilda.com` redirects
to it. The address the site declares is `site` in `astro.config.mjs`.

## Credits and licences

- Code: Apache 2.0, like the app (`LICENSE`).
- Fonts: SIL Open Font License 1.1, see `public/fonts/README.md`.
- readme.club and LesWWW names and logos belong to their owners and are used here to credit them.
- Design references: the readme.club design system (cream, terracotta) and the LesWWW brand
  (wordmark, lime, IBM Plex Mono).
