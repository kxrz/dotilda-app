// The shapes a day's dot takes, as drawn in the app's galaxy (22-unit SVG contents).
export const glyphs: Record<string, string> = {
  full: '<circle cx="11" cy="11" r="6" fill="currentColor"/>',
  half: '<circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M11 5a6 6 0 0 1 0 12z" fill="currentColor"/>',
  square: '<rect x="6" y="6" width="10" height="10" fill="currentColor"/>',
  double: '<circle cx="11" cy="11" r="5" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="11" cy="11" r="8.5" fill="none" stroke="currentColor" stroke-width="1.5"/>',
  sealed: '<path d="M11 3.5l6.5 7.5-6.5 7.5-6.5-7.5z" fill="none" stroke="currentColor" stroke-width="1.5"/>',
};
