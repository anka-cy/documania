import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

interface SubscriptionsState {
  page: number;
  status?: string;
  query?: string;
}

/**
 * Abonnements (portail staff) : liste paginée + recherche + filtre, annulation
 * avec motif (ADMIN).
 */
@Injectable({ providedIn: 'root' })
export class SubscriptionsService {
  private readonly api = inject(ApiService);
  private readonly PAGE_SIZE = 20;

  fetchSubscriptions(state: SubscriptionsState): Promise<any> {
    const params = new URLSearchParams();
    params.set('page', String(state.page));
    params.set('size', String(this.PAGE_SIZE));
    if (state.status) params.set('status', state.status);
    if (state.query) params.set('query', state.query);
    return this.api.apiFetch(`/staff/subscriptions?${params.toString()}`);
  }

  cancelSubscription(id: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/subscriptions/${encodeURIComponent(id)}/cancel`, { method: 'PATCH', body: { reason } });
  }
}
