// Page vide de la route racine (#/) : le rootRedirectGuard redirige toujours
// vers le portail adéquat ; ce composant n'existe que pour satisfaire la
// configuration du routeur.

import { Component } from '@angular/core';

@Component({ selector: 'app-blank', template: '' })
export class BlankPage {}
