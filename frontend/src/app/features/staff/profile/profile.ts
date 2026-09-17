/*
  Mon profil (staff) : le nom est éditable, l'e-mail reste en lecture seule ;
  le changement de mot de passe passe par un formulaire distinct.
*/

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiError } from '../../../core/interceptors/error.interceptor';
import { ToastService } from '../../../shared/components/toast.service';
import { rules, validateForm } from '../../../shared/validators/validators';
import { ProfileService } from './profile.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-staff-profile',
  imports: [FormsModule],
  templateUrl: './profile.html',
})
export class StaffProfile {
  private readonly profileService = inject(ProfileService);
  private readonly toast = inject(ToastService);

  protected readonly email = signal<string | null>(null);
  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly currentPassword = signal('');
  protected readonly newPassword = signal('');
  protected readonly newPasswordConfirmation = signal('');

  protected readonly showCurrent = signal(false);
  protected readonly showNew = signal(false);
  protected readonly showConfirm = signal(false);

  protected readonly profileAlert = signal<AlertState | null>(null);
  protected readonly passwordAlert = signal<AlertState | null>(null);
  protected readonly profileBusy = signal(false);
  protected readonly passwordBusy = signal(false);
  protected profileErrors: Record<string, string> = {};
  protected passwordErrors: Record<string, string> = {};

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      const staff = await this.profileService.fetchStaffProfile();
      this.firstName.set(staff.firstName || '');
      this.lastName.set(staff.lastName || '');
      this.email.set(staff.email || '');
    } catch (error: any) {
      this.profileAlert.set({ type: 'danger', message: error.message });
    }
  }

  protected async onProfileSubmit(): Promise<void> {
    this.profileAlert.set(null);
    const values = {
      firstName: this.firstName().trim(),
      lastName: this.lastName().trim(),
    };
    const { errors, isValid } = validateForm(values, {
      firstName: [rules.required('Le prénom')],
      lastName: [rules.required('Le nom')],
    });
    this.profileErrors = errors;
    if (!isValid) {
      this.profileAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }
    this.profileBusy.set(true);
    try {
      await this.profileService.updateStaffProfile(values);
      this.toast.show('Profil mis à jour.', 'success');
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.profileErrors = { ...this.profileErrors, ...error.fieldErrors };
      }
      this.profileAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.profileBusy.set(false);
    }
  }

  protected async onPasswordSubmit(): Promise<void> {
    this.passwordAlert.set(null);
    const values = {
      currentPassword: this.currentPassword(),
      newPassword: this.newPassword(),
      newPasswordConfirmation: this.newPasswordConfirmation(),
    };
    const { errors, isValid } = validateForm(values, {
      currentPassword: [rules.required('Le mot de passe actuel')],
      newPassword: [rules.password],
      newPasswordConfirmation: [rules.matches('La confirmation', 'newPassword')],
    });
    this.passwordErrors = errors;
    if (!isValid) {
      this.passwordAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }
    this.passwordBusy.set(true);
    try {
      await this.profileService.changeStaffPassword(values);
      this.currentPassword.set('');
      this.newPassword.set('');
      this.newPasswordConfirmation.set('');
      this.toast.show('Mot de passe mis à jour.', 'success');
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.passwordErrors = { ...this.passwordErrors, ...error.fieldErrors };
      }
      this.passwordAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.passwordBusy.set(false);
    }
  }
}
