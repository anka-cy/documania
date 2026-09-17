import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/** API du catalogue de services (portail staff) : CRUD et archivage. */
@Injectable({ providedIn: 'root' })
export class ServicesService {
  private readonly api = inject(ApiService);

  fetchServices(): Promise<any> {
    return this.api.apiFetch('/staff/services');
  }

  fetchArchivedServices(): Promise<any> {
    return this.api.apiFetch('/staff/services/archived');
  }

  createService(values: any): Promise<any> {
    return this.api.apiFetch('/staff/services', { method: 'POST', body: values });
  }

  updateService(id: any, values: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(id)}`, { method: 'PUT', body: values });
  }

  setServiceArchived(id: any, archive: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(id)}/${archive ? 'archive' : 'restore'}`, { method: 'PATCH', body: {} });
  }

  deleteService(id: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(id)}`, { method: 'DELETE', body: { reason } });
  }
}
