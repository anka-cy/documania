// Enveloppe Angular de confirmAction (modale Bootstrap) : les composants
// injectent ConfirmService et obtiennent la même Promise<boolean>.

import { Injectable } from '@angular/core';

import { confirmAction } from './confirmDialog';

export interface ConfirmOptions {
  title?: string;
  okText?: string;
  cancelText?: string;
  /** 'primary' (défaut) ou 'danger' pour les suppressions. */
  okVariant?: string;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  /**
   * Affiche une boîte de confirmation. `message` est un gabarit HTML
   * (les données utilisateur doivent être passées via escapeHtml()).
   * @returns true si l'utilisateur confirme.
   */
  confirm(message: string, options: ConfirmOptions = {}): Promise<boolean> {
    return confirmAction(message, options);
  }
}
