// Garde des routes avant rendu (UX uniquement — la vraie sécurité est le
// backend) : page publique → portail si déjà connecté ; page protégée →
// /login sans session ; rôle insuffisant → /not-authorized.

import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { ROLES } from '../config';

/** Portail d'accueil selon le rôle (après connexion) — chemin Angular (sans #). */
export function homePathForRole(role: string | null): string {
  if (role === ROLES.CLIENT) return '/client/dashboard';
  if (role === ROLES.STAFF || role === ROLES.ADMIN) return '/staff/dashboard';
  return '/login';
}

/** Portail d'accueil selon le rôle — format hash historique (#/...). */
export function homeForRole(role: string | null): string {
  return `#${homePathForRole(role)}`;
}

/**
 * Garde déclinée du tableur de routes (data.roles), fidèle au comportement
 * historique : roles ['public'] = page ouverte (connecté → portail pour
 * /login et /register) ; sinon session puis rôle autorisé.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const roles = (route.data['roles'] as string[]) || [];
  const path = state.url.split('?')[0];

  if (roles.includes('public')) {
    if ((path === '/login' || path === '/register') && auth.isAuthenticated()) {
      return router.parseUrl(homePathForRole(auth.getRole()));
    }
    return true;
  }

  if (!auth.isAuthenticated()) {
    auth.setRedirectMessage('Connectez-vous pour accéder à cette page.');
    auth.redirectToLogin();
    return false;
  }

  if (!auth.canAccess(roles)) {
    return router.parseUrl('/not-authorized') as UrlTree;
  }

  return true;
};

/** Racine (#/) : redirection selon l'état d'authentification. */
export const rootRedirectGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return router.parseUrl(
    auth.isAuthenticated() ? homePathForRole(auth.getRole()) : '/login',
  );
};
