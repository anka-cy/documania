/*
  Catalogue client (GET /api/client/catalogue) : commande via POST
  /api/client/orders, après confirmation puis redirection vers « Mes commandes ».
*/

import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ConfirmService } from '../../../shared/components/confirm.service';
import { ToastService } from '../../../shared/components/toast.service';
import { formatPrice } from '../../../shared/formatters/formatters';
import {
  FormatDateRangePipe,
  FormatDurationPipe,
  FormatPricePipe,
} from '../../../shared/pipes/formatters.pipes';
import { escapeHtml } from '../../../shared/utils/utils';
import { CatalogueService } from './catalogue.service';

@Component({
  selector: 'app-catalogue',
  imports: [FormatPricePipe, FormatDurationPipe, FormatDateRangePipe],
  templateUrl: './catalogue.html',
})
export class Catalogue {
  private readonly catalogue = inject(CatalogueService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly services = signal<any[]>([]);
  protected readonly orderingOfferId = signal<string | null>(null);

  constructor() {
    this.catalogue.fetchCatalogue()
      .then((data: any) => this.services.set(data))
      .catch((error: any) => this.error.set(error.message))
      .finally(() => this.loading.set(false));
  }

  protected async order(offer: any): Promise<void> {
    const confirmed = await this.confirm.confirm(
      `Commander l'offre « ${escapeHtml(offer.name)} » pour <strong>${formatPrice(offer.price)}</strong> ?<br>
       <span class="small text-muted">Votre commande sera validée par un conseiller avant activation.</span>`,
      { title: `Commander : ${escapeHtml(offer.name)}`, okText: 'Commander', okVariant: 'primary' },
    );
    if (!confirmed) return;

    this.orderingOfferId.set(offer.publicId);
    try {
      await this.catalogue.createOrder(offer.publicId);
      this.toast.show(`Commande « ${offer.name} » enregistrée.`, 'success');
      await this.router.navigate(['/client/orders']);
    } catch (error: any) {
      if (error.status === 409) {
        this.toast.show("Impossible de commander cette offre : " + error.message, 'danger');
      } else {
        this.toast.show(error.message, 'danger');
      }
    } finally {
      this.orderingOfferId.set(null);
    }
  }
}
