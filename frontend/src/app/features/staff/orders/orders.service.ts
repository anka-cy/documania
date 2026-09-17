import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

interface OrdersState {
  page: number;
  status?: string;
  query?: string;
}

/**
 * File d'attente des commandes (portail staff) : liste paginée + recherche +
 * filtre, confirmation/rejet (motif), création immédiate (ADMIN).
 */
@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly api = inject(ApiService);
  private readonly PAGE_SIZE = 20;

  fetchOrders(state: OrdersState): Promise<any> {
    const params = new URLSearchParams();
    params.set('page', String(state.page));
    params.set('size', String(this.PAGE_SIZE));
    if (state.status) params.set('status', state.status);
    if (state.query) params.set('query', state.query);
    return this.api.apiFetch(`/staff/orders?${params.toString()}`);
  }

  confirmOrder(id: any): Promise<any> {
    return this.api.apiFetch(`/staff/orders/${encodeURIComponent(id)}/confirm`, { method: 'PATCH', body: {} });
  }

  rejectOrder(id: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/orders/${encodeURIComponent(id)}/reject`, { method: 'PATCH', body: { reason } });
  }

  fetchOrderClients(): Promise<any> {
    return this.api.apiFetch('/staff/clients');
  }

  fetchOrderServices(): Promise<any> {
    return this.api.apiFetch('/staff/services');
  }

  fetchServiceOffers(serviceId: any): Promise<any> {
    return this.api.apiFetch(`/staff/services/${encodeURIComponent(serviceId)}/offers`);
  }

  createOrder(values: any): Promise<any> {
    return this.api.apiFetch('/staff/orders', { method: 'POST', body: values });
  }
}
