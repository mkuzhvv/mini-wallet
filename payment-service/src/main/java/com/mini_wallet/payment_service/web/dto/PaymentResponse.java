package com.mini_wallet.payment_service.web.dto;

import com.mini_wallet.payment_service.entity.Payment;
import com.mini_wallet.payment_service.entity.PaymentStatus;
import com.mini_wallet.payment_service.entity.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentType type,
        PaymentStatus status,
        String failureReason,
        BigDecimal amount,
        String currency,
        UUID sourceWalletId,
        UUID targetWalletId,
        Instant createdAt
) {

    public static PaymentResponse from(Payment pm) {
        return  new PaymentResponse(pm.getId(), pm.getType(), pm.getStatus(), pm.getFailureReason(), pm.getAmount(), pm.getCurrency(), pm.getSourceWalletId(), pm.getTargetWalletId(), pm.getCreatedAt());
    }
}
