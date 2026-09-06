package com.mini_wallet.ledger_service.outbox;

import com.mini_wallet.ledger_service.entity.OutboxEvent;
import com.mini_wallet.ledger_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    public static final String TOPIC = "ledger-transactions";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findBySentAtIsNullOrderByIdAsc();
        if (pending.isEmpty()) return;

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(TOPIC, keyOf(event), event.getPayload()).get();
                event.markSent();
            } catch (Exception e) {
                log.warn("failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                return;
            }
        }
        log.info("published {} outbox event(s)", pending.size());
    }

    //достаем ключ партицирования - transactionId
    private String keyOf(OutboxEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.getPayload());
            return node.path("transactionId").asString();
        } catch (Exception e) {
            return event.getId().toString();
        }
    }
}
