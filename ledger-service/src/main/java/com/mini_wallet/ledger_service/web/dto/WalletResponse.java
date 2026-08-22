package com.mini_wallet.ledger_service.web.dto;

import com.mini_wallet.ledger_service.entity.Wallet;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String userId,
        String currency,
        String status,
        String balance,
        Instant createdAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                wallet.getCreatedAt()
        );
    }
}