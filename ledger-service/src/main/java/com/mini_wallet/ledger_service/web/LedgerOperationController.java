package com.mini_wallet.ledger_service.web;

import com.mini_wallet.ledger_service.entity.LedgerTransaction;
import com.mini_wallet.ledger_service.service.LedgerOperationService;
import com.mini_wallet.ledger_service.web.dto.ExecuteOperationRequest;
import com.mini_wallet.ledger_service.web.dto.LedgerOperationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/operations")
public class LedgerOperationController {

    private final LedgerOperationService ledgerOperationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerOperationResponse execute(@Valid @RequestBody ExecuteOperationRequest request) {
        LedgerTransaction tx = ledgerOperationService.executeOperation(
                request.type(),
                request.amount(),
                request.currency(),
                request.sourceWalletId(),
                request.targetWalletId(),
                request.externalRef(),
                request.idempotencyKey());
        return LedgerOperationResponse.from(tx);
    }
}