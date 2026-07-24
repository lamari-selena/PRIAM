export const environment = {
  production: false,
  api_data: 'http://localhost:8090/data/api',
  api_const: 'http://localhost:8090/cdp/api',
  api_right: 'http://localhost:8090/right/api',
  api_actor: 'http://localhost:8090/actor',
  api_provider: 'http://localhost:8090/provider',
  gatewayOrigin: 'http://localhost:8090',
  // Provider-agnostic OIDC config — any standards-compliant issuer works
  // (Keycloak realm URL, Auth0 domain, Okta, a broker, ...). See
  // Docs/PRIAM-AUTH-OIDC.md.
  oidcIssuer: 'http://localhost:8080/realms/priam-realm',
  oidcClientId: 'Data-client',
  // Empty by default (no "back to the app" button on Home) — set per case
  // study via the Dockerfile's TARGET_APP_URL build arg.
  targetAppUrl: '',
}
