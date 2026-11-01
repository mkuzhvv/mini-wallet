package com.mini_wallet.statement_service;

import com.mini_wallet.statement_service.entity.StatementDirection;
import com.mini_wallet.statement_service.entity.StatementEntry;
import com.mini_wallet.statement_service.entity.StatementType;
import com.mini_wallet.statement_service.repository.StatementRepository;
import com.mini_wallet.statement_service.service.StatementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StatementServiceIntegrationTests {

    @Autowired
    private StatementRepository repository;

    @Autowired
    private StatementService service;

    @Test
    void optionalDatesPreserveInclusiveBoundsOrderingLimitAndOwnership() {
        UUID walletId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        Instant firstDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant lastDate = Instant.parse("2026-01-03T00:00:00Z");
        Instant middleDate = Instant.parse("2026-01-02T00:00:00Z");
        StatementEntry first = entry(walletId, userId, firstDate);
        StatementEntry last = entry(walletId, userId, lastDate);
        repository.saveAllAndFlush(List.of(
                first, last,
                entry(walletId, UUID.randomUUID().toString(), lastDate),
                entry(UUID.randomUUID(), userId, lastDate)));

        assertThat(service.getStatement(walletId, userId, null, null, 50))
                .extracting(StatementEntry::getId).containsExactly(last.getId(), first.getId());
        assertThat(service.getStatement(walletId, userId, middleDate, null, 50))
                .extracting(StatementEntry::getId).containsExactly(last.getId());
        assertThat(service.getStatement(walletId, userId, null, middleDate, 50))
                .extracting(StatementEntry::getId).containsExactly(first.getId());
        assertThat(service.getStatement(walletId, userId, firstDate, lastDate, 50))
                .extracting(StatementEntry::getId).containsExactly(last.getId(), first.getId());
        assertThat(service.getStatement(walletId, userId, lastDate, lastDate, 50))
                .extracting(StatementEntry::getId).containsExactly(last.getId());
        assertThat(service.getStatement(walletId, userId, lastDate.plusSeconds(1), null, 50)).isEmpty();
        assertThat(service.getStatement(walletId, userId, null, firstDate.minusSeconds(1), 50)).isEmpty();
        assertThat(service.getStatement(walletId, userId, null, null, 1))
                .extracting(StatementEntry::getId).containsExactly(last.getId());
    }

    private StatementEntry entry(UUID walletId, String userId, Instant createdAt) {
        return StatementEntry.create(UUID.randomUUID(), walletId, userId,
                StatementDirection.IN, new BigDecimal("10.00"), "RUB",
                UUID.randomUUID(), StatementType.TRANSFER, createdAt);
    }
}
