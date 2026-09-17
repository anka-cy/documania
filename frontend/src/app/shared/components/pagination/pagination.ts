// Équivalent composant Angular de renderPager (Spring Data Page, page
// 0-based) : DOM identique à la version vanilla (variante texte ou chevrons).

import { Component, EventEmitter, Input, Output, computed } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  // Le host s'efface de la mise en page : le conteneur .d-flex.justify-content-between
  // de la page aligne bien « Page x / N » à gauche et les boutons à droite
  // (comme le renderPager vanilla qui injectait les deux dans ce conteneur).
  styles: [':host { display: contents; }'],
  template: `
    <span class="text-muted small">{{ info() }}</span>
    @if (variant === 'icons') {
      <div class="btn-group">
        <button class="btn btn-sm btn-outline-secondary" [id]="idPrefix + '-prev'" [disabled]="safePage() <= 0" aria-label="Page précédente"
                (click)="go(safePage() - 1)"><i class="bi bi-chevron-left"></i></button>
        <button class="btn btn-sm btn-outline-secondary" [id]="idPrefix + '-next'" [disabled]="safePage() >= safeTotal() - 1" aria-label="Page suivante"
                (click)="go(safePage() + 1)"><i class="bi bi-chevron-right"></i></button>
      </div>
    } @else {
      <div class="d-flex gap-2">
        <button class="btn btn-sm btn-outline-secondary" [id]="idPrefix + '-prev'" [disabled]="safePage() <= 0" (click)="go(safePage() - 1)">Précédent</button>
        <button class="btn btn-sm btn-outline-secondary" [id]="idPrefix + '-next'" [disabled]="safePage() >= safeTotal() - 1" (click)="go(safePage() + 1)">Suivant</button>
      </div>
    }
  `,
})
export class Pagination {
  /** id des boutons : `${idPrefix}-prev` / `${idPrefix}-next`. */
  @Input() idPrefix = 'pager';
  /** Page courante (0-based, comme Spring). */
  @Input() page = 0;
  @Input() totalPages = 1;
  @Input() totalElements: number | null = null;
  @Input() pageSize = 20;
  @Input() itemCount = 0;
  @Input() itemLabel = '';
  /** 'text' (Précédent/Suivant) | 'icons' (chevrons). */
  @Input() variant: 'text' | 'icons' = 'text';
  /** Nouvelle page (0-based) choisie par l'utilisateur. */
  @Output() pageChange = new EventEmitter<number>();

  protected safeTotal = computed(() => Math.max(Number(this.totalPages) || 0, 1));
  protected safePage = computed(() => Math.min(Math.max(Number(this.page) || 0, 0), this.safeTotal() - 1));

  protected info = computed(() => {
    let text = `Page ${this.safePage() + 1} / ${this.safeTotal()}`;
    if (this.totalElements !== null && this.totalElements !== undefined) {
      const total = Number(this.totalElements) || 0;
      const start = total === 0 ? 0 : this.safePage() * this.pageSize + 1;
      const end = Math.min(start + this.itemCount - 1, total);
      text += ` · ${this.itemLabel} ${start}–${end} sur ${total}`;
    }
    return text;
  });

  protected go(next: number): void {
    if (next >= 0 && next <= this.safeTotal() - 1) this.pageChange.emit(next);
  }
}
