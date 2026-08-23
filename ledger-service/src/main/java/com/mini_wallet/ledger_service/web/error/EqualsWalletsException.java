package com.mini_wallet.ledger_service.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EqualsWalletsException extends RuntimeException {
    public EqualsWalletsException(String message) { super(message); }
}