package com.mini_wallet.ledger_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(UUID id, String eventType, String payload,
                        Instant createdAt, Instant sentAt) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static OutboxEvent create(String eventType, String payload) {
        return new OutboxEvent(UUID.randomUUID(), eventType, payload, Instant.now(), null);
    }

    public void markSent() {
        this.sentAt = Instant.now();
    }
}