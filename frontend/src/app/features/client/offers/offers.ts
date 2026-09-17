/*
  Détail d'une offre : retrouvée dans le catalogue par son publicId
  (le catalogue est la source de vérité) ; commande via POST /api/client/orders.
*/

import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { pageParams } from '../../../core/router/params';
import { ConfirmService } from '../../../shared/components/confirm.service';
import { ToastService } from '../../../shared/components/toast.service';
import { formatPrice } from '../../../shared/formatters/formatters';
import {
  FormatDateRangePipe,
  FormatDurationPipe,
  FormatPricePipe,
} from '../../../shared/pipes/formatters.pipes';
import { escapeHtml } from '../../../shared/utils/utils';
import { OffersService } from './offers.service';

@Component({
  selector: 'app-offers',
  imports: [FormatPricePipe, FormatDurationPipe, FormatDateRangePipe],
  templateUrl: './offers.html',
})
export class Offers {
  private readonly offers = inject(OffersService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly offer = signal<any | null>(null);
  protected readonly service = signal<any | null>(null);
  protected readonly busy = signal(false);

  constructor() {
    const params = pageParams(this.route.snapshot);
    const offerId = params['id'];

    this.offers.fetchCatalogue()
      .then((catalogue: any) => {
        for (const candidate of catalogue) {
          const found = candidate.offers.find((entry: any) => entry.publicId === offerId);
          if (found) {
            this.offer.set(found);
            this.service.set(candidate);
            return;
          }
        }
        this.notFound.set(true);
      })
      .catch((error: any) => this.error.set(error.message))
      .finally(() => this.loading.set(false));
  }

  protected async order(): Promise<void> {
    const offer = this.offer();
    if (!offer) return;

    const confirmed = await this.confirm.confirm(
      `Commander l'offre « ${escapeHtml(offer.name)} » pour <strong>${formatPrice(offer.price)}</strong> ?`,
      { title: `Commander : ${escapeHtml(offer.name)}`, okText: 'Commander', okVariant: 'primary' },
    );
    if (!confirmed) return;

    this.busy.set(true);
    try {
      await this.offers.createOrder(offer.publicId);
      this.toast.show(`Commande « ${offer.name} » enregistrée.`, 'success');
      await this.router.navigate(['/client/orders']);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    } finally {
      this.busy.set(false);
    }
  }
}
