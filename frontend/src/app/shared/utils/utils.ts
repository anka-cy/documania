// Utilitaires DOM et paramètres d'URL, réutilisables par toutes les pages.

/** Échappe un texte pour l'insérer sans risque dans du HTML. */
export function escapeHtml(value: unknown): string {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string | null,
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined && text !== null) node.textContent = text;
  return node;
}

/**
 * Analyse la partie hash de l'URL en {(chemin), params}.
 * Ex. "#/client/orders?page=2" -> path "/client/orders", params { page: "2" }.
 */
export function parseHash(hash: string): { path: string; params: Record<string, string> } {
  const cleaned = (hash || '').replace(/^#/, '');
  const [pathPart, queryPart] = cleaned.split('?');
  const params = new URLSearchParams(queryPart || '');
  const paramsObject: Record<string, string> = {};
  for (const [key, value] of params.entries()) {
    paramsObject[key] = value;
  }
  return { path: pathPart || '/', params: paramsObject };
}

export function orDash(value: unknown): string {
  return value === null || value === undefined || value === '' ? '—' : escapeHtml(value);
}

/** Initiales à partir d'un nom complet (pour les avatars). */
export function initials(firstName?: string | null, lastName?: string | null): string {
  return `${(firstName || '?').charAt(0)}${(lastName || '?').charAt(0)}`.toUpperCase();
}

/**
 * Demande un motif obligatoire via un modal Bootstrap stylisé.
 * Le backend reste l'autorité : min 5, max 500 caractères.
 * @returns motif saisi, ou null si annulé.
 */
export function promptReason(message: string): Promise<string | null> {
  return new Promise((resolve) => {
    const MODAL_ID = 'prompt-reason-modal';
    let modal = document.getElementById(MODAL_ID);
    if (!modal) {
      modal = document.createElement('div');
      modal.id = MODAL_ID;
      modal.className = 'modal fade';
      modal.tabIndex = -1;
      modal.setAttribute('aria-hidden', 'true');
      modal.innerHTML = `
        <div class="modal-dialog modal-dialog-centered modal-fullscreen-sm-down">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title" id="${MODAL_ID}-title"></h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
            </div>
            <div class="modal-body">
              <p class="text-muted mb-3" id="${MODAL_ID}-message"></p>
              <div class="mb-0">
                <label class="form-label" for="${MODAL_ID}-input">Motif <span class="text-muted">(5 à 500 caractères)</span></label>
                <textarea class="form-control" id="${MODAL_ID}-input" rows="3" maxlength="500" placeholder="Décrivez la raison…"></textarea>
                <div class="d-flex justify-content-between mt-1">
                  <small class="text-danger d-none" id="${MODAL_ID}-error">Le motif doit contenir au moins 5 caractères.</small>
                  <small class="text-muted ms-auto"><span id="${MODAL_ID}-count">0</span> / 500</small>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Annuler</button>
              <button type="button" class="btn btn-danger" id="${MODAL_ID}-confirm" disabled>Confirmer</button>
            </div>
          </div>
        </div>
      `;
      document.body.appendChild(modal);
    }

    const title = modal.querySelector(`#${MODAL_ID}-title`) as HTMLElement;
    const messageEl = modal.querySelector(`#${MODAL_ID}-message`) as HTMLElement;
    const textarea = modal.querySelector(`#${MODAL_ID}-input`) as HTMLTextAreaElement;
    const counter = modal.querySelector(`#${MODAL_ID}-count`) as HTMLElement;
    const error = modal.querySelector(`#${MODAL_ID}-error`) as HTMLElement;
    const confirmBtn = modal.querySelector(`#${MODAL_ID}-confirm`) as HTMLButtonElement;

    title.textContent = message;
    messageEl.textContent = 'Cette action est irréversible. Veuillez entrer un motif pour continuer.';
    textarea.value = '';
    counter.textContent = '0';
    error.classList.add('d-none');
    confirmBtn.disabled = true;

    const bsModal = new bootstrap.Modal(modal);

    function updateState() {
      const len = textarea.value.trim().length;
      counter.textContent = String(textarea.value.length);
      error.classList.toggle('d-none', len >= 5 || len === 0);
      confirmBtn.disabled = len < 5;
    }

    textarea.addEventListener('input', updateState);

    function cleanup() {
      textarea.removeEventListener('input', updateState);
      confirmBtn.removeEventListener('click', onConfirm);
      modal!.removeEventListener('hidden.bs.modal', onCancel);
    }

    function onConfirm() {
      const trimmed = textarea.value.trim();
      if (trimmed.length < 5) return;
      cleanup();
      bsModal.hide();
      resolve(trimmed);
    }

    function onCancel() {
      cleanup();
      resolve(null);
    }

    confirmBtn.addEventListener('click', onConfirm);
    modal.addEventListener('hidden.bs.modal', onCancel);

    bsModal.show();
    setTimeout(() => textarea.focus(), 150);
  });
}

export function infoRow(label: string, value: string): string {
  return `
    <div class="d-flex justify-content-between py-2 border-bottom">
      <dt class="text-muted mb-0">${label}</dt>
      <dd class="mb-0">${value}</dd>
    </div>
  `;
}

/**
 * Retarde l'exécution d'une fonction jusqu'à la fin d'une période de silence.
 * Utilisé par la recherche en direct : un seul appel après la frappe.
 */
export function debounce<A extends unknown[]>(fn: (...args: A) => void, ms = 300): (...args: A) => void {
  let timer: ReturnType<typeof setTimeout> | null = null;
  return (...args: A) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

/**
 * Filtre le schéma d'une URL avant insertion dans un href/src : n'autorise
 * que les liens internes (#…, /…) et http(s). Rejette javascript:, data:,
 * //site- tiers (redirection protocol-relative), etc. Le backend reste la
 * source des liens ; ce filtre est un garde-fou supplémentaire, jamais une
 * dispense d'échappement — à combiner avec escapeHtml sur l'attribut.
 * @param url URL candidate.
 * @param fallback Valeur de repli si le schéma est refusé.
 */
export function safeUrl(url: string | null | undefined, fallback = '#'): string {
  const value = (url === null || url === undefined) ? '' : String(url).trim();
  if (!value) return fallback;
  if (value.startsWith('#')) return value; // lien interne du SPA
  if (value.startsWith('/') && !value.startsWith('//')) return value; // chemin même origine
  if (/^https?:\/\//i.test(value)) return value; // lien absolu http(s)
  return fallback; // javascript:, data:, mailto:, //tiers…, etc.
}
