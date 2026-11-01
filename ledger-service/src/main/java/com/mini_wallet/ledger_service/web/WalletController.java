package com.mini_wallet.ledger_service.web;

import com.mini_wallet.ledger_service.entity.Wallet;
import com.mini_wallet.ledger_service.service.WalletService;
import com.mini_wallet.ledger_service.web.dto.CreateWalletRequest;
import com.mini_wallet.ledger_service.web.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Wallets")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Создать кошелёк", description = "Владелец определяется из JWT. Новый кошелёк имеет нулевой баланс.")
    @ApiResponse(responseCode = "201", description = "Кошелёк создан", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class)))
    @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<WalletResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(jwt.getSubject(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponse.from(wallet));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить кошелёк и текущий баланс")
    @ApiResponse(responseCode = "200", description = "Кошелёк текущего пользователя", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletResponse.class)))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    public ResponseEntity<WalletResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Wallet wallet = walletService.getWallet(id, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.OK).body(WalletResponse.from(wallet));
    }
}
