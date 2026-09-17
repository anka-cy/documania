// Configuration héritée du bootstrap.js vanilla : URLs #/… inchangées
// (HashLocationStrategy, liens reçus par e-mail OK), scroll en haut à chaque
// navigation, session restaurée silencieusement AVANT le premier rendu.

import {
  ApplicationConfig,
  ErrorHandler,
  Injector,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter, RouteReuseStrategy, withHashLocation, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { AuthService } from './core/auth/auth.service';
import { ApiService } from './core/api/api.service';
import { registerCoreServices } from './core/singletons';
import { GlobalErrorHandler } from './core/global-error.handler';

/**
 * Stratégie de réutilisation : toujours recréer (comme le SPA vanilla qui
 * reconstruisait layout + vue à chaque changement de hash, même sur la même
 * route avec un paramètre différent).
 */
class AlwaysRecreateRouteStrategy implements RouteReuseStrategy {
  shouldDetach(): boolean {
    return false;
  }
  store(): void {}
  shouldAttach(): boolean {
    return false;
  }
  retrieve(): null {
    return null;
  }
  shouldReuseRoute(): boolean {
    return false;
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideRouter(
      routes,
      withHashLocation(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
    ),
    { provide: RouteReuseStrategy, useClass: AlwaysRecreateRouteStrategy },
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    // Équivalent du « await restoreSession(); start(); » de bootstrap.js.
    provideAppInitializer(async () => {
      const injector = inject(Injector);
      const auth = injector.get(AuthService);
      registerCoreServices(auth, injector.get(ApiService));
      await auth.restoreSession();
    }),
  ],
};
