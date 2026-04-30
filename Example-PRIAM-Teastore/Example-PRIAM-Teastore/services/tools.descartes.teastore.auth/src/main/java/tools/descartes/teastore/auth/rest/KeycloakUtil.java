package tools.descartes.teastore.auth.rest;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KeycloakUtil {

    public static Keycloak getInstance() {
        Properties props = new Properties();
        try (InputStream input = KeycloakUtil.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            props.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load Keycloak properties", ex);
        }

        return KeycloakBuilder.builder()
            .serverUrl(props.getProperty("keycloak.admin.server-url"))
            .realm(props.getProperty("keycloak.admin.realm"))
            .username(props.getProperty("keycloak.admin.username"))
            .password(props.getProperty("keycloak.admin.password"))
            .clientId(props.getProperty("keycloak.admin.client-id"))
            .grantType(OAuth2Constants.PASSWORD)
            .build();
    }
}
