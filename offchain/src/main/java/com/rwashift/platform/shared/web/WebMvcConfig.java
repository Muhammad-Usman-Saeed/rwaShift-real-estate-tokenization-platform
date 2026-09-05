package com.rwashift.platform.shared.web;

import com.rwashift.platform.shared.security.TenantContextArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantContextArgumentResolver tenantContextArgumentResolver;

    public WebMvcConfig(TenantContextArgumentResolver tenantContextArgumentResolver) {
        this.tenantContextArgumentResolver = tenantContextArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(tenantContextArgumentResolver);
    }
}
