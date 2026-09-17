/*
  Inscription client (POST /api/public/register) : le compte reste inactif
  tant que l'e-mail n'a pas été vérifié via le lien envoyé.
*/

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiError } from '../../../core/interceptors/error.interceptor';
import { rules, validateForm } from '../../../shared/validators/validators';
import { RegisterService } from './register.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-register',
  imports: [FormsModule],
  templateUrl: './register.html',
})
export class Register {
  private readonly registerService = inject(RegisterService);

  protected firstName = '';
  protected lastName = '';
  protected companyName = '';
  protected email = '';
  protected phone = '';
  protected address = '';
  protected sector = '';
  protected password = '';
  protected passwordConfirmation = '';

  protected readonly showPassword = signal(false);
  protected readonly showPasswordConfirmation = signal(false);
  protected readonly busy = signal(false);
  protected readonly alert = signal<AlertState | null>(null);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly registered = signal(false);
  protected readonly registeredEmail = signal('');

  protected async onSubmit(): Promise<void> {
    this.alert.set(null);

    const values = {
      email: this.email.trim(),
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      companyName: this.companyName.trim(),
      phone: this.phone.trim() || null,
      address: this.address.trim() || null,
      sector: this.sector.trim() || null,
      password: this.password,
      passwordConfirmation: this.passwordConfirmation,
    };

    const fieldRules = {
      firstName: [rules.required('Le prénom')],
      lastName: [rules.required('Le nom')],
      companyName: [rules.required("Le nom d'entreprise")],
      email: [rules.required("L'adresse e-mail"), rules.email("L'adresse e-mail")],
      password: [rules.password],
      passwordConfirmation: [rules.matches('La confirmation', 'password')],
    };
    const { errors, isValid } = validateForm(values, fieldRules);
    this.errors.set(errors);
    if (!isValid) {
      this.alert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }

    this.busy.set(true);
    try {
      await this.registerService.registerAccount(values);
      this.registeredEmail.set(values.email);
      this.registered.set(true);
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 409) {
        this.alert.set({ type: 'danger', message: error.message || 'Un compte existe déjà avec cette adresse e-mail.' });
      } else if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.errors.set(error.fieldErrors);
        this.alert.set({ type: 'danger', message: 'Veuillez corriger les champs signalés.' });
      } else {
        this.alert.set({ type: 'danger', message: error.message });
      }
    } finally {
      this.busy.set(false);
    }
  }
}
