package com.mini_wallet.ledger_service.service;

import com.mini_wallet.ledger_service.entity.Wallet;
import com.mini_wallet.ledger_service.repository.WalletRepository;
import com.mini_wallet.ledger_service.web.error.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet createWallet(String userId, String currency) {
        Wallet wallet = Wallet.create(userId, currency);
        Wallet saved = walletRepository.save(wallet);
        log.info("wallet created: id={}, userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("wallet not found: id={}", id);
                    return new WalletNotFoundException("wallet not found with id = " + id);
                });
    }
}