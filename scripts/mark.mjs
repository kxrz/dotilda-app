// Dotilda as drawn in the app (ui/DotildaMark.kt), on a 100-unit grid. Shared by brand.mjs and slides.mjs.
export const INK = '#141412', PAPER = '#FBFAF6', DARK = '#151513', LIGHT_INK = '#ECEAE2';

// Lens centres per pose: peering slides them down her nose, curious glances aside (not tilted).
export const lenses = { neutral: [0, 0], peering: [0, 8], curious: [6, -3], pleased: [0, 0] };
// The app's smile: an arc of an ellipse 18 x 12.6 at (41, 66), from 20° to 160°.
const smile = '<path d="M58.46 74.45A9 6.3 0 0 1 41.54 74.45"/>';

// The glasses and smile are holes (a mask), so the file works on any background.
export const mark = (pose, fill) => {
  const [dx, dy] = lenses[pose];
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="512" height="512">
<title>Dotilda, ${pose}</title>
<mask id="m"><rect width="100" height="100" fill="#fff"/><g fill="none" stroke="#000" stroke-width="4" stroke-linecap="round"><g transform="translate(${dx} ${dy})"><circle cx="37.5" cy="60" r="10"/><circle cx="62.5" cy="60" r="10"/><path d="M47.5 60h5"/></g>${pose === 'pleased' ? smile : ''}</g></mask>
<g fill="${fill}" mask="url(#m)"><circle cx="50" cy="14" r="13"/><circle cx="50" cy="60" r="36"/></g>
</svg>
`;
};

