package com.mini_wallet.ledger_service.web.dto;

import com.mini_wallet.ledger_service.entity.LedgerTransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ExecuteOperationRequest(
        @NotNull LedgerTransactionType type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotNull UUID sourceWalletId,
        @NotNull UUID targetWalletId,
        UUID externalRef,//nullable для системных операций
        @NotBlank String idempotencyKey
) {}