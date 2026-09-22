import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MsalService } from '@azure/msal-angular';
import { InteractionRequiredAuthError } from '@azure/msal-browser';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private readonly msal: MsalService) {
    this.msal.handleRedirectObservable().subscribe((result) => {
      if (result?.account) {
        this.msal.instance.setActiveAccount(result.account);
        return;
      }

      if (!this.msal.instance.getActiveAccount()) {
        const firstAccount = this.msal.instance.getAllAccounts()[0];
        if (firstAccount) {
          this.msal.instance.setActiveAccount(firstAccount);
        }
      }
    });
  }

  isAuthenticated(): boolean {
    return this.msal.instance.getAllAccounts().length > 0;
  }

  getAccount() {
    return this.msal.instance.getActiveAccount()
      ?? this.msal.instance.getAllAccounts()[0]
      ?? null;
  }

  login(): void {
    this.msal.loginRedirect({
      scopes: ['openid', 'profile', environment.auth.apiScope],
    }).subscribe();
  }

  register(): void {
    // prompt=create abre directamente el flujo de registro de autoservicio
    // (user flow de External Identities asociado a la app) en vez del login.
    this.msal.loginRedirect({
      scopes: ['openid', 'profile', environment.auth.apiScope],
      prompt: 'create',
    }).subscribe();
  }

  logout(): void {
    this.msal.logoutRedirect({
      account: this.getAccount() ?? undefined,
    }).subscribe();
  }

  async getAccessToken(): Promise<string> {
    const account = this.getAccount();

    if (!account) {
      throw new Error('No hay una cuenta autenticada.');
    }

    try {
      const result = await firstValueFrom(
        this.msal.acquireTokenSilent({
          account,
          scopes: [environment.auth.apiScope],
        }),
      );
      return result.accessToken;
    } catch (error) {
      if (error instanceof InteractionRequiredAuthError) {
        this.msal.acquireTokenRedirect({
          account,
          scopes: [environment.auth.apiScope],
        }).subscribe();
      }
      throw error;
    }
  }

  async getRoles(): Promise<string[]> {
    try {
      const token = await this.getAccessToken();
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return payload.roles ?? [];
    } catch {
      return [];
    }
  }
}
