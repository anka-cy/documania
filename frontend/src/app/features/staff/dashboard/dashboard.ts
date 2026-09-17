/*
  Tableau de bord staff : synthèse GET /api/staff/dashboard/summary
  (cartes KPI + derniers événements d'audit).
*/

import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';

import { formatPrice } from '../../../shared/formatters/formatters';
import { StatCard } from '../../../shared/components/stat-card/stat-card';
import { FormatDateTimePipe } from '../../../shared/pipes/formatters.pipes';
import { DashboardService } from './dashboard.service';

interface StatDescriptor {
  label: string;
  value: string | number;
  hint: string | null;
  icon: string;
  variant: string;
}

const CHUNK_SIZE = 20;

@Component({
  selector: 'app-staff-dashboard',
  imports: [StatCard, FormatDateTimePipe],
  templateUrl: './dashboard.html',
})
export class StaffDashboard {
  private readonly dashboardService = inject(DashboardService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly summary = signal<any | null>(null);
  protected readonly auditsCardHidden = signal(false);
  protected readonly auditVisibleCount = signal(CHUNK_SIZE);

  private readonly auditSentinel = viewChild<ElementRef<HTMLElement>>('auditSentinel');

  protected readonly kpis = computed<StatDescriptor[]>(() => {
    const summary = this.summary();
    if (!summary) return [];
    return [
      { label: 'Clients', value: summary.clients.total, hint: `${summary.clients.active} actifs • ${summary.clients.archived} archivés`, icon: 'bi-people', variant: 'primary' },
      { label: 'Services', value: summary.services.total, hint: `${summary.services.active} actifs • ${summary.services.archived} archivés`, icon: 'bi-puzzle', variant: 'info' },
      { label: 'Offres', value: summary.offers.total, hint: `${summary.offers.active} actives • ${summary.offers.archived} archivées`, icon: 'bi-tag', variant: 'warning' },
      { label: 'Commandes', value: summary.orders.total, hint: `${summary.orders.pending} en attente • ${summary.orders.confirmed} confirmées`, icon: 'bi-receipt', variant: 'success' },
      { label: 'Abonnements', value: summary.subscriptions.total, hint: `${summary.subscriptions.active} actifs • ${summary.subscriptions.expiringSoon} arrivent à échéance`, icon: 'bi-stars', variant: 'primary' },
      { label: 'Revenus confirmés', value: formatPrice(summary.confirmedRevenue), hint: 'créances liées aux commandes confirmées', icon: 'bi-cash-stack', variant: 'success' },
    ];
  });

  protected readonly recentAudits = computed<any[]>(() => this.summary()?.recentAuditEvents || []);
  protected readonly visibleAudits = computed(() => this.recentAudits().slice(0, this.auditVisibleCount()));
  protected readonly moreAudits = computed(() => this.auditVisibleCount() < this.recentAudits().length);

  constructor() {
    void this.load();

    // Rendu progressif de l'historique d'audit au scroll (sentinelle en bas).
    effect((onCleanup) => {
      const sentinel = this.auditSentinel()?.nativeElement;
      if (!sentinel) return;
      const observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting)) {
            this.auditVisibleCount.update((count) => Math.min(count + CHUNK_SIZE, this.recentAudits().length));
          }
        },
        { rootMargin: '200px 0px', threshold: 0.01 },
      );
      observer.observe(sentinel);
      onCleanup(() => observer.disconnect());
    });
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const summary = await this.dashboardService.fetchDashboardSummary();
      this.summary.set(summary);
      // Carte audit masquée quand il n'y a aucun événement.
      this.auditsCardHidden.set(!(summary.recentAuditEvents && summary.recentAuditEvents.length));
    } catch (error: any) {
      this.error.set(error.message);
      // Sur erreur, seule la zone KPI affiche l'erreur ; la carte audit reste
      // visible (corps vide).
      this.auditsCardHidden.set(false);
    } finally {
      this.loading.set(false);
    }
  }
}
