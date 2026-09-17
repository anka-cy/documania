import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/**
 * Réinitialisation du mot de passe (demande + confirmation).
 * Le backend ne révèle jamais l'existence du compte.
 */
@Injectable({ providedIn: 'root' })
export class PasswordResetService {
  private readonly api = inject(ApiService);

  /** POST /api/public/password-reset/request → 204. */
  requestPasswordReset(email: string): Promise<any> {
    return this.api.apiFetch('/public/password-reset/request', {
      method: 'POST',
      body: { email },
      skipAuthReset: true,
    });
  }

  /** POST /api/public/password-reset/confirm → 204. */
  confirmPasswordReset({ token, password, passwordConfirmation }: { token: string; password: string; passwordConfirmation: string }): Promise<any> {
    return this.api.apiFetch('/public/password-reset/confirm', {
      method: 'POST',
      body: { token, password, passwordConfirmation },
      skipAuthReset: true,
    });
  }
}
