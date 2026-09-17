/*
  Connexion : validation légère côté client, le backend reste l'autorité.
  Session stockée en mémoire, redirection selon le rôle.
*/

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/interceptors/error.interceptor';
import { homePathForRole } from '../../../core/guards/auth.guard';
import { rules, validateForm } from '../../../shared/validators/validators';
import { showToast } from '../../../shared/components/toast';
import { LoginService } from './login.service';

interface AlertState {
  type: string;
  message: string;
}

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);

  protected email = '';
  protected password = '';
  protected readonly showPassword = signal(false);
  protected readonly busy = signal(false);
  protected readonly alert = signal<AlertState | null>(null);
  protected errors: Record<string, string> = {};

  constructor() {
    const pending = this.auth.consumeRedirectMessage();
    if (pending) showToast(pending, 'info');
  }

  protected async onSubmit(): Promise<void> {
    const values = {
      email: this.email.trim(),
      password: this.password,
    };

    const fieldRules = {
      email: [rules.required("L'adresse e-mail"), rules.email("L'adresse e-mail")],
      password: [rules.required('Le mot de passe')],
    };
    const { errors, isValid } = validateForm(values, fieldRules);
    this.errors = errors;
    if (!isValid) {
      this.alert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }

    this.busy.set(true);
    this.alert.set(null);
    try {
      await this.loginService.loginUser(values.email, values.password);
      showToast('Connexion réussie. Bienvenue !', 'success');
      await this.router.navigateByUrl(homePathForRole(this.auth.getRole()));
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 429) {
        this.alert.set({ type: 'danger', message: 'Trop de tentatives de connexion. Réessayez plus tard ou réinitialisez votre mot de passe.' });
      } else if (error instanceof ApiError && error.status === 401) {
        this.alert.set({ type: 'danger', message: 'Identifiants invalides. Vérifiez votre e-mail et votre mot de passe.' });
      } else {
        this.alert.set({ type: 'danger', message: error.message });
      }
    } finally {
      this.busy.set(false);
    }
  }
}
