package com.rwashift.platform.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Single Spring Boot deployable for V1 (modular monolith). Each business capability lives under
 * {@code com.rwashift.platform.units.*} as a self-contained composite unit with its own
 * hexagonal layering; this class only bootstraps the process — it owns no business logic.
 *
 * <p>{@code @EnableJpaRepositories}/{@code @EntityScan} are explicit here (not left to
 * auto-configuration) because Spring Boot's default JPA repository/entity scanning only looks at
 * this class's own package ({@code ...bootstrap}), while {@code scanBasePackages} below only
 * affects component scanning — every unit's {@code @Entity}/{@code JpaRepository} lives under
 * {@code com.rwashift.platform.units.*}, a sibling package, so both need to be widened explicitly
 * or every repository silently fails to wire.
 */
@SpringBootApplication(scanBasePackages = "com.rwashift.platform")
@EnableJpaRepositories(basePackages = "com.rwashift.platform")
@EntityScan(basePackages = "com.rwashift.platform")
public class RwaShiftPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RwaShiftPlatformApplication.class, args);
    }
}
