package com.tournament.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Local Tournament Management System API",
        version = "1.0.0",
        description = "Complete REST API for managing local cricket and sports tournaments. " +
                      "Designed to replace manual/WhatsApp-based tournament management in small towns.",
        contact = @Contact(name = "Tournament System", email = "support@tournament.local")
    ),
    servers = {
        @Server(url = "http://localhost:8080/api", description = "Local Development"),
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class SwaggerConfig {}
