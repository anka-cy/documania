// Enregistre les services core au démarrage pour que les façades
// core/auth/auth.ts et core/api/api.ts restent utilisables comme en vanilla
// (mêmes fonctions libres ; l'état vit dans les services injectables).

import { ApiService } from './api/api.service';
import { AuthService } from './auth/auth.service';

let authSvc: AuthService | null = null;
let apiSvc: ApiService | null = null;

/** Enregistre les instances racine (APP_INITIALIZER). */
export function registerCoreServices(auth: AuthService, api: ApiService): void {
  authSvc = auth;
  apiSvc = api;
}

export function getAuth(): AuthService {
  if (!authSvc) throw new Error('AuthService non initialisé.');
  return authSvc;
}

export function getApi(): ApiService {
  if (!apiSvc) throw new Error('ApiService non initialisé.');
  return apiSvc;
}
