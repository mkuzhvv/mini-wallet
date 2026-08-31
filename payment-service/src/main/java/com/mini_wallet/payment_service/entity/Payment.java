package com.mini_wallet.payment_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    private UUID sourceWalletId;

    @Column(nullable = false)
    private UUID targetWalletId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String failureReason;

    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(UUID id, String idempotencyKey, PaymentType type, UUID sourceWalletId, UUID targetWalletId,
                   BigDecimal amount, String currency, PaymentStatus status, String failureReason, String description,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.sourceWalletId = sourceWalletId;
        this.targetWalletId = targetWalletId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.failureReason = failureReason;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(String idempotencyKey, PaymentType type,
                                 UUID sourceWalletId, UUID targetWalletId,
                                 BigDecimal amount, String currency, String description) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Instant now = Instant.now();
        return new Payment(UUID.randomUUID(), idempotencyKey, type, sourceWalletId, targetWalletId,
                amount, currency, PaymentStatus.NEW, null, description, now, now);
    }

    public void markProcessing() { this.status = PaymentStatus.PROCESSING; this.updatedAt = Instant.now(); }
    public void markSuccess()    { this.status = PaymentStatus.SUCCESS;    this.updatedAt = Instant.now(); }
    public void markRejected(String reason) { this.status = PaymentStatus.REJECTED; this.failureReason = reason; this.updatedAt = Instant.now(); }
    public void markFailed(String reason)   { this.status = PaymentStatus.FAILED;   this.failureReason = reason; this.updatedAt = Instant.now(); }


    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentType getType() {
        return type;
    }

    public UUID getSourceWalletId() {
        return sourceWalletId;
    }

    public UUID getTargetWalletId() {
        return targetWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
