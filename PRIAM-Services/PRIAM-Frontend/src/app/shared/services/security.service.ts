import { Injectable } from "@angular/core";
import { OAuthService } from "angular-oauth2-oidc";

// Provider-agnostic: reads standard ID token claims via angular-oauth2-oidc,
// which decodes them identically regardless of which OIDC issuer signed the
// token. `idReference` is a custom claim the issuer must be configured to
// include in the ID token (e.g. a Keycloak "User Attribute" protocol
// mapper) — not a provider-specific API call. See Docs/PRIAM-AUTH-OIDC.md.
@Injectable({ providedIn: "root" })
export class SecurityService {
  public claims?: Record<string, unknown>;

  constructor(public oauthService: OAuthService) {
    this.init();
  }

  init() {
    if (this.oauthService.hasValidIdToken()) {
      this.claims = this.oauthService.getIdentityClaims() as Record<string, unknown> | undefined;
    }
    this.oauthService.events.subscribe((event) => {
      if (event.type === 'token_received') {
        this.claims = this.oauthService.getIdentityClaims() as Record<string, unknown> | undefined;
      }
    });
  }

  // Returns the raw idRef string, exactly as PRIAM's backend expects it
  // (Data-service, Consent-service and the Provider bridge all take idRef
  // as a String — see PRIAM-Actor-service's DataSubject.idRef column,
  // varchar). Previously forced through parseInt(), which silently
  // returned null for any non-numeric idRef (e.g. a target app using UUID
  // or free-form string user ids, not just apps whose ids happened to be
  // numeric strings) — every consumer treats null as "skip this call
  // entirely", so a non-numeric idRef made the Access Request and Consent
  // pages appear to load with no data and no error, and consent toggles
  // silently did nothing (no HTTP request was ever made).
  getIdReference(): string | null {
    const raw = this.claims?.['idReference'];
    const value = Array.isArray(raw) ? raw[0] : raw;
    if (typeof value === 'string' && value.length > 0) {
      return value;
    }
    if (typeof value === 'number') {
      return String(value);
    }
    return null;
  }

  // Standard OIDC claims (requested via scope "openid profile email"),
  // populated identically regardless of issuer.
  getDisplayName(): string | null {
    const name = this.claims?.['given_name'] ?? this.claims?.['name'] ?? this.claims?.['preferred_username'];
    return typeof name === 'string' ? name : null;
  }

  isLoggedIn(): boolean {
    return this.oauthService.hasValidIdToken();
  }

  logout(): void {
    this.oauthService.logOut();
  }
}
