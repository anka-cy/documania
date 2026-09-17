import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class TicketsService {
  private readonly api = inject(ApiService);

  fetchTickets(): Promise<any> {
    return this.api.apiFetch('/staff/tickets');
  }
}
