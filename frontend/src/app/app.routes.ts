// Table des routes, miroir du routeur vanilla : chaque entrée conserve le
// chemin (#/…, HashLocationStrategy), le layout (public | client | staff),
// les rôles autorisés et le titre d'onglet. Les gardes rejouent applyGuard
// (voir core/guards/auth.guard.ts).

import { Routes } from '@angular/router';

import { ROLES } from './core/config';
import { authGuard, rootRedirectGuard } from './core/guards/auth.guard';
import { PublicLayout } from './layout/public-layout/public-layout';
import { ClientLayout } from './layout/client-layout/client-layout';
import { StaffLayout } from './layout/staff-layout/staff-layout';
import { BlankPage } from './core/router/blank';

const PUBLIC = 'public';

export const routes: Routes = [
  // ---- Racine : redirection selon l'état d'authentification -----------------
  {
    path: '',
    pathMatch: 'full',
    canActivate: [rootRedirectGuard],
    loadComponent: () => import('./core/router/blank').then((m) => m.BlankPage),
  },

  // ---- Public -------------------------------------------------------------
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: 'login', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Connexion', path: '/login', dir: 'authentication/login', name: 'login' }, loadComponent: () => import('./features/authentication/login/login').then((m) => m.Login) },
      { path: 'register', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Inscription', path: '/register', dir: 'authentication/register', name: 'register' }, loadComponent: () => import('./features/authentication/register/register').then((m) => m.Register) },
      { path: 'verify-email', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Vérification e-mail', path: '/verify-email', dir: 'authentication/verification', name: 'verification' }, loadComponent: () => import('./features/authentication/verification/verification').then((m) => m.Verification) },
      { path: 'activate-account', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Activation du compte', path: '/activate-account', dir: 'authentication/activation', name: 'activation' }, loadComponent: () => import('./features/authentication/activation/activation').then((m) => m.Activation) },
      { path: 'password-reset', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Réinitialisation du mot de passe', path: '/password-reset', dir: 'authentication/password-reset', name: 'password-reset' }, loadComponent: () => import('./features/authentication/password-reset/password-reset').then((m) => m.PasswordReset) },
      { path: 'password-reset-confirm', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Nouveau mot de passe', path: '/password-reset-confirm', dir: 'authentication/password-reset', name: 'password-reset' }, loadComponent: () => import('./features/authentication/password-reset/password-reset').then((m) => m.PasswordReset) },
      { path: 'not-authorized', canActivate: [authGuard], data: { roles: [PUBLIC], title: 'Accès refusé', path: '/not-authorized', dir: 'authentication/not-authorized', name: 'not-authorized' }, loadComponent: () => import('./features/authentication/not-authorized/not-authorized').then((m) => m.NotAuthorized) },
    ],
  },

  // ---- Portail client -------------------------------------------------------
  {
    path: 'client',
    component: ClientLayout,
    children: [
      { path: 'dashboard', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Espace client', path: '/client/dashboard', dir: 'client/dashboard', name: 'dashboard' }, loadComponent: () => import('./features/client/dashboard/dashboard').then((m) => m.ClientDashboard) },
      { path: 'profile', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Mon profil', path: '/client/profile', dir: 'client/profile', name: 'profile' }, loadComponent: () => import('./features/client/profile/profile').then((m) => m.ClientProfile) },
      { path: 'catalogue', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Catalogue', path: '/client/catalogue', dir: 'client/catalogue', name: 'catalogue' }, loadComponent: () => import('./features/client/catalogue/catalogue').then((m) => m.Catalogue) },
      { path: 'offers/:id', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Offre', path: '/client/offers/:id', dir: 'client/offers', name: 'offers' }, loadComponent: () => import('./features/client/offers/offers').then((m) => m.Offers) },
      { path: 'orders', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Mes commandes', path: '/client/orders', dir: 'client/orders/list', name: 'orders' }, loadComponent: () => import('./features/client/orders/list/orders').then((m) => m.ClientOrders) },
      { path: 'orders/:id', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Commande', path: '/client/orders/:id', dir: 'client/orders/detail', name: 'order-detail' }, loadComponent: () => import('./features/client/orders/detail/order-detail').then((m) => m.ClientOrderDetail) },
      { path: 'orders/:id/invoice', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Facture', path: '/client/orders/:id/invoice', dir: 'client/invoice', name: 'invoice' }, loadComponent: () => import('./features/client/invoice/invoice').then((m) => m.Invoice) },
      { path: 'subscriptions', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Mes abonnements', path: '/client/subscriptions', dir: 'client/subscriptions/list', name: 'subscriptions' }, loadComponent: () => import('./features/client/subscriptions/list/subscriptions').then((m) => m.ClientSubscriptions) },
      { path: 'subscriptions/:id', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Abonnement', path: '/client/subscriptions/:id', dir: 'client/subscriptions/detail', name: 'subscription-detail' }, loadComponent: () => import('./features/client/subscriptions/detail/subscription-detail').then((m) => m.ClientSubscriptionDetail) },
      { path: 'tickets', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Mes tickets', path: '/client/tickets', dir: 'client/tickets/list', name: 'tickets' }, loadComponent: () => import('./features/client/tickets/list/tickets').then((m) => m.ClientTickets) },
      { path: 'tickets/:id', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Ticket', path: '/client/tickets/:id', dir: 'client/tickets/detail', name: 'ticket-detail' }, loadComponent: () => import('./features/client/tickets/detail/ticket-detail').then((m) => m.ClientTicketDetail) },
      { path: 'notifications', canActivate: [authGuard], data: { roles: [ROLES.CLIENT], title: 'Notifications', path: '/client/notifications', dir: 'client/notifications', name: 'notifications' }, loadComponent: () => import('./features/client/notifications/notifications').then((m) => m.ClientNotifications) },
    ],
  },

  // ---- Portail staff --------------------------------------------------------
  {
    path: 'staff',
    component: StaffLayout,
    children: [
      { path: 'dashboard', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Tableau de bord', path: '/staff/dashboard', dir: 'staff/dashboard', name: 'dashboard' }, loadComponent: () => import('./features/staff/dashboard/dashboard').then((m) => m.StaffDashboard) },
      { path: 'clients', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Clients', path: '/staff/clients', dir: 'staff/clients/list', name: 'clients' }, loadComponent: () => import('./features/staff/clients/list/clients').then((m) => m.StaffClients) },
      { path: 'clients/:id', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Client', path: '/staff/clients/:id', dir: 'staff/clients/detail', name: 'client-detail' }, loadComponent: () => import('./features/staff/clients/detail/client-detail').then((m) => m.StaffClientDetail) },
      { path: 'services', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Services', path: '/staff/services', dir: 'staff/services', name: 'services' }, loadComponent: () => import('./features/staff/services/services').then((m) => m.StaffServices) },
      { path: 'offers', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Offres', path: '/staff/offers', dir: 'staff/offers', name: 'offers' }, loadComponent: () => import('./features/staff/offers/offers').then((m) => m.StaffOffers) },
      { path: 'orders', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Commandes', path: '/staff/orders', dir: 'staff/orders', name: 'orders' }, loadComponent: () => import('./features/staff/orders/orders').then((m) => m.StaffOrders) },
      { path: 'subscriptions', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Abonnements', path: '/staff/subscriptions', dir: 'staff/subscriptions', name: 'subscriptions' }, loadComponent: () => import('./features/staff/subscriptions/subscriptions').then((m) => m.StaffSubscriptions) },
      { path: 'tickets', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Tickets', path: '/staff/tickets', dir: 'staff/tickets/list', name: 'tickets' }, loadComponent: () => import('./features/staff/tickets/list/tickets').then((m) => m.StaffTickets) },
      { path: 'tickets/:id', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Ticket', path: '/staff/tickets/:id', dir: 'staff/tickets/detail', name: 'ticket-detail' }, loadComponent: () => import('./features/staff/tickets/detail/ticket-detail').then((m) => m.StaffTicketDetail) },
      { path: 'accounts', canActivate: [authGuard], data: { roles: [ROLES.ADMIN], title: 'Comptes staff', path: '/staff/accounts', dir: 'staff/accounts', name: 'accounts' }, loadComponent: () => import('./features/staff/accounts/accounts').then((m) => m.StaffAccounts) },
      { path: 'audits', canActivate: [authGuard], data: { roles: [ROLES.ADMIN], title: 'Audits', path: '/staff/audits', dir: 'staff/audits', name: 'audits' }, loadComponent: () => import('./features/staff/audits/audits').then((m) => m.StaffAudits) },
      { path: 'exports', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Exports', path: '/staff/exports', dir: 'staff/exports', name: 'exports' }, loadComponent: () => import('./features/staff/exports/exports').then((m) => m.StaffExports) },
      { path: 'profile', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Mon profil', path: '/staff/profile', dir: 'staff/profile', name: 'profile' }, loadComponent: () => import('./features/staff/profile/profile').then((m) => m.StaffProfile) },
      { path: 'notifications', canActivate: [authGuard], data: { roles: [ROLES.STAFF, ROLES.ADMIN], title: 'Notifications', path: '/staff/notifications', dir: 'staff/notifications', name: 'notifications' }, loadComponent: () => import('./features/staff/notifications/notifications').then((m) => m.StaffNotifications) },
    ],
  },

  // ---- Inconnu : accès refusé -----------------------------------------------
  { path: '**', redirectTo: 'not-authorized' },
];
