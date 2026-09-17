import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly api = inject(ApiService);

  fetchProfile(): Promise<any> {
    return this.api.apiFetch('/client/profile');
  }

  changePassword(values: any): Promise<any> {
    return this.api.apiFetch('/client/change-password', { method: 'POST', body: values });
  }
}
