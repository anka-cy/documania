/** File des tickets staff : filtres côté client et rendu progressif par lots de 20. */
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

import {
  FormatDatePipe,
  StatusBadgeClassPipe,
  TicketCategoryLabelPipe,
  TicketPriorityLabelPipe,
  TicketStatusLabelPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { TicketsService } from './tickets.service';

const CHUNK = 20;

@Component({
  selector: 'app-staff-tickets',
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
export class StaffTickets implements AfterViewInit, OnDestroy {
  private readonly svc = inject(TicketsService);

  protected readonly tickets = signal<any[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly searchTerm = signal('');
  protected readonly statusTerm = signal('');
  protected readonly categoryTerm = signal('');
  protected readonly priorityTerm = signal('');

  protected readonly visibleCount = signal(0);

  protected readonly filtered = computed(() => this.tickets().filter((ticket) => this.matches(ticket)));
  protected readonly visible = computed(() => this.filtered().slice(0, this.visibleCount()));
  protected readonly hasMore = computed(() => this.visibleCount() < this.filtered().length);
  protected readonly anyFilter = computed(
    () => !!(this.searchTerm() || this.statusTerm() || this.categoryTerm() || this.priorityTerm()),
  );

  protected readonly sentinel = viewChild<ElementRef<HTMLElement>>('sentinel');
  private observer: IntersectionObserver | null = null;

  constructor() {
    void this.load();
    effect(() => {
      const el = this.sentinel()?.nativeElement;
      if (el && this.observer) this.observer.observe(el);
    });
  }

  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && this.visibleCount() < this.filtered().length) {
            this.visibleCount.update((c) => c + CHUNK);
          }
        }
      },
      { rootMargin: '200px 0px', threshold: 0.01 },
    );
    // La sentinelle peut exister avant la fin de ngAfterViewInit : re-observe.
    const el = this.sentinel()?.nativeElement;
    if (el) this.observer.observe(el);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private matches(ticket: any): boolean {
    const statusTerm = this.statusTerm();
    const categoryTerm = this.categoryTerm();
    const priorityTerm = this.priorityTerm();
    const searchTerm = this.searchTerm();
    if (statusTerm && ticket.status !== statusTerm) return false;
    if (categoryTerm && ticket.category !== categoryTerm) return false;
    if (priorityTerm && ticket.priority !== priorityTerm) return false;
    if (!searchTerm) return true;
    const haystack = [
      ticket.subject, ticket.clientCompanyName, ticket.orderNumber,
      ticket.assignedToName, ticket.assignedToEmail, ticket.category,
    ].filter(Boolean).join(' ').toLowerCase();
    return haystack.includes(searchTerm);
  }

  private async load(): Promise<void> {
    try {
      const tickets = await this.svc.fetchTickets();
      this.tickets.set(tickets ?? []);
      this.resetVisible();
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  private resetVisible(): void {
    this.visibleCount.set(Math.min(CHUNK, this.filtered().length));
  }

  protected onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value.trim().toLowerCase());
    this.resetVisible();
  }

  protected onStatusChange(event: Event): void {
    this.statusTerm.set((event.target as HTMLSelectElement).value);
    this.resetVisible();
  }

  protected onCategoryChange(event: Event): void {
    this.categoryTerm.set((event.target as HTMLSelectElement).value);
    this.resetVisible();
  }

  protected onPriorityChange(event: Event): void {
    this.priorityTerm.set((event.target as HTMLSelectElement).value);
    this.resetVisible();
  }

  protected ticketHref(publicId: string): string {
    return `#/staff/tickets/${publicId}`;
  }
}
