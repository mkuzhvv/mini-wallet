package com.mini_wallet.user_service.web.dto;

public record CurrentUserResponse(
        String userId,
        String role
) {
}
