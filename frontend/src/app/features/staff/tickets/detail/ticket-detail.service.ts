import { Injectable, inject } from '@angular/core';

import { ApiService, saveBlob } from '../../../../core/api/api.service';

/**
 * Détail d'un ticket (portail staff) : consultation, marquage lu, pièces
 * jointes (upload/download), tâches, messages et contrôles (priorité,
 * clôture, suppression).
 */
@Injectable({ providedIn: 'root' })
export class TicketDetailService {
  private readonly api = inject(ApiService);

  fetchTicketDetail(ticketId: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}`);
  }

  markTicketRead(ticketId: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}/read`, { method: 'POST' });
  }

  downloadTicketAttachment(ticketId: any, attachmentId: any): Promise<{ blob: Blob; filename: string | null }> {
    return this.api.download(
      `/staff/tickets/${encodeURIComponent(ticketId)}/attachments/${encodeURIComponent(attachmentId)}`,
    );
  }

  async downloadAndSaveTicketAttachment(ticketId: any, attachmentId: any, fallbackName: any): Promise<any> {
    const { blob, filename } = await this.downloadTicketAttachment(ticketId, attachmentId);
    saveBlob(blob, filename || fallbackName || 'piece-jointe');
    return filename;
  }

  uploadTicketAttachment(ticketId: any, file: File): Promise<any> {
    return this.api.apiUpload(`/staff/tickets/${encodeURIComponent(ticketId)}/attachments`, file).then((created: any) => created.publicId);
  }

  createTask(ticketId: any, title: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}/tasks`, { method: 'POST', body: { title } });
  }

  sendTicketMessage(ticketId: any, message: any, attachmentIds: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}/messages`, {
      method: 'POST',
      body: { message, attachmentIds },
    });
  }

  patchTicket(ticketId: any, body: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}`, { method: 'PATCH', body });
  }

  updateTask(ticketId: any, taskId: any, body: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}/tasks/${encodeURIComponent(taskId)}`, {
      method: 'PATCH',
      body,
    });
  }

  deleteTask(ticketId: any, taskId: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}/tasks/${encodeURIComponent(taskId)}`, {
      method: 'DELETE',
    });
  }

  closeTicket(ticketId: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}/close`, { method: 'POST', body: { reason } });
  }

  deleteTicket(ticketId: any, reason: any): Promise<any> {
    return this.api.apiFetch(`/staff/tickets/${encodeURIComponent(ticketId)}`, { method: 'DELETE', body: { reason } });
  }
}
