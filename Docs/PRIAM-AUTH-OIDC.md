# PRIAM Authentication — generic OIDC resource server + client

> Complements `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` without being merged into it:
> this document covers only authentication/authorization (Gateway + Frontend), not
> the rest of a case-study integration (annotation, Provider bridge, CEP, rights
> workflow — already covered by the playbook). The playbook links back here from
> its §7 and its final checklist.
>
> **Status**: all three pieces (Gateway resource server, both OIDC client
> frontends — `PRIAM-Frontend` and `PRIAM-Frontend-Provider`) are written,
> generic, and validated end-to-end against a real IdP (Keycloak, reused as a
> concrete example — see §5, §5bis), including CORS (§4bis), token refresh
> (§3bis), and a real test from the browser (§10). What remains to be done is
> listed explicitly at the end of the document (§10), not hidden.

## 1. The problem this solves

`PRIAM-Gateway` had an authentication mechanism (`KeycloakLoginCheckFilter` +
`KeycloakService`, now removed) that was **not generic by design**: it called
Keycloak's admin API directly to read a custom `isLoggedIn` attribute — never
written anywhere in the repository, so broken — and validated no JWT, no
signature, nothing standard. `PRIAM-Frontend` had the same problem on the client
side: wired directly to `keycloak-angular`/`keycloak-js` (Keycloak's proprietary
SDK), with a realm/clientId hardcoded in `app.module.ts`, and **no HTTP
interceptor** attached the token to API calls — even once logged in, no request
to PRIAM would ever have carried an `Authorization: Bearer ...` header.

Both problems are now solved by the same principle: **speak only the standard
OIDC protocol**, never a specific provider's proprietary API.

## 2. The generic approach — Gateway (resource server)

`PRIAM-Gateway/build.gradle` has declared
`spring-boot-starter-oauth2-resource-server` from the start — never wired up
until now. An OAuth2 resource server is **provider-agnostic by construction**: it
only knows an `issuer-uri` (standard OIDC discovery,
`.well-known/openid-configuration`) and verifies JWT signatures against the
public keys it exposes. Keycloak, Auth0, Okta, Entra ID, Cognito, or a home-grown
IdP — the Gateway never knows which one it is talking to.

`PRIAM-Services/PRIAM-Gateway/.../SecurityConfig.java`:

```java
@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
private String issuerUri;

@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
private String jwkSetUri;  // see §4 — the split-network Docker case

@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable);

    if (issuerUri == null || issuerUri.isBlank()) {
        http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
    } else {
        http.authorizeExchange(exchanges -> exchanges
                        .pathMatchers(MACHINE_TO_MACHINE_ROUTES).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(buildJwtDecoder())));
    }
    return http.build();
}
```

- **Machine-to-machine routes** (`/data/**`, `/actor/**`, `/provider/**`,
  `/eureka/**`, `/health`): always `permitAll()`, regardless of the config's
  state. These are PRIAM-to-PRIAM calls (`Consent → Gateway → Data/Actor` to
  resolve a name/idRef, `Right → Gateway → Provider` for a right's
  auto-execution) that never carry a human session.
- **Human-facing routes** (`/right/**`, `/cdp/**`): require a valid JWT **once
  `issuer-uri` is configured**.

Authorization decisions are now **centralized** in `SecurityConfig.java` — the
routes themselves (`GatewayApplication.java`) now only do routing, they no
longer reference any per-route auth filter.

## 3. The generic approach — Frontend (OIDC client)

`PRIAM-Frontend` now uses **`angular-oauth2-oidc`**, a neutral Angular library
(not a provider SDK), configured entirely through `environment.ts`:

```typescript
// app.module.ts — excerpt
export function authConfigFactory(): AuthConfig {
  return {
    issuer: environment.oidcIssuer,      // any OIDC provider
    clientId: environment.oidcClientId,
    redirectUri: window.location.origin + '/',
    responseType: 'code',                // Authorization Code + PKCE
    scope: 'openid profile email',
  };
}
```

Three pieces, all generic:

1. **`APP_INITIALIZER` (`oidcInitializer`)** — reproduces "login-required"
   behavior: on startup, it tries to restore a session (a return from a
   redirect); otherwise it calls `initLoginFlow()`, which redirects to **the
   configured issuer's login page**, whichever one it is.
2. **HTTP interceptor (`DefaultOAuthInterceptor`, provided by the library)** —
   THE fix that was missing: automatically attaches the token to every request
   toward `environment.gatewayOrigin` (configured via
   `resourceServer.allowedUrls`). Before this, even logged in, the frontend
   never sent the token to PRIAM's APIs.
3. **`SecurityService`** — no longer reads a proprietary API (the old
   `KeycloakService.loadUserProfile()` called Keycloak's specific "account"
   API). It now reads the **ID token's claims** via
   `oauthService.getIdentityClaims()` — a standard OIDC mechanism that works
   identically regardless of the issuer. `idReference` (the link between the
   OIDC identity and the target application's `dataSubjectId`/`idRef`) is a
   custom claim that **the IdP must be configured to include in the token** —
   see §5.

## 3bis. Pitfall found and fixed — automatic token refresh (frontend)

A Keycloak access token expires after **5 minutes** by default. Neither
`PRIAM-Frontend` nor `PRIAM-Frontend-Provider` originally called
`oauthService.setupAutomaticSilentRefresh()` — past that delay, every API call
silently fails with `401` (`DefaultOAuthInterceptor` still attaches the stale
token, and the Gateway rejects it). No redirect and no visible error message:
just data that stops arriving ("No data available", "No requests available")
even though the filters and the database state are correct — easy to miss in a
quick test, systematic as soon as a session lasts more than 5 minutes.

Fixed: a call added at the end of `oidcInitializer`, once
`hasValidAccessToken()` is confirmed:

```typescript
// uses the refresh_token in the background — no visible interruption
oauthService.setupAutomaticSilentRefresh();
```

**If you add a new OIDC frontend to the project, remember this same call** —
nothing in `angular-oauth2-oidc` does it by default.

## 4. Docker network pitfall — `issuer-uri` split from `jwk-set-uri`

**Encountered and fixed this session, not to be rediscovered.** In a Docker
Compose deployment where the IdP is exposed both on the internal network
(`http://keycloak:8080`, reachable by containers) and published on the host
(`http://localhost:8080`, reachable by the browser):

- A token obtained by the **browser** carries
  `iss: http://localhost:8080/realms/...` (the address it actually used to
  reach the IdP).
- The **Gateway**, inside its container, cannot resolve `localhost:8080` to
  the IdP (that would point back at itself) — it must reach it via
  `http://keycloak:8080`.

If `issuer-uri` is configured with the internal address, validating the `iss`
claim fails (`"The iss claim is not valid"`) since the token contains the
external address. If configured with the external address, it is **fetching
the public keys** (a real network call, not just a string comparison) that
fails from inside the container.

**Solution**: split the two responsibilities. `issuer-uri` is only used to
validate the `iss` claim (a string comparison, must match what the browser
sees); `jwk-set-uri`, if set, is only used to fetch the keys (must be
reachable from the Gateway):

```java
private ReactiveJwtDecoder buildJwtDecoder() {
    if (jwkSetUri == null || jwkSetUri.isBlank()) {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri); // simple case: everything is in the same place
    }
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
    return decoder;
}
```

`CUSTOM_OIDC_JWK_SET_URI` stays **empty by default** — this fix only applies if
the deployment genuinely has this split network view (the typical case: a
containerized IdP + a containerized Gateway + a browser on the host). A
production application with a single external IdP usually does not need it.

## 4bis. Pitfall found and fixed — CORS ineffective on proxied routes

`GatewayApplication` carried a `@CrossOrigin` annotation — the classic Spring
MVC pattern, **with no effect whatsoever** on the routes `RouteLocator` proxies
(reactive Spring Cloud Gateway never goes through the annotated dispatch that
`@CrossOrigin` intercepts). Symptom, reported by the user ("rights, consent...
unavailable"): the browser's `OPTIONS` preflight received `401` with no
`Access-Control-Allow-*` header, so **every** cross-origin call (any frontend
talking to the Gateway) was blocked on the browser side — invisible in curl (no
preflight there), so invisible in the §5/§6 tests as long as they stayed in
curl.

Fixed: CORS configured explicitly **inside `SecurityWebFilterChain`** (the only
place that actually has authority over the proxied routes), before the
fail-open/authenticated branch:

```java
http.csrf(ServerHttpSecurity.CsrfSpec::disable)
    .cors(cors -> cors.configurationSource(corsConfigurationSource()));
```

Allowed origins are driven by `CUSTOM_FRONTEND_ORIGINS` (a comma-separated list
— see §5). `@CrossOrigin` removed (dead code). Verified with curl: the
`OPTIONS` preflight returns `200` with the right headers; then validated under
real conditions with a rectification test from a real browser (§10).

**If you add a new frontend on a new port/origin, add it to
`CUSTOM_FRONTEND_ORIGINS`** — otherwise the same symptom recurs, invisible in
curl.

## 5. How to configure — environment variables

| Variable | Where | Role |
|---|---|---|
| `CUSTOM_OIDC_ISSUER_URI` | root `.env` (Gateway) | Must match the `iss` claim of real tokens — i.e. the address **the browser** uses to reach the IdP. Empty = fail-open (see below). |
| `CUSTOM_OIDC_JWK_SET_URI` | root `.env` (Gateway) | Optional — only if the Gateway cannot reach `CUSTOM_OIDC_ISSUER_URI` directly (§4). |
| `CUSTOM_FRONTEND_ORIGINS` | root `.env` (Gateway) | Comma-separated list of origins allowed by CORS (§4bis) — one entry per deployed human-facing frontend (`http://localhost:4200,http://localhost:4000`, ...). |
| `OIDC_ISSUER` / `OIDC_CLIENT_ID` | build args of `PRIAM-Frontend/Dockerfile`, `PRIAM-Frontend-Provider/Dockerfile` | Same values as `CUSTOM_OIDC_ISSUER_URI` on the browser side, plus the OIDC client id (a distinct client per frontend, §5bis). |

### Concrete example validated this session (Keycloak as the test IdP)

Keycloak is not a generic prerequisite — **any OIDC IdP works**; it just served
as a concrete provider to validate the mechanism end-to-end (see §6). Reusable
as-is as a starting point:

- A `keycloak` service added to the root `docker-compose.yml`
  (`quay.io/keycloak/keycloak`, `start-dev --import-realm`, realm imported from
  `Keycloak/priam-realm-realm.json`). **Naming pitfall**: Keycloak's
  directory-based import provider requires the file to be named exactly
  `<realm-name>-realm.json` (so `priam-realm-realm.json` for a `priam-realm`
  realm) — a file named just `priam-realm.json` (the intuitive name) fails at
  startup with `ERROR: File name / realm name mismatch`, and the server does
  not start at all. Encountered during the Habitica integration.
- `Keycloak/priam-realm-realm.json` defines: the `priam-realm` realm, a public
  client `Data-client` (Authorization Code + PKCE, `directAccessGrantsEnabled`
  for curl tests), a **standard protocol mapper**
  (`oidc-usermodel-attribute-mapper`) that exposes the user attribute
  `idReference` as an `idReference` claim in both the ID token and the access
  token — a generic OIDC mechanism (every IdP has an equivalent), not a
  proprietary API.
- A test user (`jane.doe` / `TestPass123!`, attribute `idReference: "1"`,
  matching FastAPI-Healthcare-PRIAM's patient `id=1`).
- `.env`:
  ```bash
  CUSTOM_OIDC_ISSUER_URI=http://localhost:8080/realms/priam-realm
  CUSTOM_OIDC_JWK_SET_URI=http://keycloak:8080/realms/priam-realm/protocol/openid-connect/certs
  ```
- `docker-compose.yml`, `frontuser` service (uncommented), build args:
  `OIDC_ISSUER: localhost:8080/realms/priam-realm`,
  `OIDC_CLIENT_ID: Data-client`.

## 5bis. Second frontend — `PRIAM-Frontend-Provider`, the same pattern with a distinct OIDC client

`PRIAM-Frontend-Provider` (role: data controller, not data subject) reuses
exactly the same mechanism as `PRIAM-Frontend` (§3) — `angular-oauth2-oidc`,
the same `app.module.ts` pattern, the same interceptor, the same
`setupAutomaticSilentRefresh()` (§3bis). Only two differences:

- **A separate OIDC client**: `Provider-client` (redirect
  `http://localhost:4000/*`) in `Keycloak/priam-realm-realm.json`, distinct
  from `Data-client` — a different application role deserves a distinct client
  rather than sharing `Data-client`, even though both point to the same
  realm/IdP.
- **No `idReference` claim**: this frontend does not need to resolve a
  `dataSubjectId` for the logged-in user (it is not a data subject), so no
  custom protocol mapper on this client.
- A dedicated test user, `app.owner` / `OwnerPass123!`, distinct from
  `jane.doe` (which stays the data-subject account on the `PRIAM-Frontend`
  side).

Reminder: port `4000` must be listed in `CUSTOM_FRONTEND_ORIGINS` (§4bis, §5)
— missed initially, causing a CORS block identical to §4bis's but on this
second frontend specifically.

## 6. Fail-open behavior when unconfigured

If `CUSTOM_OIDC_ISSUER_URI` is empty, `.oauth2ResourceServer(...)` **is never
called** on the Gateway side — it is this exact call that triggers looking up/
building the `ReactiveJwtDecoder`. By staying in the `if` branch, the Gateway
never attempts to reach an auth server, so nothing about it can fail, and it
starts normally (with a `WARN` in the logs).

This is a deliberate choice, consistent with the pattern already used by the
CEP on the target-application side (`app/priam/consent.py get_consent()`: `if
not PRIAM_CDP_URL: return True`) — check the configuration **before**
engaging logic that depends on it, rather than trying and catching the failure
afterward.

**Practical note**: this choice is fixed at container startup. Changing
`CUSTOM_OIDC_ISSUER_URI` requires restarting the Gateway.

## 7. The three families of target applications

The choice of *which* IdP to put behind `issuer-uri`/`oidcIssuer` depends on
the target application — and **never** touches PRIAM's code (Gateway or
Frontend), only config/infrastructure.

### Family 1 — the application has no auth system of its own

Example: TeaStore (cf. the ICSOC paper — *"TeaStore does not provide this kind
of authorization server"*). An IdP (Keycloak or otherwise) **becomes**
directly the application's identity provider. No delegation, accounts created
directly in the IdP. Work involved: deploy the IdP + a realm/client + the user
accounts, zero code. **This is the pattern demonstrated in §5** (the Keycloak
accounts created — `jane.doe` — are not linked to FastAPI's own login system,
they are standalone IdP accounts, exactly the Family 1 pattern).

### Family 2 — the application already has its own accounts, to be reused (SSO)

Example: FastAPI-Healthcare-PRIAM has its own system (a home-grown JWT, `POST
/api/auth/login`, a `users` table). Wiring PRIAM to it via Family 1 (as in §5)
works for a demo, but creates a **separate account** — the user would have to
remember two passwords. For real SSO on existing accounts, a **Keycloak SPI**
is required (*Service Provider Interface* — a Java plugin Keycloak loads
through its standard extension mechanism, without modifying Keycloak itself)
of type *User Storage Provider* or *Authenticator custom*, which delegates
credential verification to the application's existing login endpoint instead
of Keycloak's own user database.

This SPI is specific to each application's login contract (URL, request shape,
response shape) — but it can be written **once, generically and
configurably** (login URL, request template, JSON path of the "user id" field,
all in config, not in code), so that each new application only needs
configuration afterward, not a rewrite. Work involved: deploy the IdP + write
the generic SPI once + configure it per application.

`issuer-uri`/`oidcIssuer` point, in both families, to the same kind of
endpoint (the IdP) — neither the Gateway nor the Frontend ever sees the
difference.

### Family 3 (untested track) — the application already has a genuine native OIDC IdP

No case study in this repository (TeaStore, SportTracker,
FastAPI-Healthcare-PRIAM, Habitica, Ghostfolio) has a genuine natively
integrated IdP — all of them either have no auth at all (Family 1) or a
proprietary home-grown system (JWT/session, which would call for Family 2 and
its SPI, never written to date, see §7 above). A third case, simpler than
Family 2, does exist however: an application that **already** uses an
OIDC-compliant IdP (Keycloak or otherwise) as its own native authentication
mechanism. In that case, no SPI is needed: PRIAM can point
`issuer-uri`/`oidcIssuer` directly at that same already-in-place IdP/realm —
real SSO, zero delegation code, just an OIDC client configuration
(Data-client/Provider-client) on that existing IdP rather than on a Keycloak
dedicated to PRIAM.

**Track to explore, not yet tested in this repository**: applications
generated by [JHipster](https://www.jhipster.tech/) (a Spring Boot +
Angular/React generator, widely cited in the microservices literature) offer
an "OAuth2/OIDC" authentication option that scaffolds a working Keycloak
integration by default (realm, client, mappers already in place). A case study
built with this option would be a natural candidate to validate Family 3 under
real conditions. Important nuance: JHipster is a **generator**, not a business
application with a fixed domain (unlike TeaStore/e-commerce,
FastAPI-Healthcare/healthcare, Ghostfolio/personal finance) — the generated
application's business domain depends entirely on what is built on top of it.
Choosing JHipster addresses a different axis (available authentication
architecture), orthogonal to the choice of business domain; if both criteria
matter for the next case study, one would need to find a real JHipster
application with its own business domain (healthcare/e-commerce/finance, or a
domain not yet covered), not just the generator's minimal example app (which
has no identifiable business domain, just generic CRUD entities).

## 8. Step-by-step guide — wiring up authentication for a new case study

Assumes the SQL annotation, the Provider bridge, and the CEP are already in
place (§1-4 of the playbook). Steps specific to authentication:

1. **Decide the family** (§7): does the target application already have its
   own account system that should be reused as SSO (Family 2), or already a
   genuine native OIDC IdP that can be reused as-is (Family 3), or neither
   (Family 1)?
2. **Deploy an OIDC IdP** (Keycloak or otherwise) — except in Family 3, where
   the IdP already in place on the target-application side is reused
   directly, nothing to deploy. For Families 1 and 2, use
   `Keycloak/priam-realm-realm.json` (§5) as a template: a dedicated realm, a
   public client with Authorization Code + PKCE enabled, a redirect URI
   pointing to the frontend's origin.
3. **Create/map the `idReference` attribute**: every IdP account must expose,
   as a standard ID token claim, the target application's real
   `idRef`/`dataSubjectId` for that user (the `oidc-usermodel-attribute-mapper`
   protocol mapper under Keycloak — look for the equivalent "custom claim from
   user attribute" for another IdP).
   - Family 1: assign the attribute manually to every account created in the
     IdP.
   - Family 2: the delegation SPI must know/resolve this id during
     authentication (usually already available in the target application's
     login response) and inject it as a user attribute before the token is
     issued.
   - Family 3 (untested track): map the attribute onto the target
     application's native IdP's already-existing accounts — via its admin API
     if the `idRef` is already stored there as a user attribute, otherwise add
     it at sign-up on the target-application side (the same pattern as the
     playbook's §4bis Keycloak provisioning, applied to the already-in-place
     IdP rather than to a Keycloak dedicated to PRIAM).
4. **Configure `CUSTOM_OIDC_ISSUER_URI`** (root `.env`) with the address the
   *browser* will use to reach the IdP — not necessarily Docker's internal
   address. If the Gateway and the browser do not share the same network view
   of the IdP, add `CUSTOM_OIDC_JWK_SET_URI` (§4).
5. **Configure the Frontend**: the `OIDC_ISSUER`/`OIDC_CLIENT_ID` build args
   of the `frontuser` service in `docker-compose.yml` (same values as in point
   4, from the browser's side).
6. **Test** (method from playbook §9 — proof by real state, not just the
   absence of an HTTP error):
   - No token → `401` on `/right/**`, `/cdp/**` through the Gateway.
   - A valid token (obtained through a real browser flow, or via a test using
     *Direct Grant* if enabled on the IdP client — see the curl commands in
     the appendix of `case-studies/.../ETAPES-FAITES.md`) → `200`, and the
     `idReference` claim present in the ID token with the right value.
   - Token absent/invalid/expired → always `401`.
   - Machine-to-machine routes (`/data/**`, `/actor/**`, `/provider/**`) stay
     reachable without a token, in every case — a non-regression to
     re-verify.

## 9. Relationship to Fig. 1 of the ICSOC paper

The flow (redirect to the identity server → code → token → PRIAM session)
remains valid as-is — this work repairs/completes the implementation so it
actually works, without changing the shape of the flow. The only nuance: the
figure's *"Identity and authorization server"* box can, under a Family 2
regime, be made up of a broker (IdP + SPI) delegating behind the scenes to the
application — neither the Gateway nor the Frontend ever sees this internal
composition, they only ever talk to a single OIDC endpoint, exactly as the
figure shows.

## 10. What is validated, what is not yet

**Validated end-to-end, including from a real browser** (not just reviewed/
written, nor just curl):
- Gateway left unconfigured: starts, logs the expected `WARN`, M2M paths
  unchanged.
- Gateway configured against a real Keycloak: `401` with no token, `401` with
  an invalid token, `200` with a real signed token — including after
  discovering and fixing the split issuer/jwk-set-uri pitfall (§4).
- The custom `idReference` claim correctly appears in a real ID token.
- `PRIAM-Frontend` and `PRIAM-Frontend-Provider`: compile and serve without
  error (Docker), integrated into `docker-compose.yml` (`frontuser` and
  `frontprovider` services), started successfully against the full stack
  (Gateway + Keycloak + microservices).
- CORS (§4bis): the `OPTIONS` preflight returns `200` with the right headers
  for both frontends (`:4200` and `:4000`).
- Automatic token refresh (§3bis): a long session (> 5 min) no longer loses
  authentication.
- **Real test from the browser**: a rectification request submitted through
  `localhost:4200` (`PRIAM-Frontend`) was found in the PRIAM database,
  approved, and the change propagated all the way to the target application's
  real database — the first end-to-end proof from a browser, not just curl.
- `PRIAM-Frontend-Provider` (§5bis): authentication, CORS, and JWT verified
  (the `app.owner` token accepted, no token → `401`), dashboard functional.

**Not yet confirmed by the user**:
- A fresh browser pass on `PRIAM-Frontend-Provider` after the token-refresh
  fix (§3bis) and the dashboard's stale-reference bug fix (see
  `case-studies/.../ETAPES-FAITES.md`, Step 9 point 6) — rebuilt and
  re-verified `healthy` on the server side, but not yet reconfirmed in a real
  long session by the user.

**Not started**:
- The generic delegation SPI (Family 2) — would still need to be written and
  packaged for Keycloak (or whichever IdP is chosen) to get real SSO on
  existing FastAPI-Healthcare-PRIAM accounts rather than standalone IdP
  accounts.