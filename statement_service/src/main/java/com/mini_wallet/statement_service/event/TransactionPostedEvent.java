package com.mini_wallet.statement_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionPostedEvent(
        UUID transactionId,
        String type,
        BigDecimal amount,
        String currency,
        UUID sourceWalletId,
        String sourceUserId,
        UUID targetWalletId,
        String targetUserId,
        Instant createdAt
) {}
