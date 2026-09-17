/* Détail d'un abonnement : fiche globale + historique des périodes. */

import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pageParams } from '../../../../core/router/params';
import {
  FormatDatePipe,
  FormatDurationPipe,
  FormatPricePipe,
  StatusBadgeClassPipe,
  SubscriptionStatusLabelPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { SubscriptionDetailService } from './subscription-detail.service';

@Component({
  selector: 'app-client-subscription-detail',
  imports: [
    FormatDatePipe,
    FormatDurationPipe,
    FormatPricePipe,
    StatusBadgeClassPipe,
    SubscriptionStatusLabelPipe,
  ],
  templateUrl: './subscription-detail.html',
})
export class ClientSubscriptionDetail implements OnInit {
  private readonly service = inject(SubscriptionDetailService);
  private readonly route = inject(ActivatedRoute);

  protected readonly subscription = signal<any | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notFound = signal(false);

  async ngOnInit(): Promise<void> {
    const subscriptionId = pageParams(this.route.snapshot)['id'];
    let subscriptions: any;
    try {
      subscriptions = await this.service.fetchSubscriptions();
    } catch (error: any) {
      this.error.set(error.message);
      this.loading.set(false);
      return;
    }
    const subscription = subscriptions.find((entry: any) => entry.publicId === subscriptionId);
    if (!subscription) {
      this.notFound.set(true);
    } else {
      this.subscription.set(subscription);
    }
    this.loading.set(false);
  }

  /** Période courante : dernière entrée de `periods`, ou null. */
  protected currentPeriod(subscription: any): any {
    const periods = subscription.periods || [];
    return periods.length ? periods[periods.length - 1] : null;
  }

  protected dash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }

  protected numOrDash(value: unknown): string {
    return String(value ?? '—');
  }
}
