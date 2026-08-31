package com.mini_wallet.payment_service.web.dto;

import com.mini_wallet.payment_service.entity.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull PaymentType type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        UUID sourceWalletId,
        @NotNull UUID targetWalletId,
        String description
) {}