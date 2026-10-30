# Fonts

Latin subsets in WOFF2, taken from the Fontsource npm packages (v5.3.0).
All are licensed under the SIL Open Font License 1.1 (full text in `OFL.txt`).

| File | Family | Copyright |
| --- | --- | --- |
| `instrument-serif-*` | Instrument Serif | The Instrument Serif Project Authors |
| `geist-*` | Geist | The Geist Project Authors |
| `space-mono-*` | Space Mono | Google / Colophon Foundry |
| `ibm-plex-mono-*` | IBM Plex Mono | IBM Corp. |
| `space-grotesk-*` | Space Grotesk | The Space Grotesk Project Authors |

When adding a language that needs more glyphs (Greek, Cyrillic, Vietnamese...),
add the matching subset files and `@font-face` rules with a `unicode-range`.
