// Layout des pages publiques (login, register, vérification, activation,
// password-reset) : topbar (logo) + contenu + footer.

import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet],
  templateUrl: './public-layout.html',
})
export class PublicLayout {
  protected readonly year = new Date().getFullYear();
}
