import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
//DEFAULT
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
// ADDED MATERIALS
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';
// ADDED MODULES
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
// ADDED COMPONENTS
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AccessRequestComponent } from './pages/access-request/access-request.component';
import { RectificationComponent } from './pages/rectification/rectification.component';
import { SuppressionComponent } from './pages/suppression/suppression.component';
import { AuthConfig, DefaultOAuthInterceptor, OAuthModule, OAuthModuleConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../environment/environment';

// Provider-agnostic OIDC config — see PRIAM-Frontend/app.module.ts for the
// same pattern with more detailed comments, and Docs/PRIAM-AUTH-OIDC.md.
export function authConfigFactory(): AuthConfig {
  return {
    issuer: environment.oidcIssuer,
    clientId: environment.oidcClientId,
    redirectUri: window.location.origin + '/',
    responseType: 'code',
    scope: 'openid profile email',
    showDebugInformation: !environment.production,
    // See PRIAM-Frontend/app.module.ts for the full explanation - this app's
    // build currently happens to ship with production:false so it never hit
    // the bug, but the same `environment.production` mapping would break the
    // redirect the moment that build flag changes. 'remoteOnly' still
    // requires HTTPS for any non-localhost issuer.
    requireHttps: 'remoteOnly',
  };
}

export function oauthModuleConfigFactory(): OAuthModuleConfig {
  return {
    resourceServer: {
      allowedUrls: [environment.gatewayOrigin],
      sendAccessToken: true,
    },
  };
}

export function oidcInitializer(oauthService: OAuthService): () => Promise<void> {
  return async () => {
    try {
      await oauthService.loadDiscoveryDocumentAndTryLogin();
    } catch {
      // fall through to initLoginFlow() below
    }
    if (!oauthService.hasValidAccessToken()) {
      oauthService.initLoginFlow();
      return new Promise<void>(() => {});
    }
    // Access tokens are short-lived (Keycloak default: 5 min) — without this,
    // every API call silently starts failing (401) once it expires. Uses the
    // refresh_token transparently, no re-login/redirect involved.
    oauthService.setupAutomaticSilentRefresh();
  };
}

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    NavbarComponent,
    FooterComponent,
    AccessRequestComponent,
    RectificationComponent,
    SuppressionComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    OAuthModule.forRoot(),
    MatToolbarModule,
    MatSidenavModule,
    MatButtonModule,
    MatListModule,
    MatSlideToggleModule,
    MatTableModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatButtonToggleModule,
    MatSnackBarModule,
  ],
  providers: [
    { provide: AuthConfig, useFactory: authConfigFactory },
    { provide: OAuthModuleConfig, useFactory: oauthModuleConfigFactory },
    { provide: HTTP_INTERCEPTORS, useClass: DefaultOAuthInterceptor, multi: true },
    {
      provide: APP_INITIALIZER,
      deps: [OAuthService],
      useFactory: oidcInitializer,
      multi: true,
    },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
