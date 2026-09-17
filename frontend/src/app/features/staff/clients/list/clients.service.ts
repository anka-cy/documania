import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../../core/api/api.service';

/**
 * Clients (portail staff) : liste active/archivée et création.
 * POST /api/staff/clients : le compte invité est créé sans mot de passe,
 * son activation se fait depuis la fiche client.
 */
@Injectable({ providedIn: 'root' })
export class ClientsService {
  private readonly api = inject(ApiService);

  fetchActiveClients(): Promise<any> {
    return this.api.apiFetch('/staff/clients');
  }

  fetchArchivedClients(): Promise<any> {
    return this.api.apiFetch('/staff/clients/archived');
  }

  createClient(values: any): Promise<any> {
    return this.api.apiFetch('/staff/clients', { method: 'POST', body: values });
  }
}
