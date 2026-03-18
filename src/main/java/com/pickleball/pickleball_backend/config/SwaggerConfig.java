package com.pickleball.pickleball_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pickleball Court Booking Platform API")
                        .version("1.0.0")
                        .description(
                                "A full-stack court booking platform built with " +
                                        "Java Spring Boot + PostgreSQL. " +
                                        "Supports dual roles: Court Owner and Booker. " +
                                        "Features: JWT authentication, real-time availability grid, " +
                                        "atomic checkout with concurrency protection, " +
                                        "venue management, and booking rescheduling."
                        )
                        .contact(new Contact()
                                .name("Vaibhav Udhane")
                                .email("vaibhavudhane1@gmail.com")
                        )
                )
                // Add JWT Bearer token support to Swagger UI
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Enter your JWT token here. " +
                                                        "Get it from /api/auth/login or /api/auth/register"
                                        )
                        )
                );
    }
}