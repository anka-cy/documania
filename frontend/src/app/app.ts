// Racine de l'application : titre d'onglet « Documania — <page> » à chaque
// navigation + filet de sécurité global (erreurs JS non capturées), hérité du
// bootstrap.js d'origine.

import { Component, OnDestroy, inject } from '@angular/core';
import { Router, RouterOutlet, ActivationEnd } from '@angular/router';
import { filter } from 'rxjs';

import { APP_NAME } from './core/config';
import { showToast } from './shared/components/toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  styleUrl: './app.scss',
  template: '<router-outlet />',
})
export class App implements OnDestroy {
  private readonly router = inject(Router);

  private readonly titleSubscription = this.router.events.pipe(
    filter((event): event is ActivationEnd => event instanceof ActivationEnd),
  ).subscribe((event) => {
    // Titre de la vue la plus profondément activée.
    const title = event.snapshot.data['title'] as string | undefined;
    if (title) document.title = `${APP_NAME} — ${title}`;
  });

  private readonly reportUnexpectedError = () =>
    showToast('Une erreur inattendue est survenue. Réessayez.', 'danger');

  constructor() {
    window.addEventListener('error', this.reportUnexpectedError);
    window.addEventListener('unhandledrejection', this.reportUnexpectedError);
  }

  ngOnDestroy(): void {
    this.titleSubscription.unsubscribe();
    window.removeEventListener('error', this.reportUnexpectedError);
    window.removeEventListener('unhandledrejection', this.reportUnexpectedError);
  }
}
