import { Injectable, inject } from '@angular/core';

import { ApiService, saveBlob } from '../../../core/api/api.service';

/**
 * Téléchargement des exports staff (GET /api/staff/export/{type}, XLSX).
 */
@Injectable({ providedIn: 'root' })
export class ExportsService {
  private readonly api = inject(ApiService);

  async downloadExport(type: string): Promise<string> {
    const { blob, filename } = await this.api.download(`/staff/export/${type}`);
    const resolved = filename || `${type}.xlsx`;
    saveBlob(blob, resolved);
    return resolved;
  }
}
