package ar.edu.unlp.sistemanacional.config;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;

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
                                + "de competencia, compromiso (bloqueo) y liberación de recursos.\n\n"
                                + "**Cómo probarla:** `POST /api/auth/login` (ej. `bonita` / `bonita123`), copiar el "
                                + "`token`, tocar **Authorize** y pegarlo.\n\n"
                                + "Los errores salen como ProblemDetail (`status`, `title`, `detail`). "
                                + "Las fechas están en hora argentina (GMT-3)."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas("Error", errorSchema()))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    // Agrega a cada endpoint los errores que puede devolver, mirando su firma:
    // body → 400, token → 401, @PreAuthorize → 403, path variable → 404.
    // Los 409 se documentan a mano en los controllers, porque dependen de la regla de negocio.
    @Bean
    public OperationCustomizer erroresComunes() {
        return (operation, handlerMethod) -> {
            ApiResponses respuestas = operation.getResponses();
            MethodParameter[] parametros = handlerMethod.getMethodParameters();

            if (Arrays.stream(parametros).anyMatch(p -> p.hasParameterAnnotation(RequestBody.class))) {
                agregar(respuestas, "400", "Datos inválidos (el detalle por campo viene en `errores`)");
            }
            if (!handlerMethod.hasMethodAnnotation(SecurityRequirements.class)) {
                agregar(respuestas, "401", "Falta el token, es inválido o venció");
            }
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize != null) {
                agregar(respuestas, "403", "El rol no tiene permiso (" + preAuthorize.value() + ")");
            }
            if (Arrays.stream(parametros).anyMatch(p -> p.hasParameterAnnotation(PathVariable.class))) {
                agregar(respuestas, "404", "No existe el recurso indicado en la URL");
            }
            return operation;
        };
    }

    private static void agregar(ApiResponses respuestas, String codigo, String descripcion) {
        if (respuestas.containsKey(codigo)) return;   // ya documentado a mano en el controller
        respuestas.addApiResponse(codigo, new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error")))));
    }

    // Forma de los errores (ProblemDetail de Spring). "errores" e "items" solo aparecen en algunos casos.
    @SuppressWarnings("rawtypes")
    private static Schema errorSchema() {
        return new ObjectSchema()
                .description("Error en formato ProblemDetail (RFC 9457)")
                .addProperty("status", new IntegerSchema().example(409))
                .addProperty("title", new StringSchema().example("Conflict"))
                .addProperty("detail", new StringSchema().example("No se pueden comprometer los recursos"))
                .addProperty("instance", new StringSchema().example("/api/compromisos"))
                .addProperty("errores", new ObjectSchema()
                        .description("Solo en 400: mensaje por campo inválido")
                        .additionalProperties(new StringSchema()))
                .addProperty("items", new ArraySchema()
                        .description("Solo en el 409 de un compromiso: el resultado de cada aporte")
                        .items(new Schema<>().$ref("#/components/schemas/ItemVerificacion")));
    }
}
