package com.mini_wallet.ledger_service.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
        @Schema(example = "RUB", description = "Валюта кошелька") @NotBlank @Pattern(regexp = "RUB") String currency
) {
}
