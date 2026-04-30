package priam.gateway.priamgateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class KeycloakLoginCheckFilter implements GatewayFilter, Ordered {

    private final KeycloakService keycloakService;

    public KeycloakLoginCheckFilter(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();


        if (path.startsWith("/health") || 
            path.startsWith("/info") || 
            path.startsWith("/actuator") ||
            path.startsWith("/eureka")) {
            return chain.filter(exchange);
        }

        // Vérifier si on a bien un username dans le header
        String username = exchange.getRequest().getHeaders().getFirst("X-Username");

        if (username == null || username.isBlank()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Vérifier avec Keycloak si l'utilisateur est connecté
        return keycloakService.checkIsLoggedIn(username)
                .flatMap(isLoggedIn -> {
                    if (Boolean.TRUE.equals(isLoggedIn)) {
                        return chain.filter(exchange); // continuer la requête
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return -1; // priorité haute
    }
}

/*@Component
@Order(2)
public class KeycloakLoginCheckFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakLoginCheckFilter.class);

    private final KeycloakService keycloakService;

    public KeycloakLoginCheckFilter(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Entering KeycloakLoginCheckFilter");
        ServerHttpRequest request = exchange.getRequest();
        String username = request.getHeaders().getFirst("X-Username"); // Assuming username is passed in a header

        logger.info("Checking login status for user: {}", username);

        if (username == null || username.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return keycloakService.checkIsLoggedIn(username)
                .flatMap(isLoggedIn -> {
                    if (isLoggedIn) {
                        return chain.filter(exchange);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }
}*/

