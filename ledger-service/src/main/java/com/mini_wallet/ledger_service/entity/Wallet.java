package com.mini_wallet.ledger_service.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
public class Wallet {

    public static final String SYSTEM_USER_ID = "SYSTEM";

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

    public Wallet(UUID id, String userId, String currency, WalletStatus status,
                  BigDecimal balance, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public static Wallet create(String userId, String currency) {
        return new Wallet(UUID.randomUUID(), userId, currency,
                WalletStatus.ACTIVE, BigDecimal.ZERO, Instant.now());
    }

    public boolean isSystem() {
        return SYSTEM_USER_ID.equals(userId);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        BigDecimal result = this.balance.subtract(amount);
        if (!isSystem() && result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("wallet balance can't be negative");
        }
        this.balance = result;
    }
}