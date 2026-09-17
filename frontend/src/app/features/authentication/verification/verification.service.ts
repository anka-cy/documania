import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/** Vérifie une adresse e-mail (POST /api/public/account-verification). */
@Injectable({ providedIn: 'root' })
export class VerificationService {
  private readonly api = inject(ApiService);

  verifyAccount(token: string): Promise<any> {
    return this.api.apiFetch('/public/account-verification', {
      method: 'POST',
      body: { token },
      skipAuthReset: true,
    });
  }
}
