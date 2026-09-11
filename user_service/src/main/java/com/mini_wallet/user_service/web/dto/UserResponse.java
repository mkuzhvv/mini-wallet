package com.mini_wallet.user_service.web.dto;

import com.mini_wallet.user_service.entity.UserAccount;
import com.mini_wallet.user_service.entity.UserRole;
import com.mini_wallet.user_service.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
