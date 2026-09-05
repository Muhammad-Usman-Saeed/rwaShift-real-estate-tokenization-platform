package com.rwashift.platform;

import com.rwashift.platform.bootstrap.RwaShiftPlatformApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for capability integration tests: boots the full Spring context against a real
 * MySQL 8 container (Liquibase runs for real, {@code ddl-auto=validate} for real) rather than
 * H2 — catching the class of bug where an entity mapping is valid on an in-memory database but
 * not on MySQL's actual type/constraint behavior.
 *
 * <p>Uses the "singleton container" pattern (started once in a static initializer, never
 * explicitly stopped — Testcontainers' Ryuk reaper cleans it up when the JVM exits) rather than
 * {@code @Testcontainers}/{@code @Container}: those annotations stop the container in
 * {@code afterAll} for whichever concrete test class ran them, but the container instance is a
 * single static field shared across every subclass here, so the first subclass to finish would
 * silently kill the container out from under every subclass that runs after it.
 *
 * <p>Uses the default {@code MOCK} web environment (a real {@code WebApplicationContext}, no
 * bound network port) rather than {@code NONE} — Spring Security's {@code HttpSecurity}
 * prototype bean (needed by {@code AuthorizationServerConfig}/{@code ResourceServerConfig}) is
 * only registered when a web application context exists, even though these tests call
 * application services directly rather than issuing real HTTP requests.
 */
@SpringBootTest(classes = RwaShiftPlatformApplication.class)
public abstract class AbstractIntegrationTest {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("rwashift")
            .withUsername("rwashift")
            .withPassword("rwashift");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("rwashift.seed.enabled", () -> "false");
    }
}
