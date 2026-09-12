package com.mini_wallet.ledger_service.web;

import com.mini_wallet.ledger_service.entity.Wallet;
import com.mini_wallet.ledger_service.service.WalletService;
import com.mini_wallet.ledger_service.web.dto.CreateWalletRequest;
import com.mini_wallet.ledger_service.web.dto.WalletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<WalletResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(jwt.getSubject(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponse.from(wallet));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Wallet wallet = walletService.getWallet(id, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.OK).body(WalletResponse.from(wallet));
    }
}
