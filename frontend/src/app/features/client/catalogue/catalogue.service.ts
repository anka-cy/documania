import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class CatalogueService {
  private readonly api = inject(ApiService);

  fetchCatalogue(): Promise<any> {
    return this.api.apiFetch('/client/catalogue');
  }

  createOrder(offerId: string): Promise<any> {
    return this.api.apiFetch('/client/orders', { method: 'POST', body: { offerId } });
  }
}
