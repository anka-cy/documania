// Bloc d'erreur visible, utilisé par les pages (chargement de liste) et les
// formulaires.

import { escapeHtml } from '../utils/utils';

export function renderError(parent: Element, message: string): void {
  parent.innerHTML = `
    <div class="inline-error" role="alert">
      <span class="fw-semibold">Erreur :</span> ${escapeHtml(message)}
    </div>
  `;
}
