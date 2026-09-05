package com.rwashift.platform.units.iam.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rwashift.platform.units.iam.infrastructure.security.WalletAuthenticationProvider;
import com.rwashift.platform.units.iam.infrastructure.security.WalletLoginFilter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Everything that is NOT an Authorization Server endpoint: the platform's own REST API
 * (JWT bearer, {@code /api/**}), the Authorization Server's login page (form login, since the
 * AS needs somewhere to authenticate a resource-owner), and public docs/health endpoints.
 * Runs at a lower {@link Order} than {@link AuthorizationServerConfig}'s chain so the AS
 * endpoints are matched first.
 */
@Configuration
@EnableMethodSecurity
class ResourceServerConfig {

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, @Value("${rwashift.frontend-url}") String frontendUrl,
            AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, WalletAuthenticationProvider walletAuthenticationProvider,
            ObjectMapper objectMapper) throws Exception {
        // Registered directly into HttpSecurity's own local AuthenticationManagerBuilder (the one
        // formLogin() below actually consults) — NOT via `.authenticationManager(...)` on
        // HttpSecurity itself. That global override was tried first and broke bearer-token API
        // calls platform-wide: it replaces the manager for the ENTIRE filter chain, including
        // BearerTokenAuthenticationFilter, which needs oauth2ResourceServer()'s own
        // JwtAuthenticationProvider-backed manager, not this password/wallet one — every JWT
        // request 401'd with "No AuthenticationProvider found for BearerTokenAuthenticationToken"
        // once that override was in place. Registering providers into the local builder instead
        // (never calling .build() on it ourselves — formLogin()'s own configurer does that later)
        // leaves oauth2ResourceServer()'s independent wiring completely untouched.
        DaoAuthenticationProvider passwordProvider = new DaoAuthenticationProvider();
        passwordProvider.setUserDetailsService(userDetailsService);
        passwordProvider.setPasswordEncoder(passwordEncoder);
        AuthenticationManagerBuilder localBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        localBuilder.authenticationProvider(passwordProvider);
        localBuilder.authenticationProvider(walletAuthenticationProvider);

        WalletLoginFilter walletLoginFilter = new WalletLoginFilter(authenticationManager, objectMapper);
        // AbstractAuthenticationProcessingFilter's own default SecurityContextRepository only
        // stores the context as a request attribute (visible for the rest of THIS request, gone
        // after) — it never persists to the session, so no Set-Cookie ever goes out and the
        // subsequent /oauth2/authorize redirect never sees an authenticated principal. Every
        // filter Spring Security's own DSL builds (formLogin's UsernamePasswordAuthenticationFilter
        // included) persists to the session instead — matching that here, explicitly, is what
        // makes wallet sign-in actually stick. (Not pulled from http.getSharedObject(...): that
        // shared object isn't populated yet at this point in HttpSecurity's build lifecycle.)
        walletLoginFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login", "/login/wallet", "/error",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/info",
                                "/api/v1/public/**")
                        .permitAll()
                        .anyRequest().authenticated())
                // defaultSuccessUrl only applies when there's no saved OAuth2 authorization
                // request to return to (alwaysUse=false) — i.e. someone reached /login directly
                // instead of via the frontend's "Sign in" -> /oauth2/authorize redirect. Without
                // this, Spring Security's own default ("/") 404s, since this API-only backend has
                // no page mapped there.
                .formLogin(form -> form.defaultSuccessUrl(frontendUrl, false))
                // Same base class as the password filter formLogin() registers, listening on
                // POST /login/wallet instead of /login — see WalletLoginFilter's Javadoc for why
                // placing it ahead of that filter is what makes a successful wallet sign-in
                // produce an identical authenticated session (and therefore identical JWT) to a
                // password login, with zero changes to token issuance itself.
                .addFilterBefore(walletLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>(scopesConverter.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                authorities.addAll(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));
            }
            return authorities;
        });
        return converter;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Built as a plain {@link ProviderManager} over an explicit provider list, rather than via
     * {@code http.getSharedObject(AuthenticationManagerBuilder.class)} — that shared-object
     * approach silently dropped the password provider once a second {@code AuthenticationProvider}
     * bean ({@link WalletAuthenticationProvider}) existed in the context (Spring Boot's
     * {@code InitializeUserDetailsManagerConfigurer} backs off the {@code UserDetailsService}
     * wiring whenever it sees a custom provider bean), breaking username/password login entirely.
     * Constructing the {@link DaoAuthenticationProvider} directly here is unambiguous: both
     * providers are always present, and {@link ProviderManager} dispatches to whichever one's
     * {@code supports()} matches the {@code Authentication} type it's given.
     */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
            WalletAuthenticationProvider walletAuthenticationProvider) {
        DaoAuthenticationProvider passwordProvider = new DaoAuthenticationProvider();
        passwordProvider.setUserDetailsService(userDetailsService);
        passwordProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(passwordProvider, walletAuthenticationProvider);
    }
}
