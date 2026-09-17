// Petits helpers réutilisés par les pages : sélection, état « chargement »
// des boutons, alertes et rendu progressif de listes.

import { escapeHtml } from '../utils/utils';

export function query<K extends keyof HTMLElementTagNameMap>(
  container: Element | Document | null,
  selector: K,
): HTMLElementTagNameMap[K] | null;
export function query(container: Element | Document | null, selector: string): Element | null;
export function query(container: Element | Document | null, selector: string): Element | null {
  return container ? container.querySelector(selector) : null;
}

/**
 * Passe un bouton en état « chargement » (spinner + libellé) ou le restaure.
 */
export function showBusy(button: HTMLButtonElement | null, label: string): void {
  if (!button) return;
  // Rappel : mémoriser le libellé d'origine au premier appel.
  if (!button.dataset.originalLabel) {
    button.dataset.originalLabel = button.textContent?.trim() ?? '';
  }
  const isBusy = label !== button.dataset.originalLabel;
  button.disabled = isBusy;
  button.innerHTML = isBusy
    ? `<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>${label}`
    : label;
}

/** Active/désactive tous les champs et boutons d'un formulaire. */
export function setFormDisabled(form: HTMLFormElement, value: boolean): void {
  Array.from(form.querySelectorAll<HTMLButtonElement | HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
    'button, input, select, textarea',
  )).forEach((node) => {
    node.disabled = value;
  });
}

/**
 * Affiche un message dans une zone d'alerte d'id donné (vide = efface).
 * @param alertId id du conteneur cible (ex. 'login-alert').
 * @param message Message à afficher, ou '' pour vider.
 * @param type    Variante Bootstrap : 'danger' (défaut), 'success', 'info'…
 */
export function showAlert(alertId: string, message: string, type = 'danger'): void {
  const node = window.document.getElementById(alertId);
  if (!node) return;
  node.innerHTML = message
    ? `<div class="alert alert-${type} py-2" role="alert">${escapeHtml(message)}</div>`
    : '';
}

/**
 * Rendu progressif d'une liste (infinite scroll).
 * Affiche d'abord `chunkSize` éléments, puis en ajoute au fur et à mesure
 * que l'utilisateur s'approche du bas, via un IntersectionObserver posé sur
 * une « sentinelle » en fin de liste. Le rendu de chaque lot se fait en une
 * seule insertion DOM (sans reflow à chaque ligne).
 */
export function setupProgressiveScroll<T extends object>(
  container: HTMLElement,
  renderItem: (item: T, index: number) => string,
  chunkSize = 20,
): { update: (items: T[]) => void; destroy: () => void } {
  // Une <tr> dans un <tbody>, une <div> ailleurs.
  const sentinel = document.createElement(container.tagName === 'TBODY' ? 'tr' : 'div');
  sentinel.className = 'scroll-sentinel';
  sentinel.setAttribute('aria-hidden', 'true');
  sentinel.style.height = '1px';

  let items: T[] = [];
  let rendered = 0;
  let observer: IntersectionObserver | null = null;

  function appendNextChunk() {
    // Sentinelle détachée du document (page remplacée par le routeur) :
    // la liste n'a plus besoin d'être suivie, on libère l'observateur.
    if (!sentinel.isConnected) {
      destroy();
      return;
    }
    const end = Math.min(rendered + chunkSize, items.length);
    let html = '';
    for (let i = rendered; i < end; i += 1) {
      html += renderItem(items[i], i);
    }
    sentinel.insertAdjacentHTML('beforebegin', html);
    rendered = end;
    if (rendered >= items.length) observer?.unobserve(sentinel);
  }

  function observe() {
    observer?.disconnect();
    observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) appendNextChunk();
      },
      { rootMargin: '200px 0px', threshold: 0.01 },
    );
    if (rendered < items.length) observer.observe(sentinel);
  }

  function update(newItems: T[]) {
    observer?.disconnect();
    items = newItems;
    rendered = 0;
    container.innerHTML = '';
    if (!items.length) return;
    container.appendChild(sentinel);
    appendNextChunk();
    observe();
  }

  function destroy() {
    observer?.disconnect();
  }

  return { update, destroy };
}

/**
 * Bascule l'affichage (texte/password) d'un champ mot de passe.
 * À brancher sur un bouton [data-toggle-password="<id du champ>"].
 */
export function bindTogglePassword(scope: ParentNode): void {
  scope.querySelectorAll<HTMLElement>('[data-toggle-password]').forEach((toggle) => {
    toggle.addEventListener('click', () => {
      const target = document.getElementById(toggle.dataset.togglePassword ?? '') as HTMLInputElement | null;
      if (!target) return;
      const hidden = target.type === 'password';
      target.type = hidden ? 'text' : 'password';
      toggle.textContent = hidden ? 'Masquer' : 'Afficher';
    });
  });
}
