# dotilda.com

The landing page for [Dotilda](https://github.com/kxrz/dotilda-app), the gentle daily journaling
app for e-ink. An app by readme.club, made and maintained by LesWWW.

This `website` branch holds only the site; the app lives on `main`. Vercel deploys this branch
to production on every push.

## Stack

Static HTML, CSS and a little JS. No framework, no build step, no database, no cookies, no analytics.

| File | Role |
| --- | --- |
| `index.html` | The page (English). Every visible string lives here, including the "Try one" questions. |
| `styles.css` | Tokens, self-hosted fonts, layout. |
| `main.js` | Month line, "Try one", the galaxy canvas, the latest release from the GitHub API. |
| `fonts/` | Self-hosted WOFF2 (SIL OFL), see `fonts/README.md`. |
| `vercel.json` | Clean URLs, security headers (strict CSP), font caching. |
| `og.png`, `favicon.svg`, `apple-touch-icon.png` | Sharing and icons. |

The only outside request is to `api.github.com` for the latest release (version, date, APK size,
direct download link). If it fails, the download button still opens the releases page.

## Rules

Same spirit as the app: pure ink on paper, no animation, nothing that tracks.
`#D1613E` (readme.club terracotta) appears once, for today's dot. LesWWW lime appears only under the
LesWWW logo.

## Run locally

```bash
python3 -m http.server 8000
```

## Adding a language

The site follows the app's languages. To add one, say French:

1. Copy `index.html` to `fr/index.html` and translate every text node and `data-*` attribute
   (`data-left`, `data-done`, `data-label`, `data-line`, `data-locale`), the `<title>`, meta tags
   and the `questions` JSON (take them from the app's translated bank).
2. Set `<html lang="fr">`, the canonical to `https://dotilda.com/fr`, and add the
   `<link rel="alternate" hreflang="...">` lines to every language's page.
3. Add the URL to `sitemap.xml` and a language link in the header.

## Vercel setup

- Production branch: `website`
- Framework preset: Other, no build command, output directory: `.` (root)
- Domain: `dotilda.com` (and `www.dotilda.com` redirecting to it)
