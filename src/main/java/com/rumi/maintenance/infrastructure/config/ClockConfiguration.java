package com.rumi.maintenance.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Clock used by the use cases. Instants are stored in UTC; calendar dates, such as the date of an
 * inspection, follow the time zone of the buildings (Lima). Tests replace it with a fixed clock.
 */
@Configuration
public class ClockConfiguration {

    static final ZoneId BUILDINGS_TIME_ZONE = ZoneId.of("America/Lima");

    @Bean
    public Clock clock() {
        return Clock.system(BUILDINGS_TIME_ZONE);
    }
}
