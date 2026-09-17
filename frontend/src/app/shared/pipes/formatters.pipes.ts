// Enveloppe Angular (pipes impurs : valeurs dépendant du contexte) autour de
// shared/formatters/formatters.ts. Aucune logique n'est dupliquée.

import { Pipe, PipeTransform } from '@angular/core';

import {
  archivedBadgeClass,
  archivedLabel,
  formatDate,
  formatDateRange,
  formatDateTime,
  formatDuration,
  formatTime,
  formatPrice,
  formatRelativeTime,
  orderStatusLabel,
  statusBadgeClass,
  subscriptionStatusLabel,
  taskStatusBadgeClass,
  ticketCategoryLabel,
  ticketPriorityLabel,
  ticketStatusLabel,
  ticketTaskStatusLabel,
} from '../formatters/formatters';
import { safeUrl } from '../utils/utils';

@Pipe({ name: 'formatDate' })
export class FormatDatePipe implements PipeTransform {
  transform(value?: string | null): string { return formatDate(value); }
}

@Pipe({ name: 'formatDateTime' })
export class FormatDateTimePipe implements PipeTransform {
  transform(value?: string | null): string { return formatDateTime(value); }
}

@Pipe({ name: 'formatRelativeTime', pure: false })
export class FormatRelativeTimePipe implements PipeTransform {
  transform(value?: string | null): string { return formatRelativeTime(value); }
}

@Pipe({ name: 'formatPrice' })
export class FormatPricePipe implements PipeTransform {
  transform(value?: number | string | null): string { return formatPrice(value); }
}

@Pipe({ name: 'formatDateRange' })
export class FormatDateRangePipe implements PipeTransform {
  transform(start: string, end: string): string { return formatDateRange(start, end); }
}

@Pipe({ name: 'formatTime' })
export class FormatTimePipe implements PipeTransform {
  transform(value?: string | null): string { return formatTime(value); }
}

@Pipe({ name: 'formatDuration' })
export class FormatDurationPipe implements PipeTransform {
  transform(months?: number | null): string { return formatDuration(months); }
}

@Pipe({ name: 'orderStatusLabel' })
export class OrderStatusLabelPipe implements PipeTransform {
  transform(status?: string | null): string { return orderStatusLabel(status); }
}

@Pipe({ name: 'subscriptionStatusLabel' })
export class SubscriptionStatusLabelPipe implements PipeTransform {
  transform(status?: string | null): string { return subscriptionStatusLabel(status); }
}

@Pipe({ name: 'archivedLabel' })
export class ArchivedLabelPipe implements PipeTransform {
  transform(archived?: boolean | null): string { return archivedLabel(archived); }
}

@Pipe({ name: 'statusBadgeClass' })
export class StatusBadgeClassPipe implements PipeTransform {
  transform(status?: string | null): string { return statusBadgeClass(status); }
}

@Pipe({ name: 'archivedBadgeClass' })
export class ArchivedBadgeClassPipe implements PipeTransform {
  transform(archived?: boolean | null): string { return archivedBadgeClass(archived); }
}

@Pipe({ name: 'ticketStatusLabel' })
export class TicketStatusLabelPipe implements PipeTransform {
  transform(status?: string | null): string { return ticketStatusLabel(status); }
}

@Pipe({ name: 'ticketCategoryLabel' })
export class TicketCategoryLabelPipe implements PipeTransform {
  transform(category?: string | null): string { return ticketCategoryLabel(category); }
}

@Pipe({ name: 'ticketPriorityLabel' })
export class TicketPriorityLabelPipe implements PipeTransform {
  transform(priority?: string | null): string { return ticketPriorityLabel(priority); }
}

@Pipe({ name: 'ticketTaskStatusLabel' })
export class TicketTaskStatusLabelPipe implements PipeTransform {
  transform(status?: string | null): string { return ticketTaskStatusLabel(status); }
}

@Pipe({ name: 'taskStatusBadgeClass' })
export class TaskStatusBadgeClassPipe implements PipeTransform {
  transform(status?: string | null): string { return taskStatusBadgeClass(status); }
}

@Pipe({ name: 'safeUrl' })
export class SafeUrlPipe implements PipeTransform {
  transform(url?: string | null, fallback = '#'): string { return safeUrl(url, fallback); }
}
