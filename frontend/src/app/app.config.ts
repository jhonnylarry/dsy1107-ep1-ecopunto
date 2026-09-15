import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { MSAL_INSTANCE, MsalBroadcastService, MsalService } from '@azure/msal-angular';
import type { IPublicClientApplication } from '@azure/msal-browser';
import { routes } from './app.routes';
import { msalInstanceFactory } from './auth/auth.config';
import { authInterceptor } from './auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    {
      provide: MSAL_INSTANCE,
      useFactory: msalInstanceFactory,
    },
    MsalService,
    MsalBroadcastService,
    provideHttpClient(withInterceptors([authInterceptor])),
    // MSAL Browser v3+ exige initialize() antes de usar cualquier otra API, y
    // hay que esperar handleRedirectPromise() antes de que la app renderice
    // la primera vez; si no, la cuenta activa no está lista todavía y hace
    // falta una segunda interacción para que la UI se entere del login.
    provideAppInitializer(() => {
      const msalInstance = inject(MSAL_INSTANCE) as IPublicClientApplication;
      return msalInstance.initialize().then(() => msalInstance.handleRedirectPromise());
    }),
  ]
};
