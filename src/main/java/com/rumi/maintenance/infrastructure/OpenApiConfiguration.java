package com.rumi.maintenance.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI maintenanceServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Rumi Maintenance Service API")
                .description("REST API of the Building Maintenance bounded context: the calendar of structural "
                        + "inspections (US17) and their findings (US18), the damage reported by residents (US21) "
                        + "and the damage board of the administrator (US22).")
                .version("0.2.0"));
    }
}
