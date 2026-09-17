/*
  Profil client : informations en lecture seule + changement de mot de passe.
  L'e-mail et les informations du compte ne sont pas modifiables par le client.
*/

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ToastService } from '../../../shared/components/toast.service';
import { rules, validateForm } from '../../../shared/validators/validators';
import { ProfileService } from './profile.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-client-profile',
  imports: [FormsModule],
  templateUrl: './profile.html',
})
export class ClientProfile {
  private readonly profile = inject(ProfileService);
  private readonly toast = inject(ToastService);

  protected readonly client = signal<any | null>(null);
  protected readonly profileAlert = signal<AlertState | null>(null);
  protected readonly passwordAlert = signal<AlertState | null>(null);

  protected currentPassword = '';
  protected newPassword = '';
  protected newPasswordConfirmation = '';

  protected readonly showCurrent = signal(false);
  protected readonly showNew = signal(false);
  protected readonly showConfirm = signal(false);

  protected readonly busy = signal(false);
  protected errors: Record<string, string> = {};

  constructor() {
    this.profile.fetchProfile()
      .then((data: any) => this.client.set(data))
      .catch((error: any) => this.profileAlert.set({ type: 'danger', message: error.message }));
  }

  protected orDash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }

  protected async onSubmit(): Promise<void> {
    this.passwordAlert.set(null);

    const values = {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword,
      newPasswordConfirmation: this.newPasswordConfirmation,
    };
    const { errors, isValid } = validateForm(values, {
      currentPassword: [rules.required('Le mot de passe actuel')],
      newPassword: [rules.password],
      newPasswordConfirmation: [rules.matches('La confirmation', 'newPassword')],
    });
    this.errors = errors;
    if (!isValid) {
      this.passwordAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }

    this.busy.set(true);
    try {
      await this.profile.changePassword(values);
      this.currentPassword = '';
      this.newPassword = '';
      this.newPasswordConfirmation = '';
      this.showCurrent.set(false);
      this.showNew.set(false);
      this.showConfirm.set(false);
      this.toast.show('Mot de passe mis à jour.', 'success');
    } catch (error: any) {
      if (error.status === 400 && error.fieldErrors) {
        this.errors = error.fieldErrors;
      }
      this.passwordAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.busy.set(false);
    }
  }
}
