// Affichage en français des données : dates jj/mm/aaaa, prix en MAD,
// libellés et classes CSS des statuts backend (commandes, abonnements…).

/** Formate une date ISO (yyyy-MM-dd) ou datetime en 'jj/mm/aaaa'. */
export function formatDate(value?: string | null): string {
  if (!value) return '—';
  const iso = String(value);
  const datePart = iso.length > 10 ? iso.slice(0, 10) : iso;
  const [y, m, d] = datePart.split('-');
  if (!y || !m || !d) return iso;
  return `${d}/${m}/${y}`;
}

/** Parse un datetime ISO ou LocalDateTime en Date. */
function parseDateTime(value: string): Date | null {
  const s = String(value).trim();
  // Si pas de 'Z' ni de offset (+/-), on ajoute 'Z' pour forcer UTC
  // (le backend stocke en LocalDateTime = UTC dans notre cas).
  const iso = /[Zz]|[+\-]\d{2}:\d{2}$/.test(s) ? s : s + 'Z';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** Formate une date-heure ISO en 'jj/mm/aaaa hh:MM' (timezone navigateur). */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—';
  const date = parseDateTime(String(value));
  if (!date) return '—';
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(date);
}

/** Formate une date-heure ISO en 'hh:MM' (timezone navigateur). */
export function formatTime(value?: string | null): string {
  if (!value) return '—';
  const date = parseDateTime(String(value));
  if (!date) return '—';
  return new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit', minute: '2-digit',
  }).format(date);
}

/** Formate une date-heure ISO en temps relatif ('il y a 5 min'). */
export function formatRelativeTime(value?: string | null): string {
  if (!value) return '—';
  // Le backend stocke les dates en LocalDateTime (pas de fuseau horaire).
  // On suffixe 'Z' (UTC) pour que new Date() ne les interprète pas en
  // heure locale, ce qui créerait un décalage erroné ("il y a 1h" pour
  // un message qui vient d'être posté).
  const iso = String(value).endsWith('Z') ? value : String(value) + 'Z';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return formatDateTime(value);
  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.round(diffMs / 60000);
  if (diffMinutes < 1) return "à l'instant";
  if (diffMinutes < 60) return `il y a ${diffMinutes} min`;
  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24) return `il y a ${diffHours} h`;
  const diffDays = Math.round(diffHours / 24);
  if (diffDays < 7) return `il y a ${diffDays} j`;
  return formatDateTime(value);
}

/** Formate un prix avec deux décimales et symbole MAD (dirham). */
export function formatPrice(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') return '—';
  const number = Number(value);
  if (Number.isNaN(number)) return '—';
  return `${number.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} MAD`;
}

export function formatDateRange(start: string, end: string): string {
  return `du ${formatDate(start)} au ${formatDate(end)}`;
}

export function formatDuration(months?: number | null): string {
  if (months === null || months === undefined) return '—';
  if (months === 1) return '1 mois';
  if (months === 12) return '1 an';
  if (months % 12 === 0) return `${months / 12} ans`;
  return `${months} mois`;
}

/** Libellés français des statuts backend. */
const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  CONFIRMED: 'Confirmée',
  REJECTED: 'Rejetée',
  CANCELLED: 'Annulée',
};

const SUBSCRIPTION_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Active',
  EXPIRED: 'Expirée',
  CANCELLED: 'Annulée',
};

export function orderStatusLabel(status?: string | null): string {
  return ORDER_STATUS_LABELS[status as string] || status || '—';
}

export function subscriptionStatusLabel(status?: string | null): string {
  return SUBSCRIPTION_STATUS_LABELS[status as string] || status || '—';
}

export function archivedLabel(archived?: boolean | null): string {
  return archived ? 'Archivé(e)' : 'Actif(ve)';
}

/** Classe CSS du badge de statut (voir status.css). */
export function statusBadgeClass(status?: string | null): string {
  return `badge-status badge-status--${status || 'UNKNOWN'}`;
}

export function archivedBadgeClass(archived?: boolean | null): string {
  return `badge-status badge-status--archived-${archived ? 'true' : 'false'}`;
}

/* ---------------------------------------------------------------------------
 * Tickets, priorités et tâches de checklist
 * ------------------------------------------------------------------------- */

const TICKET_STATUS_LABELS: Record<string, string> = {
  OPEN: 'Ouvert',
  IN_PROGRESS: 'En cours',
  CLOSED: 'Clôturé',
};

const TICKET_CATEGORY_LABELS: Record<string, string> = {
  ORDER_REQUIREMENTS: 'Exigences commande',
  MODULE_CUSTOMIZATION: 'Personnalisation',
  SERVICE_DOWN: 'Panne service',
  GENERAL: 'Général',
};

const TICKET_PRIORITY_LABELS: Record<string, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  CRITICAL: 'Critique',
};

const TICKET_TASK_STATUS_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Terminé',
};

export function ticketStatusLabel(status?: string | null): string {
  return TICKET_STATUS_LABELS[status as string] || status || '—';
}

export function ticketCategoryLabel(category?: string | null): string {
  return TICKET_CATEGORY_LABELS[category as string] || category || '—';
}

export function ticketPriorityLabel(priority?: string | null): string {
  return TICKET_PRIORITY_LABELS[priority as string] || priority || '—';
}

export function ticketTaskStatusLabel(status?: string | null): string {
  return TICKET_TASK_STATUS_LABELS[status as string] || status || '—';
}

/** Classe CSS du badge de statut de tâche (préfixe task-, voir status.css). */
export function taskStatusBadgeClass(status?: string | null): string {
  return `badge-status badge-status--task-${status || 'UNKNOWN'}`;
}
