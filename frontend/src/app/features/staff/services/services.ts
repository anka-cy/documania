/*
  Services du catalogue (portail staff) : listes actifs/archivés, modale de
  création/édition, archivage/restauration/suppression (motif) gated par les
  autorités SERVICE_* (suppression : ADMIN).
*/

import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/interceptors/error.interceptor';
import { ToastService } from '../../../shared/components/toast.service';
import { ConfirmService } from '../../../shared/components/confirm.service';
import {
  ArchivedBadgeClassPipe,
  ArchivedLabelPipe,
  FormatDateTimePipe,
} from '../../../shared/pipes/formatters.pipes';
import { rules, validateForm } from '../../../shared/validators/validators';
import { promptReason } from '../../../shared/utils/utils';
import { ServicesService } from './services.service';

interface AlertState {
  type: string;
  message: string;
}

const CHUNK = 20;

@Component({
  selector: 'app-staff-services',
  imports: [FormsModule, FormatDateTimePipe, ArchivedLabelPipe, ArchivedBadgeClassPipe],
  templateUrl: './services.html',
})
export class StaffServices implements AfterViewInit, OnDestroy {
  private readonly svc = inject(ServicesService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly admin = this.auth.isAdmin();
  protected readonly canCreate = this.admin || this.auth.hasAuthority('SERVICE_CREATE');
  protected readonly canUpdate = this.admin || this.auth.hasAuthority('SERVICE_UPDATE');
  protected readonly canArchive = this.admin || this.auth.hasAuthority('SERVICE_ARCHIVE');
  protected readonly canRestore = this.admin || this.auth.hasAuthority('SERVICE_RESTORE');
  protected readonly canDelete = this.admin;
  protected readonly canShowActions =
    this.admin || this.canUpdate || this.canArchive || this.canRestore || this.canDelete;

  // --- Listes ---
  protected readonly services = signal<any[]>([]);
  protected readonly archived = signal<any[] | null>(null);
  protected readonly activeLoading = signal(false);
  protected readonly activeError = signal<string | null>(null);
  protected readonly archivedLoading = signal(false);
  protected readonly archivedError = signal<string | null>(null);
  protected readonly archivedDenied = signal(false);
  protected readonly activeVisible = signal(0);
  protected readonly archivedVisible = signal(0);

  protected readonly visibleActive = computed(() => this.services().slice(0, this.activeVisible()));
  protected readonly visibleArchived = computed(() =>
    (this.archived() ?? []).slice(0, this.archivedVisible()),
  );
  protected readonly hasMoreActive = computed(() => this.activeVisible() < this.services().length);
  protected readonly hasMoreArchived = computed(() =>
    this.archivedVisible() < (this.archived()?.length ?? 0),
  );

  protected readonly activeSentinel = viewChild<ElementRef<HTMLElement>>('activeSentinel');
  protected readonly archivedSentinel = viewChild<ElementRef<HTMLElement>>('archivedSentinel');

  // --- Modale création / édition ---
  private readonly modalEl = viewChild<ElementRef<HTMLElement>>('serviceModal');
  private modal: any;
  protected readonly modalTitle = signal('Nouveau service');
  private readonly editId = signal<string | null>(null);
  protected readonly formName = signal('');
  protected readonly formCategory = signal('');
  protected readonly formDescription = signal('');
  protected readonly formBusy = signal(false);
  protected readonly formAlert = signal<AlertState | null>(null);
  protected formErrors: Record<string, string> = {};

  private readonly observer: IntersectionObserver;

  constructor() {
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          const kind = (entry.target as HTMLElement).dataset.sentinel;
          if (kind === 'active' && this.activeVisible() < this.services().length) {
            this.activeVisible.update((c) => c + CHUNK);
          } else if (kind === 'archived' && this.archivedVisible() < (this.archived()?.length ?? 0)) {
            this.archivedVisible.update((c) => c + CHUNK);
          }
        }
      },
      { rootMargin: '200px 0px', threshold: 0.01 },
    );
    // (Re)observe la sentinelle à chaque rendu d'un nouveau lot.
    effect(() => {
      const el = this.activeSentinel()?.nativeElement;
      if (el) this.observer.observe(el);
    });
    effect(() => {
      const el = this.archivedSentinel()?.nativeElement;
      if (el) this.observer.observe(el);
    });
    void this.reload();
  }

  ngAfterViewInit(): void {
    const el = this.modalEl()?.nativeElement;
    if (el) this.modal = new bootstrap.Modal(el);
  }

  ngOnDestroy(): void {
    this.observer.disconnect();
  }

  // --- Chargement ---
  private async loadActive(): Promise<void> {
    this.activeLoading.set(true);
    this.activeError.set(null);
    try {
      const items = await this.svc.fetchServices() ?? [];
      this.services.set(items);
      this.activeVisible.set(Math.min(CHUNK, items.length));
    } catch (error: any) {
      this.activeError.set(error.message);
    } finally {
      this.activeLoading.set(false);
    }
  }

  private async loadArchived(silent = false): Promise<void> {
    if (!silent) this.archivedLoading.set(true);
    this.archivedError.set(null);
    this.archivedDenied.set(false);
    try {
      const items = await this.svc.fetchArchivedServices() ?? [];
      this.archived.set(items);
      this.archivedVisible.set(Math.min(CHUNK, items.length));
    } catch (error: any) {
      if (error.status !== 403) {
        this.archivedError.set(error.message);
      } else {
        this.archivedDenied.set(true);
      }
    } finally {
      if (!silent) this.archivedLoading.set(false);
    }
  }

  private async reload(): Promise<void> {
    await this.loadActive();
    if (this.archived() !== null) {
      await this.loadArchived(true);
    }
  }

  protected onArchivedTabClick(): void {
    if (this.archived() === null && !this.archivedLoading()) {
      void this.loadArchived();
    }
  }

  // --- Actions de lignes ---
  protected async setArchived(id: string, archive: boolean): Promise<void> {
    const verb = archive ? 'Archiver' : 'Restaurer';
    const confirmed = await this.confirm.confirm(`${verb} ce service ?`, {
      title: verb,
      okText: verb,
      okVariant: archive ? 'warning' : 'success',
    });
    if (!confirmed) return;
    try {
      await this.svc.setServiceArchived(id, archive);
      this.toast.show(archive ? 'Service archivé.' : 'Service restauré.', 'success');
      await this.reload();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async removeService(id: string): Promise<void> {
    const reason = await promptReason('Supprimer définitivement ce service ?');
    if (reason === null) return;
    try {
      await this.svc.deleteService(id, reason);
      this.toast.show('Service supprimé.', 'success');
      await this.reload();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  // --- Modale ---
  protected openEdit(id: string): void {
    const service =
      this.services().find((entry) => entry.publicId === id) ??
      (this.archived() ?? []).find((entry) => entry.publicId === id);
    if (!service) return;
    this.editId.set(id);
    this.modalTitle.set('Modifier le service');
    this.formName.set(service.name || '');
    this.formCategory.set(service.category || '');
    this.formDescription.set(service.description || '');
    this.formErrors = {};
    this.formAlert.set(null);
    this.modal?.show();
  }

  protected openCreate(): void {
    this.editId.set(null);
    this.modalTitle.set('Nouveau service');
    this.formName.set('');
    this.formCategory.set('');
    this.formDescription.set('');
    this.formErrors = {};
    this.formAlert.set(null);
    this.modal?.show();
  }

  protected async onFormSubmit(): Promise<void> {
    this.formAlert.set(null);
    const values = {
      name: this.formName().trim(),
      category: this.formCategory().trim() || null,
      description: this.formDescription().trim() || null,
    };
    const { errors, isValid } = validateForm(values, { name: [rules.required('Le nom')] });
    this.formErrors = errors;
    if (!isValid) {
      this.formAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }
    this.formBusy.set(true);
    try {
      const id = this.editId();
      if (id) {
        await this.svc.updateService(id, values);
        this.toast.show('Service mis à jour.', 'success');
      } else {
        await this.svc.createService(values);
        this.toast.show('Service créé.', 'success');
      }
      this.modal?.hide();
      await this.reload();
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.formErrors = { ...this.formErrors, ...error.fieldErrors };
      }
      this.formAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.formBusy.set(false);
    }
  }
}
