package com.mini_wallet.user_service.web.dto;

import com.mini_wallet.user_service.service.model.LoginResult;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn()
        );
    }
}