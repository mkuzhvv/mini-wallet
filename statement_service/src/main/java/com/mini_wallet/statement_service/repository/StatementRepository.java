package com.mini_wallet.statement_service.repository;

import com.mini_wallet.statement_service.entity.StatementEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<StatementEntry, UUID> {
    boolean existsByTransactionId(UUID transactionId);

    @Query("""
        SELECT e FROM StatementEntry e WHERE e.walletId = :walletId
         AND e.userId = :userId
         AND (CAST(:from AS Instant) IS NULL OR e.createdAt >= :from)
         AND (CAST(:to AS Instant) IS NULL OR e.createdAt <= :to)
        ORDER BY e.createdAt DESC
        """)
    List<StatementEntry> findStatement(@Param("walletId") UUID walletId,
                                       @Param("userId") String userId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       Pageable pageable);
}
