import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
// DEFAULT
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CommonModule } from '@angular/common';
// ANGULAR MATERIALS
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
// ADDED MODULES
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
// ADDED COMPONENTS
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { AccessRequestComponent } from './pages/access-request/access-request.component';
import { RequestsComponent } from './pages/requests/requests.component';
import { ConsentComponent } from './pages/consent/consent.component';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ArSelectionComponent } from './pages/ar-selection/ar-selection.component';
import { RightsComponent } from './pages/rights/rights.component';
import { RectificationComponent } from './pages/rectification/rectification.component';
import { SuppressionComponent } from './pages/suppression/suppression.component';
import { AuthConfig, DefaultOAuthInterceptor, OAuthModule, OAuthModuleConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../environment/environment';

// Provider-agnostic OIDC config: any standards-compliant issuer works here
// (Keycloak, Auth0, Okta, a broker delegating to the target application's own
// login, ...) — this app only ever talks to the issuer's standard
// .well-known/openid-configuration + token/authorize endpoints, never a
// provider-specific API. See Docs/PRIAM-AUTH-OIDC.md.
export function authConfigFactory(): AuthConfig {
  return new AuthConfig({
    issuer: environment.oidcIssuer,
    clientId: environment.oidcClientId,
    // Preserves the path the browser actually landed on (e.g. a target app
    // redirecting straight to "/consent") - hardcoding this to the origin
    // root silently dropped it after the Keycloak round trip, sending every
    // user back to "/" regardless of where they were meant to go. Keycloak's
    // registered redirect URI for this client is a "/*" wildcard, so any
    // path here is already accepted - see Docs/PRIAM-INTEGRATION-PLAYBOOK.md §8.
    redirectUri: window.location.origin + window.location.pathname,
    responseType: 'code',
    scope: 'openid profile email',
    showDebugInformation: !environment.production,
    // Pre-existing PRIAM bug (present in the untouched PRIAM-Frontend too,
    // see Docs/PRIAM-INTEGRATION-PLAYBOOK.md §8): this used to be
    // `environment.production` (true for this app's Dockerfile build).
    // angular-oauth2-oidc's `validateUrlForHttps()` treats `requireHttps:
    // true` as "reject every http:// url, no exceptions" - only the string
    // 'remoteOnly' gets the localhost carve-out. Since the discovery
    // document's `authorization_endpoint` is `http://localhost:8080/...`
    // (Keycloak over plain HTTP locally), this made `initLoginFlow()` throw
    // synchronously from inside an unawaited call, silently rejecting the
    // APP_INITIALIZER promise - permanently blank page, no console output
    // (the rejection never reaches main.ts's `.catch`), no redirect. Fixed
    // generically: 'remoteOnly' still requires HTTPS for any non-localhost
    // issuer, so this is strictly safer than the original, not a dev-only
    // bypass.
    requireHttps: 'remoteOnly',
  });
}

export function oauthModuleConfigFactory(): OAuthModuleConfig {
  return {
    resourceServer: {
      // The Gateway origin: every PRIAM API call the frontend makes goes
      // through it, so the token only ever needs to be attached here.
      allowedUrls: [environment.gatewayOrigin],
      sendAccessToken: true,
    },
  };
}

// Mirrors the previous "login-required" behaviour: block app bootstrap until
// either a valid session is restored, or the browser is redirected to the
// issuer's login page (in which case this promise deliberately never
// resolves — the page is navigating away).
export function oidcInitializer(oauthService: OAuthService): () => Promise<void> {
  return async () => {
    try {
      await oauthService.loadDiscoveryDocumentAndTryLogin();
    } catch {
      // Discovery/login failed (issuer unreachable, misconfigured, ...) —
      // fall through to initLoginFlow() below, same as an unauthenticated
      // session would.
    }
    if (!oauthService.hasValidAccessToken()) {
      // Forward ?login_hint=... (if the target app's redirect included one)
      // to the issuer's authorize call, so its login form can pre-fill the
      // username - target apps with no memorable username of their own
      // (e.g. a generated id) have no other way to tell the user what to
      // type. Generic: any OIDC issuer honours login_hint if present, and
      // this is a no-op when the param is absent. Set via customQueryParams
      // (not as initLoginFlow() call args) so the call below stays identical
      // to PRIAM-Frontend-Provider's - passing explicit args here previously
      // caused initLoginFlow() to silently never redirect (confirmed via a
      // real regression: Provider redirected to the issuer, Frontend didn't,
      // with otherwise identical Keycloak config).
      const loginHint = new URLSearchParams(window.location.search).get('login_hint');
      if (loginHint) {
        oauthService.customQueryParams = { login_hint: loginHint };
      }
      oauthService.initLoginFlow();
      // Deliberately never resolves: the browser is navigating away to the
      // issuer's login page, mirroring the previous "login-required" wait.
      return new Promise<void>(() => {});
    }
    // Access tokens are short-lived (Keycloak default: 5 min). Without this,
    // every API call silently starts failing (401) once the token expires,
    // well before the user's session "feels" like it should have ended —
    // uses the refresh_token transparently, no re-login/redirect involved.
    oauthService.setupAutomaticSilentRefresh();
  };
}

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    AccessRequestComponent,
    RequestsComponent,
    ConsentComponent,
    LoginComponent,
    HomeComponent,
    FooterComponent,
    ArSelectionComponent,
    RightsComponent,
    RectificationComponent,
    SuppressionComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    FormsModule,
    OAuthModule.forRoot(),
    MatToolbarModule,
    MatGridListModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatMenuModule,
    MatTableModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    CommonModule,
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
  bootstrap: [AppComponent],
})
export class AppModule {}
