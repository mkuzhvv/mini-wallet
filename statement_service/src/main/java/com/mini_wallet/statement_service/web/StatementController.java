package com.mini_wallet.statement_service.web;

import com.mini_wallet.statement_service.service.StatementService;
import com.mini_wallet.statement_service.web.dto.StatementEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/statement")
@Tag(name = "Statements")
@SecurityRequirement(name = "bearerAuth")
public class StatementController {

    private final StatementService statementService;

    @GetMapping
    @Operation(summary = "Получить выписку по кошельку", description = "Свежие операции первыми. Границы дат включительны. Выписка обновляется асинхронно из Kafka; чужой кошелёк возвращает пустой список.")
    @ApiResponse(responseCode = "200", description = "Записи выписки текущего пользователя", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = StatementEntryResponse.class))))
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    public List<StatementEntryResponse> getStatement(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Идентификатор кошелька") @RequestParam UUID walletId,
            @Parameter(description = "Нижняя граница времени UTC", example = "2026-01-01T00:00:00Z") @RequestParam(required = false) Instant from,
            @Parameter(description = "Верхняя граница времени UTC", example = "2026-12-31T23:59:59Z") @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "50") int limit) {
        return statementService.getStatement(walletId, jwt.getSubject(), from, to, limit).stream()
                .map(StatementEntryResponse::from)
                .toList();
    }
}
