/*
  Réinitialisation du mot de passe en deux modes (demande puis confirmation
  via le jeton ?token=…, mode choisi d'après data.path de la route).
  Le backend ne révèle jamais l'existence du compte.
*/

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ApiError } from '../../../core/interceptors/error.interceptor';
import { pageParams } from '../../../core/router/params';
import { rules, validateForm } from '../../../shared/validators/validators';
import { PasswordResetService } from './password-reset.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-password-reset',
  imports: [FormsModule],
  templateUrl: './password-reset.html',
})
export class PasswordReset implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly passwordResetService = inject(PasswordResetService);

  protected token: string | null = null;
  protected email = '';
  protected password = '';
  protected passwordConfirmation = '';

  protected readonly confirmMode = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly showPasswordConfirmation = signal(false);
  protected readonly requestBusy = signal(false);
  protected readonly requestDone = signal(false);
  protected readonly confirmBusy = signal(false);
  protected readonly confirmDone = signal(false);
  protected readonly alert = signal<AlertState | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  ngOnInit(): void {
    const params = pageParams(this.route.snapshot);
    this.token = params['token'] || null;

    // Mode confirmation pour la route /password-reset-confirm.
    if (this.route.snapshot.data['path'] === '/password-reset-confirm') {
      this.confirmMode.set(true);
    }
  }

  protected async onRequestSubmit(): Promise<void> {
    this.alert.set(null);

    const email = this.email.trim();
    const { errors, isValid } = validateForm({ email }, { email: [rules.required("L'adresse e-mail"), rules.email("L'adresse e-mail")] });
    this.errors.set(errors);
    if (!isValid) return;

    this.requestBusy.set(true);
    try {
      await this.passwordResetService.requestPasswordReset(email);
      this.requestDone.set(true);
      this.alert.set({
        type: 'success',
        message: 'Si un compte existe pour cette adresse, un lien de réinitialisation vient de vous être envoyé par e-mail.',
      });
    } catch (error: any) {
      this.alert.set({ type: 'danger', message: error.message });
    } finally {
      this.requestBusy.set(false);
    }
  }

  protected async onConfirmSubmit(): Promise<void> {
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

    this.confirmBusy.set(true);
    try {
      await this.passwordResetService.confirmPasswordReset(values);
      this.confirmDone.set(true);
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.errors.set(error.fieldErrors);
        this.alert.set({ type: 'danger', message: 'Veuillez corriger les champs signalés.' });
      } else {
        this.alert.set({
          type: 'danger',
          message:
            error instanceof ApiError && (error.status === 404 || error.status === 409)
              ? 'Le lien est invalide ou a déjà été utilisé. Demandez un nouveau lien.'
              : error.message,
        });
      }
    } finally {
      this.confirmBusy.set(false);
    }
  }
}
