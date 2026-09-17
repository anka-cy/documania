import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../../core/api/api.service';

/**
 * Fiche client (portail staff) : lecture, édition, archivage/restauration,
 * suppression (ADMIN) et envoi du lien d'activation.
 */
@Injectable({ providedIn: 'root' })
export class ClientDetailService {
  private readonly api = inject(ApiService);

  fetchClient(clientId: string): Promise<any> {
    return this.api.apiFetch(`/staff/clients/${encodeURIComponent(clientId)}`);
  }

  updateClient(clientId: string, values: any): Promise<any> {
    return this.api.apiFetch(`/staff/clients/${encodeURIComponent(clientId)}`, { method: 'PUT', body: values });
  }

  sendClientActivationToken(clientId: string): Promise<any> {
    return this.api.apiFetch(`/staff/clients/${encodeURIComponent(clientId)}/activation-token`, { method: 'POST', body: {} });
  }

  archiveClient(clientId: string): Promise<any> {
    return this.api.apiFetch(`/staff/clients/${encodeURIComponent(clientId)}/archive`, { method: 'PATCH', body: {} });
  }

  restoreClient(clientId: string): Promise<any> {
    return this.api.apiFetch(`/staff/clients/${encodeURIComponent(clientId)}/restore`, { method: 'PATCH', body: {} });
  }

  deleteClient(clientId: string, reason: string): Promise<any> {
    return this.api.apiFetch(`/staff/clients/${encodeURIComponent(clientId)}`, { method: 'DELETE', body: { reason } });
  }
}
