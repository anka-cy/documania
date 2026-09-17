import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

export const PAGE_SIZE = 20;

export interface AuditState {
  query: string;
  action: string;
  entityType: string;
  page: number;
}

/**
 * Journal d'audit paginé (GET /api/staff/audits?page=&size=&query=&action=&entityType=).
 */
@Injectable({ providedIn: 'root' })
export class AuditsService {
  private readonly api = inject(ApiService);

  fetchAudits(state: AuditState): Promise<any> {
    const params = new URLSearchParams();
    params.set('page', String(state.page));
    params.set('size', String(PAGE_SIZE));
    if (state.query) params.set('query', state.query);
    if (state.action) params.set('action', state.action);
    if (state.entityType) params.set('entityType', state.entityType);
    return this.api.apiFetch(`/staff/audits?${params.toString()}`);
  }
}
