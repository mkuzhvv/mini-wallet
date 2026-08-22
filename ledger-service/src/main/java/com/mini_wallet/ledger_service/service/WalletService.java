package com.mini_wallet.ledger_service.service;

import com.mini_wallet.ledger_service.entity.Wallet;
import com.mini_wallet.ledger_service.repository.WalletRepository;
import com.mini_wallet.ledger_service.web.dto.CreateWalletRequest;
import com.mini_wallet.ledger_service.web.dto.WalletResponse;
import com.mini_wallet.ledger_service.web.error.WalletNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        Wallet wallet = Wallet.create(request.userId(), request.currency());
        Wallet saved = walletRepository.save(wallet);

        log.info("create wallet successfully with id = {}", saved.getId());

        return WalletResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("wallet not found with id = " + id));
        return WalletResponse.from(wallet);
    }
}