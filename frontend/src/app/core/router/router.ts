// Routage en hash (HashLocationStrategy) : les URLs #/… sont inchangées et
// les liens reçus par e-mail (/#/verify-email?token=…) fonctionnent à
// l'identique. Table des routes : app.routes.ts ; gardes : core/guards.

import { homeForRole } from '../guards/auth.guard';

export { homeForRole };

/** Bascule de page courante → nouvelle URL (format '#/…'). */
export function navigate(hash: string): void {
  if (window.location.hash === hash) {
    // même hash : retirer puis reposer le fragment pour forcer un hashchange
    // (re-rendu de la page).
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    window.location.hash = hash;
  } else {
    window.location.hash = hash;
  }
}

/** Redirection interne (sans message). */
export function redirect(hash: string): void {
  window.location.hash = hash;
}
