// Portail staff (STAFF et ADMIN) : sidebar fixe + topbar + contenu + footer.
// Les sections réservées ADMIN ne sont PAS affichées pour un STAFF ; la
// sécurité réelle est garantie par le backend (/api/staff/accounts, audits…
// exigent hasRole('ADMIN')). Ce menu est un confort d'UX uniquement.

import { AfterViewInit, Component, ElementRef, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { getEmail, isAdmin, logout } from '../../core/auth/auth';
import { StaffNotificationBell } from '../../features/staff/notifications/notification-bell';

interface NavItem {
  href: string;
  label: string;
  icon: string;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

/** Sections du menu : les entrées ADMIN ne sont ajoutées que pour ADMIN. */
function buildNavSections(isAdminUser: boolean): NavSection[] {
  const sections: NavSection[] = [
    {
      title: 'Général',
      items: [
        { href: '#/staff/dashboard', label: 'Tableau de bord', icon: 'bi-grid' },
      ],
    },
    {
      title: 'Gestion',
      items: [
        { href: '#/staff/clients', label: 'Clients', icon: 'bi-people' },
        { href: '#/staff/services', label: 'Services', icon: 'bi-puzzle' },
        { href: '#/staff/offers', label: 'Offres', icon: 'bi-tag' },
        { href: '#/staff/orders', label: 'Commandes', icon: 'bi-receipt' },
        { href: '#/staff/subscriptions', label: 'Abonnements', icon: 'bi-stars' },
        { href: '#/staff/tickets', label: 'Tickets', icon: 'bi-headset' },
      ],
    },
  ];

  if (isAdminUser) {
    sections.push({
      title: 'Administration',
      items: [
        { href: '#/staff/accounts', label: 'Comptes staff', icon: 'bi-person-badge' },
        { href: '#/staff/audits', label: 'Audits', icon: 'bi-file-text' },
      ],
    });
  }

  sections.push({
    title: 'Données',
    items: [
      { href: '#/staff/exports', label: 'Exports', icon: 'bi-download' },
    ],
  });

  return sections;
}

@Component({
  selector: 'app-staff-layout',
  imports: [RouterOutlet, StaffNotificationBell],
  templateUrl: './staff-layout.html',
})
export class StaffLayout implements AfterViewInit {
  private readonly host = inject(ElementRef);
  private readonly router = inject(Router);

  protected readonly year = new Date().getFullYear();
  protected readonly email = getEmail();
  protected readonly admin = isAdmin();
  protected readonly navSections = buildNavSections(isAdmin());

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

    // Le drawer mobile doit se fermer après la navigation par lien (hash) :
    // on n'utilise PAS data-bs-dismiss sur les <a> car Bootstrap bloque alors
    // le changement de hash (preventDefault), la navigation ne se ferait pas.
    const drawer = app.querySelector('#staff-nav-drawer');
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

    // Sidebar repliable (pin) — le bouton topbar réaffiche la sidebar.
    const layout = app.querySelector('.staff-layout') as HTMLElement;
    const toggleBtn = app.querySelector('.js-sidebar-toggle');
    const expandBtn = app.querySelector('.js-sidebar-expand');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        layout.classList.add('is-sidebar-collapsed');
      });
    }
    if (expandBtn) {
      expandBtn.addEventListener('click', () => {
        layout.classList.remove('is-sidebar-collapsed');
      });
    }
  }
}
