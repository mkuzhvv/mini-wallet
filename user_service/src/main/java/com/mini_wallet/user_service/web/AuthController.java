package com.mini_wallet.user_service.web;

import com.mini_wallet.user_service.entity.UserAccount;
import com.mini_wallet.user_service.service.LoginService;
import com.mini_wallet.user_service.service.RegistrationService;
import com.mini_wallet.user_service.service.model.LoginResult;
import com.mini_wallet.user_service.web.dto.LoginRequest;
import com.mini_wallet.user_service.web.dto.LoginResponse;
import com.mini_wallet.user_service.web.dto.RegisterRequest;
import com.mini_wallet.user_service.web.dto.UserResponse;
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
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount user = registrationService.registerUser(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginService.login(request.email(), request.password());
        return ResponseEntity.ok(LoginResponse.from(result));
    }
}
