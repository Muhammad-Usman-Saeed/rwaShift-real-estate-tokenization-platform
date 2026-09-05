package com.rwashift.platform.bootstrap;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} — used by Tokenization's blockchain transaction/event pollers. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
