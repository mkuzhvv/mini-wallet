package com.mini_wallet.ledger_service.repository;

import com.mini_wallet.ledger_service.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
