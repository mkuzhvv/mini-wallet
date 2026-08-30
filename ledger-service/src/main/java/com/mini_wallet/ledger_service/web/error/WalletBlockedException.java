package com.mini_wallet.ledger_service.web.error;

public class WalletBlockedException extends RuntimeException {
    public WalletBlockedException(String message) {
        super(message);
    }
}
