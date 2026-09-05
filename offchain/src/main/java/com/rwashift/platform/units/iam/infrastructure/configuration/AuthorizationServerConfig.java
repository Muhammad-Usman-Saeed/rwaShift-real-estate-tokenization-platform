package com.rwashift.platform.units.iam.infrastructure.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Authorization Server (OAuth 2.0 + OIDC) that issues access tokens for both the Next.js
 * BFF/SPA client and machine-to-machine callers. This is the ONLY place a raw {@code Jwt}
 * (private key, signing) is handled — every other unit only ever sees a validated,
 * claims-carrying token via {@link com.rwashift.platform.shared.security.TenantContext}.
 *
 * <p>The RSA signing key is generated once and persisted to disk (see {@link #loadOrGenerateRsaKey})
 * rather than regenerated every startup — a fresh key on every restart would silently invalidate
 * every access/ID token issued before that restart (they'd fail signature verification even
 * though not actually time-expired), defeating token refresh and forcing a full re-login on every
 * deploy. A production deployment would source this from a KMS/secret store instead — see README
 * security notes.
 */
@Configuration
class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (configurer) -> configurer.oidc(withDefaults -> {
                }))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"),
                        new org.springframework.security.web.util.matcher.MediaTypeRequestMatcher(
                                org.springframework.http.MediaType.TEXT_HTML)))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${rwashift.authorization-server.issuer}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(@Value("${rwashift.jwt.signing-key-path}") String signingKeyPath) {
        RSAKey rsaKey = loadOrGenerateRsaKey(Path.of(signingKeyPath));
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration
                .OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * PKCS8/X.509 DER bytes, base64-encoded, one key per line ({@code privateKey\npublicKey\nkeyId}) —
     * intentionally not PEM (no need for the extra parsing complexity here, this file is never
     * consumed by anything other than this method).
     */
    private static RSAKey loadOrGenerateRsaKey(Path path) {
        if (Files.exists(path)) {
            try {
                var lines = Files.readAllLines(path);
                var keyFactory = KeyFactory.getInstance("RSA");
                var privateKey = (RSAPrivateKey) keyFactory
                        .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(lines.get(0))));
                var publicKey = (RSAPublicKey) keyFactory
                        .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(lines.get(1))));
                log.info("Loaded persisted JWT signing key from {}", path);
                return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(lines.get(2)).build();
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new IllegalStateException("Failed to load JWT signing key from " + path, e);
            }
        }

        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to generate RSA signing key", ex);
        }
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String keyId = UUID.randomUUID().toString();

        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.write(path, List.of(
                    Base64.getEncoder().encodeToString(privateKey.getEncoded()),
                    Base64.getEncoder().encodeToString(publicKey.getEncoded()),
                    keyId));
            log.info("Generated new JWT signing key and persisted it to {}", path);
        } catch (IOException e) {
            log.warn("Generated a JWT signing key but could not persist it to {} — it will not survive a restart", path, e);
        }

        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).build();
    }
}
