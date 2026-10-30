// One JSON file per language, named by its code (en.json, fr.json...). Adding a file adds a page:
// English lives at /, every other language at /<code>. Missing keys fall back to English.
import en from './en.json';

export type Strings = typeof en;

const files = import.meta.glob<{ default: Strings }>('./*.json', { eager: true });

export const defaultLocale = 'en';

export const locales = Object.keys(files)
  .map(path => path.slice(2, -5))
  .sort((a, b) => (a === defaultLocale ? -1 : b === defaultLocale ? 1 : a.localeCompare(b)));

function merge<T>(base: T, over: unknown): T {
  if (Array.isArray(base) || typeof base !== 'object' || base === null) return (over ?? base) as T;
  const out: Record<string, unknown> = { ...base };
  for (const key of Object.keys(base)) out[key] = merge((base as Record<string, unknown>)[key], (over as Record<string, unknown> | undefined)?.[key]);
  return out as T;
}

export const t = (locale: string): Strings => merge(en, files[`./${locale}.json`]?.default);

export const pathFor = (locale: string) => (locale === defaultLocale ? '/' : `/${locale}`);
