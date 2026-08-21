package com.mini_wallet.ledger_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryDirection direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(UUID id, UUID transactionId, UUID walletId, LedgerEntryDirection direction, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.direction = direction;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static LedgerEntry create(UUID transactionId, UUID walletId, LedgerEntryDirection direction, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return new LedgerEntry(UUID.randomUUID(), transactionId, walletId, direction, amount, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public LedgerEntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
