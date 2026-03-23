package com.musyan.stok.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Control Microservice APIs")
                        .description("Professional REST APIs for managing stock and products")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Mustafa Yanmaz")
                                .email("mustafa@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Stock Control Microservice Documentation"));
    }
}