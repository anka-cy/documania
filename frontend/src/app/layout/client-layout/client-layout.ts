// Portail client : topbar avec navigation + menu utilisateur (déconnexion).
// Sous md (768 px), la navigation passe en tiroir Offcanvas (D-025). La
// sécurité réelle est côté backend : ce menu n'est qu'un confort d'affichage.

import { AfterViewInit, Component, ElementRef, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { getEmail, logout } from '../../core/auth/auth';
import { ClientNotificationBell } from '../../features/client/notifications/notification-bell';

const NAV_LINKS = [
  { href: '#/client/dashboard', label: 'Tableau de bord', icon: 'bi-grid' },
  { href: '#/client/catalogue', label: 'Catalogue', icon: 'bi-book' },
  { href: '#/client/orders', label: 'Mes commandes', icon: 'bi-receipt' },
  { href: '#/client/subscriptions', label: 'Mon abonnement', icon: 'bi-stars' },
  { href: '#/client/tickets', label: 'Support', icon: 'bi-headset' },
];

@Component({
  selector: 'app-client-layout',
  imports: [RouterOutlet, ClientNotificationBell],
  templateUrl: './client-layout.html',
})
export class ClientLayout implements AfterViewInit {
  private readonly host = inject(ElementRef);
  private readonly router = inject(Router);

  protected readonly year = new Date().getFullYear();
  protected readonly email = getEmail();
  protected readonly navLinks = NAV_LINKS;

  /** Lien de navigation actif (même comparaison que le layout vanilla). */
  protected isActive(hash: string): boolean {
    return this.router.url.split('?')[0] === hash.replace('#', '');
  }

  ngAfterViewInit(): void {
    const app = this.host.nativeElement as HTMLElement;

    // Lien d'évitement : focus le contenu sans toucher au hash (routeur).
    (app.querySelector('.skip-link') as HTMLElement).addEventListener('click', (event) => {
      event.preventDefault();
      const main = app.querySelector('#layout-content') as HTMLElement;
      main.setAttribute('tabindex', '-1');
      main.focus();
    });

    // Le drawer mobile se ferme après la navigation par lien (hash) : on n'utilise
    // PAS data-bs-dismiss sur les <a> car Bootstrap bloque alors le hash (preventDefault).
    const drawer = app.querySelector('#client-nav-drawer');
    if (drawer) {
      drawer.addEventListener('click', (event) => {
        const link = (event.target as Element).closest('a[href^="#/"]');
        if (link) bootstrap.Offcanvas.getOrCreateInstance(drawer).hide();
      });
    }

    app.querySelectorAll('.js-logout').forEach((btn) => {
      btn.addEventListener('click', async () => {
        await logout();
      });
    });
  }
}
