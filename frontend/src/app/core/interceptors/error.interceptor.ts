// Convertit toute réponse HTTP en erreur (ou absence de réponse) en ApiError
// au message français exploitable par les pages.

import { HttpErrorResponse } from '@angular/common/http';

/** Erreur API unifiée, présentable telle quelle à l'utilisateur. */
export class ApiError extends Error {
  status: number;
  fieldErrors: Record<string, string>;
  path: string | null;
  raw: unknown;

  constructor({ status, message, fieldErrors = {}, path = null, raw = null }: {
    status?: number; message?: string; fieldErrors?: Record<string, string>;
    path?: string | null; raw?: unknown;
  } = {}) {
    super(message || 'Une erreur est survenue.');
    this.name = 'ApiError';
    this.status = status as number;
    this.fieldErrors = fieldErrors; // { champ: message } du backend (validation 400)
    this.path = path;
    this.raw = raw; // détails techniques conservés pour le diagnostic (jamais de secrets)
  }
}

/** Message français par défaut pour chaque code d'état HTTP. */
export const STATUS_MESSAGES: Record<number, string> = {
  0: 'Impossible de joindre le serveur. Vérifiez votre connexion.',
  400: 'Les données envoyées ne sont pas valides.',
  401: 'Votre session a expiré. Veuillez vous reconnecter.',
  403: "Vous n'avez pas la permission d'effectuer cette action.",
  404: 'La ressource demandée est introuvable.',
  409: 'Conflit avec les données existantes.',
  429: 'Trop de tentatives. Réessayez plus tard.',
  500: 'Erreur interne du serveur. Réessayez plus tard.',
  502: 'Le serveur est momentanément indisponible.',
  503: 'Le serveur est momentanément indisponible.',
  504: 'Le serveur met trop de temps à répondre. Réessayez plus tard.',
};

/** Choisit un message user-friendly : message backend (déjà français) sinon code HTTP. */
function friendlyMessage(status: number, backendMessage: unknown): string {
  if (backendMessage && typeof backendMessage === 'string' && backendMessage.trim()) {
    return backendMessage.trim();
  }
  return STATUS_MESSAGES[status] || STATUS_MESSAGES[500];
}

interface ErrorBody {
  message?: string;
  fieldErrors?: Record<string, string>;
  path?: string;
}

/** Convertit un corps d'erreur HTTP (JSON ou Blob de texte) en un corps exploitable. */
export async function parseErrorBody(error: HttpErrorResponse): Promise<ErrorBody | null> {
  let body: unknown = error.error;
  if (body instanceof Blob) {
    try {
      body = JSON.parse(await body.text());
    } catch {
      body = null;
    }
  }
  if (body && typeof body === 'object') {
    return body as ErrorBody;
  }
  return null;
}

export async function toApiError(error: HttpErrorResponse): Promise<ApiError> {
  const body = await parseErrorBody(error);
  const status = error.status;
  return new ApiError({
    status,
    message: friendlyMessage(status, body ? body.message : null),
    fieldErrors: body && body.fieldErrors ? body.fieldErrors : {},
    path: body ? body.path ?? null : null,
  });
}
