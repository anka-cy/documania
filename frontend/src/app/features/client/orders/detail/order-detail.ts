/*
  Détail d'une commande : fiche avec statut, détail de l'offre, motif de rejet
  éventuel et lien vers la facture des commandes confirmées.
*/

import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { pageParams } from '../../../../core/router/params';
import { ToastService } from '../../../../shared/components/toast.service';
import {
  FormatDatePipe,
  FormatDateTimePipe,
  FormatDurationPipe,
  FormatPricePipe,
  OrderStatusLabelPipe,
  StatusBadgeClassPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { OrderDetailService } from './order-detail.service';

@Component({
  selector: 'app-client-order-detail',
  imports: [
    FormatDatePipe,
    FormatDateTimePipe,
    FormatDurationPipe,
    FormatPricePipe,
    OrderStatusLabelPipe,
    StatusBadgeClassPipe,
  ],
  templateUrl: './order-detail.html',
})
export class ClientOrderDetail implements OnInit {
  private readonly service = inject(OrderDetailService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly order = signal<any | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly busy = signal(false);

  async ngOnInit(): Promise<void> {
    const orderId = pageParams(this.route.snapshot)['id'];
    let orders: any;
    try {
      orders = await this.service.fetchOrders();
    } catch (error: any) {
      this.error.set(error.message);
      this.loading.set(false);
      return;
    }
    const order = orders.find((entry: any) => entry.publicId === orderId);
    if (!order) {
      this.notFound.set(true);
    } else {
      this.order.set(order);
    }
    this.loading.set(false);
  }

  protected breadcrumbLabel(): string {
    const order = this.order();
    return order ? `Commande ${order.orderNumber}` : 'Commande';
  }

  protected dash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }

  protected numOrDash(value: unknown): string {
    return String(value ?? '—');
  }

  /** Annulation sans confirmation, avec redirection vers la liste après succès. */
  protected async onCancel(): Promise<void> {
    const order = this.order();
    if (!order) return;
    this.busy.set(true);
    try {
      await this.service.cancelOrder(order.publicId);
      await this.router.navigate(['/client/orders']);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
      this.busy.set(false);
    }
  }
}
