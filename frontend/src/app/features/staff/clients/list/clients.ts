/*
  Clients (portail staff) : listes active/archivée, recherche locale sur les
  listes déjà chargées, création via modale.
*/

import { AfterViewInit, Component, DestroyRef, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../../core/auth/auth.service';
import { rules, validateForm } from '../../../../shared/validators/validators';
import { ToastService } from '../../../../shared/components/toast.service';
import { ClientsService } from './clients.service';

interface AlertState {
  type: string;
  message: string;
}

const CHUNK_SIZE = 20;

@Component({
  selector: 'app-staff-clients',
  imports: [FormsModule],
  templateUrl: './clients.html',
})
export class StaffClients implements AfterViewInit {
  private readonly clientsService = inject(ClientsService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly canCreate = this.auth.isAdmin() || this.auth.hasAuthority('CLIENT_CREATE');

  protected readonly searchText = signal('');
  protected readonly searchTerm = computed(() => this.searchText().trim().toLowerCase());

  protected readonly activeClients = signal<any[]>([]);
  protected readonly archivedClients = signal<any[]>([]);
  protected readonly activeLoading = signal(false);
  protected readonly activeError = signal<string | null>(null);
  protected readonly archivedLoading = signal(false);
  protected readonly archivedError = signal<string | null>(null);
  protected readonly archivedLoaded = signal(false);
  protected readonly archivedDenied = signal(false);

  // Rendu progressif par lots.
  protected readonly activeVisibleCount = signal(CHUNK_SIZE);
  protected readonly archivedVisibleCount = signal(CHUNK_SIZE);
  private readonly activeSentinel = viewChild<ElementRef<HTMLElement>>('activeSentinel');
  private readonly archivedSentinel = viewChild<ElementRef<HTMLElement>>('archivedSentinel');
  private readonly tabArchived = viewChild<ElementRef<HTMLElement>>('tabArchived');
  private readonly createModal = viewChild<ElementRef<HTMLElement>>('createModal');

  // Formulaire de création.
  protected readonly firstName = signal('');
  protected readonly lastName = signal('');
  protected readonly companyName = signal('');
  protected readonly email = signal('');
  protected readonly phone = signal('');
  protected readonly sector = signal('');
  protected readonly address = signal('');
  protected readonly busy = signal(false);
  protected readonly createAlert = signal<AlertState | null>(null);
  protected errors: Record<string, string> = {};

  protected readonly activeFiltered = computed(() => this.activeClients().filter((c) => this.matches(c)));
  protected readonly archivedFiltered = computed(() => this.archivedClients().filter((c) => this.matches(c)));
  protected readonly visibleActive = computed(() => this.activeFiltered().slice(0, this.activeVisibleCount()));
  protected readonly visibleArchived = computed(() => this.archivedFiltered().slice(0, this.archivedVisibleCount()));
  protected readonly moreActive = computed(() => this.activeVisibleCount() < this.activeFiltered().length);
  protected readonly moreArchived = computed(() => this.archivedVisibleCount() < this.archivedFiltered().length);

  constructor() {
    void this.loadActive();

    effect((onCleanup) => {
      const el = this.activeSentinel()?.nativeElement;
      if (!el) return;
      const observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting)) {
            this.activeVisibleCount.update((count) => Math.min(count + CHUNK_SIZE, this.activeFiltered().length));
          }
        },
        { rootMargin: '200px 0px', threshold: 0.01 },
      );
      observer.observe(el);
      onCleanup(() => observer.disconnect());
    });

    effect((onCleanup) => {
      const el = this.archivedSentinel()?.nativeElement;
      if (!el) return;
      const observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting)) {
            this.archivedVisibleCount.update((count) => Math.min(count + CHUNK_SIZE, this.archivedFiltered().length));
          }
        },
        { rootMargin: '200px 0px', threshold: 0.01 },
      );
      observer.observe(el);
      onCleanup(() => observer.disconnect());
    });
  }

  ngAfterViewInit(): void {
    // Chargement paresseux des archives au premier affichage de l'onglet.
    const tab = this.tabArchived()?.nativeElement;
    if (tab) {
      const handler = () => void this.loadArchived();
      tab.addEventListener('shown.bs.tab', handler);
      this.destroyRef.onDestroy(() => tab.removeEventListener('shown.bs.tab', handler));
    }
  }

  private matches(client: any): boolean {
    const term = this.searchTerm();
    if (!term) return true;
    const haystack = [
      client.companyName,
      client.firstName,
      client.lastName,
      client.email,
      client.phone,
      client.sector,
    ].filter(Boolean).join(' ').toLowerCase();
    return haystack.includes(term);
  }

  protected onSearchInput(value: string): void {
    this.searchText.set(value);
    this.activeVisibleCount.set(CHUNK_SIZE);
    this.archivedVisibleCount.set(CHUNK_SIZE);
  }

  private async loadActive(): Promise<void> {
    this.activeLoading.set(true);
    this.activeError.set(null);
    try {
      this.activeClients.set(await this.clientsService.fetchActiveClients());
    } catch (error: any) {
      this.activeError.set(error.message);
    } finally {
      this.activeLoading.set(false);
    }
  }

  private async loadArchived(): Promise<void> {
    if (this.archivedLoaded()) return;
    this.archivedLoading.set(true);
    this.archivedError.set(null);
    this.archivedDenied.set(false);
    try {
      this.archivedClients.set(await this.clientsService.fetchArchivedClients());
      this.archivedLoaded.set(true);
    } catch (error: any) {
      if (error.status !== 403) {
        this.archivedError.set(error.message);
      } else {
        // 403 : état « non autorisé » réessayable, pas une erreur bloquante.
        this.archivedDenied.set(true);
      }
    } finally {
      this.archivedLoading.set(false);
    }
  }

  protected async onCreate(): Promise<void> {
    this.createAlert.set(null);
    const values = {
      email: this.email().trim(),
      firstName: this.firstName().trim(),
      lastName: this.lastName().trim(),
      companyName: this.companyName().trim(),
      phone: this.phone().trim() || null,
      sector: this.sector().trim() || null,
      address: this.address().trim() || null,
    };
    const { errors, isValid } = validateForm(values, {
      firstName: [rules.required('Le prénom')],
      lastName: [rules.required('Le nom')],
      companyName: [rules.required("Le nom d'entreprise")],
      email: [rules.required("L'adresse e-mail"), rules.email("L'adresse e-mail")],
    });
    this.errors = errors;
    if (!isValid) {
      this.createAlert.set({ type: 'danger', message: 'Veuillez corriger les champs en rouge.' });
      return;
    }

    this.busy.set(true);
    try {
      const created = await this.clientsService.createClient(values);
      bootstrap.Modal.getOrCreateInstance(this.createModal()!.nativeElement).hide();
      this.firstName.set('');
      this.lastName.set('');
      this.companyName.set('');
      this.email.set('');
      this.phone.set('');
      this.sector.set('');
      this.address.set('');
      this.errors = {};
      this.toast.show(`Client « ${created.companyName} » créé.`, 'success');
      this.activeClients.set(await this.clientsService.fetchActiveClients());
      this.activeVisibleCount.set(CHUNK_SIZE);
    } catch (error: any) {
      if (error.status === 400 && error.fieldErrors) {
        this.errors = error.fieldErrors;
      }
      this.createAlert.set({ type: 'danger', message: error.message });
    } finally {
      this.busy.set(false);
    }
  }

  protected dash(value: unknown): string {
    return value === null || value === undefined || value === '' ? '—' : String(value);
  }
}
