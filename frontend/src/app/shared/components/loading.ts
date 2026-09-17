// Bloc « chargement… » avec spinner, posé dans un conteneur avant chaque
// appel API.

import { escapeHtml } from '../utils/utils';

function clearLoading(container: Element) {
  const loader = container.querySelector('.loading-block');
  if (loader) loader.remove();
}

export function showLoading(container: Element, message = 'Chargement…'): void {
  clearLoading(container); // retire tout loader résiduel avant d'afficher le nouveau
  container.innerHTML = `
    <div class="loading-block" role="status" aria-live="polite">
      <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
      <span>${escapeHtml(message)}</span>
    </div>
  `;
}
