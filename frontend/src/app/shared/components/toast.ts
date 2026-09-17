// Notifications non intrusives en haut à droite, via l'API Toast de Bootstrap
// (chargée par index.html).

import { el, escapeHtml } from '../utils/utils';

const TOAST_TYPES: Record<string, string> = {
  success: 'bg-success text-white',
  danger: 'bg-danger text-white',
  warning: 'bg-warning text-dark',
  info: 'bg-info text-white',
};

function ensureContainer(): Element {
  let container = document.querySelector('.toast-container-custom');
  if (!container) {
    container = el('div', 'toast-container-custom');
    document.body.appendChild(container);
  }
  return container;
}

/** @param type 'success' | 'danger' | 'warning' | 'info' */
export function showToast(message: string, type = 'info'): void {
  if (typeof bootstrap === 'undefined') {
    // Bootstrap non chargé (edge case) : repli sur une alerte native.
    window.alert(message);
    return;
  }
  const container = ensureContainer();
  const toast = el('div', `toast align-items-center text-white border-0 ${TOAST_TYPES[type] || TOAST_TYPES.info}`);
  toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body">${escapeHtml(message)}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto"
              data-bs-dismiss="toast" aria-label="Fermer"></button>
    </div>
  `;
  container.appendChild(toast);
  const instance = new bootstrap.Toast(toast, { delay: 4000 });
  instance.show();
  toast.addEventListener('hidden.bs.toast', () => toast.remove());
}
