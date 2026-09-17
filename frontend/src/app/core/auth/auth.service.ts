// Décision D-019 (option A) : le jeton d'ACCÈS (15 min) vit uniquement en
// mémoire ; le refresh token (7 j, rotatif) est un cookie HttpOnly posé par le
// backend, jamais visible du JavaScript. Un rechargement conserve ainsi la
// session ; sans cookie valide, restoreSession() échoue en silence → login.

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ROLES } from '../config';
import { ApiError, toApiError } from '../interceptors/error.interceptor';

interface Session {
  accessToken: string;
  email: string;
  role: string | null;
  authorities: string[];
}

interface AuthResponse {
  token: string;
  email: string;
  role?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** État de session — le jeton d'accès est gardé UNIQUEMENT en mémoire. */
  private session: Session | null = null;

  /** Évite de lancer deux rafraîchissements simultanés. */
  private refreshPromise: Promise<AuthResponse> | null = null;

  /** Message d'erreur à afficher sur l'écran de connexion après une redirection. */
  private redirectMessage: string | null = null;

  /** Le backend renvoie déjà `role` dans la réponse : simple lecture. */
  private roleFromResponse(loginResponse: AuthResponse): string | null {
    return loginResponse.role || null;
  }

  /** Décodage SANS vérification de la charge utile du JWT (base64) pour lire les autorités. */
  private decodeJwtPayload(token: string): { authorities?: unknown } | null {
    try {
      const part = token.split('.')[1] || '';
      const json = atob(part.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decodeURIComponent(Array.from(json).map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join('')));
    } catch (error) {
      // Un jeton illisible en base64 sera ignoré ; le backend reste l'autorité.
      return null;
    }
  }

  /** Extrait la liste des autorités (ROLE_*) depuis le jeton d'accès. */
  private extractAuthorities(accessToken: string): string[] {
    const payload = this.decodeJwtPayload(accessToken);
    if (!payload || !Array.isArray(payload.authorities)) {
      return [];
    }
    return payload.authorities as string[];
  }

  /** (Re)construit la session en mémoire à partir d'une réponse login/refresh. */
  private adoptSession(response: AuthResponse, expectedRole?: string): Session {
    const authorities = this.extractAuthorities(response.token);
    const role = this.roleFromResponse(response) || expectedRole || null;
    this.session = {
      accessToken: response.token,
      email: response.email,
      role,
      authorities,
    };
    return this.session;
  }

  /** Renvoie true si une session existe en mémoire (ne vérifie pas l'expiration : le backend tranche). */
  isAuthenticated(): boolean {
    return this.session !== null;
  }

  /** Renvoie le rôle courant (ADMIN / STAFF / CLIENT) ou null. */
  getRole(): string | null {
    return this.session ? this.session.role : null;
  }

  getEmail(): string | null {
    return this.session ? this.session.email : null;
  }

  /** Jeton d'accès en mémoire (jamais exposé dans les logs). */
  getAccessToken(): string | null {
    return this.session ? this.session.accessToken : null;
  }

  /** Vérifie si la session possède une autorité précise (ex. 'OFFER_READ'). */
  hasAuthority(authority: string): boolean {
    return this.session ? this.session.authorities.includes(authority) : false;
  }

  isAdmin(): boolean {
    return this.getRole() === ROLES.ADMIN;
  }

  /** Efface la session en mémoire (déconnexion locale ou échec de rafraîchissement). */
  private clearSession(): void {
    this.session = null;
    this.refreshPromise = null;
  }

  /**
   * Appel POST simple vers l'API publique d'authentification (sans re-tentative
   * de refresh : ce sont des erreurs utilisateur directes).
   */
  private postAuth<T>(path: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.post<T>(`/api${path}`, body)).catch(async (error) => {
      if (error instanceof ApiError) throw error;
      throw await toApiError(error);
    });
  }

  /** Connexion au backend ; la session (jeton en mémoire) est construite sur la réponse. */
  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await this.postAuth<AuthResponse>('/public/auth/login', { email, password });
    this.adoptSession(response);
    return response;
  }

  /**
   * Rafraîchit le jeton d'accès (rotatif), appelé silencieusement sur 401.
   * Le refresh token voyage uniquement dans le cookie HttpOnly. En cas
   * d'échec : session effacée et redirection vers le login.
   */
  async refresh(): Promise<AuthResponse> {
    if (!this.session) {
      throw new Error('Aucune session à rafraîchir');
    }
    if (!this.refreshPromise) {
      this.refreshPromise = this.postAuth<AuthResponse>('/public/auth/refresh', {})
        .then((response) => {
          this.adoptSession(response);
          return response;
        })
        .catch((error) => {
          this.clearSession();
          this.redirectToLogin('Votre session a expiré. Veuillez vous reconnecter.');
          throw error;
        })
        .finally(() => {
          this.refreshPromise = null;
        });
    }
    return this.refreshPromise;
  }

  /** Pages publiques : aucune session à restaurer (évite un refresh 401 dans la console). */
  private readonly PUBLIC_PATHS = [
    '/login',
    '/register',
    '/verify-email',
    '/activate-account',
    '/password-reset',
    '/password-reset-confirm',
    '/not-authorized',
  ];

  private isPublicPage(): boolean {
    const path = (window.location.hash || '#/login').replace(/^#/, '').split('?')[0];
    return this.PUBLIC_PATHS.includes(path);
  }

  /**
   * Restaure une session au démarrage : tente un refresh silencieux en se
   * fiant uniquement au cookie HttpOnly. Si le cookie absent/invalide renvoie
   * 401, on reste déconnecté SANS message d'erreur (premier chargement).
   * Sur une page publique, on saute l'appel /refresh (inutile).
   */
  async restoreSession(): Promise<Session | null> {
    if (!this.session) {
      if (this.isPublicPage()) {
        return null;
      }
      try {
        const response = await this.postAuth<AuthResponse>('/public/auth/refresh', {});
        this.adoptSession(response);
      } catch (error) {
        this.clearSession();
        this.redirectToLogin();
      }
    }
    return this.session;
  }

  /**
   * Déconnexion : révoque le refresh token côté backend (cookie HttpOnly) puis
   * efface la mémoire. Même si l'appel réseau échoue, la session locale est
   * toujours nettoyée.
   */
  async logout(): Promise<void> {
    try {
      await this.postAuth<void>('/public/auth/logout', {});
    } catch (error) {
      // Le backend peut renvoyer une erreur si le cookie est déjà révoqué :
      // on ignore l'erreur et on finalise la déconnexion locale.
      console.error('Déconnexion backend ignorée :', (error as Error).message);
    } finally {
      this.clearSession();
      this.redirectToLogin();
    }
  }

  setRedirectMessage(message: string): void {
    this.redirectMessage = message;
  }

  /** Lit puis efface le message de connexion en attente. */
  consumeRedirectMessage(): string | null {
    const message = this.redirectMessage;
    this.redirectMessage = null;
    return message;
  }

  redirectToLogin(message?: string): void {
    if (message) this.setRedirectMessage(message);
    window.location.hash = '#/login';
  }

  /** Garde d'une route par rôle (UX uniquement — la vraie sécurité est le backend). */
  canAccess(allowedRoles?: string[]): boolean {
    if (!allowedRoles || allowedRoles.includes('public')) {
      return true;
    }
    const role = this.getRole();
    return allowedRoles.includes(role as string);
  }
}
