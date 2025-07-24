package priam.gateway.priamgateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Component
public class KeycloakService {

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.username}")
    private String username;

    @Value("${keycloak.password}")
    private String password;

    private final WebClient webClient;

    public KeycloakService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(keycloakBaseUrl).build();
    }

    public Mono<String> getAdminAccessToken() {
        return webClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue("client_id=" + clientId +
                        "&username=" + username +
                        "&password=" + password +
                        "&grant_type=password")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"));
    }

    public Mono<Boolean> checkIsLoggedIn(String username) {
        return getAdminAccessToken().flatMap(adminAccessToken ->
                webClient.get()
                        .uri("/admin/realms/{realm}/users?username={username}", realm, username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(response -> {
                            // Assuming the response is a list of users and we take the first one
                            Map<String, Object> user = ((java.util.List<Map<String, Object>>) response.get("users")).get(0);
                            Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
                            if (attributes != null && attributes.containsKey("isLoggedIn")) {
                                return ((java.util.List<String>) attributes.get("isLoggedIn")).contains("true");
                            }
                            return false;
                        })
        );
    }
}
