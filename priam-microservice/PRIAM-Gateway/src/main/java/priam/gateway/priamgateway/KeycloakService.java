package priam.gateway.priamgateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakService {

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final WebClient webClient;

    public KeycloakService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(keycloakBaseUrl).build();
    }

    private Mono<String> getClientAccessToken() {
        return webClient.post()
                .uri("/realms/" + realm + "/protocol/openid-connect/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue("client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&grant_type=client_credentials")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"));
    }

    public Mono<Boolean> checkIsLoggedIn(String username) {
        return getClientAccessToken()
                .flatMap(accessToken ->
                        webClient.get()
                                .uri("/admin/realms/{realm}/users?username={username}&exact=true", realm, username)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .retrieve()
                                .bodyToMono(List.class)
                                .map(users -> {
                                    if (users == null || users.isEmpty()) return false;

                                    Map<String, Object> user = (Map<String, Object>) users.get(0);
                                    Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
                                    if (attributes == null) return false;

                                    Object isLoggedInAttr = attributes.get("isLoggedIn");
                                    if (isLoggedInAttr instanceof List) {
                                        List<String> list = (List<String>) isLoggedInAttr;
                                        return list.contains("true");
                                    }
                                    return false;
                                })
                )
                .onErrorResume(e -> Mono.just(false));
    }
}


/*@Component
@Order(1)
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
    return getAdminAccessToken()
            .flatMap(adminAccessToken ->
                webClient.get()
                        .uri("/admin/realms/{realm}/users?username={username}", realm, username)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
                        .retrieve()
                        .bodyToMono(java.util.List.class) // directement la liste des users
                        .map(users -> {
                            if (users.isEmpty()) return false;
                            Map<String, Object> user = (Map<String, Object>) users.get(0);
                            Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
                            return attributes != null && attributes.containsKey("isLoggedIn")
                                   && ((java.util.List<String>) attributes.get("isLoggedIn")).contains("true");
                        })
            );
}
}*/

