// Pied de pagination unique pour toutes les listes paginées côté serveur
// (Spring Data Page : { items, page, size, totalElements, totalPages },
// page 0-based) : un seul style et un seul comportement à maintenir.

export interface PagerOptions {
  idPrefix?: string;
  page?: number;
  totalPages?: number;
  totalElements?: number | null;
  pageSize?: number;
  itemCount?: number;
  itemLabel?: string;
  variant?: 'text' | 'icons';
  onPage?: (nouvellePage: number) => void;
}

/** Rend un pied de pagination dans `mount` et lie les boutons. */
export function renderPager(mount: Element | null, options: PagerOptions = {}): void {
  if (!mount) return;
  const {
    idPrefix = 'pager',
    page = 0,
    totalPages = 1,
    totalElements = null,
    pageSize = 20,
    itemCount = 0,
    itemLabel = '',
    variant = 'text',
    onPage = () => {},
  } = options;

  const safeTotal = Math.max(Number(totalPages) || 0, 1);
  const safePage = Math.min(Math.max(Number(page) || 0, 0), safeTotal - 1);

  let info = `Page ${safePage + 1} / ${safeTotal}`;
  if (totalElements !== null && totalElements !== undefined) {
    const total = Number(totalElements) || 0;
    const start = total === 0 ? 0 : safePage * pageSize + 1;
    const end = Math.min(start + itemCount - 1, total);
    info += ` · ${itemLabel} ${start}–${end} sur ${total}`;
  }

  const buttons = variant === 'icons'
    ? `
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" id="${idPrefix}-prev" ${safePage > 0 ? '' : 'disabled'} aria-label="Page précédente"><i class="bi bi-chevron-left"></i></button>
        <button class="btn btn-sm btn-outline-secondary" id="${idPrefix}-next" ${safePage < safeTotal - 1 ? '' : 'disabled'} aria-label="Page suivante"><i class="bi bi-chevron-right"></i></button>
      </div>`
    : `
      <div class="d-flex gap-2">
        <button class="btn btn-sm btn-outline-secondary" id="${idPrefix}-prev" ${safePage > 0 ? '' : 'disabled'}>Précédent</button>
        <button class="btn btn-sm btn-outline-secondary" id="${idPrefix}-next" ${safePage < safeTotal - 1 ? '' : 'disabled'}>Suivant</button>
      </div>`;

  mount.innerHTML = `<span class="text-muted small">${info}</span>${buttons}`;

  mount.querySelector(`#${idPrefix}-prev`)!.addEventListener('click', () => {
    if (safePage > 0) onPage(safePage - 1);
  });
  mount.querySelector(`#${idPrefix}-next`)!.addEventListener('click', () => {
    if (safePage < safeTotal - 1) onPage(safePage + 1);
  });
}
