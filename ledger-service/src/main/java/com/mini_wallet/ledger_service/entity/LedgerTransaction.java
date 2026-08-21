package com.mini_wallet.ledger_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerTransactionStatus status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private UUID sourceWalletId;

    @Column(nullable = false)
    private UUID targetWalletId;

    @Column
    private UUID externalRef;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    private LedgerTransaction(UUID id, LedgerTransactionType type, LedgerTransactionStatus status, BigDecimal amount, String currency, UUID sourceWalletId, UUID targetWalletId, UUID externalRef, String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.sourceWalletId = sourceWalletId;
        this.targetWalletId = targetWalletId;
        this.externalRef = externalRef;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static LedgerTransaction create(LedgerTransactionType type, BigDecimal amount, String currency,
                                           UUID sourceWalletId, UUID targetWalletId, UUID externalRef,
                                           String idempotencyKey) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return new LedgerTransaction(UUID.randomUUID(), type, LedgerTransactionStatus.POSTED, amount, currency,
                sourceWalletId, targetWalletId, externalRef, idempotencyKey, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public LedgerTransactionType getType() {
        return type;
    }

    public LedgerTransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public UUID getSourceWalletId() {
        return sourceWalletId;
    }

    public UUID getTargetWalletId() {
        return targetWalletId;
    }

    public UUID getExternalRef() {
        return externalRef;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
