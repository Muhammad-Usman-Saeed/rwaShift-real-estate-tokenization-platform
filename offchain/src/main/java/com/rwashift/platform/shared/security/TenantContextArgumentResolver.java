package com.rwashift.platform.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lets any {@code @RestController} in any unit take a {@link TenantContext} as a plain method
 * parameter, resolved from the access token's claims (see IAM's token customizer for the claim
 * contract: {@code org_id}, {@code roles}, {@code investor_id}). This is the boundary between
 * Spring Security's {@link Jwt} and the rest of the platform — no other unit touches
 * {@code Jwt}/{@code Authentication} directly.
 */
@Component
public class TenantContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return TenantContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) webRequest.getUserPrincipal();
        if (auth == null) {
            throw new IllegalStateException("TenantContext requested on an unauthenticated request");
        }
        Jwt jwt = auth.getToken();
        // `getAuthorities()` mixes real business roles (`ROLE_x`, from the token's `roles` claim)
        // with OAuth2 scope authorities (`SCOPE_openid`, `SCOPE_api.read`, ...) that Spring's
        // resource-server support adds from the token's `scope` claim — only the former belongs in
        // `roles()`. Filtering to `ROLE_`-prefixed authorities (rather than stripping the prefix
        // when present and keeping everything else) keeps scope strings from silently ending up in
        // `roles()`, where e.g. `roles().stream().findFirst()` callers assume every entry is a real
        // role (see ActivityApplicationService#recordVisit, which this exact pollution corrupted).
        Set<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toUnmodifiableSet());
        String organizationId = jwt.getClaimAsString("org_id");
        String investorId = jwt.getClaimAsString("investor_id");
        HttpServletRequest httpRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        if (httpRequest != null) {
            httpRequest.setAttribute("tenantOrganizationId", organizationId);
        }
        return new TenantContext(jwt.getSubject(), organizationId, roles, investorId);
    }
}
