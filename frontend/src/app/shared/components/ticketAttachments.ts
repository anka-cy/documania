// Pièces jointes de tickets, communes aux deux portails. La liste MIME DOIT
// rester alignée avec TicketAttachmentService.ALLOWED_TYPES côté backend ;
// `accept` n'est qu'une aide UX, le backend reste l'autorité.

import { escapeHtml } from '../utils/utils';

export const TICKET_ATTACHMENT_ACCEPT =
  'image/jpeg,image/png,' +
  'application/pdf,' +
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document,' +
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,' +
  'text/plain,text/csv';

interface Attachment {
  publicId: string;
  fileName: string;
  fileSize?: number;
}

/** Taille de fichier lisible (o / Ko / Mo). */
function formatFileSize(bytes?: number): string {
  if (!bytes || bytes === 0) return '—';
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} Ko`;
  return `${(bytes / 1048576).toFixed(1)} Mo`;
}

/** Bouton de téléchargement d'une pièce jointe déjà envoyée. */
export function renderTicketAttachment(att: Attachment): string {
  const size = formatFileSize(att.fileSize);
  return `
    <button type="button" class="attachment-link js-attachment-download"
            data-attachment-id="${escapeHtml(att.publicId)}"
            data-file-name="${escapeHtml(att.fileName)}"
            title="Télécharger ${escapeHtml(att.fileName)}">
      <i class="bi bi-paperclip" aria-hidden="true"></i>
      <span class="text-truncate">${escapeHtml(att.fileName)}</span>
      <span class="attachment-link__size">${size}</span>
    </button>
  `;
}

/** Badges des fichiers choisis mais pas encore envoyés (avec bouton retirer). */
export function renderPendingAttachments(pendingFiles: File[]): string {
  if (!pendingFiles.length) return '';
  return pendingFiles.map((file, index) => `
    <span class="badge text-bg-light border d-inline-flex align-items-center gap-1">
      <i class="bi bi-paperclip" aria-hidden="true"></i>
      <span class="text-truncate" style="max-width: 180px;">${escapeHtml(file.name)}</span>
      <button type="button" class="btn-close btn-close-sm ms-1 js-remove-attach" data-index="${index}"
              aria-label="Retirer"></button>
    </span>
  `).join('');
}
