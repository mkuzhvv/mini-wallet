package com.mini_wallet.ledger_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
        @NotBlank String userId,
        @NotBlank @Pattern(regexp = "RUB") String currency
) {
}