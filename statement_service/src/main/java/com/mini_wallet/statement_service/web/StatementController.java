package com.mini_wallet.statement_service.web;

import com.mini_wallet.statement_service.service.StatementService;
import com.mini_wallet.statement_service.web.dto.StatementEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/statement")
public class StatementController {

    private final StatementService statementService;

    @GetMapping
    public List<StatementEntryResponse> getStatement(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID walletId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "50") int limit) {
        return statementService.getStatement(walletId, jwt.getSubject(), from, to, limit).stream()
                .map(StatementEntryResponse::from)
                .toList();
    }
}
