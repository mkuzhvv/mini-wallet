package com.mini_wallet.user_service.security;

public record IssuedAccessToken(
        String value,
        long expiresIn
) {
}
