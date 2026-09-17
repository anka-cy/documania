// Équivalent composant Angular de statCard() : DOM identique à la version
// vanilla.

import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  // Le host s'efface de la mise en page : le div .col-* interne reste bien
  // un enfant direct de la .row Bootstrap (sinon les cartes s'empilent).
  styles: [`:host { display: block; height: 100%; }`],
  template: `
    <div class="card stat-card stat-card--{{ variant }} h-100">
      <div class="card-body">
        <div class="stat-card__head">
          <span class="stat-card__label">{{ label }}</span>
          <span class="stat-card__icon"><i class="bi {{ icon }}" aria-hidden="true"></i></span>
        </div>
        <span class="stat-card__value">{{ value }}</span>
        @if (hint) {
          <span class="stat-card__hint">{{ hint }}</span>
        }
      </div>
    </div>
  `,
})
export class StatCard {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() hint: string | null = null;
  @Input() icon = '';
  /** 'primary' | 'success' | 'warning' | 'info'. */
  @Input() variant = 'primary';
}
