import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  fetchOrders(): Promise<any> {
    return this.api.apiFetch('/client/orders');
  }

  fetchSubscriptions(): Promise<any> {
    return this.api.apiFetch('/client/subscriptions');
  }

  fetchTickets(): Promise<any> {
    return this.api.apiFetch('/client/tickets');
  }
}
