/*
  Détail d'un ticket (portail staff) : checklist des livrables, fil de
  discussion avec pièces jointes, clôture avec motif ; la suppression
  définitive est réservée à l'ADMIN et tracée en journal d'audit.
*/

import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../../../core/auth/auth.service';
import { EventStream, openEventStream } from '../../../../core/api/sse';
import { ToastService } from '../../../../shared/components/toast.service';
import {
  FormatDateTimePipe,
  StatusBadgeClassPipe,
  TaskStatusBadgeClassPipe,
  TicketCategoryLabelPipe,
  TicketPriorityLabelPipe,
  TicketStatusLabelPipe,
  TicketTaskStatusLabelPipe,
} from '../../../../shared/pipes/formatters.pipes';
import { TICKET_ATTACHMENT_ACCEPT } from '../../../../shared/components/ticketAttachments';
import { pageParams } from '../../../../core/router/params';
import { promptReason } from '../../../../shared/utils/utils';
import { TicketDetailService } from './ticket-detail.service';

const TICKET_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

@Component({
  selector: 'app-staff-ticket-detail',
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
export class StaffTicketDetail {
  private readonly svc = inject(TicketDetailService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly admin = this.auth.isAdmin();
  protected readonly ticketId = pageParams(this.route.snapshot).id;

  private stream: EventStream | null = null;

  protected readonly detail = signal<any>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly unreadCount = signal(0);
  protected readonly chatOpen = signal(false);
  protected readonly pendingFiles = signal<File[]>([]);

  protected readonly ticket = computed(() => this.detail()?.ticket ?? null);
  protected readonly messages = computed<any[]>(() => this.detail()?.messages ?? []);
  protected readonly tasks = computed<any[]>(() => this.detail()?.tasks ?? []);
  protected readonly progress = computed<number>(() => this.detail()?.progressPercent ?? 0);
  protected readonly closed = computed(() => this.ticket()?.status === 'CLOSED');

  protected readonly priorities = TICKET_PRIORITIES;
  protected readonly attachmentAccept = TICKET_ATTACHMENT_ACCEPT;

  protected readonly newTaskTitle = signal('');
  protected readonly taskBusy = signal(false);
  protected readonly messageText = signal('');
  protected readonly sendBusy = signal(false);

  private readonly chatMessagesEl = viewChild<ElementRef<HTMLElement>>('chatMessages');
  private readonly attachInputEl = viewChild<ElementRef<HTMLInputElement>>('attachInput');

  constructor() {
    effect(() => {
      const msgs = this.messages();
      const open = this.chatOpen();
      const el = this.chatMessagesEl()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
    void this.load();
    this.stream = openEventStream(`/staff/tickets/${this.ticketId}/stream`, (event) => {
      if (event === 'ticket-message') void this.refresh();
    });
    this.destroyRef.onDestroy(() => this.stream?.close());
  }

  private async load(): Promise<void> {
    try {
      const detail = await this.svc.fetchTicketDetail(this.ticketId);
      this.applyDetail(detail);
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  private applyDetail(detail: any): void {
    this.detail.set(detail);
    this.unreadCount.set(detail?.ticket?.unreadCount ?? 0);
  }

  private async refresh(): Promise<void> {
    try {
      const detail = await this.svc.fetchTicketDetail(this.ticketId);
      this.applyDetail(detail);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async toggleChat(): Promise<void> {
    const next = !this.chatOpen();
    this.chatOpen.set(next);
    if (next && this.unreadCount() > 0) {
      try {
        await this.svc.markTicketRead(this.ticketId);
        this.unreadCount.set(0);
      } catch {
        /* best-effort */
      }
    }
  }

  protected formatFileSize(bytes?: number): string {
    if (!bytes || bytes === 0) return '—';
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / 1048576).toFixed(1)} Mo`;
  }

  protected badgeLabel(count: number): string {
    return count > 99 ? '99+' : String(count);
  }

  protected async downloadAttachment(attachment: any): Promise<void> {
    await this.svc.downloadAndSaveTicketAttachment(this.ticketId, attachment.publicId, attachment.fileName);
  }

  protected openFilePicker(): void {
    this.attachInputEl()?.nativeElement.click();
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    this.pendingFiles.update((list) => list.concat(files));
    input.value = '';
  }

  protected removePending(index: number): void {
    this.pendingFiles.update((list) => {
      const copy = [...list];
      copy.splice(index, 1);
      return copy;
    });
  }

  protected async onSendMessage(): Promise<void> {
    const text = this.messageText().trim();
    if (!text && this.pendingFiles().length === 0) return;

    const files = this.pendingFiles();
    this.messageText.set('');
    this.pendingFiles.set([]);
    this.sendBusy.set(true);

    const optimisticMsg = {
      publicId: `optimistic-${Date.now()}`,
      authorName: this.auth.getEmail() || 'Vous',
      authorRole: this.auth.getRole() || 'STAFF',
      message: text,
      createdAt: new Date().toISOString(),
      attachments: files.map((f, i) => ({ publicId: `att-temp-${i}`, fileName: f.name, fileSize: f.size })),
    };
    this.detail.update((d: any) => d ? { ...d, messages: [...d.messages, optimisticMsg] } : d);

    try {
      const attachmentIds: string[] = [];
      for (const file of files) {
        attachmentIds.push(await this.svc.uploadTicketAttachment(this.ticketId, file));
      }
      const detail = await this.svc.sendTicketMessage(this.ticketId, text, attachmentIds);
      this.applyDetail(detail);
    } catch (error: any) {
      this.detail.update((d: any) => d ? { ...d, messages: d.messages.filter((m: any) => m.publicId !== optimisticMsg.publicId) } : d);
      this.toast.show(error.message, 'danger');
    } finally {
      this.sendBusy.set(false);
    }
  }

  protected async onAddTask(): Promise<void> {
    const title = this.newTaskTitle().trim();
    if (!title) return;
    this.newTaskTitle.set('');
    this.taskBusy.set(true);

    const tempId = `temp-${Date.now()}`;
    const optimisticTask = { publicId: tempId, title, status: 'PENDING', createdAt: new Date().toISOString() };
    this.detail.update((d: any) => d ? { ...d, tasks: [...d.tasks, optimisticTask] } : d);

    try {
      const detail = await this.svc.createTask(this.ticketId, title);
      this.applyDetail(detail);
    } catch (error: any) {
      this.detail.update((d: any) => d ? { ...d, tasks: d.tasks.filter((t: any) => t.publicId !== tempId) } : d);
      this.toast.show(error.message, 'danger');
    } finally {
      this.taskBusy.set(false);
    }
  }

  protected async updateTask(taskId: string, status: string): Promise<void> {
    this.detail.update((d: any) => d ? { ...d, tasks: d.tasks.map((t: any) => t.publicId === taskId ? { ...t, status } : t) } : d);
    try {
      const detail = await this.svc.updateTask(this.ticketId, taskId, { status });
      this.applyDetail(detail);
    } catch (error: any) {
      this.refresh();
      this.toast.show(error.message, 'danger');
    }
  }

  protected async deleteTask(taskId: string): Promise<void> {
    const snapshot = this.detail();
    this.detail.update((d: any) => d ? { ...d, tasks: d.tasks.filter((t: any) => t.publicId !== taskId) } : d);
    try {
      const detail = await this.svc.deleteTask(this.ticketId, taskId);
      this.toast.show('Tâche supprimée.', 'success');
      this.applyDetail(detail);
    } catch (error: any) {
      this.detail.set(snapshot);
      this.toast.show(error.message, 'danger');
    }
  }

  protected async onPriorityChange(event: Event): Promise<void> {
    await this.patchTicket({ priority: (event.target as HTMLSelectElement).value });
  }

  private async patchTicket(body: any): Promise<void> {
    try {
      const detail = await this.svc.patchTicket(this.ticketId, body);
      this.toast.show('Ticket mis à jour.', 'success');
      this.applyDetail(detail);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async closeTicket(): Promise<void> {
    const reason = await promptReason('Clôturer ce ticket ?');
    if (reason === null) return;
    try {
      const detail = await this.svc.closeTicket(this.ticketId, reason);
      this.toast.show('Ticket clôturé.', 'success');
      this.applyDetail(detail);
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async deleteTicket(): Promise<void> {
    const reason = await promptReason('Supprimer définitivement ce ticket ?');
    if (reason === null) return;
    try {
      await this.svc.deleteTicket(this.ticketId, reason);
      this.toast.show('Ticket supprimé.', 'success');
      await this.router.navigateByUrl('/staff/tickets');
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }
}
