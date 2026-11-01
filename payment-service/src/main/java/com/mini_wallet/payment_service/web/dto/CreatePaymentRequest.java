package com.mini_wallet.payment_service.web.dto;

import com.mini_wallet.payment_service.entity.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull PaymentType type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @Schema(description = "Для TRANSFER обязателен; для DEPOSIT поле нужно опустить") UUID sourceWalletId,
        @NotNull UUID targetWalletId,
        String description
) {}
