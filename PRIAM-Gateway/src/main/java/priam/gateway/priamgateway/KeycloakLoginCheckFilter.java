package priam.gateway.priamgateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
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
}
