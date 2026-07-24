export const environment = {
  production: false,
 /* api_data: 'http://localhost:8081/api',
  api_right: 'http://localhost:8083/api',
  api_actor: 'http://localhost:8082/api',
  api_provider: 'http://localhost:8086',
  keycloak: "http://localhost:8080"*/
  api_data: 'http://localhost:8090/data/api',
  api_const: 'http://localhost:8090/cdp/api',
  api_right: 'http://localhost:8090/right/api',
  api_actor: 'http://localhost:8090/actor/api',
  api_provider: 'http://localhost:8090/provider',
  gatewayOrigin: 'http://localhost:8090',
  // Provider-agnostic OIDC config — any standards-compliant issuer works. See
  // Docs/PRIAM-AUTH-OIDC.md. Separate client from PRIAM-Frontend's
  // (Provider-client vs Data-client): this app is used by the application
  // owner/DPO, not a data subject.
  oidcIssuer: 'http://localhost:8080/realms/priam-realm',
  oidcClientId: 'Provider-client',
}
