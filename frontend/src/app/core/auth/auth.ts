// Facade fonctionnelle de l'authentification : mêmes exports que la version
// vanilla, délègue au AuthService racine (jeton d'accès en mémoire, refresh
// rotatif via cookie HttpOnly).

import { getAuth } from '../singletons';

export function isAuthenticated(): boolean {
  return getAuth().isAuthenticated();
}

export function getRole(): string | null {
  return getAuth().getRole();
}

export function getEmail(): string | null {
  return getAuth().getEmail();
}

export function getAccessToken(): string | null {
  return getAuth().getAccessToken();
}

export function hasAuthority(authority: string): boolean {
  return getAuth().hasAuthority(authority);
}

export function isAdmin(): boolean {
  return getAuth().isAdmin();
}

export function login(email: string, password: string): Promise<any> {
  return getAuth().login(email, password);
}

export function refresh(): Promise<any> {
  return getAuth().refresh();
}

export function restoreSession(): Promise<any> {
  return getAuth().restoreSession();
}

export function logout(): Promise<void> {
  return getAuth().logout();
}

export function setRedirectMessage(message: string): void {
  getAuth().setRedirectMessage(message);
}

export function consumeRedirectMessage(): string | null {
  return getAuth().consumeRedirectMessage();
}

export function redirectToLogin(message?: string): void {
  getAuth().redirectToLogin(message);
}

export function canAccess(allowedRoles?: string[]): boolean {
  return getAuth().canAccess(allowedRoles);
}
