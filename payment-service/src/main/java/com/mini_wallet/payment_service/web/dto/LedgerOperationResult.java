package com.mini_wallet.payment_service.web.dto;

import com.mini_wallet.payment_service.entity.Payment;

import java.util.UUID;

public record LedgerOperationResult(UUID id, String status) {

    public static LedgerOperationResult from(Payment pm) {
        return new LedgerOperationResult(pm.getId(), pm.getStatus().name());
    }
}
