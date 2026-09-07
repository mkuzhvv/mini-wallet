package com.mini_wallet.statement_service.web.dto;

import com.mini_wallet.statement_service.entity.StatementEntry;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record StatementEntryResponse(
        UUID transactionId,
        UUID walletId,
        String direction,
        String amount,
        String currency,
        UUID counterpartyWalletId,
        String type,
        Instant createdAt
) {
    public static StatementEntryResponse from(StatementEntry e) {
        return new StatementEntryResponse(
                e.getTransactionId(), e.getWalletId(), e.getDirection().name(),
                e.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                e.getCurrency(), e.getCounterpartyWalletId(), e.getType().name(), e.getCreatedAt());
    }
}