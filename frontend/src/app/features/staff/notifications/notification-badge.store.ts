/*
  État partagé du badge de la cloche : le compteur de non-lues vit dans ce
  signal, rafraîchi par la cloche et par toute page qui marque des
  notifications lues.
*/

import { Injectable, inject, signal } from '@angular/core';

import { ApiService } from '../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class StaffNotificationBadge {
  private readonly api = inject(ApiService);

  readonly count = signal(0);

  /** Rafraîchit le compteur de non-lues affiché sur la cloche. */
  async refresh(): Promise<void> {
    try {
      const { count } = await this.api.apiFetch('/staff/notifications/unread-count');
      this.count.set(count ?? 0);
    } catch (error) {
      // silencieux : le badge se mettra à jour au prochain chargement
    }
  }
}
