import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly api = inject(ApiService);

  fetchInvoice(orderId: string): Promise<any> {
    return this.api.get(`/client/orders/${encodeURIComponent(orderId)}/invoice`);
  }
}
