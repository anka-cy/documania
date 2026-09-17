// Ajoute automatiquement l'en-tête Authorization: Bearer <jeton> à chaque
// requête dès qu'une session existe en mémoire.

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const accessToken = inject(AuthService).getAccessToken();
  if (accessToken) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${accessToken}` },
    });
  }
  return next(req);
};
