/*
  Abonnements (portail staff) : liste paginée côté serveur (recherche debouncée
  + filtre statut), annulation avec motif réservée à l'ADMIN.
*/

import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../shared/components/toast.service';
import { Pagination } from '../../../shared/components/pagination/pagination';
import {
  FormatDatePipe,
  FormatPricePipe,
  StatusBadgeClassPipe,
  SubscriptionStatusLabelPipe,
} from '../../../shared/pipes/formatters.pipes';
import { debounce, promptReason } from '../../../shared/utils/utils';
import { SubscriptionsService } from './subscriptions.service';

@Component({
  selector: 'app-staff-subscriptions',
  imports: [
    FormsModule,
    Pagination,
    FormatDatePipe,
    FormatPricePipe,
    StatusBadgeClassPipe,
    SubscriptionStatusLabelPipe,
  ],
  templateUrl: './subscriptions.html',
})
export class StaffSubscriptions {
  private readonly svc = inject(SubscriptionsService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly admin = this.auth.isAdmin();

  private readonly query = signal('');
  protected readonly status = signal('');
  protected readonly page = signal(0);

  protected readonly items = signal<any[]>([]);
  protected readonly totalPages = signal(1);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly unfiltered = computed(() => !this.query() && !this.status());

  private readonly debouncedLoad = debounce(() => void this.load(), 300);

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await this.svc.fetchSubscriptions({
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

  protected latestPeriod(subscription: any): any {
    const periods = subscription?.periods;
    return periods && periods.length ? periods[periods.length - 1] : null;
  }

  protected async cancelSubscription(id: string): Promise<void> {
    const reason = await promptReason("Annuler cet abonnement ?");
    if (reason === null) return;
    try {
      await this.svc.cancelSubscription(id, reason);
      this.toast.show('Abonnement annulé.', 'success');
      await this.load();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }
}
