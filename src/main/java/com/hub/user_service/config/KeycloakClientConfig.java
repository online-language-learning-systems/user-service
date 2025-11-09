package com.hub.user_service.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakClientConfig {
    private final KeycloakPropsConfig keycloakPropsConfig;

    public KeycloakClientConfig(KeycloakPropsConfig keycloakPropsConfig) {
        this.keycloakPropsConfig = keycloakPropsConfig;
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakPropsConfig.getAuthServerUrl())
                .realm(keycloakPropsConfig.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(keycloakPropsConfig.getResource())
                .clientSecret(keycloakPropsConfig.getCredentials().getSecret())
                //.resteasyClient(new ResteasyClientBuilder().connectionPoolSize(20).build())
                .build();
    }
}
