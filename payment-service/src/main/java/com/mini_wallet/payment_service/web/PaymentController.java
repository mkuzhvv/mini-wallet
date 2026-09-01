package com.mini_wallet.payment_service.web;

import com.mini_wallet.payment_service.entity.Payment;
import com.mini_wallet.payment_service.service.PaymentService;
import com.mini_wallet.payment_service.web.dto.CreatePaymentRequest;
import com.mini_wallet.payment_service.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestHeader("Idempotency-Key") String key,
                                  @Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request, key);
        return PaymentResponse.from(payment);
    }
}
