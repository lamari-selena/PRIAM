package priam.gateway.priamgateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Provider-agnostic authentication: any OIDC-compliant issuer (Keycloak, the
 * target application's own IdP if it has one, or any other) works identically
 * here, since Spring's resource server only needs a signed-JWT issuer to
 * validate against. See Docs/PRIAM-AUTH-OIDC.md for the full approach.
 *
 * Machine-to-machine routes (Consent -> Gateway -> Data/Actor, Right ->
 * Gateway -> Provider) carry no JWT — they are PRIAM-internal calls with no
 * human session behind them — and stay open regardless of auth config.
 *
 * Human-facing routes (/right/**, /cdp/**) require a valid JWT once
 * spring.security.oauth2.resourceserver.jwt.issuer-uri (CUSTOM_OIDC_ISSUER_URI)
 * is set. If it isn't set, this deliberately fails OPEN (permitAll, with a
 * warning) rather than failing to start — same "absent config preserves
 * pre-PRIAM behaviour" philosophy already used by the CEP
 * (FastAPI-Healthcare-PRIAM/app/priam/consent.py get_consent()), so the
 * Gateway keeps working for local/M2M-only testing without requiring an IdP.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String[] MACHINE_TO_MACHINE_ROUTES = {
            "/data/**", "/actor/**", "/provider/**", "/eureka/**", "/health",
            // /right/** is otherwise human-facing (accessRequest, answer, requestList, ...)
            // and requires a JWT below, but /right/api/isAccepted is a genuine M2M
            // sub-path: PRIAM-Data-service's RightRestClient calls it internally (via this
            // same Gateway) from DataService.getProcessedPersonalDataList to check whether
            // an INDIRECT/PRODUCED data item was ever granted through a real access
            // request, with no human session behind that call. Left authenticated like the
            // rest of /right/**, every such Feign call 401s (no token to attach), which
            // throws inside DataService and 500s any endpoint that reaches this branch —
            // never noticed before because no case study had annotated INDIRECT/PRODUCED
            // data with a processed_data row until now.
            "/right/api/isAccepted"
    };

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    // Split from issuer-uri to survive Docker Compose's split network view: a
    // browser-issued token's `iss` claim matches the externally-published
    // address (e.g. http://localhost:8080/realms/x), but the Gateway
    // container needs a Docker-network-reachable address (e.g.
    // http://keycloak:8080/...) to actually fetch signing keys. When both
    // sides of a deployment see the issuer the same way (no container split),
    // leave this blank — the issuer-uri alone is enough (standard OIDC
    // discovery). CUSTOM_OIDC_JWK_SET_URI in .env.
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    // @CrossOrigin on GatewayApplication has no effect on proxied routes —
    // those go through the RouteLocator's filter chain, never through
    // annotation-based dispatch, so it never applied here. Without explicit
    // CORS handling in the security chain, the browser's preflight OPTIONS
    // request itself gets rejected (401, no Access-Control-Allow-Origin
    // header) before the real GET/POST is even attempted, breaking every
    // call from PRIAM-Frontend to /right/** and /cdp/** — not an auth
    // problem, a CORS one, but it looks identical from the browser (blocked
    // request, no readable response).
    @Value("${custom.frontend-origins:http://localhost:4200}")
    private List<String> frontendOrigins;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (issuerUri == null || issuerUri.isBlank()) {
            logger.warn("spring.security.oauth2.resourceserver.jwt.issuer-uri (CUSTOM_OIDC_ISSUER_URI) " +
                    "is not set: human-facing routes (/right/**, /cdp/**) are NOT authenticated. " +
                    "Set it to any OIDC-compliant provider before exposing this Gateway beyond " +
                    "local machine-to-machine testing.");
            http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        } else {
            http.authorizeExchange(exchanges -> exchanges
                            .pathMatchers(MACHINE_TO_MACHINE_ROUTES).permitAll()
                            .anyExchange().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(buildJwtDecoder())));
        }

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(frontendOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private ReactiveJwtDecoder buildJwtDecoder() {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            // Common case: issuer-uri is reachable as-is (no container/host
            // split) — standard OIDC discovery handles everything.
            return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
