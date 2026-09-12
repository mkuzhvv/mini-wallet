package com.mini_wallet.user_service.service.model;

public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
