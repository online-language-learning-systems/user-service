package com.hub.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String PREFIX = "ROLE_";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(
                    author ->
                        author
                                .requestMatchers("/storefront/**").hasAnyRole("lecturer", "student", "admin")
                                .requestMatchers("/backoffice/**").hasRole("admin")
                                .anyRequest().authenticated())
                .oauth2ResourceServer(
                        // Parse token from Authorization: Bearer <token>
                        // Validate JWT using issuer-uri or jwk-set-uri
                        // If valid, create JwtAuthenticationToken -> accessible via @AuthenticationPrincipal Jwt
                    oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                )
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak() {

        Converter<Jwt, Collection<GrantedAuthority>> jwtCollectionConverter =
            jwt -> {
                Map<String, Collection<String>> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
                Collection<String> roles = realmAccess.get(ROLES_CLAIM);
                return roles.stream().map(role -> new SimpleGrantedAuthority(PREFIX + role))
                        .collect(Collectors.toList());
            };

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtCollectionConverter);
        return jwtAuthenticationConverter;

        /*
            - JWT included Header, Payload, Signature
            Claim is a key-value pair in payload
         */
    }

}
