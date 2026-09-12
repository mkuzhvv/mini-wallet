package com.mini_wallet.statement_service.consumer;

import com.mini_wallet.statement_service.entity.StatementDirection;
import com.mini_wallet.statement_service.entity.StatementEntry;
import com.mini_wallet.statement_service.entity.StatementType;
import com.mini_wallet.statement_service.event.TransactionPostedEvent;
import com.mini_wallet.statement_service.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionPostedConsumer {

    private final StatementRepository statementRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ledger-transactions", groupId = "statement-service")
    @Transactional
    public void onMessage(String message) {
        TransactionPostedEvent event;
        try {
            event = objectMapper.readValue(message, TransactionPostedEvent.class);
        } catch (JacksonException e) {
            log.error("unparseable event, skipping: {}", e.getMessage());
            return;
        }
        if (statementRepository.existsByTransactionId(event.transactionId())) {
            log.info("duplicate event skipped: tx= {}", event.transactionId());
            return;
        }
        if (event.sourceUserId() == null || event.targetUserId() == null) {
            log.warn("legacy event without wallet owners skipped: tx={}", event.transactionId());
            return;
        }

        StatementType type = StatementType.valueOf(event.type());

        StatementEntry out = StatementEntry.create(event.transactionId(), event.sourceWalletId(),
                event.sourceUserId(),
                StatementDirection.OUT, event.amount(), event.currency(),
                event.targetWalletId(), type, event.createdAt());

        StatementEntry in = StatementEntry.create(event.transactionId(), event.targetWalletId(),
                event.targetUserId(),
                StatementDirection.IN, event.amount(), event.currency(),
                event.sourceWalletId(), type, event.createdAt());

        statementRepository.saveAll(List.of(out, in));
        log.info("statement updated for tx={}", event.transactionId());
    }

}
