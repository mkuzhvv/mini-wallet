package com.mini_wallet.payment_service.web;

import com.mini_wallet.payment_service.entity.Payment;
import com.mini_wallet.payment_service.repository.PaymentRepository;
import com.mini_wallet.payment_service.service.PaymentService;
import com.mini_wallet.payment_service.web.dto.CreatePaymentRequest;
import com.mini_wallet.payment_service.web.dto.PaymentResponse;
import com.mini_wallet.payment_service.web.error.PaymentNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
})
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/{id}")
    @Operation(summary = "Получить состояние платежа")
    @ApiResponse(responseCode = "200", description = "Платёж текущего пользователя", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    public ResponseEntity<PaymentResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {

        Payment pm = paymentRepository.findByIdAndUserId(id, jwt.getSubject())
                .orElseThrow(() -> new PaymentNotFoundException("payment not found: " + id));

        return ResponseEntity.status(HttpStatus.OK).body(PaymentResponse.from(pm));
    }

    @PostMapping
    @Operation(summary = "Пополнить кошелёк или перевести средства",
            description = "Результат операции находится в status: SUCCESS, REJECTED или FAILED. Бизнес-отказ возвращается в теле платежа. Для повтора передайте исходное тело и тот же Idempotency-Key.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Пополнение", value = """
                            {"type":"DEPOSIT","amount":"100.00","currency":"RUB","targetWalletId":"22222222-2222-4222-8222-222222222222","description":"Пополнение кошелька"}
                            """),
                    @ExampleObject(name = "Перевод", value = """
                            {"type":"TRANSFER","amount":"25.00","currency":"RUB","sourceWalletId":"22222222-2222-4222-8222-222222222222","targetWalletId":"33333333-3333-4333-8333-333333333333","description":"Перевод"}
                            """)
            })))
    @ApiResponse(responseCode = "201", description = "Новый платёж; бизнес-результат указан в status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "200", description = "Повтор существующего платежа", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationError")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    public ResponseEntity<PaymentResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                  @Parameter(description = "Уникальный ключ поручения. Сохраните его для повторной отправки.", example = "11111111-1111-4111-8111-111111111111")
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
