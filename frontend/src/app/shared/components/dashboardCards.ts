// Carte KPI des tableaux de bord (client et staff), variante de couleur
// 'primary' | 'success' | 'warning' | 'info'.

export function statCard(label: string, value: string, hint: string | null, icon: string, variant: string): string {
  return `
    <div class="col-sm-6 col-lg-3">
      <div class="card stat-card stat-card--${variant} h-100">
        <div class="card-body">
          <div class="stat-card__head">
            <span class="stat-card__label">${label}</span>
            <span class="stat-card__icon"><i class="bi ${icon}" aria-hidden="true"></i></span>
          </div>
          <span class="stat-card__value">${value}</span>
          ${hint ? `<span class="stat-card__hint">${hint}</span>` : ''}
        </div>
      </div>
    </div>
  `;
}
