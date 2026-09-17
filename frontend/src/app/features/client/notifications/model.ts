/* Notification du portail client (API /api/client/notifications). */

export interface Notification {
  publicId: string;
  type: string;
  title: string;
  message: string;
  link?: string | null;
  read?: boolean;
  createdAt?: string;
}
