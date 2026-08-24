package com.mini_wallet.ledger_service.web.dto;

import com.mini_wallet.ledger_service.entity.LedgerTransaction;

import java.util.UUID;

public record LedgerOperationResponse(UUID id, String status) {

    public static LedgerOperationResponse from(LedgerTransaction tx) {
        return new LedgerOperationResponse(tx.getId(), tx.getStatus().name());
    }
}
