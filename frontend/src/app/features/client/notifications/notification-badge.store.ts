/* Badge de non-lues partagé entre la cloche et les pages qui marquent comme lu. */

import { Injectable, inject, signal } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class ClientNotificationBadge {
  private readonly api = inject(ApiService);

  readonly count = signal(0);

  async refresh(): Promise<void> {
    try {
      const { count } = await this.api.apiFetch('/client/notifications/unread-count');
      this.count.set(count ?? 0);
    } catch (error) {
      // silencieux : le badge se mettra à jour au prochain chargement
    }
  }
}
