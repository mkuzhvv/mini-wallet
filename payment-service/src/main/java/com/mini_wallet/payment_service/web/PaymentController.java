package com.mini_wallet.payment_service.web;

import com.mini_wallet.payment_service.entity.Payment;
import com.mini_wallet.payment_service.repository.PaymentRepository;
import com.mini_wallet.payment_service.service.PaymentService;
import com.mini_wallet.payment_service.web.dto.CreatePaymentRequest;
import com.mini_wallet.payment_service.web.dto.PaymentResponse;
import com.mini_wallet.payment_service.web.error.PaymentNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {

        Payment pm = paymentRepository.findByIdAndUserId(id, jwt.getSubject())
                .orElseThrow(() -> new PaymentNotFoundException("payment not found: " + id));

        return ResponseEntity.status(HttpStatus.OK).body(PaymentResponse.from(pm));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                  @RequestHeader("Idempotency-Key") String key,
                                                  @Valid @RequestBody CreatePaymentRequest request) {
        String userId = jwt.getSubject();
        Optional<Payment> existing = paymentRepository.findByUserIdAndIdempotencyKey(userId, key);
        boolean wasCreated = existing.isEmpty();

        Payment pm = paymentService.createPayment(request, key, userId, authorization);

        HttpStatus status = wasCreated ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(PaymentResponse.from(pm));
    }
}
