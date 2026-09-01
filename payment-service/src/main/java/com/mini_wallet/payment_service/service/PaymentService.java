package com.mini_wallet.payment_service.service;

import com.mini_wallet.payment_service.entity.Payment;
import com.mini_wallet.payment_service.entity.PaymentType;
import com.mini_wallet.payment_service.repository.PaymentRepository;
import com.mini_wallet.payment_service.web.dto.CreatePaymentRequest;
import com.mini_wallet.payment_service.web.dto.LedgerOperationCommand;
import com.mini_wallet.payment_service.web.dto.LedgerOperationResult;
import com.mini_wallet.payment_service.web.error.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestClient ledgerClient;

    private static final UUID SYSTEM_WALLET_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");


    @Transactional
    public Payment createPayment(CreatePaymentRequest request, String idempotencyKey) {

        if (request.type().equals(PaymentType.TRANSFER) && request.sourceWalletId() == null) {
            log.warn("invalid source wallet: {}", (Object) null);
            throw new PaymentValidationException("for TRANSFER source wallet must be not null");
        }
        if (request.type().equals(PaymentType.DEPOSIT) && request.sourceWalletId() != null) {
            log.warn("invalid source wallet: {}", request.sourceWalletId());
            throw new PaymentValidationException("for DEPOSIT source wallet must be null");
        }

        Payment pm = Payment.create(idempotencyKey, request.type(), resolveSource(request),
                request.targetWalletId(), request.amount(), request.currency(), request.description());

        paymentRepository.save(pm);
        pm.markProcessing();

        //сборка команды для ledger service
        LedgerOperationCommand cmd = new LedgerOperationCommand(pm.getType(), pm.getAmount(), pm.getCurrency(),
                pm.getSourceWalletId(), pm.getTargetWalletId(), pm.getId(), pm.getId().toString());

        //вызов ledger service по его денежной ручке
        LedgerOperationResult res = ledgerClient.post()
                .uri("/internal/v1/operations")
                .body(cmd)
                .retrieve()
                .body(LedgerOperationResult.class);

        pm.markSuccess();
        return pm;
    }

    private UUID resolveSource(CreatePaymentRequest request) {
        return request.type() == PaymentType.DEPOSIT ? SYSTEM_WALLET_ID : request.sourceWalletId();
    }
}
