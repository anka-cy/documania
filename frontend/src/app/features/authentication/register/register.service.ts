import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/** Auto-inscription client (POST /api/public/register). */
@Injectable({ providedIn: 'root' })
export class RegisterService {
  private readonly api = inject(ApiService);

  registerAccount(values: Record<string, unknown>): Promise<any> {
    return this.api.apiFetch('/public/register', {
      method: 'POST',
      body: values,
      skipAuthReset: true,
    });
  }
}
