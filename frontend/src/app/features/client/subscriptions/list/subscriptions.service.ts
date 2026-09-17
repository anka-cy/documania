import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class SubscriptionsService {
  private readonly api = inject(ApiService);

  fetchSubscriptions(): Promise<any> {
    return this.api.apiFetch('/client/subscriptions');
  }

  createTicket(data: any): Promise<any> {
    return this.api.apiFetch('/client/tickets', { method: 'POST', body: data });
  }
}
