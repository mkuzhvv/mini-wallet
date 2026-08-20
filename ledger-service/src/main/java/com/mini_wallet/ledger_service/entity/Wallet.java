package com.mini_wallet.ledger_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private Instant createdAt;

    protected Wallet() {
    }

    public static Wallet create(String userId, String currency) {
        return new Wallet(UUID.randomUUID(), userId, currency,
                WalletStatus.ACTIVE, BigDecimal.ZERO, Instant.now());
    }

    public Wallet(UUID id, String userId, String currency, WalletStatus status, BigDecimal balance, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
