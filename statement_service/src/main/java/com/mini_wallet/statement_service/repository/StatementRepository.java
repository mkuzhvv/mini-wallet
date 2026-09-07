package com.mini_wallet.statement_service.repository;

import com.mini_wallet.statement_service.entity.StatementEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<StatementEntry, UUID> {
    boolean existsByTransactionId(UUID transactionId);
    List<StatementEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
