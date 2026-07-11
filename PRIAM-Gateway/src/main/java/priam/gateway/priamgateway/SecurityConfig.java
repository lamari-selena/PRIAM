package priam.gateway.priamgateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

// Without this, Spring Security's default reactive auto-configuration denies
// every request with 401 before it reaches the route filters below, since no
// SecurityWebFilterChain bean was defined. Authorization is already handled
// per-route by KeycloakLoginCheckFilter (human-facing routes) and by the
// absence of that filter (machine-to-machine routes), so this chain just
// steps out of the way instead of applying its own deny-all default.
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
}
