import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/** Active un compte invité (POST /api/public/account-activation). */
@Injectable({ providedIn: 'root' })
export class ActivationService {
  private readonly api = inject(ApiService);

  activateAccount({ token, password, passwordConfirmation }: { token: string; password: string; passwordConfirmation: string }): Promise<any> {
    return this.api.apiFetch('/public/account-activation', {
      method: 'POST',
      body: { token, password, passwordConfirmation },
      skipAuthReset: true,
    });
  }
}
