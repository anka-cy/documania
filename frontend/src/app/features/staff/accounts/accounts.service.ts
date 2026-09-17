import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/** API des comptes du personnel (portail staff — ADMIN) : CRUD et activation. */
@Injectable({ providedIn: 'root' })
export class AccountsService {
  private readonly api = inject(ApiService);

  fetchAccounts(): Promise<any> {
    return this.api.apiFetch('/staff/accounts');
  }

  createAccount(values: any): Promise<any> {
    return this.api.apiFetch('/staff/accounts', { method: 'POST', body: values });
  }

  sendActivationToken(id: any): Promise<any> {
    return this.api.apiFetch(`/staff/accounts/${encodeURIComponent(id)}/activation-token`, { method: 'POST', body: {} });
  }

  setAccountEnabled(id: any, enabled: any): Promise<any> {
    return this.api.apiFetch(`/staff/accounts/${encodeURIComponent(id)}/enabled`, { method: 'PATCH', body: { enabled } });
  }

  deleteAccount(id: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/accounts/${encodeURIComponent(id)}`, { method: 'DELETE', body: { reason } });
  }
}
