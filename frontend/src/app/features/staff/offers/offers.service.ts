import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/** API des offres commerciales par service (portail staff). */
@Injectable({ providedIn: 'root' })
export class OffersService {
  private readonly api = inject(ApiService);

  fetchServices(): Promise<any> {
    return this.api.apiFetch('/staff/services');
  }

  fetchOffers(serviceId: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(serviceId)}/offers`);
  }

  fetchArchivedOffers(serviceId: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(serviceId)}/offers/archived`);
  }

  createOffer(serviceId: any, values: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(serviceId)}/offers`, { method: 'POST', body: values });
  }

  updateOffer(offerId: any, values: any): Promise<any> {
    return this.api.apiFetch(`/staff/offers/${encodeURIComponent(offerId)}`, { method: 'PUT', body: values });
  }

  setOfferArchived(offerId: any, archive: any): Promise<any> {
    return this.api.apiFetch(`/staff/offers/${encodeURIComponent(offerId)}/${archive ? 'archive' : 'restore'}`, { method: 'PATCH', body: {} });
  }

  deleteOffer(offerId: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/offers/${encodeURIComponent(offerId)}`, { method: 'DELETE', body: { reason } });
  }
}
