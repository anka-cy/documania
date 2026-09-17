/*
  Mes abonnements : cartes avec période courante, détail et actions rapides
  (panne SERVICE_DOWN / modification MODULE_CUSTOMIZATION via ticket lié).
*/

import { Component, ElementRef, OnInit, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ToastService } from '../../../../shared/components/toast.service';
import {
  FormatDatePipe,
  FormatPricePipe,
  StatusBadgeClassPipe,
  SubscriptionStatusLabelPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { SubscriptionsService } from './subscriptions.service';

const CHUNK_SIZE = 20;

@Component({
  selector: 'app-client-subscriptions',
  imports: [FormsModule, FormatDatePipe, FormatPricePipe, StatusBadgeClassPipe, SubscriptionStatusLabelPipe],
  templateUrl: './subscriptions.html',
})
export class ClientSubscriptions implements OnInit {
  private readonly service = inject(SubscriptionsService);
  private readonly toast = inject(ToastService);

  protected readonly subscriptions = signal<any[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empty = signal(false);
  protected readonly visibleCount = signal(CHUNK_SIZE);

  private readonly ticketModalEl = viewChild.required<ElementRef>('ticketModal');
  private get modal(): any {
    return bootstrap.Modal.getOrCreateInstance(this.ticketModalEl().nativeElement as HTMLElement);
  }

  private target = { subscriptionId: '', category: '', priority: '' };
  protected readonly modalTitle = signal('Nouveau ticket');
  protected readonly alert = signal<{ type: string; message: string } | null>(null);
  protected readonly descLen = signal(0);
  protected readonly showDescError = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitDisabled = signal(true);
  protected subject = '';
  protected desc = '';

  private readonly sentinel = viewChild<ElementRef>('sentinel');
  private observer: IntersectionObserver | null = null;

  constructor() {
    effect(() => {
      const element = this.sentinel()?.nativeElement;
      const visible = this.visibleCount();
      const total = this.subscriptions().length;
      this.observer?.disconnect();
      this.observer = null;
      if (!element || visible >= total) return;
      this.observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting)) {
            this.visibleCount.update((count) => Math.min(count + CHUNK_SIZE, this.subscriptions().length));
          }
        },
        { rootMargin: '200px 0px', threshold: 0.01 },
      );
      this.observer.observe(element);
    });
  }

  async ngOnInit(): Promise<void> {
    let subscriptions: any;
    try {
      subscriptions = await this.service.fetchSubscriptions();
    } catch (error: any) {
      this.error.set(error.message);
      this.loading.set(false);
      return;
    }
    if (!subscriptions.length) {
      this.empty.set(true);
      this.loading.set(false);
      return;
    }
    this.subscriptions.set(subscriptions);
    this.visibleCount.set(CHUNK_SIZE);
    this.loading.set(false);
  }

  /** Période courante : dernière entrée de `periods`, ou null. */
  protected currentPeriod(subscription: any): any {
    return subscription.periods && subscription.periods.length
      ? subscription.periods[subscription.periods.length - 1]
      : null;
  }

  protected openQuickTicket(subscription: any, category: string, priority: string): void {
    this.target = { subscriptionId: subscription.publicId, category, priority };
    const isOutage = category === 'SERVICE_DOWN';
    this.modalTitle.set(isOutage ? 'Signaler une panne' : 'Demander une modification');
    const current = this.currentPeriod(subscription);
    const service = current?.serviceName || subscription.serviceName || '';
    this.subject = isOutage ? `Panne sur ${service}` : `Personnalisation : ${service}`;
    this.desc = '';
    this.descLen.set(0);
    this.showDescError.set(false);
    this.alert.set(null);
    this.submitting.set(false);
    this.submitDisabled.set(true);
    this.modal.show();
  }

  protected onDescInput(event: Event): void {
    const len = (event.target as HTMLTextAreaElement).value.length;
    this.descLen.set(len);
    this.showDescError.set(len > 0 && len < 5);
    this.submitDisabled.set(len < 5);
  }

  protected async onTicketSubmit(): Promise<void> {
    this.alert.set(null);
    const description = this.desc.trim();
    if (description.length < 5) {
      this.alert.set({ type: 'danger', message: 'La description doit contenir au moins 5 caractères.' });
      return;
    }
    const data = {
      subscriptionId: this.target.subscriptionId,
      category: this.target.category,
      priority: this.target.priority,
      subject: this.subject.trim(),
      description,
    };
    this.submitDisabled.set(true);
    this.submitting.set(true);
    try {
      await this.service.createTicket(data);
      this.modal.hide();
      this.subject = '';
      this.desc = '';
      this.descLen.set(0);
      this.showDescError.set(false);
      this.toast.show('Ticket créé avec succès. Vous pouvez le suivre dans Support.', 'success');
    } catch (error: any) {
      this.alert.set({ type: 'danger', message: error.message });
    } finally {
      this.submitting.set(false);
      this.submitDisabled.set(false);
    }
  }
}
