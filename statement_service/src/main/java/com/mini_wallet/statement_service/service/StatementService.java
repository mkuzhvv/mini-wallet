package com.mini_wallet.statement_service.service;

import com.mini_wallet.statement_service.entity.StatementEntry;
import com.mini_wallet.statement_service.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository repository;

    @Transactional(readOnly = true)
    public List<StatementEntry> getStatement(UUID walletId, String userId,
                                             Instant from, Instant to, int limit) {
        return repository.findStatement(walletId, userId, from, to, PageRequest.of(0, limit));
    }
}
