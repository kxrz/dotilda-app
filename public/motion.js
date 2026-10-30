// Runs before first paint: hides what GSAP will reveal, only for people who accept motion.
// If the animation script never starts, everything comes back after two seconds.
(function () {
  var root = document.documentElement;
  if (!window.matchMedia || !matchMedia('(prefers-reduced-motion: no-preference)').matches) return;
  root.classList.add('motion');
  setTimeout(function () {
    if (!root.classList.contains('motion-ready')) root.classList.remove('motion');
  }, 2000);
})();
