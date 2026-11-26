
package com.pedidos360.ms.cupones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String ISSUER =
            "https://sts.windows.net/0844a9ad-f458-47d0-8036-0f2080309ddc/";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth

                        // Los clientes autenticados pueden validar cupones.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/cupones/validar/*"
                        ).authenticated()

                        // Los clientes autenticados pueden calcular
                        // descuentos sin consumir usos del cupon.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/cupones/calcular"
                        ).authenticated()

                        // Aplicar un cupon consume un uso.
                        // Se mantiene restringido a ADMIN hasta
                        // integrarlo de forma segura con MS-Orders.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/cupones/aplicar"
                        ).hasRole("ADMIN")

                        // Crear, listar, consultar y desactivar cupones
                        // requiere permisos administrativos.
                        .requestMatchers(
                                "/api/cupones",
                                "/api/cupones/**"
                        ).hasRole("ADMIN")

                        // El resto requiere autenticacion.
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        NimbusJwtDecoder decoder =
                (NimbusJwtDecoder)
                        JwtDecoders.fromIssuerLocation(ISSUER);

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(ISSUER);

        OAuth2TokenValidator<Jwt> audienceValidator =
                new AudienceValidator();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            List<GrantedAuthority> authorities =
                    new ArrayList<>();

            List<String> roles =
                    jwt.getClaimAsStringList("roles");

            if (roles != null) {
                for (String role : roles) {
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    );
                }
            }

            return authorities;
        });

        return converter;
    }
}
