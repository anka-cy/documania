/* Vérification e-mail via POST /api/public/account-verification (jeton reçu par e-mail). */

import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { pageParams } from '../../../core/router/params';
import { VerificationService } from './verification.service';

@Component({
  selector: 'app-verification',
  templateUrl: './verification.html',
})
export class Verification implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly verificationService = inject(VerificationService);

  protected readonly state = signal<'loading' | 'invalid' | 'success' | 'error'>('loading');
  protected readonly errorMessage = signal('');

  async ngOnInit(): Promise<void> {
    const token = pageParams(this.route.snapshot)['token'];

    if (!token) {
      this.state.set('invalid');
      return;
    }

    try {
      await this.verificationService.verifyAccount(token);
      this.state.set('success');
    } catch (error: any) {
      this.errorMessage.set(
        error.status === 400 || error.status === 404 || error.status === 409
          ? 'Le lien est invalide ou a déjà été utilisé. Demandez un nouveau lien.'
          : error.message,
      );
      this.state.set('error');
    }
  }
}
