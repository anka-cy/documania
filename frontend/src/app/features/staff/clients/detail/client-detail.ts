/*
  Fiche client (portail staff) : suppression définitive réservée à l'ADMIN avec
  motif obligatoire ; compte client créé sans mot de passe, l'activation se fait
  par lien e-mail (ADMIN).
*/

import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { pageParams } from '../../../../core/router/params';
import { AuthService } from '../../../../core/auth/auth.service';
import { archivedBadgeClass, archivedLabel, formatDateTime } from '../../../../shared/formatters/formatters';
import { rules, validateForm } from '../../../../shared/validators/validators';
import { escapeHtml, promptReason } from '../../../../shared/utils/utils';
import { ToastService } from '../../../../shared/components/toast.service';
import { ConfirmService } from '../../../../shared/components/confirm.service';
import { ClientDetailService } from './client-detail.service';

interface AlertState {
  type: string;
  message: string;
}

interface InfoRow {
  label: string;
  value: string;
  badge?: { klass: string; label: string };
}

@Component({
  selector: 'app-staff-client-detail',
  imports: [FormsModule],
  templateUrl: './client-detail.html',
})
export class StaffClientDetail {
  private readonly detailService = inject(ClientDetailService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly clientId = pageParams(this.route.snapshot).id;

  protected readonly client = signal<any | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly breadcrumb = signal('Client');
  protected readonly activationBusy = signal(false);

  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly companyName = signal('');
  protected readonly phone = signal('');
  protected readonly sector = signal('');
  protected readonly address = signal('');
  protected readonly busy = signal(false);
  protected readonly editAlert = signal<AlertState | null>(null);
  protected errors: Record<string, string> = {};

  private readonly editModal = viewChild<ElementRef<HTMLElement>>('editModal');

  // ADMIN implicite toutes les autorités ; suppression définitive réservée ADMIN.
  protected readonly isAdmin = this.auth.isAdmin();
  protected readonly canUpdate = this.isAdmin || this.auth.hasAuthority('CLIENT_UPDATE');
  protected readonly canArchive = this.isAdmin || this.auth.hasAuthority('CLIENT_ARCHIVE');
  protected readonly canRestore = this.isAdmin || this.auth.hasAuthority('CLIENT_RESTORE');
  protected readonly canDelete = this.isAdmin;

  protected readonly infoRows = computed<InfoRow[]>(() => {
    const client = this.client();
    if (!client) return [];
    return [
      { label: 'Entreprise', value: this.dash(client.companyName) },
      { label: 'Contact', value: `${client.firstName || ''} ${client.lastName || ''}` },
      { label: 'E-mail', value: this.dash(client.email) },
      { label: 'Téléphone', value: this.dash(client.phone) },
      { label: 'Secteur', value: this.dash(client.sector) },
      { label: 'Adresse', value: this.dash(client.address) },
      { label: 'État', value: '', badge: { klass: archivedBadgeClass(client.archived), label: archivedLabel(client.archived) } },
      { label: 'Créé le', value: formatDateTime(client.createdAt) },
    ];
  });

  protected readonly editButtonHidden = computed(() => {
    const client = this.client();
    return client ? !this.canUpdate || client.archived : false;
  });

  protected readonly accountCardHidden = computed(() => !!this.client()?.emailVerified);

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    this.errors = {};
    try {
      const client = await this.detailService.fetchClient(this.clientId);
      this.client.set(client);
      this.breadcrumb.set(`Client ${client.companyName || client.email || ''}`);
      this.firstName.set(client.firstName || '');
      this.lastName.set(client.lastName || '');
      this.companyName.set(client.companyName || '');
      this.phone.set(client.phone || '');
      this.sector.set(client.sector || '');
      this.address.set(client.address || '');
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  protected async onSendActivation(): Promise<void> {
    this.activationBusy.set(true);
    try {
      await this.detailService.sendClientActivationToken(this.clientId);
      this.toast.show("Lien d'activation envoyé par e-mail.", 'success');
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    } finally {
      this.activationBusy.set(false);
    }
  }

  protected async onArchive(): Promise<void> {
    const client = this.client();
    const confirmed = await this.confirm.confirm(`Archiver le client « ${escapeHtml(client?.companyName)} » ?`, {
      title: 'Archiver',
      okText: 'Archiver',
      okVariant: 'warning',
    });
    if (!confirmed) return;
    try {
      await this.detailService.archiveClient(this.clientId);
      this.toast.show('Client archivé.', 'success');
      await this.load();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async onRestore(): Promise<void> {
    try {
      await this.detailService.restoreClient(this.clientId);
      this.toast.show('Client restauré.', 'success');
      await this.load();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async onDelete(): Promise<void> {
    const client = this.client();
    const reason = await promptReason(`Supprimer définitivement « ${client?.companyName} » ?`);
    if (reason === null) return;
    try {
      await this.detailService.deleteClient(this.clientId, reason);
      this.toast.show('Client supprimé.', 'success');
      await this.router.navigate(['/staff/clients']);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async onEdit(): Promise<void> {
    this.editAlert.set(null);
    const values = {
      firstName: this.firstName().trim(),
      lastName: this.lastName().trim(),
      companyName: this.companyName().trim(),
      phone: this.phone().trim() || null,
      sector: this.sector().trim() || null,
      address: this.address().trim() || null,
    };
    const { errors, isValid } = validateForm(values, {
      firstName: [rules.required('Le prénom')],
      lastName: [rules.required('Le nom')],
      companyName: [rules.required("Le nom d'entreprise")],
    });
    this.errors = errors;
    if (!isValid) {
      this.editAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }

    this.busy.set(true);
    try {
      await this.detailService.updateClient(this.clientId, values);
      bootstrap.Modal.getOrCreateInstance(this.editModal()!.nativeElement).hide();
      this.toast.show('Client mis à jour.', 'success');
      await this.load();
    } catch (error: any) {
      if (error.status === 400 && error.fieldErrors) {
        this.errors = error.fieldErrors;
      }
      this.editAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.busy.set(false);
    }
  }

  protected dash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }
}
