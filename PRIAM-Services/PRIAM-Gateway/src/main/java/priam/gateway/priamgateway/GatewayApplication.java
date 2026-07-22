package priam.gateway.priamgateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

// ReactiveOAuth2ResourceServerAutoConfiguration excluded: Spring Boot activates
// it as soon as spring.security.oauth2.resourceserver.jwt.issuer-uri is a
// *present* property key, even resolved to "" (CUSTOM_OIDC_ISSUER_URI unset) -
// independent of SecurityConfig.java's own fail-open branch, which builds its
// own SecurityWebFilterChain manually and never uses this autoconfigured
// bean. Left in, the Gateway crashes on boot with "jwkSetUri cannot be empty"
// whenever no OIDC issuer is configured, instead of the documented fail-open
// behaviour (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §6).
@SpringBootApplication(exclude = {R2dbcAutoConfiguration.class, ReactiveOAuth2ResourceServerAutoConfiguration.class})
@Configuration
@CrossOrigin
@RestController
@PropertySource("classpath:custom.properties")
public class GatewayApplication {

    //Logger logger = LoggerFactory.getLogger(GatewayApplication.class);
    @Value("${custom.keycloak.url:http://localhost:8080}")
    private String keycloakURL;

    @Value("${custom.data.url:http://localhost:8081}")
    private String dataServiceURL;

    @Value("${custom.actor.url:http://localhost:8082}")
    private String actorServiceURL;

    @Value("${custom.right.url:http://localhost:8083}")
    private String rightServiceURL;

    @Value("${custom.provider.url:http://localhost:8086}")
    private String providerServiceURL;

    @Value("${custom.eureka.url:http://localhost:8761}")
    private String eurekaURL;

    @Value("${custom.cdp.url:http://localhost:8089}")
    private String cdpURL;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    // scheme://authority only - see the /provider/** route below for why the
    // path component (if any) of providerServiceURL must not be passed to
    // .uri() directly.
    private String providerBaseUri() {
        java.net.URI full = java.net.URI.create(providerServiceURL);
        return full.getScheme() + "://" + full.getAuthority();
    }

    // Route definitions only — authorization (which paths require a valid JWT
    // vs. which are open machine-to-machine PRIAM-internal calls) lives
    // centrally in SecurityConfig.java, not per-route here. See
    // Docs/PRIAM-AUTH-OIDC.md.
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p
                .path("/health")
                // Le moyen le plus simple pour vérifier si il marche
                // TODO: Rendre local le healthcheck
                .uri("https://http.cat/status/418")
                )
                .route(p -> p
                .path("/data/**")
                .filters(f -> f.rewritePath("/data/(?<segment>.*)", "/${segment}"))
                .uri(dataServiceURL)
                )
                .route(p -> p
                .path("/right/**")
                .filters(f -> f.rewritePath("/right/(?<segment>.*)", "/${segment}"))
                .uri(rightServiceURL)
                )
                .route(p -> p
                .path("/cdp/**")
                .filters(f -> f.rewritePath("/cdp/(?<segment>.*)", "/${segment}"))
                .uri(cdpURL)
                )
                .route(p -> p
                .path("/actor/**")
                .filters(f -> f.rewritePath("/actor/(?<segment>.*)", "/${segment}"))
                .uri(actorServiceURL)
                )
                .route(p -> p
                .path("/provider/**")
                // Spring Cloud Gateway's RouteToRequestUrlFilter only ever
                // takes scheme/host/port from a route's .uri() - any path
                // component is silently dropped when merging with the
                // already-rewritten exchange path, not proxied anywhere
                // (confirmed via a real end-to-end test, not just reading
                // Spring's source: same rewritePath filter, same
                // CUSTOM_PROVIDER_URL correctly resolved in the container's
                // env, yet a target app whose Provider bridge lives under a
                // non-root context path - e.g. any Java/WAR-deployed target
                // app, not just TeaStore - got a 404 on every /provider/**
                // call until this was added). Every other *_URL below has
                // always been a bare host:port so far, never exercising this
                // path; providerServiceURL is the only one that legitimately
                // varies by target app per playbook §2, so re-attach any
                // path component explicitly via prefixPath() instead of
                // relying on .uri() to carry it.
                .filters(f -> {
                    String path = java.net.URI.create(providerServiceURL).getPath();
                    return (path == null || path.isEmpty())
                            ? f.rewritePath("/provider/(?<segment>.*)", "/${segment}")
                            : f.rewritePath("/provider/(?<segment>.*)", "/${segment}").prefixPath(path);
                })
                .uri(providerBaseUri())
                )
                .route(p -> p
                .path("/eureka/**")
                .filters(f -> f.rewritePath("/eureka/(?<segment>.*)", "/${segment}"))
                .uri(eurekaURL)
                )
//                .route(p -> p
//                .path("/**")
//                .uri(keycloakURL)
//                )

                .build();
    }

}
