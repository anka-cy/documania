/* Tableau de bord client : synthèse commandes, abonnements et tickets, chargés en parallèle. */

import { Component, computed, inject, signal } from '@angular/core';

import { StatCard } from '../../../shared/components/stat-card/stat-card';
import {
  FormatDatePipe,
  OrderStatusLabelPipe,
  StatusBadgeClassPipe,
  SubscriptionStatusLabelPipe,
} from '../../../shared/pipes/formatters.pipes';
import { DashboardService } from './dashboard.service';

@Component({
  selector: 'app-client-dashboard',
  imports: [StatCard, FormatDatePipe, OrderStatusLabelPipe, StatusBadgeClassPipe, SubscriptionStatusLabelPipe],
  templateUrl: './dashboard.html',
})
export class ClientDashboard {
  private readonly dashboard = inject(DashboardService);

  // Trois sources indépendantes : null = pas encore chargée.
  protected readonly ordersLoading = signal(true);
  protected readonly ordersError = signal<string | null>(null);
  protected readonly ordersData = signal<any[] | null>(null);

  protected readonly subscriptionsLoading = signal(true);
  protected readonly subscriptionsError = signal<string | null>(null);
  protected readonly subscriptionsData = signal<any[] | null>(null);

  protected readonly ticketsData = signal<any[] | null>(null);

  protected readonly recentOrders = computed(() => (this.ordersData() || []).slice(0, 5));
  protected readonly pendingOrdersCount = computed(
    () => (this.ordersData() || []).filter((o: any) => o.status === 'PENDING').length,
  );

  protected readonly recentSubscriptions = computed(() => (this.subscriptionsData() || []).slice(0, 5));
  protected readonly activeSubscriptionsCount = computed(
    () => (this.subscriptionsData() || []).filter((s: any) => s.status === 'ACTIVE').length,
  );
  protected readonly inactiveSubscriptionsCount = computed(
    () => (this.subscriptionsData() || []).filter((s: any) => s.status !== 'ACTIVE').length,
  );

  protected readonly openTicketsCount = computed(
    () => (this.ticketsData() || []).filter((t: any) => t.status === 'OPEN' || t.status === 'IN_PROGRESS').length,
  );

  // Alertes (commande en attente, échéance proche)

  protected readonly pendingOrder = computed(
    () => (this.ordersData() || []).find((o: any) => o.status === 'PENDING') || null,
  );

  protected readonly expiringSoonCount = computed(() =>
    (this.subscriptionsData() || []).filter((s: any) => {
      if (s.status !== 'ACTIVE') return false;
      const [y, m, d] = String(s.endDate).split('-').map(Number);
      const end = new Date(y, m - 1, d);
      const diff = Math.ceil((end.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
      return diff >= 0 && diff <= 30;
    }).length,
  );

  constructor() {
    this.dashboard.fetchOrders()
      .then((data: any) => this.ordersData.set(data))
      .catch((error: any) => this.ordersError.set(error.message))
      .finally(() => this.ordersLoading.set(false));

    this.dashboard.fetchSubscriptions()
      .then((data: any) => this.subscriptionsData.set(data))
      .catch((error: any) => this.subscriptionsError.set(error.message))
      .finally(() => this.subscriptionsLoading.set(false));

    this.dashboard.fetchTickets()
      .then((data: any) => this.ticketsData.set(data))
      .catch(() => { /* échec des tickets ignoré : le tableau reste utilisable */ });
  }
}
