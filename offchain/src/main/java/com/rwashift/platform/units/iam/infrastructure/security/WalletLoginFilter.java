package com.rwashift.platform.units.iam.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * The wallet-login counterpart to Spring Security's own {@code UsernamePasswordAuthenticationFilter}
 * — same base class, same job: turn one HTTP request into an {@code Authentication} attempt via
 * the shared {@code AuthenticationManager}. On success, the base class does exactly what password
 * login does (marks the {@code SecurityContext} authenticated, persists it to the session) so the
 * in-flight {@code /oauth2/authorize} request this was reached from can resume normally — see
 * {@code ResourceServerConfig} for how this is wired into the filter chain, and why that's what
 * makes the resulting JWT identical to a password-issued one.
 *
 * <p>Responses are JSON (204/401), not the redirect a browser form-post would get — the caller is
 * always the frontend's {@code fetch}, submitting after the wallet has already signed a message
 * (see {@code WalletAuthController} for the nonce/message this is verifying against).
 */
public class WalletLoginFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper;

    public WalletLoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        super(new AntPathRequestMatcher("/login/wallet", "POST"));
        this.objectMapper = objectMapper;
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler((request, response, authResult) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT));
        setAuthenticationFailureHandler((request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"%s\"}".formatted(exception.getMessage()));
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        WalletLoginRequestBody body;
        try {
            body = objectMapper.readValue(request.getInputStream(), WalletLoginRequestBody.class);
        } catch (IOException e) {
            throw new BadCredentialsException("Malformed wallet sign-in request", e);
        }
        if (body.walletAddress() == null || body.signature() == null || body.message() == null) {
            throw new BadCredentialsException("Missing walletAddress, signature, or message");
        }
        WalletAuthenticationToken authRequest =
                WalletAuthenticationToken.unauthenticated(body.walletAddress(), body.signature(), body.message());
        return getAuthenticationManager().authenticate(authRequest);
    }
}
