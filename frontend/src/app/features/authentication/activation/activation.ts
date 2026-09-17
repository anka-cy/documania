/*
  Activation du compte invité : définit le mot de passe via POST
  /api/public/account-activation. Sans jeton, lien invalide et formulaire masqué.
*/

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ApiError } from '../../../core/interceptors/error.interceptor';
import { pageParams } from '../../../core/router/params';
import { rules, validateForm } from '../../../shared/validators/validators';
import { ActivationService } from './activation.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-activation',
  imports: [FormsModule],
  templateUrl: './activation.html',
})
export class Activation implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly activationService = inject(ActivationService);

  protected token: string | null = null;
  protected password = '';
  protected passwordConfirmation = '';

  protected readonly showPassword = signal(false);
  protected readonly showPasswordConfirmation = signal(false);
  protected readonly invalidLink = signal(false);
  protected readonly busy = signal(false);
  protected readonly done = signal(false);
  protected readonly alert = signal<AlertState | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  ngOnInit(): void {
    this.token = pageParams(this.route.snapshot)['token'];

    if (!this.token) {
      this.alert.set({ type: 'danger', message: 'Le lien d’activation est invalide ou incomplet. Contactez un administrateur.' });
      this.invalidLink.set(true);
    }
  }

  protected async onSubmit(): Promise<void> {
    this.alert.set(null);

    const values = {
      token: this.token as string,
      password: this.password,
      passwordConfirmation: this.passwordConfirmation,
    };
    const { errors, isValid } = validateForm(values, {
      password: [rules.password],
      passwordConfirmation: [rules.matches('La confirmation', 'password')],
    });
    this.errors.set(errors);
    if (!isValid) {
      this.alert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }

    this.busy.set(true);
    try {
      await this.activationService.activateAccount(values);
      this.done.set(true);
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.errors.set(error.fieldErrors);
        this.alert.set({ type: 'danger', message: 'Veuillez corriger les champs signalés.' });
      } else {
        this.alert.set({
          type: 'danger',
          message:
            error instanceof ApiError && (error.status === 404 || error.status === 409)
              ? 'Le lien est invalide ou a déjà été utilisé. Contactez un administrateur.'
              : error.message,
        });
      }
    } finally {
      this.busy.set(false);
    }
  }
}
