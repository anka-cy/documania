import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

/**
 * Synthèse du tableau de bord staff (GET /api/staff/dashboard/summary).
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);

  fetchDashboardSummary(): Promise<any> {
    return this.api.apiFetch('/staff/dashboard/summary');
  }
}
