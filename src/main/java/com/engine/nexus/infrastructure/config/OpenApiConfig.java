package com.engine.nexus.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nexusWorkflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexusWorkflow Engine API")
                        .description("Code-First Resilient Distributed Workflow & DAG Engine powered by Java 21 Virtual Threads and Event Sourcing")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Alexandr Motologa")
                                .url("https://github.com/alexandrmotologa/nexus-workflow"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
