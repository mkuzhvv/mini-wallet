package com.mini_wallet.statement_service.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_entries")
@Getter
public class StatementEntry {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatementDirection direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "counterparty_wallet_id")
    private UUID counterpartyWalletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatementType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StatementEntry() {}

    private StatementEntry(UUID id, UUID transactionId, UUID walletId, String userId,
                           StatementDirection direction,
                           BigDecimal amount, String currency, UUID counterpartyWalletId,
                           StatementType type, Instant createdAt) {
        this.id = id; this.transactionId = transactionId; this.walletId = walletId;
        this.userId = userId;
        this.direction = direction; this.amount = amount; this.currency = currency;
        this.counterpartyWalletId = counterpartyWalletId; this.type = type; this.createdAt = createdAt;
    }

    public static StatementEntry create(UUID transactionId, UUID walletId, String userId,
                                        StatementDirection direction,
                                        BigDecimal amount, String currency, UUID counterparty,
                                        StatementType type, Instant createdAt) {
        return new StatementEntry(UUID.randomUUID(), transactionId, walletId, userId, direction,
                amount, currency, counterparty, type, createdAt);
    }
}
