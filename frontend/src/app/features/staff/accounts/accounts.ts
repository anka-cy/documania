/*
  Comptes staff (ADMIN) : la création produit toujours un compte inactif,
  l'accès n'étant possible qu'après envoi d'un lien d'activation par e-mail.
  Liste en rendu progressif par lots (sentinelle IntersectionObserver).
*/

import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/interceptors/error.interceptor';
import { ToastService } from '../../../shared/components/toast.service';
import { ConfirmService } from '../../../shared/components/confirm.service';
import { FormatDateTimePipe } from '../../../shared/pipes/formatters.pipes';
import { initials, promptReason } from '../../../shared/utils/utils';
import { rules, validateForm } from '../../../shared/validators/validators';
import { AccountsService } from './accounts.service';

interface AlertState {
  type: string;
  message: string;
}

const CHUNK = 20;

@Component({
  selector: 'app-staff-accounts',
  imports: [FormsModule, FormatDateTimePipe],
  templateUrl: './accounts.html',
})
export class StaffAccounts implements AfterViewInit, OnDestroy {
  private readonly svc = inject(AccountsService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);

  protected readonly currentEmail = this.auth.getEmail();

  protected readonly accounts = signal<any[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly visibleCount = signal(0);
  protected readonly visible = computed(() => this.accounts().slice(0, this.visibleCount()));
  protected readonly hasMore = computed(() => this.visibleCount() < this.accounts().length);

  protected readonly sentinel = viewChild<ElementRef<HTMLElement>>('sentinel');
  private observer: IntersectionObserver | null = null;

  // --- Modale de création ---
  private readonly modalEl = viewChild<ElementRef<HTMLElement>>('accountModal');
  private modal: any;
  protected readonly formFirstName = signal('');
  protected readonly formLastName = signal('');
  protected readonly formEmail = signal('');
  protected readonly formBusy = signal(false);
  protected readonly formAlert = signal<AlertState | null>(null);
  protected formErrors: Record<string, string> = {};

  constructor() {
    effect(() => {
      const el = this.sentinel()?.nativeElement;
      if (el && this.observer) this.observer.observe(el);
    });
    void this.reload();
  }

  ngAfterViewInit(): void {
    const modalEl = this.modalEl()?.nativeElement;
    if (modalEl) this.modal = new bootstrap.Modal(modalEl);
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && this.visibleCount() < this.accounts().length) {
            this.visibleCount.update((c) => c + CHUNK);
          }
        }
      },
      { rootMargin: '200px 0px', threshold: 0.01 },
    );
    const el = this.sentinel()?.nativeElement;
    if (el) this.observer.observe(el);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  protected initials(firstName: string, lastName: string): string {
    return initials(firstName, lastName);
  }

  protected isSelf(account: any): boolean {
    return account.email === this.currentEmail;
  }

  private async reload(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const accounts = await this.svc.fetchAccounts() ?? [];
      this.accounts.set(accounts);
      this.visibleCount.set(Math.min(CHUNK, accounts.length));
    } catch (error: any) {
      this.error.set(error.message);
    } finally {
      this.loading.set(false);
    }
  }

  // --- Actions ---
  protected async sendActivation(id: string): Promise<void> {
    try {
      await this.svc.sendActivationToken(id);
      this.toast.show("Lien d'activation envoyé par e-mail.", 'success');
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async toggleEnabled(id: string, currentlyEnabled: boolean): Promise<void> {
    const confirmed = await this.confirm.confirm(
      currentlyEnabled ? 'Désactiver ce compte ?' : 'Réactiver ce compte ?',
      { title: currentlyEnabled ? 'Désactiver' : 'Activer', okText: currentlyEnabled ? 'Désactiver' : 'Activer', okVariant: 'warning' },
    );
    if (!confirmed) return;
    try {
      await this.svc.setAccountEnabled(id, !currentlyEnabled);
      this.toast.show(currentlyEnabled ? 'Compte désactivé.' : 'Compte activé.', 'success');
      await this.reload();
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  protected async deleteAccount(id: string, name: string): Promise<void> {
    const reason = await promptReason(`Supprimer définitivement le compte de ${name} ?`);
    if (reason === null) return;
    try {
      await this.svc.deleteAccount(id, reason);
      this.toast.show('Compte supprimé.', 'success');
      await this.router.navigateByUrl('/staff/accounts');
    } catch (error: any) {
      this.toast.show(error.message, 'danger');
    }
  }

  // --- Modale ---
  protected openCreate(): void {
    this.formFirstName.set('');
    this.formLastName.set('');
    this.formEmail.set('');
    this.formErrors = {};
    this.formAlert.set(null);
    this.modal?.show();
  }

  protected async onFormSubmit(): Promise<void> {
    this.formAlert.set(null);
    const values = {
      firstName: this.formFirstName().trim(),
      lastName: this.formLastName().trim(),
      email: this.formEmail().trim(),
    };
    const { errors, isValid } = validateForm(values, {
      firstName: [rules.required('Le prénom')],
      lastName: [rules.required('Le nom')],
      email: [rules.required("L'adresse e-mail"), rules.email("L'adresse e-mail")],
    });
    this.formErrors = errors;
    if (!isValid) return;
    this.formBusy.set(true);
    try {
      await this.svc.createAccount(values);
      this.modal?.hide();
      this.formFirstName.set('');
      this.formLastName.set('');
      this.formEmail.set('');
      this.toast.show("Compte créé. Envoyez maintenant le lien d'activation.", 'success');
      await this.reload();
    } catch (error: any) {
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors) {
        this.formErrors = { ...error.fieldErrors };
      }
      this.formAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.formBusy.set(false);
    }
  }
}
