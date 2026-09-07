package com.mini_wallet.statement_service.repository;

import com.mini_wallet.statement_service.entity.StatementEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<StatementEntry, UUID> {
    boolean existsByTransactionId(UUID transactionId);

    @Query("""
        SELECT e FROM StatementEntry e WHERE e.walletId = :walletId
         AND (:from IS NULL OR e.createdAt >= :from)
         AND (:to   IS NULL OR e.createdAt <= :to)
        ORDER BY e.createdAt DESC
        """)
    List<StatementEntry> findStatement(@Param("walletId") UUID walletId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       Pageable pageable);
}
