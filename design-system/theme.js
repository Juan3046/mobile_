/* Shared dark/light preview switch for index.html + concept.html + icons.html.
   data-theme: '' (dark, the default — the app is dark-first) | 'light'.
   Remembered per browser so the pages stay in sync; ?theme=light preselects it.
   Storage failures (private mode, file://) are ignored. */
(function () {
  var root = document.documentElement, KT = 'rm-ds-theme', t = '';
  try { t = localStorage.getItem(KT) || ''; } catch (e) {}
  var q = new URLSearchParams(location.search);
  if (q.get('theme') !== null) t = q.get('theme');
  function apply() {
    if (t) root.setAttribute('data-theme', t); else root.removeAttribute('data-theme');
    document.dispatchEvent(new CustomEvent('ds-theme-change'));
  }
  apply();
  document.addEventListener('DOMContentLoaded', function () {
    var ts = document.getElementById('themeSel');
    if (!ts) return;
    ts.value = t;
    ts.addEventListener('change', function () { t = ts.value; apply(); try { localStorage.setItem(KT, t); } catch (e) {} });
  });
})();
