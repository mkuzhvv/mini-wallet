package com.mini_wallet.ledger_service.service;

import com.mini_wallet.ledger_service.entity.*;
import com.mini_wallet.ledger_service.repository.LedgerEntryRepository;
import com.mini_wallet.ledger_service.repository.LedgerTransactionRepository;
import com.mini_wallet.ledger_service.repository.WalletRepository;
import com.mini_wallet.ledger_service.web.error.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerOperationService {

    private final WalletRepository walletRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    @Transactional
    public LedgerTransaction executeOperation(LedgerTransactionType type, BigDecimal amount, String currency,
                                              UUID sourceId, UUID targetId,
                                              UUID externalRef, String idempotencyKey) {

        // идемпотентность: если уже проводили — возвращаем существующую
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    log.info("idempotent replay: key={}", idempotencyKey);
                    return existing;
                })
                .orElseGet(() -> doExecute(type, amount, currency, sourceId, targetId, externalRef, idempotencyKey));
    }

    private LedgerTransaction doExecute(LedgerTransactionType type, BigDecimal amount, String currency,
                                        UUID sourceId, UUID targetId,
                                        UUID externalRef, String idempotencyKey) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("invalid amount: {}", amount);
            throw new InvalidOperationException("amount must be positive");
        }
        if (sourceId.equals(targetId)) {
            log.warn("source == target: {}", sourceId);
            throw new EqualsWalletsException("source wallet = target wallet");
        }

        // блокируем в фиксированном порядке (защита от deadlock)
        UUID firstId  = sourceId.compareTo(targetId) < 0 ? sourceId  : targetId;
        UUID secondId = sourceId.compareTo(targetId) < 0 ? targetId  : sourceId;

        Wallet first  = lockWallet(firstId);
        Wallet second = lockWallet(secondId);

        Wallet source = first.getId().equals(sourceId) ? first : second;
        Wallet target = second.getId().equals(targetId) ? second : first;

        validateStatus(source);
        validateStatus(target);

        if (type == LedgerTransactionType.DEPOSIT && !source.isSystem()) {
            log.warn("DEPOSIT from non-SYSTEM wallet: {}", sourceId);
            throw new InvalidOperationException("DEPOSIT must come from SYSTEM wallet");
        }
        if (type == LedgerTransactionType.TRANSFER && source.isSystem()) {
            log.warn("TRANSFER from SYSTEM wallet: {}", sourceId);
            throw new InvalidOperationException("TRANSFER can't come from SYSTEM wallet");
        }
        if (type == LedgerTransactionType.TRANSFER
                && source.getBalance().compareTo(amount) < 0) {
            log.warn("insufficient funds: wallet={}, balance={}, requested={}",
                    source.getId(), source.getBalance(), amount);
            throw new InsufficientFundsException("insufficient funds on wallet " + source.getId());
        }

        LedgerTransaction tx = LedgerTransaction.create(
                type, amount, currency, sourceId, targetId, externalRef, idempotencyKey);

        LedgerEntry debit  = LedgerEntry.create(tx.getId(), sourceId, LedgerEntryDirection.DEBIT, amount);
        LedgerEntry credit = LedgerEntry.create(tx.getId(), targetId, LedgerEntryDirection.CREDIT, amount);

        source.debit(amount);
        target.credit(amount);

        transactionRepository.save(tx);
        entryRepository.saveAll(List.of(debit, credit));

        log.info("operation posted: id={}, type={}, amount={}, from={}, to={}",
                tx.getId(), type, amount, sourceId, targetId);
        return tx;
    }

    private Wallet lockWallet(UUID id) {
        return walletRepository.findByIdForUpdate(id)
                .orElseThrow(() -> {
                    log.warn("wallet not found: id={}", id);
                    return new WalletNotFoundException("wallet not found with id = " + id);
                });
    }

    private void validateStatus(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("wallet blocked: id={}", wallet.getId());
            throw new WalletBlockedException("wallet " + wallet.getId() + " is blocked");
        }
    }
}