// Runs before first paint: applies the theme chosen with the header button, if any.
// Without a choice, the page follows the system (prefers-color-scheme).
(function () {
  var root = document.documentElement;
  root.classList.add('js');
  try {
    var theme = localStorage.getItem('dotilda-theme');
    if (theme === 'light' || theme === 'dark') root.setAttribute('data-theme', theme);
  } catch (e) {}
})();
