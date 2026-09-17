/*
  Mes commandes : annulation d'une commande PENDING après confirmation.
  Le ticket d'exigences lié (catégorie ORDER_REQUIREMENTS, ouvert
  automatiquement à la création) est résolu via la liste des tickets.
*/

import { Component, ElementRef, OnInit, computed, effect, inject, signal, viewChild } from '@angular/core';

import { ConfirmService } from '../../../../shared/components/confirm.service';
import { ToastService } from '../../../../shared/components/toast.service';
import { escapeHtml } from '../../../../shared/utils/utils';
import {
  FormatDatePipe,
  FormatPricePipe,
  OrderStatusLabelPipe,
  StatusBadgeClassPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { OrdersService } from './orders.service';

const CHUNK_SIZE = 20;

@Component({
  selector: 'app-client-orders',
  imports: [FormatDatePipe, FormatPricePipe, OrderStatusLabelPipe, StatusBadgeClassPipe],
  templateUrl: './orders.html',
})
export class ClientOrders implements OnInit {
  private readonly service = inject(OrdersService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  protected readonly orders = signal<any[]>([]);
  protected readonly ticketByOrder = signal<Record<string, string>>({});
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly visibleCount = signal(CHUNK_SIZE);
  protected readonly cancellingId = signal<string | null>(null);

  private readonly sentinel = viewChild<ElementRef>('sentinel');
  private observer: IntersectionObserver | null = null;

  constructor() {
    effect(() => {
      const element = this.sentinel()?.nativeElement;
      const visible = this.visibleCount();
      const total = this.orders().length;
      this.observer?.disconnect();
      this.observer = null;
      if (!element || visible >= total) return;
      this.observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting)) {
            this.visibleCount.update((count) => Math.min(count + CHUNK_SIZE, this.orders().length));
          }
        },
        { rootMargin: '200px 0px', threshold: 0.01 },
      );
      this.observer.observe(element);
    });
  }

  protected readonly isEmpty = computed(() => !this.loading() && !this.error() && this.orders().length === 0);

  async ngOnInit(): Promise<void> {
    let orders: any;
    try {
      orders = await this.service.fetchOrders();
    } catch (error: any) {
      this.error.set(error.message);
      this.loading.set(false);
      return;
    }
    this.orders.set(orders);
    this.ticketByOrder.set(await this.loadTicketByOrder());
    this.visibleCount.set(CHUNK_SIZE);
    this.loading.set(false);
  }

  protected ticketFor(order: any): string | undefined {
    return this.ticketByOrder()[order.publicId];
  }

  protected dash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }

  protected async onCancel(order: any): Promise<void> {
    const confirmed = await this.confirm.confirm(
      `Annuler la commande <strong>${escapeHtml(order.orderNumber)}</strong> ?`,
      { title: 'Annuler la commande', okText: 'Annuler la commande', okVariant: 'danger' },
    );
    if (!confirmed) return;

    this.cancellingId.set(order.publicId);
    try {
      await this.service.cancelOrder(order.publicId);
      this.toast.show('Commande annulée.', 'success');
      let fresh: any;
      try {
        fresh = await this.service.fetchOrders();
      } catch (error) {
        fresh = [];
      }
      this.orders.set(fresh);
      this.ticketByOrder.set(await this.loadTicketByOrder());
      this.visibleCount.set(CHUNK_SIZE);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    } finally {
      this.cancellingId.set(null);
    }
  }

  /** Liaison orderId -> ticket d'exigences. */
  private async loadTicketByOrder(): Promise<Record<string, string>> {
    const map: Record<string, string> = {};
    try {
      const tickets = await this.service.fetchTickets();
      for (const ticket of tickets) {
        if (ticket.category === 'ORDER_REQUIREMENTS' && ticket.orderId) {
          map[ticket.orderId] = ticket.publicId;
        }
      }
    } catch (error) {
      // La liaison d'exigences est un confort : on garde la liste des commandes.
    }
    return map;
  }
}
