import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { MSAL_INSTANCE, MsalBroadcastService, MsalService } from '@azure/msal-angular';
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
  ]
};
