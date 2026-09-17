/*
  Détail d'un ticket : checklist en lecture seule (le client ne peut ni éditer
  les tâches ni le statut), fil de discussion avec pièces jointes (upload
  séquentiel puis POST), marquage lu best-effort à la lecture.
*/

import { Component, DestroyRef, ElementRef, ViewChild, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { pageParams } from '../../../../core/router/params';
import { EventStream, openEventStream } from '../../../../core/api/sse';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmService } from '../../../../shared/components/confirm.service';
import { ToastService } from '../../../../shared/components/toast.service';
import { TICKET_ATTACHMENT_ACCEPT } from '../../../../shared/components/ticketAttachments';
import {
  FormatDateTimePipe,
  StatusBadgeClassPipe,
  TaskStatusBadgeClassPipe,
  TicketCategoryLabelPipe,
  TicketPriorityLabelPipe,
  TicketStatusLabelPipe,
  TicketTaskStatusLabelPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { TicketDetailService } from './ticket-detail.service';

const CLOSED_CONFIRM_MESSAGE =
  'Clôturer ce ticket ?<br><span class="small text-muted">Vous pourrez en ouvrir un nouveau si besoin.</span>';

@Component({
  selector: 'app-client-ticket-detail',
  imports: [
    FormsModule,
    FormatDateTimePipe,
    StatusBadgeClassPipe,
    TaskStatusBadgeClassPipe,
    TicketCategoryLabelPipe,
    TicketPriorityLabelPipe,
    TicketStatusLabelPipe,
    TicketTaskStatusLabelPipe,
  ],
  templateUrl: './ticket-detail.html',
})
export class ClientTicketDetail {
  private readonly service = inject(TicketDetailService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private ticketId = '';
  private stream: EventStream | null = null;

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly detail = signal<any>(null);
  protected readonly pendingFiles = signal<File[]>([]);
  protected readonly sending = signal(false);

  protected readonly canClose = computed(() => this.ticket()?.status !== 'CLOSED');
  protected readonly ticket = computed(() => (this.detail() ? this.detail().ticket : null));
  protected readonly messages = computed<any[]>(() => (this.detail() && this.detail().messages) || []);
  protected readonly tasks = computed<any[]>(() => (this.detail() && this.detail().tasks) || []);
  protected readonly progress = computed<number>(() => (this.detail() && this.detail().progressPercent) || 0);
  protected readonly unread = computed<number>(() => {
    const t = this.ticket();
    return t ? t.unreadCount || 0 : 0;
  });

  protected message = '';
  protected readonly attachmentAccept = TICKET_ATTACHMENT_ACCEPT;

  @ViewChild('messagesContainer') private threadRef?: ElementRef<HTMLDivElement>;

  constructor() {
    effect(() => {
      this.detail();
      const thread = this.threadRef?.nativeElement;
      if (thread) thread.scrollTop = thread.scrollHeight;
    });
  }

  async ngOnInit(): Promise<void> {
    this.ticketId = pageParams(this.route.snapshot)['id'];
    this.stream = openEventStream(`/client/tickets/${this.ticketId}/stream`, (event) => {
      if (event === 'ticket-message') void this.refresh();
    });
    this.destroyRef.onDestroy(() => this.stream?.close());
    try {
      this.detail.set(await this.service.fetchTicketDetail(this.ticketId));
    } catch (error: any) {
      this.error.set(error.message);
      return;
    } finally {
      this.loading.set(false);
    }
  }

  private async refresh(): Promise<void> {
    try {
      this.detail.set(await this.service.fetchTicketDetail(this.ticketId));
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async markRead(): Promise<void> {
    const t = this.ticket();
    if (!t || !t.unreadCount) return;
    try {
      await this.service.markTicketRead(this.ticketId);
      this.detail.update((d: any) => ({ ...d, ticket: { ...d.ticket, unreadCount: 0 } }));
    } catch (error) { /* best-effort */ }
  }

  protected async downloadAttachment(att: any): Promise<void> {
    await this.service.downloadTicketAttachment(this.ticketId, att.publicId, att.fileName);
  }

  protected onFilesPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    this.pendingFiles.update((list) => list.concat(files));
    input.value = '';
  }

  protected removePendingAttachment(index: number): void {
    this.pendingFiles.update((list) => {
      const copy = list.slice();
      copy.splice(index, 1);
      return copy;
    });
  }

  protected async onSendMessage(): Promise<void> {
    const text = this.message.trim();
    if (!text && this.pendingFiles().length === 0) return;

    const files = this.pendingFiles();
    this.message = '';
    this.pendingFiles.set([]);
    this.sending.set(true);

    const optimisticMsg = {
      publicId: `optimistic-${Date.now()}`,
      authorName: this.auth.getEmail() || 'Vous',
      authorRole: this.auth.getRole() || 'CLIENT',
      message: text,
      createdAt: new Date().toISOString(),
      attachments: files.map((f: File, i: number) => ({ publicId: `att-temp-${i}`, fileName: f.name, fileSize: f.size })),
    };
    this.detail.update((d: any) => d ? { ...d, messages: [...d.messages, optimisticMsg] } : d);

    try {
      const attachmentIds: string[] = [];
      for (const file of files) {
        attachmentIds.push(await this.service.uploadTicketAttachment(this.ticketId, file));
      }
      const detail = await this.service.sendTicketMessage(this.ticketId, { message: text, attachmentIds });
      this.detail.set(detail);
    } catch (error: any) {
      this.detail.update((d: any) => d ? { ...d, messages: d.messages.filter((m: any) => m.publicId !== optimisticMsg.publicId) } : d);
      this.toast.show(error.message, 'danger');
    } finally {
      this.sending.set(false);
    }
  }

  protected async onCloseTicket(): Promise<void> {
    const confirmed = await this.confirm.confirm(CLOSED_CONFIRM_MESSAGE, {
      title: 'Clôturer le ticket',
      okText: 'Clôturer',
      okVariant: 'danger',
    });
    if (!confirmed) return;
    try {
      const detail = await this.service.closeTicket(this.ticketId);
      this.toast.show('Ticket clôturé.', 'success');
      this.detail.set(detail);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected formatFileSize(bytes?: number): string {
    if (!bytes || bytes === 0) return '—';
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / 1048576).toFixed(1)} Mo`;
  }
}
