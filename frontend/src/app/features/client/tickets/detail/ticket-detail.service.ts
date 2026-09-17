import { Injectable, inject } from '@angular/core';

import { ApiService, saveBlob } from '../../../../core/api/api.service';

@Injectable({ providedIn: 'root' })
export class TicketDetailService {
  private readonly api = inject(ApiService);

  fetchTicketDetail(ticketId: string): Promise<any> {
    return this.api.apiFetch(`/client/tickets/${encodeURIComponent(ticketId)}`);
  }

  markTicketRead(ticketId: string): Promise<any> {
    return this.api.apiFetch(`/client/tickets/${encodeURIComponent(ticketId)}/read`, { method: 'POST' });
  }

  closeTicket(ticketId: string): Promise<any> {
    return this.api.apiFetch(`/client/tickets/${encodeURIComponent(ticketId)}/close`, { method: 'PATCH' });
  }

  sendTicketMessage(ticketId: string, { message, attachmentIds }: { message: string; attachmentIds: string[] }): Promise<any> {
    return this.api.apiFetch(`/client/tickets/${encodeURIComponent(ticketId)}/messages`, {
      method: 'POST',
      body: { message, attachmentIds },
    });
  }

  uploadTicketAttachment(ticketId: string, file: File): Promise<any> {
    return this.api.apiUpload(`/client/tickets/${encodeURIComponent(ticketId)}/attachments`, file).then(
      (created) => created.publicId,
    );
  }

  async downloadTicketAttachment(ticketId: string, attachmentId: string, fallbackName: string): Promise<void> {
    const { blob, filename } = await this.api.download(
      `/client/tickets/${encodeURIComponent(ticketId)}/attachments/${encodeURIComponent(attachmentId)}`,
    );
    saveBlob(blob, filename || fallbackName || 'piece-jointe');
  }
}
