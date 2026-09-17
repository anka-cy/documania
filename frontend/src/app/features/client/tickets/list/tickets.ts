/*
  Mes tickets : recherche locale, filtre par statut et création via modale
  (priorité déduite de la catégorie : SERVICE_DOWN → haute, sinon moyenne).
*/

import { Component, ElementRef, OnDestroy, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ToastService } from '../../../../shared/components/toast.service';
import {
  FormatDatePipe,
  StatusBadgeClassPipe,
  TicketCategoryLabelPipe,
  TicketPriorityLabelPipe,
  TicketStatusLabelPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { TicketsService } from './tickets.service';

interface AlertState {
  type: string;
  message: string;
}

const CHUNK_SIZE = 20;

@Component({
  selector: 'app-client-tickets',
  imports: [
    FormsModule,
    FormatDatePipe,
    StatusBadgeClassPipe,
    TicketCategoryLabelPipe,
    TicketPriorityLabelPipe,
    TicketStatusLabelPipe,
  ],
  templateUrl: './tickets.html',
})
export class ClientTickets implements OnDestroy {
  private readonly ticketsService = inject(TicketsService);
  private readonly toast = inject(ToastService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly tickets = signal<any[]>([]);
  protected readonly interacted = signal(false);

  protected readonly searchTerm = signal('');
  protected readonly statusTerm = signal('');

  protected readonly visibleCount = signal(CHUNK_SIZE);
  private observer: IntersectionObserver | null = null;
  private readonly sentinel = viewChild<ElementRef<HTMLTableRowElement>>('sentinel');

  constructor() {
    effect(() => {
      const count = this.visibleCount();
      const native = this.sentinel()?.nativeElement;
      this.observer?.disconnect();
      this.observer = null;
      if (!native || count >= this.filtered().length) return;
      this.observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting) && this.visibleCount() < this.filtered().length) {
            this.visibleCount.update((c) => c + CHUNK_SIZE);
          }
        },
        { rootMargin: '200px 0px', threshold: 0.01 },
      );
      this.observer.observe(native);
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  /* Formulaire de création (modale) */

  protected category = '';
  protected subject = '';
  protected description = '';
  protected readonly descriptionLength = signal(0);
  protected readonly descErrorVisible = signal(false);
  protected readonly descTooShort = signal(false);
  protected readonly busy = signal(false);
  protected readonly alert = signal<AlertState | null>(null);

  private readonly modalElement = viewChild<ElementRef<HTMLElement>>('newTicketModal');

  async ngOnInit(): Promise<void> {
    try {
      this.tickets.set(await this.ticketsService.fetchTickets());
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  protected readonly filtered = computed(() => {
    const searchTerm = this.searchTerm();
    const statusTerm = this.statusTerm();
    return this.tickets().filter((ticket: any) => {
      const statusOk = !statusTerm || ticket.status === statusTerm;
      if (!statusOk) return false;
      if (!searchTerm) return true;
      const haystack = [ticket.subject, ticket.orderNumber, ticket.category, ticket.status]
        .filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(searchTerm);
    });
  });

  protected readonly visibleTickets = computed(() => this.filtered().slice(0, this.visibleCount()));

  protected onSearchTermChange(value: string): void {
    this.searchTerm.set((value || '').trim().toLowerCase());
    this.onFilterChange();
  }

  protected onStatusTermChange(value: string): void {
    this.statusTerm.set(value);
    this.onFilterChange();
  }

  private onFilterChange(): void {
    this.interacted.set(true);
    this.visibleCount.set(CHUNK_SIZE);
  }

  protected onDescriptionInput(value: string): void {
    const len = (value || '').length;
    this.descriptionLength.set(len);
    this.descErrorVisible.set(!(len >= 5 || len === 0));
    this.descTooShort.set(len < 5);
  }

  protected async onCreateTicket(): Promise<void> {
    this.alert.set(null);
    const data: any = {
      category: this.category,
      subject: this.subject.trim(),
      description: this.description.trim(),
    };
    if (!data.category || !data.subject || data.description.length < 5) {
      this.alert.set({ type: 'danger', message: 'Veuillez remplir tous les champs (description ≥ 5 car.).' });
      return;
    }
    data.priority = data.category === 'SERVICE_DOWN' ? 'HIGH' : 'MEDIUM';
    this.busy.set(true);
    try {
      await this.ticketsService.createTicket(data);
      const modalEl = this.modalElement();
      if (modalEl) bootstrap.Modal.getOrCreateInstance(modalEl.nativeElement).hide();
      this.category = '';
      this.subject = '';
      this.description = '';
      this.descriptionLength.set(0);
      this.descErrorVisible.set(false);
      this.descTooShort.set(false);
      this.toast.show('Ticket créé avec succès.', 'success');
      this.tickets.set(await this.ticketsService.fetchTickets());
      this.visibleCount.set(CHUNK_SIZE);
    } catch (error: any) {
      this.alert.set({ type: 'danger', message: error.message });
    } finally {
      this.busy.set(false);
    }
  }
}
