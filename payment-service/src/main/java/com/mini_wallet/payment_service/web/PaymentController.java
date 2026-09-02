package com.mini_wallet.payment_service.web;

import com.mini_wallet.payment_service.entity.Payment;
import com.mini_wallet.payment_service.repository.PaymentRepository;
import com.mini_wallet.payment_service.service.PaymentService;
import com.mini_wallet.payment_service.web.dto.CreatePaymentRequest;
import com.mini_wallet.payment_service.web.dto.PaymentResponse;
import com.mini_wallet.payment_service.web.error.PaymentNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {

        Payment pm = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("payment not found: " + id));

        return ResponseEntity.status(HttpStatus.OK).body(PaymentResponse.from(pm));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@RequestHeader("Idempotency-Key") String key,
                                                  @Valid @RequestBody CreatePaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);
        boolean wasCreated = existing.isEmpty();

        Payment pm = paymentService.createPayment(request, key);

        HttpStatus status = wasCreated ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(PaymentResponse.from(pm));
    }
}
