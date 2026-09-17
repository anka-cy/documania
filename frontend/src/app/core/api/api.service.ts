// Client API : couche unique de communication avec le backend (/api).
// Règles de sécurité : ne jamais journaliser un jeton, un mot de passe ou une
// clé API ; le backend reste la frontière réelle (ce client n'ajoute aucune
// règle métier).

import {
  HttpClient,
  HttpHeaders,
  HttpErrorResponse,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';

import { apiUrl } from '../config';
import { AuthService } from '../auth/auth.service';
import { ApiError, toApiError } from '../interceptors/error.interceptor';

// Ré-exporté pour les pages qui affichent les erreurs (ex. login : instanceof ApiError).
export { ApiError };

export interface ApiFetchOptions {
  method?: string;
  body?: unknown;
  /** true pour empêcher le refresh automatique sur 401 (login, refresh, logout). */
  skipAuthReset?: boolean;
  /** true pour recevoir la Response brute (téléchargements). */
  raw?: boolean;
  headers?: Record<string, string>;
}

interface FetchContext {
  skipAuthReset: boolean;
  retried: boolean;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  /**
   * Requête API générique.
   * @param path  Chemin relatif à /api (ex. "/client/profile").
   */
  apiFetch(path: string, options: ApiFetchOptions = {}): Promise<any> {
    const method = (options.method || 'GET').toUpperCase();
    const url = apiUrl(path);

    const skipAuthReset = options.skipAuthReset === true;

    return this.doFetch(url, options, method, { skipAuthReset, retried: false });
  }

  /** Exécution effective avec gestion du 401 (refresh puis relecture, une seule fois). */
  private async doFetch(
    url: string,
    options: ApiFetchOptions,
    method: string,
    { skipAuthReset, retried }: FetchContext,
  ): Promise<any> {
    // observe 'response' : on garde l'HttpResponse complète (en-têtes utiles
    // aux téléchargements : Content-Disposition, 401…).
    const request = (this.http as any).request(method, url, {
      body: options.body !== undefined && options.body !== null ? options.body : null,
      observe: 'response',
      responseType: options.raw ? 'blob' : 'json',
      headers: options.headers,
    }) as Observable<HttpResponse<any>>;

    let response: HttpResponse<any>;
    try {
      response = await firstValueFrom(request);
    } catch (error) {
      const httpError = error as HttpErrorResponse;

      if (httpError.status === 401 && !skipAuthReset && this.auth.isAuthenticated() && !retried) {
        try {
          // jetons rotatifs côté backend : refresh puis relecture UNE seule fois
          await this.auth.refresh();
          return this.doFetch(url, options, method, { skipAuthReset, retried: true });
        } catch (refreshError) {
          // échec du refresh : session déjà effacée et utilisateur redirigé par AuthService.
          throw new ApiError({
            status: 401,
            message: 'Votre session a expiré. Veuillez vous reconnecter.',
            raw: refreshError,
          });
        }
      }

      throw await toApiError(httpError);
    }

    if (options.raw) {
      return response; // l'appelant gère le blob (téléchargement de fichier)
    }
    return response.body ?? null;
  }

  /**
   * Télécharge un fichier protégé (exports) ; 401 géré comme apiFetch. Le nom
   * de fichier est celui proposé par le backend (Content-Disposition) si présent.
   * @param path  Ex. "/staff/export/clients?format=xlsx"
   */
  async download(path: string): Promise<{ blob: Blob; filename: string | null }> {
    const response = await this.apiFetch(path, { method: 'GET', raw: true });
    return {
      blob: response.body as Blob,
      filename: this.filenameFromContentDisposition(response.headers.get('Content-Disposition')),
    };
  }

  /**
   * Upload multipart vers un endpoint protégé. L'Authorization est posé par
   * l'intercepteur JWT ; le Content-Type doit rester absent pour que le
   * navigateur fixe la boundary multipart.
   * @param path  Ex. "/client/tickets/{id}/attachments"
   */
  async apiUpload(path: string, file: File): Promise<any> {
    const formData = new FormData();
    formData.append('file', file);
    try {
      return await firstValueFrom(this.http.post<any>(apiUrl(path), formData));
    } catch (error) {
      if (error instanceof ApiError) throw error;
      throw await toApiError(error as HttpErrorResponse);
    }
  }

  /** Extrait le nom de fichier d'un en-tête Content-Disposition (filename* UTF-8 inclus). */
  private filenameFromContentDisposition(header: string | null): string | null {
    if (!header) return null;
    const matchStar = /filename\*=UTF-8''([^;]+)/i.exec(header);
    if (matchStar && matchStar[1]) {
      try {
        return decodeURIComponent(matchStar[1].trim());
      } catch {
        return matchStar[1].trim();
      }
    }
    const match = /filename="?([^";]+)"?/i.exec(header);
    return match && match[1] ? match[1].trim() : null;
  }

  /** Raccourcis HTTP standard. */
  get(path: string, options: ApiFetchOptions = {}): Promise<any> {
    return this.apiFetch(path, { ...options, method: 'GET' });
  }
}

/** Déclenche le téléchargement côté navigateur à partir d'un Blob et d'un nom de fichier. */
export function saveBlob(blob: Blob, filename: string | null): void {
  const blobUrl = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = blobUrl;
  a.download = filename || 'telechargement';
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(blobUrl);
}
