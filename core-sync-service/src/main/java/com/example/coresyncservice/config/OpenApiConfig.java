package com.example.coresyncservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Swagger / OpenAPI documentation UI.
 *
 * ACTION REQUIRED: Replace the placeholder contact details and production URL
 * before going live.
 */
@OpenAPIDefinition(
        info = @Info(
                // ACTION REQUIRED: Replace with your team's contact details.
                contact = @Contact(
                        name  = "Core Sync Team",
                        email = "team@example.com",
                        url   = "https://example.com"
                ),
                description  = "REST API documentation for Core Sync Service",
                title        = "Core Sync Service API",
                version      = "1.0",
                license      = @License(
                        name = "MIT License",
                        url  = "https://opensource.org/licenses/MIT"
                ),
                termsOfService = "https://example.com/terms"
        ),
        servers = {
                @Server(description = "Local",      url = "http://localhost:8080"),
                // ACTION REQUIRED: Replace with your production base URL.
                @Server(description = "Production", url = "https://api.example.com")
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name         = "bearerAuth",
        description  = "Provide your JWT access token (obtained from /api/v1/auth/authenticate).",
        scheme       = "bearer",
        type         = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in           = SecuritySchemeIn.HEADER
)
@Configuration
public class OpenApiConfig {
    // No extra beans needed — annotations do all the work.
}
