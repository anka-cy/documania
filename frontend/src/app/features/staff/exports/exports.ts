import { Component, inject, signal } from '@angular/core';

import { ToastService } from '../../../shared/components/toast.service';
import { ExportsService } from './exports.service';

interface ExportType {
  key: string;
  label: string;
  icon: string;
  description: string;
}

@Component({
  selector: 'app-staff-exports',
  templateUrl: './exports.html',
})
export class StaffExports {
  private readonly exportsService = inject(ExportsService);
  private readonly toast = inject(ToastService);

  protected readonly exportTypes: ExportType[] = [
    { key: 'clients', label: 'Clients', icon: 'bi-people', description: 'Liste complète des clients et entreprises.' },
    { key: 'orders', label: 'Commandes', icon: 'bi-receipt', description: 'Historique des commandes et leurs statuts.' },
    { key: 'subscriptions', label: 'Abonnements', icon: 'bi-stars', description: 'Souscriptions et périodes de facturation.' },
  ];

  protected readonly busyType = signal<string | null>(null);

  protected async onExport(type: string): Promise<void> {
    this.busyType.set(type);
    try {
      await this.exportsService.downloadExport(type);
      this.toast.show('Export téléchargé.', 'success');
    } catch (error: any) {
      this.toast.show(error.message || 'Échec du téléchargement.', 'danger');
    } finally {
      this.busyType.set(null);
    }
  }
}
