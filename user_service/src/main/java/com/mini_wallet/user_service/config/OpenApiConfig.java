package com.mini_wallet.user_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI walletOpenApi() {
        Schema<?> problem = new ObjectSchema()
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("code", new StringSchema().example("VALIDATION_ERROR"));
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                .addSchemas("ProblemDetail", problem)
                .addResponses("ValidationError", problem(400, "VALIDATION_ERROR", "Запрос не прошёл валидацию"))
                .addResponses("Unauthorized", problem(401, "UNAUTHORIZED", "Требуется действительный JWT"))
                .addResponses("InvalidCredentials", problem(401, "INVALID_CREDENTIALS", "Неверный email или пароль"))
                .addResponses("NotFound", problem(404, "NOT_FOUND", "Ресурс не найден или принадлежит другому пользователю"))
                .addResponses("Conflict", problem(409, "EMAIL_ALREADY_EXISTS", "Конфликт с существующими данными"))
                .addResponses("InternalError", problem(500, "INTERNAL_ERROR", "Внутренняя ошибка сервиса"));
        return new OpenAPI()
                .info(new Info().title("mini-wallet · User API").version("v1")
                        .description("Локальный учебный кошелёк. Суммы передаются в десятичном виде, валюта проекта — RUB."))
                .servers(List.of(new Server().url("/").description("Текущий адрес API")))
                .components(components);
    }

    private ApiResponse problem(int status, String code, String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/problem+json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                        .example(Map.of("type", "about:blank", "title", HttpStatus.valueOf(status).getReasonPhrase(),
                                "status", status, "detail", description, "code", code))));
    }
}
