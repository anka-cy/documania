// Constantes non sensibles du frontend. Aucun secret ici.
// Le frontend est servi par le backend (même origine) : pas de CORS à configurer.

/** Racine de l'API backend (même origine — servi par Spring Boot). */
export const API_BASE = '/api';

/** Version applicative du frontend — sert de cache-buster sur les assets. */
export const APP_VERSION = '2.0.1';

/** Nom applicatif (titres d'onglet). */
export const APP_NAME = 'Documania';

/** Rôles connus du backend (retournés par login/refresh). */
export const ROLES = Object.freeze({
  ADMIN: 'ADMIN',
  STAFF: 'STAFF',
  CLIENT: 'CLIENT',
});

export function apiUrl(path: string): string {
  const normalized = String(path || '');
  const withSlash = normalized.startsWith('/') ? normalized : `/${normalized}`;
  return `${API_BASE}${withSlash}`;
}

/**
 * Ajoute le cache-buster versionné aux assets dynamiques (vues HTML, modules
 * JS, CSS de feature). Ne jamais versionner les appels /api (données).
 */
export function versionedUrl(path: string): string {
  const separator = String(path).includes('?') ? '&' : '?';
  return `${path}${separator}v=${encodeURIComponent(APP_VERSION)}`;
}
