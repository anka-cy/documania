/*
  Historique paginé des notifications du portail staff
  (GET /api/staff/notifications, 20/page). Chaque portail garde sa propre copie
  du module notifications (features/{client,staff}) : aucun module partagé.
*/

import { Component, OnInit, inject, signal } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';
import { safeUrl } from '../../../shared/utils/utils';
import { ToastService } from '../../../shared/components/toast.service';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { FormatDateTimePipe, SafeUrlPipe } from '../../../shared/pipes/formatters.pipes';
import { StaffNotificationBadge } from './notification-badge.store';
import { Notification } from './model';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-staff-notifications',
  imports: [Pagination, FormatDateTimePipe, SafeUrlPipe],
  templateUrl: './notifications.html',
})
export class StaffNotifications implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly badge = inject(StaffNotificationBadge);

  protected readonly items = signal<Notification[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  protected readonly totalElements = signal<number | null>(null);

  async ngOnInit(): Promise<void> {
    await this.load(0);
  }

  /** Charge une page d'historique (0-based, comme Spring). */
  protected async load(page: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await this.api.apiFetch(`/staff/notifications?page=${page}&size=${PAGE_SIZE}`);
      this.page.set(page);
      this.items.set(data.items || []);
      this.totalPages.set(data.totalPages);
      this.totalElements.set(data.totalElements);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.loading.set(false);
    }
  }

  /** Marque la notification lue, navigue vers son lien, puis rafraîchit le badge. */
  protected async open(notification: Notification, event: Event): Promise<void> {
    event.preventDefault();
    try {
      await this.api.apiFetch(`/staff/notifications/${encodeURIComponent(notification.publicId)}/read`, { method: 'PATCH', body: {} });
      this.items.update((list) => list.map((n) => (n === notification ? { ...n, read: true } : n)));
    } catch (error) {
      // Échec ignoré : la lecture ne doit jamais bloquer la navigation.
    }
    const link = safeUrl(notification.link);
    if (link && link !== '#') {
      window.location.hash = link.replace('#', '');
    }
    await this.badge.refresh();
  }

  protected async markAll(): Promise<void> {
    try {
      await this.api.apiFetch('/staff/notifications/read-all', { method: 'PATCH', body: {} });
      this.toast.show('Toutes les notifications ont été marquées comme lues.', 'success');
      await this.badge.refresh();
      await this.load(this.page());
    } catch (error) {
      this.toast.show((error as Error).message, 'danger');
    }
  }
}
