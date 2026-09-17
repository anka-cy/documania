// Enveloppe Angular de showToast : les composants injectent ToastService au
// lieu d'importer la fonction libre.

import { Injectable } from '@angular/core';

import { showToast } from './toast';

@Injectable({ providedIn: 'root' })
export class ToastService {
  /** Affiche une notification toast ('success' | 'danger' | 'warning' | 'info'). */
  show(message: string, type = 'info'): void {
    showToast(message, type);
  }
}
