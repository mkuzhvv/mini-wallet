package com.mini_wallet.ledger_service.web;

import com.mini_wallet.ledger_service.entity.Wallet;
import com.mini_wallet.ledger_service.service.WalletService;
import com.mini_wallet.ledger_service.web.dto.CreateWalletRequest;
import com.mini_wallet.ledger_service.web.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse create(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request.userId(), request.currency());
        return WalletResponse.from(wallet);
    }

    @GetMapping("/{id}")
    public WalletResponse get(@PathVariable UUID id) {
        Wallet wallet = walletService.getWallet(id);
        return WalletResponse.from(wallet);
    }
}