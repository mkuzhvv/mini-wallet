package com.mini_wallet.ledger_service.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class WalletBlockedException extends RuntimeException {
    public WalletBlockedException(String message) {
        super(message);
    }
}
