/*
  Cloche de notifications (topbar du portail staff) : badge de non-lues via le
  store partagé StaffNotificationBadge, panneau déroulant des notifications
  récentes, rafraîchies en temps réel via le flux SSE (D-042).
*/

import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';
import { EventStream, openEventStream } from '../../../core/api/sse';
import { StaffNotificationBadge } from './notification-badge.store';
import { FormatDateTimePipe, SafeUrlPipe } from '../../../shared/pipes/formatters.pipes';
import { Notification } from './model';

@Component({
  selector: 'app-staff-notification-bell',
  imports: [FormatDateTimePipe, SafeUrlPipe],
  templateUrl: './notification-bell.html',
})
export class StaffNotificationBell implements OnInit {
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly badge = inject(StaffNotificationBadge);

  protected readonly items = signal<Notification[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly panelOpen = signal(false);
  protected readonly tick = signal(0);

  private stream: EventStream | null = null;

  async ngOnInit(): Promise<void> {
    await this.badge.refresh();
    const timer = setInterval(() => {
      this.tick.update((t) => t + 1);
      void this.badge.refresh();
    }, 10_000);
    this.stream = openEventStream('/staff/notifications/stream', (event) => {
      if (event === 'notification') {
        void this.badge.refresh();
        if (this.panelOpen()) void this.loadNotifications();
      }
    });
    this.destroyRef.onDestroy(() => { clearInterval(timer); this.stream?.close(); });
  }

  async onBellClick(): Promise<void> {
    this.panelOpen.set(true);
    await Promise.all([this.loadNotifications(), this.badge.refresh()]);
  }

  private async loadNotifications(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const notifications = await this.api.apiFetch('/staff/notifications/recent');
      if (!notifications.length) {
        this.items.set([]);
        this.loading.set(false);
        return;
      }
      this.items.set(notifications);
      this.loading.set(false);
    } catch (error) {
      this.error.set((error as Error).message);
      this.loading.set(false);
    }
  }

  async markAll(): Promise<void> {
    try {
      await this.api.apiFetch('/staff/notifications/read-all', { method: 'PATCH', body: {} });
      await Promise.all([this.badge.refresh(), this.loadNotifications()]);
    } catch (error) {
      // Échec silencieux : le badge se mettra à jour au prochain chargement.
    }
  }

  /** Marque la notification lue ; un échec du PATCH ne bloque jamais la navigation. */
  async open(notification: Notification, event: Event): Promise<void> {
    event.preventDefault();
    try {
      await this.api.apiFetch(`/staff/notifications/${encodeURIComponent(notification.publicId)}/read`, { method: 'PATCH', body: {} });
      this.items.update((list) => list.map((n) => (n === notification ? { ...n, read: true } : n)));
    } catch (error) {
      // échec ignoré : la navigation prime.
    }
    const link = notification.link;
    if (link && link !== '#') {
      window.location.hash = link.replace('#', '');
    }
  }
}
