/*
  Journal d'audit (ADMIN) : pagination serveur, recherche debouncée (300 ms),
  retour à la page 0 à chaque changement de filtre.
  Réponse API : { items, page, size, totalElements, totalPages }.
*/

import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { debounce } from '../../../shared/utils/utils';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { FormatDateTimePipe } from '../../../shared/pipes/formatters.pipes';
import { AuditsService, PAGE_SIZE } from './audits.service';

const DEBOUNCE_MS = 300;

/** Types d'action disponibles dans le journal (enum AuditAction). */
const ACTION_OPTIONS = [
  'STAFF_CREATED', 'STAFF_UPDATED', 'STAFF_ENABLED', 'STAFF_DISABLED', 'STAFF_DELETED',
  'CLIENT_CREATED', 'CLIENT_UPDATED', 'CLIENT_ARCHIVED', 'CLIENT_RESTORED', 'CLIENT_DELETED',
  'SERVICE_CREATED', 'SERVICE_UPDATED', 'SERVICE_ARCHIVED', 'SERVICE_RESTORED', 'SERVICE_DELETED',
  'OFFER_CREATED', 'OFFER_UPDATED', 'OFFER_ARCHIVED', 'OFFER_RESTORED', 'OFFER_DELETED',
  'ORDER_CREATED', 'ORDER_CANCELLED', 'ORDER_REJECTED', 'ORDER_CONFIRMED',
  'SUBSCRIPTION_CREATED', 'SUBSCRIPTION_CANCELLED', 'SUBSCRIPTION_EXPIRED',
  'LOGIN_FAILED',
];

/** Types d'entités présents dans le journal. */
const ENTITY_TYPE_OPTIONS = ['CLIENT', 'ORDER', 'SUBSCRIPTION', 'SERVICE', 'OFFER', 'STAFF', 'USER'];

@Component({
  selector: 'app-staff-audits',
  imports: [FormsModule, Pagination, FormatDateTimePipe],
  templateUrl: './audits.html',
})
export class StaffAudits {
  private readonly auditsService = inject(AuditsService);

  protected readonly actionOptions = ACTION_OPTIONS;
  protected readonly entityTypeOptions = ENTITY_TYPE_OPTIONS;

  // Filtres réactifs.
  protected readonly query = signal('');
  protected readonly action = signal('');
  protected readonly entityType = signal('');

  protected readonly page = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly data = signal<any | null>(null);

  protected readonly items = computed<any[]>(() => this.data()?.items || []);
  protected readonly totalElements = computed<number>(() => this.data()?.totalElements || 0);
  protected readonly totalPages = computed<number>(() => this.data()?.totalPages || 1);
  protected readonly rangeStart = computed(() => this.page() * PAGE_SIZE + 1);
  protected readonly rangeEnd = computed(() => Math.min(this.rangeStart() + this.items().length - 1, this.totalElements()));

  private readonly debouncedLoad = debounce(() => void this.load(), DEBOUNCE_MS);

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await this.auditsService.fetchAudits({
        query: this.query().trim(),
        action: this.action(),
        entityType: this.entityType(),
        page: this.page(),
      });
      this.data.set(data);
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  protected onSearchInput(value: string): void {
    this.query.set(value);
    this.page.set(0);
    this.debouncedLoad();
  }

  protected onActionChange(value: string): void {
    this.action.set(value);
    this.page.set(0);
    void this.load();
  }

  protected onEntityTypeChange(value: string): void {
    this.entityType.set(value);
    this.page.set(0);
    void this.load();
  }

  protected onPage(next: number): void {
    this.page.set(next);
    void this.load();
  }

  protected onReset(): void {
    this.query.set('');
    this.action.set('');
    this.entityType.set('');
    this.page.set(0);
    void this.load();
  }

  /** Libellé lisible d'une action : 'STAFF_CREATED' → 'STAFF CREATED'. */
  protected actionLabel(action?: string | null): string {
    return action ? action.replace(/_/g, ' ') : (action ?? '');
  }

  /** Valeur affichable ou tiret ; l'échappement HTML est géré par Angular. */
  protected dash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }
}
