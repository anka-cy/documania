// État vide d'une liste : message explicite et neutre.

import { escapeHtml } from '../utils/utils';

export function renderEmpty(parent: Element, message = 'Aucune donnée à afficher.'): void {
  parent.innerHTML = `
    <div class="empty-state">
      <p class="mb-0">${escapeHtml(message)}</p>
    </div>
  `;
}
