package com.usinometria.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Usinometria API",
        version = "v0",
        description = """
                Estimativa de tempo de fabricação por usinagem.

                **Unidades:** volume em mm³; tempos em horas (decimal); taxa em mm³/h.

                **Erros:** todo erro segue o schema `ErrorResponse`; o campo `error` é um código estável. \
                Rotas inexistentes retornam 404 `ROUTE_NOT_FOUND`; métodos não permitidos, 405 `METHOD_NOT_ALLOWED`.
                """))
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer internalServerErrorResponse() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation ->
                        operation.getResponses().addApiResponse("500",
                                new io.swagger.v3.oas.models.responses.ApiResponse()
                                        .description("Erro inesperado (INTERNAL_SERVER_ERROR)")
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(new Schema<>()
                                                        .$ref("#/components/schemas/ErrorResponse")))))));
    }
}
