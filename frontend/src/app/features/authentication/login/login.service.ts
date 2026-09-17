import { Injectable, inject } from '@angular/core';

import { AuthService } from '../../../core/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private readonly auth = inject(AuthService);

  loginUser(email: string, password: string): Promise<any> {
    return this.auth.login(email, password);
  }
}
