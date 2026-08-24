package com.mini_wallet.ledger_service.service;

import com.mini_wallet.ledger_service.entity.*;
import com.mini_wallet.ledger_service.repository.LedgerEntryRepository;
import com.mini_wallet.ledger_service.repository.LedgerTransactionRepository;
import com.mini_wallet.ledger_service.repository.WalletRepository;
import com.mini_wallet.ledger_service.web.dto.ExecuteOperationRequest;
import com.mini_wallet.ledger_service.web.dto.LedgerOperationResponse;
import com.mini_wallet.ledger_service.web.error.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerOperationService {

    private final WalletRepository walletRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    @Transactional
    public LedgerOperationResponse executeOperation(ExecuteOperationRequest request) {

        //если транзакция уже была создана вовзращаем готовую из бд
        Optional<LedgerTransaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return LedgerOperationResponse.from(existing.get());
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("amount must be positive");
        }
        if (request.sourceWalletId().equals(request.targetWalletId())) {
            throw new EqualsWalletsException("source wallet = target wallet");
        }

        //блокируем кошельки в фиксированном порядке по меньшему uuid(защита от deadlock)
        UUID firstId  = request.sourceWalletId().compareTo(request.targetWalletId()) < 0 ? request.sourceWalletId()  : request.targetWalletId();
        UUID secondId = request.sourceWalletId().compareTo(request.targetWalletId()) < 0 ? request.targetWalletId()  : request.sourceWalletId();

        Wallet first  = lockWallet(firstId);
        Wallet second = lockWallet(secondId);

        Wallet source = first.getId().equals(request.sourceWalletId()) ? first : second;
        Wallet target = second.getId().equals(request.targetWalletId()) ? second : first;

        //валидации
        validateStatus(source);
        validateStatus(target);

        if (request.type() == LedgerTransactionType.DEPOSIT && !source.isSystem()) {
            throw new InvalidOperationException("DEPOSIT must come from SYSTEM wallet");
        }
        if (request.type() == LedgerTransactionType.TRANSFER && source.isSystem()) {
            throw new InvalidOperationException("TRANSFER can't come from SYSTEM wallet");
        }
        if (request.type() == LedgerTransactionType.TRANSFER
                && source.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("insufficient funds on wallet " + source.getId());
        }

        //создаем операцию и две проводки
        LedgerTransaction tx = LedgerTransaction.create(
                request.type(), request.amount(), request.currency(), request.sourceWalletId(),
                request.targetWalletId(), request.externalRef(), request.idempotencyKey());

        LedgerEntry debit  = LedgerEntry.create(tx.getId(), tx.getSourceWalletId(), LedgerEntryDirection.DEBIT, tx.getAmount());
        LedgerEntry credit = LedgerEntry.create(tx.getId(), tx.getTargetWalletId(), LedgerEntryDirection.CREDIT, tx.getAmount());

        source.debit(tx.getAmount());
        target.credit(tx.getAmount());

        transactionRepository.save(tx);
        entryRepository.saveAll(List.of(debit, credit));

        log.info("operation {} posted: {} {} from {} to {}", tx.getId(), tx.getAmount(), tx.getCurrency(), tx.getSourceWalletId(), tx.getTargetWalletId());

        return LedgerOperationResponse.from(tx);
    }

    private Wallet lockWallet(UUID id) {
        return walletRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new WalletNotFoundException("wallet not found with id = " + id));
    }

    private void validateStatus(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletBlockedException("wallet " + wallet.getId() + " is blocked");
        }
    }
}