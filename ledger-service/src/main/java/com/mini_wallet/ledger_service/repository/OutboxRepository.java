package com.mini_wallet.ledger_service.repository;

import com.mini_wallet.ledger_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findBySentAtIsNullOrderByIdAsc(); //то есть еще не отправлено
}
