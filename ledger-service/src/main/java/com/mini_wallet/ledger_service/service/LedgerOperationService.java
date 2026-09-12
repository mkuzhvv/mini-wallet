package com.mini_wallet.ledger_service.service;

import com.mini_wallet.ledger_service.entity.*;
import com.mini_wallet.ledger_service.repository.LedgerEntryRepository;
import com.mini_wallet.ledger_service.repository.LedgerTransactionRepository;
import com.mini_wallet.ledger_service.repository.OutboxRepository;
import com.mini_wallet.ledger_service.repository.WalletRepository;
import com.mini_wallet.ledger_service.web.dto.TransactionPostedEvent;
import com.mini_wallet.ledger_service.web.error.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LedgerTransaction executeOperation(LedgerTransactionType type, BigDecimal amount, String currency,
                                              UUID sourceId, UUID targetId,
                                              UUID externalRef, String idempotencyKey, String userId) {

        //идемпотентность если уже проводили — возвращаем существующую
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    validateExistingOperationOwner(existing, userId);
                    log.info("idempotent replay: key={}", idempotencyKey);
                    return existing;
                })
                .orElseGet(() -> doExecute(type, amount, currency, sourceId, targetId,
                        externalRef, idempotencyKey, userId));
    }

    private LedgerTransaction doExecute(LedgerTransactionType type, BigDecimal amount, String currency,
                                        UUID sourceId, UUID targetId,
                                        UUID externalRef, String idempotencyKey, String userId) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("invalid amount: {}", amount);
            throw new InvalidOperationException("amount must be positive");
        }
        if (sourceId.equals(targetId)) {
            log.warn("source == target: {}", sourceId);
            throw new EqualsWalletsException("source wallet = target wallet");
        }

        //блокируем в фиксированном порядке (защита от deadlock)
        //всегда сначала блокируем кошелек с меньшим UUID
        boolean sourceFirst = sourceId.compareTo(targetId) < 0;
        UUID firstId  = sourceFirst ? sourceId : targetId;
        UUID secondId = sourceFirst ? targetId : sourceId;

        Wallet first  = lockWallet(firstId);
        Wallet second = lockWallet(secondId);

        Wallet source = first.getId().equals(sourceId) ? first : second;
        Wallet target = second.getId().equals(targetId) ? second : first;

        validateStatus(source);
        validateStatus(target);
        validateOwner(type, source, target, userId);

        if (type == LedgerTransactionType.DEPOSIT && !source.isSystem()) {
            log.warn("DEPOSIT from non-SYSTEM wallet: {}", sourceId);
            throw new InvalidOperationException("DEPOSIT must come from SYSTEM wallet");
        }
        if (type == LedgerTransactionType.TRANSFER && source.isSystem()) {
            log.warn("TRANSFER from SYSTEM wallet: {}", sourceId);
            throw new InvalidOperationException("TRANSFER can't come from SYSTEM wallet");
        }
        if (type == LedgerTransactionType.TRANSFER && source.getBalance().compareTo(amount) < 0) {
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

        //отправляем событие в outbox в одной транзакции
        TransactionPostedEvent event = new TransactionPostedEvent(
                tx.getId(), tx.getType().name(), tx.getAmount(), tx.getCurrency(),
                tx.getSourceWalletId(), source.getUserId(),
                tx.getTargetWalletId(), target.getUserId(),
                tx.getCreatedAt());
        outboxRepository.save(OutboxEvent.create("TRANSACTION_POSTED", toJson(event)));

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

    private void validateExistingOperationOwner(LedgerTransaction transaction, String userId) {
        Wallet source = findWallet(transaction.getSourceWalletId());
        Wallet target = findWallet(transaction.getTargetWalletId());
        validateOwner(transaction.getType(), source, target, userId);
    }

    private Wallet findWallet(UUID id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("wallet not found with id = " + id));
    }

    private void validateOwner(LedgerTransactionType type, Wallet source, Wallet target, String userId) {
        boolean owned = switch (type) {
            case DEPOSIT -> target.getUserId().equals(userId);
            case TRANSFER -> source.getUserId().equals(userId);
        };

        if (!owned) {
            UUID protectedWalletId = type == LedgerTransactionType.DEPOSIT ? target.getId() : source.getId();
            log.warn("wallet does not belong to user: walletId={}, userId={}", protectedWalletId, userId);
            throw new WalletNotFoundException("wallet not found with id = " + protectedWalletId);
        }
    }

    private String toJson(TransactionPostedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("failed to serialize outbox event", e);
        }
    }
}
