/*
  Offres par service (portail staff) : listes actives/archivées, modale de
  création/édition, archivage/restauration/suppression gated par les autorités
  OFFER_* (suppression : ADMIN).
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
  FormatDateRangePipe,
  FormatDurationPipe,
  FormatPricePipe,
} from '../../../shared/pipes/formatters.pipes';
import { rules, validateForm } from '../../../shared/validators/validators';
import { promptReason } from '../../../shared/utils/utils';
import { OffersService } from './offers.service';

interface AlertState {
  type: string;
  message: string;
}

const CHUNK = 20;

@Component({
  selector: 'app-staff-offers',
  imports: [
    FormsModule,
    FormatPricePipe,
    FormatDurationPipe,
    FormatDateRangePipe,
    ArchivedLabelPipe,
    ArchivedBadgeClassPipe,
  ],
  templateUrl: './offers.html',
})
export class StaffOffers implements AfterViewInit, OnDestroy {
  private readonly svc = inject(OffersService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly admin = this.auth.isAdmin();
  protected readonly canCreate = this.admin || this.auth.hasAuthority('OFFER_CREATE');
  protected readonly canUpdate = this.admin || this.auth.hasAuthority('OFFER_UPDATE');
  protected readonly canArchive = this.admin || this.auth.hasAuthority('OFFER_ARCHIVE');
  protected readonly canRestore = this.admin || this.auth.hasAuthority('OFFER_RESTORE');
  protected readonly canDelete = this.admin;
  protected readonly canShowActions =
    this.admin || this.canUpdate || this.canArchive || this.canRestore || this.canDelete;

  // --- Sélecteur de service ---
  protected readonly services = signal<any[]>([]);
  protected readonly servicesError = signal<string | null>(null);
  protected readonly servicesLoaded = signal(false);
  protected readonly selectedServiceId = signal<string | null>(null);
  protected readonly selectedServiceName = signal<string>('');

  // --- Listes ---
  protected readonly offers = signal<any[]>([]);
  protected readonly archivedOffers = signal<any[]>([]);
  protected readonly activeLoading = signal(false);
  protected readonly activeError = signal<string | null>(null);
  protected readonly archivedLoading = signal(false);
  protected readonly archivedLoaded = signal(false);
  protected readonly archivedError = signal<string | null>(null);
  protected readonly archivedDenied = signal(false);
  protected readonly activeVisible = signal(0);
  protected readonly archivedVisible = signal(0);

  protected readonly visibleActive = computed(() => this.offers().slice(0, this.activeVisible()));
  protected readonly visibleArchived = computed(() =>
    this.archivedOffers().slice(0, this.archivedVisible()),
  );
  protected readonly hasMoreActive = computed(() => this.activeVisible() < this.offers().length);
  protected readonly hasMoreArchived = computed(() =>
    this.archivedVisible() < this.archivedOffers().length,
  );

  protected readonly activeSentinel = viewChild<ElementRef<HTMLElement>>('activeSentinel');
  protected readonly archivedSentinel = viewChild<ElementRef<HTMLElement>>('archivedSentinel');

  // --- Modale ---
  private readonly modalEl = viewChild<ElementRef<HTMLElement>>('offerModal');
  private modal: any;
  protected readonly modalTitle = signal('Nouvelle offre');
  private readonly editId = signal<string | null>(null);
  protected readonly formName = signal('');
  protected readonly formPrice = signal<string | number>('');
  protected readonly formDuration = signal<string | number>('');
  protected readonly formUsers = signal<string | number>('');
  protected readonly formStart = signal('');
  protected readonly formEnd = signal('');
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
          if (kind === 'active' && this.activeVisible() < this.offers().length) {
            this.activeVisible.update((c) => c + CHUNK);
          } else if (kind === 'archived' && this.archivedVisible() < this.archivedOffers().length) {
            this.archivedVisible.update((c) => c + CHUNK);
          }
        }
      },
      { rootMargin: '200px 0px', threshold: 0.01 },
    );
    effect(() => {
      const el = this.activeSentinel()?.nativeElement;
      if (el) this.observer.observe(el);
    });
    effect(() => {
      const el = this.archivedSentinel()?.nativeElement;
      if (el) this.observer.observe(el);
    });
    void this.init();
  }

  ngAfterViewInit(): void {
    const el = this.modalEl()?.nativeElement;
    if (el) this.modal = new bootstrap.Modal(el);
  }

  ngOnDestroy(): void {
    this.observer.disconnect();
  }

  private async init(): Promise<void> {
    try {
      const services = await this.svc.fetchServices() ?? [];
      this.services.set(services);
      this.servicesLoaded.set(true);
    } catch (error: any) {
      this.servicesError.set(error.message);
    }
  }

  // --- Sélection d'un service (dropdown) ---
  protected selectService(service: any): void {
    this.selectedServiceId.set(service.publicId || null);
    this.selectedServiceName.set(service.name || '');
    if (service.publicId) {
      void this.loadOffers(service.publicId);
    }
  }

  // --- Chargement des offres d'un service ---
  private async loadOffers(serviceId: string): Promise<void> {
    this.activeLoading.set(true);
    this.activeError.set(null);
    try {
      const items = await this.svc.fetchOffers(serviceId) ?? [];
      this.offers.set(items);
      this.activeVisible.set(Math.min(CHUNK, items.length));
    } catch (error: any) {
      this.offers.set([]);
      this.activeError.set(error.message);
    } finally {
      this.activeLoading.set(false);
    }
    // Les archives sont rechargées seulement si l'onglet a déjà été ouvert.
    if (this.archivedLoaded()) {
      await this.loadArchivedInto(serviceId, true);
    }
  }

  private async loadArchivedInto(serviceId: string, silent: boolean): Promise<void> {
    if (!silent) this.archivedLoading.set(true);
    this.archivedError.set(null);
    this.archivedDenied.set(false);
    try {
      const items = await this.svc.fetchArchivedOffers(serviceId) ?? [];
      this.archivedOffers.set(items);
      this.archivedLoaded.set(true);
      this.archivedVisible.set(Math.min(CHUNK, items.length));
    } catch (error: any) {
      this.archivedOffers.set([]);
      if (error.status !== 403) {
        this.archivedError.set(error.message);
      } else {
        this.archivedDenied.set(true);
      }
    } finally {
      if (!silent) this.archivedLoading.set(false);
    }
  }

  // Chargement paresseux au premier clic sur l'onglet « Archivées ».
  protected onArchivedTabClick(): void {
    const serviceId = this.selectedServiceId();
    if (!serviceId) return;
    if (this.archivedLoaded()) return;
    void this.loadArchivedInto(serviceId, false);
  }

  // --- Actions de lignes ---
  protected async setArchived(id: string, archive: boolean): Promise<void> {
    const verb = archive ? 'Archiver' : 'Restaurer';
    const confirmed = await this.confirm.confirm(`${verb} cette offre ?`, {
      title: verb,
      okText: verb,
      okVariant: archive ? 'warning' : 'success',
    });
    if (!confirmed) return;
    try {
      await this.svc.setOfferArchived(id, archive);
      this.toast.show(archive ? 'Offre archivée.' : 'Offre restaurée.', 'success');
      const serviceId = this.selectedServiceId();
      if (serviceId) await this.loadOffers(serviceId);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async removeOffer(id: string): Promise<void> {
    const reason = await promptReason('Supprimer définitivement cette offre ?');
    if (reason === null) return;
    try {
      await this.svc.deleteOffer(id, reason);
      this.toast.show('Offre supprimée.', 'success');
      const serviceId = this.selectedServiceId();
      if (serviceId) await this.loadOffers(serviceId);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  // --- Modale ---
  protected openEdit(id: string): void {
    const offer = this.offers().find((entry) => entry.publicId === id);
    if (!offer) return;
    this.editId.set(id);
    this.modalTitle.set("Modifier l'offre");
    this.formName.set(offer.name || '');
    this.formPrice.set(offer.price ?? '');
    this.formDuration.set(offer.durationMonths ?? '');
    this.formUsers.set(offer.numberOfUsers ?? '');
    this.formStart.set(offer.commercialStartDate || '');
    this.formEnd.set(offer.commercialEndDate || '');
    this.formDescription.set(offer.description || '');
    this.formErrors = {};
    this.formAlert.set(null);
    this.modal?.show();
  }

  protected openCreate(): void {
    if (!this.selectedServiceId()) {
      this.toast.show("Sélectionnez d'abord un service.", 'warning');
      return;
    }
    this.editId.set(null);
    this.modalTitle.set('Nouvelle offre');
    this.formName.set('');
    this.formPrice.set('');
    this.formDuration.set('');
    this.formUsers.set('');
    this.formStart.set('');
    this.formEnd.set('');
    this.formDescription.set('');
    this.formErrors = {};
    this.formAlert.set(null);
    this.modal?.show();
  }

  protected async onFormSubmit(): Promise<void> {
    this.formAlert.set(null);
    const values = {
      name: this.formName().trim(),
      price: Number(this.formPrice()),
      durationMonths: Number(this.formDuration()),
      numberOfUsers: Number(this.formUsers()),
      commercialStartDate: this.formStart(),
      commercialEndDate: this.formEnd(),
      description: this.formDescription().trim() || null,
    };
    const { errors, isValid } = validateForm(values, {
      name: [rules.required('Le nom')],
      price: [rules.required('Le prix')],
      durationMonths: [rules.required('La durée')],
      numberOfUsers: [rules.required("Les utilisateurs")],
      commercialStartDate: [rules.required('La date de début')],
      commercialEndDate: [rules.required('La date de fin')],
    });
    this.formErrors = errors;
    if (!isValid) {
      this.formAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }
    // L'ordre des dates (fin ≥ début) est vérifié par le backend : pas de règle dupliquée.
    this.formBusy.set(true);
    try {
      const id = this.editId();
      if (id) {
        await this.svc.updateOffer(id, values);
        this.toast.show('Offre mise à jour.', 'success');
      } else {
        await this.svc.createOffer(this.selectedServiceId(), values);
        this.toast.show('Offre créée.', 'success');
      }
      this.modal?.hide();
      const serviceId = this.selectedServiceId();
      if (serviceId) await this.loadOffers(serviceId);
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.formErrors = { ...error.fieldErrors };
      }
      this.formAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.formBusy.set(false);
    }
  }
}
