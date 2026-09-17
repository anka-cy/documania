/*
  Facture d'une commande confirmée (GET /api/client/orders/{id}/invoice).
  L'identifiant vient de params["id"] ou params["orderId"].
*/

import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pageParams } from '../../../core/router/params';
import { FormatDatePipe, FormatPricePipe } from '../../../shared/pipes/formatters.pipes';
import { InvoiceService } from './invoice.service';

@Component({
  selector: 'app-invoice',
  imports: [FormatDatePipe, FormatPricePipe],
  templateUrl: './invoice.html',
})
export class Invoice {
  private readonly invoiceService = inject(InvoiceService);
  private readonly route = inject(ActivatedRoute);

  protected readonly invoice = signal<any | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const params = pageParams(this.route.snapshot);
    const orderId = params && (params['id'] || params['orderId']);

    if (!orderId) {
      this.error.set('Identifiant de commande manquant.');
      return;
    }

    this.invoiceService.fetchInvoice(orderId)
      .then((data: any) => this.invoice.set(data))
      .catch((error: any) => this.error.set(error.message || 'Impossible de charger la facture.'));
  }
}
