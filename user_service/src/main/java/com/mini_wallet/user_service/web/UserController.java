package com.mini_wallet.user_service.web;

import com.mini_wallet.user_service.web.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Получить текущего пользователя", description = "Возвращает идентификатор и роль из JWT.")
    @ApiResponse(responseCode = "200", description = "Данные текущего пользователя", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CurrentUserResponse.class)))
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUserResponse(jwt.getSubject(), jwt.getClaimAsString("role"));
    }
}
