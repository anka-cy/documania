/* Accès refusé : vue statique affichée par les gardes de rôle. */

import { Component } from '@angular/core';

@Component({
  selector: 'app-not-authorized',
  templateUrl: './not-authorized.html',
})
export class NotAuthorized {}
