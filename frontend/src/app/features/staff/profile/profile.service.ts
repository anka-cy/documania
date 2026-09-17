import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/**
 * Profil du compte staff : GET/PUT /api/staff/profile et changement de mot de
 * passe (POST /api/staff/change-password).
 */
@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly api = inject(ApiService);

  fetchStaffProfile(): Promise<any> {
    return this.api.apiFetch('/staff/profile');
  }

  updateStaffProfile(values: any): Promise<any> {
    return this.api.apiFetch('/staff/profile', { method: 'PUT', body: values });
  }

  changeStaffPassword(values: any): Promise<any> {
    return this.api.apiFetch('/staff/change-password', { method: 'POST', body: values });
  }
}
