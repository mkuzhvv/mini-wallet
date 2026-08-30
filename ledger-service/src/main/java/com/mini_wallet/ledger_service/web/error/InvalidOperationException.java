package com.mini_wallet.ledger_service.web.error;

public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) { super(message); }
}
