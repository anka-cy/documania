/*
  Commandes (portail staff) : file paginée côté serveur (recherche debouncée +
  filtre statut). Confirmation → activation auto de la souscription ; rejet et
  création immédiate réservés à l'ADMIN.
*/

import { AfterViewInit, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../shared/components/toast.service';
import { ConfirmService } from '../../../shared/components/confirm.service';
import { Pagination } from '../../../shared/components/pagination/pagination';
import {
  FormatDatePipe,
  FormatPricePipe,
  OrderStatusLabelPipe,
  StatusBadgeClassPipe,
} from '../../../shared/pipes/formatters.pipes';
import { debounce, escapeHtml, promptReason } from '../../../shared/utils/utils';
import { OrdersService } from './orders.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-staff-orders',
  imports: [
    FormsModule,
    Pagination,
    FormatDatePipe,
    FormatPricePipe,
    OrderStatusLabelPipe,
    StatusBadgeClassPipe,
  ],
  templateUrl: './orders.html',
})
export class StaffOrders implements AfterViewInit {
  private readonly svc = inject(OrdersService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly admin = this.auth.isAdmin();

  // --- État serveur (recherche, filtre, page) ---
  private readonly query = signal('');
  protected readonly status = signal('');
  protected readonly page = signal(0);

  protected readonly items = signal<any[]>([]);
  protected readonly totalPages = signal(1);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly unfiltered = computed(() => !this.query() && !this.status());

  // --- Modale de création ---
  private readonly modalEl = viewChild<ElementRef<HTMLElement>>('orderModal');
  private modal: any;
  protected readonly formClients = signal<any[]>([]);
  protected readonly formOffers = signal<any[]>([]);
  protected readonly offersEmpty = signal(false);
  protected readonly clientId = signal('');
  protected readonly offerId = signal('');
  protected readonly formBusy = signal(false);
  protected readonly formAlert = signal<AlertState | null>(null);

  private readonly debouncedLoad = debounce(() => void this.load(), 300);

  constructor() {
    void this.load();
  }

  ngAfterViewInit(): void {
    const el = this.modalEl()?.nativeElement;
    if (el) this.modal = new bootstrap.Modal(el);
  }

  // --- Chargement de la liste ---
  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await this.svc.fetchOrders({
        page: this.page(),
        status: this.status() || undefined,
        query: this.query() || undefined,
      });
      this.items.set(data.items ?? []);
      this.totalPages.set(data.totalPages ?? 1);
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  protected onSearch(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value.trim().toLowerCase());
    this.page.set(0);
    this.debouncedLoad();
  }

  protected onStatusChange(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value);
    this.page.set(0);
    void this.load();
  }

  protected onPageChange(next: number): void {
    this.page.set(next);
    void this.load();
  }

  // --- Confirmation / rejet ---
  protected async confirmOrder(id: string, number: string): Promise<void> {
    const confirmed = await this.confirm.confirm(
      `Confirmer la commande <strong>${escapeHtml(number)}</strong> ?<br>
       <span class="small text-muted">La souscription associée sera activée automatiquement.</span>`,
      { title: 'Confirmer la commande', okText: 'Confirmer', okVariant: 'success' },
    );
    if (!confirmed) return;
    try {
      await this.svc.confirmOrder(id);
      this.toast.show('Commande confirmée, souscription activée.', 'success');
      await this.load();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async rejectOrder(id: string, number: string): Promise<void> {
    const reason = await promptReason(`Rejeter la commande ${number} ?`);
    if (reason === null) return;
    try {
      await this.svc.rejectOrder(id, reason);
      this.toast.show('Commande rejetée.', 'success');
      await this.load();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  // --- Création (ADMIN) ---
  protected async onOpenCreate(): Promise<void> {
    this.formAlert.set(null);
    this.clientId.set('');
    this.offerId.set('');
    try {
      const clients = await this.svc.fetchOrderClients() ?? [];
      const services = await this.svc.fetchOrderServices() ?? [];
      const offersByService = await Promise.all(
        services.map((service: any) =>
          this.svc
            .fetchServiceOffers(service.publicId)
            .then((offers: any) => (offers ?? []).map((offer: any) => ({ ...offer, _serviceName: service.name })))
            .catch(() => []),
        ),
      );
      const offers = offersByService.flat();
      this.formClients.set(clients);
      this.formOffers.set(offers);
      this.offersEmpty.set(offers.length === 0);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
      return;
    }
    this.modal?.show();
  }

  protected async onFormSubmit(): Promise<void> {
    this.formAlert.set(null);
    const values = { clientId: this.clientId(), offerId: this.offerId() };
    if (!values.clientId || !values.offerId) {
      this.formAlert.set({ type: 'danger', message: 'Veuillez choisir un client et une offre.' });
      return;
    }
    this.formBusy.set(true);
    try {
      await this.svc.createOrder(values);
      this.modal?.hide();
      this.clientId.set('');
      this.offerId.set('');
      this.toast.show('Commande confirmée : souscription activée.', 'success');
      this.page.set(0);
      await this.load();
    } catch (error: any) {
      this.formAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.formBusy.set(false);
    }
  }

  protected canProcess(order: any): boolean {
    return this.admin && order.status === 'PENDING';
  }
}
