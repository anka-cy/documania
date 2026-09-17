import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly api = inject(ApiService);

  fetchOrders(): Promise<any> {
    return this.api.apiFetch('/client/orders');
  }

  cancelOrder(orderId: any): Promise<any> {
    return this.api.apiFetch(`/client/orders/${encodeURIComponent(orderId)}/cancel`, { method: 'PATCH', body: {} });
  }

  fetchTickets(): Promise<any> {
    return this.api.apiFetch('/client/tickets');
  }
}
