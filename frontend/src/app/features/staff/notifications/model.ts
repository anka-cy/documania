/** Forme renvoyée par l'API /api/staff/notifications* (liste récente et paginée). */
export interface Notification {
  publicId: string;
  type: string;
  title: string;
  message: string;
  link?: string | null;
  read?: boolean;
  createdAt?: string;
}
