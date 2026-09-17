// Filet de sécurité global : toute erreur JS non capturée est signalée à
// l'utilisateur par un toast au lieu de rester silencieuse (équivalent du
// filet de bootstrap.js).

import { ErrorHandler, Injectable } from '@angular/core';

import { showToast } from '../shared/components/toast';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
    // Détail technique en console uniquement, message français à l'utilisateur.
    console.error('Erreur inattendue :', error);
    showToast('Une erreur inattendue est survenue. Réessayez.', 'danger');
  }
}
