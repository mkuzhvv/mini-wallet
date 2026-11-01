package com.mini_wallet.user_service.web;

import com.mini_wallet.user_service.entity.UserAccount;
import com.mini_wallet.user_service.service.LoginService;
import com.mini_wallet.user_service.service.RegistrationService;
import com.mini_wallet.user_service.service.model.LoginResult;
import com.mini_wallet.user_service.web.dto.LoginRequest;
import com.mini_wallet.user_service.web.dto.LoginResponse;
import com.mini_wallet.user_service.web.dto.RegisterRequest;
import com.mini_wallet.user_service.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Регистрация и получение JWT")
@ApiResponses({
        @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    @PostMapping("/register")
    @Operation(summary = "Зарегистрировать пользователя", description = "Создаёт учётную запись. Для получения JWT выполните login.")
    @ApiResponse(responseCode = "201", description = "Пользователь создан", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount user = registrationService.registerUser(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Получить access token", description = "Скопируйте accessToken из ответа в кнопку Authorize без префикса Bearer.")
    @ApiResponse(responseCode = "200", description = "JWT выдан; expiresIn указан в секундах", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "401", ref = "#/components/responses/InvalidCredentials")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginService.login(request.email(), request.password());
        return ResponseEntity.ok(LoginResponse.from(result));
    }
}
