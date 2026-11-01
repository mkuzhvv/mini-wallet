package com.mini_wallet.user_service.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        @Schema(example = "alice@example.com") String email,

        @NotBlank
        @Schema(example = "WalletPass123!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password
) {
}
