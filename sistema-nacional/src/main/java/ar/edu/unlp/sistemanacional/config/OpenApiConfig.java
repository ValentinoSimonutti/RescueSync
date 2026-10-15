package ar.edu.unlp.sistemanacional.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Datos generales que muestra Swagger UI (/swagger-ui.html).
// "bearerAuth": botón Authorize de Swagger, donde se pega el token del login.
// Por defecto todos los endpoints lo piden; los públicos lo anulan con @SecurityRequirements.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sistemaNacionalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema Nacional de Gestión de Recursos y Riesgos")
                        .version("1.0")
                        .description("API externa de RescueSync: onboarding y consulta de ONGs, validación por niveles "
                                + "de competencia, compromiso (bloqueo) y liberación de recursos."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
