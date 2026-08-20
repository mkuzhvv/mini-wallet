# Контракт API mini-wallet

## Общие соглашения

- Публичные ручки: `/api/v1/...`. Внутренние (сервис-сервис): `/internal/v1/...`
- Порты локально: ledger 8081, payment 8082, statement 8083
- Все id — UUID
- Время — ISO-8601 UTC, например `2026-08-14T10:30:00Z`
- Суммы в JSON — строки с двумя знаками: `"100.00"`
  Причина: числа с плавающей точкой в JSON могут терять точность у потребителей
  строка сохраняет точное десятичное значение. Внутри Java — BigDecimal
- Имена полей в JSON — camelCase, в БД — snake_case
- Заголовок `Idempotency-Key` обязателен для `POST /api/v1/payments`

Формат ошибки:

```json
{
  "code": "INSUFFICIENT_FUNDS",
  "message": "Недостаточно средств на кошельке-источнике"
}
```

## ledger-service (8081)

### POST /api/v1/wallets — создать кошелек

Request:

```json
{ "userId": "user-1", "currency": "RUB" }
```

Response 201:

```json
{
  "id": "9f2c...", "userId": "user-1", "currency": "RUB",
  "status": "ACTIVE", "balance": "0.00",
  "createdAt": "2026-08-14T10:30:00Z"
}
```

Ошибки: 400 VALIDATION_ERROR

### GET /api/v1/wallets/{walletId} — кошелек с балансом

Response 200 — тот же DTO. Ошибки: 404 WALLET_NOT_FOUND

### POST /internal/v1/operations — выполнить операцию (команда)

Вызывается только payment-service

Request:

```json
{
  "idempotencyKey": "7d1e...",
  "type": "TRANSFER",
  "sourceWalletId": "9f2c...",
  "targetWalletId": "4b8a...",
  "amount": "100.00",
  "currency": "RUB",
  "externalRef": "payment-id"
}
```

Response 200 (выполнена или уже выполнена ранее):

```json
{ "id": "tx-id", "status": "POSTED" }
```

Ошибки:
- 400 VALIDATION_ERROR (сумма <= 0, source == target);
- 404 WALLET_NOT_FOUND;
- 409 IDEMPOTENCY_KEY_CONFLICT (тот же ключ, но другой payload);
- 422 INSUFFICIENT_FUNDS / WALLET_BLOCKED.

## payment-service (8082)

### POST /api/v1/payments — создать платеж

Заголовок: `Idempotency-Key: <uuid>` (обязателен).

Request (перевод):

```json
{
  "type": "TRANSFER",
  "sourceWalletId": "9f2c...",
  "targetWalletId": "4b8a...",
  "amount": "100.00",
  "currency": "RUB",
  "description": "за обед"
}
```

Request (пополнение): `sourceWalletId` отсутствует

Response 201 (первое выполнение) / 200 (идемпотентный повтор):

```json
{
  "id": "payment-id",
  "type": "TRANSFER",
  "sourceWalletId": "9f2c...",
  "targetWalletId": "4b8a...",
  "amount": "100.00",
  "currency": "RUB",
  "status": "SUCCESS",
  "failureReason": null,
  "createdAt": "...", "updatedAt": "..."
}
```

Бизнес-отказ — это НЕ HTTP-ошибка: платеж создан, статус в теле
Пример: 201 + `"status": "REJECTED", "failureReason": "INSUFFICIENT_FUNDS"`
Причина: на HTTP-уровне запрос обработан; бизнес-результат — часть данных

Ошибки:
- 400 VALIDATION_ERROR;
- 409 IDEMPOTENCY_KEY_CONFLICT;
- 503 LEDGER_UNAVAILABLE (платеж сохранен со статусом FAILED, можно ретраить с тем же ключом)

### GET /api/v1/payments/{paymentId} — статус платежа

Response 200 — payment DTO. Ошибки: 404 PAYMENT_NOT_FOUND.

## statement-service (8083)

### GET /api/v1/wallets/{walletId}/statement — выписка

Query-параметры: `from`, `to` (ISO-даты, опционально), `page` (default 0), `size` (default 20).

Response 200:

```json
{
  "items": [
    {
      "id": "...",
      "transactionId": "...",
      "direction": "OUT",
      "amount": "-100.00",
      "counterpartyWalletId": "4b8a...",
      "description": "за обед",
      "occurredAt": "2026-08-14T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

Сортировка: `occurred_at DESC`.
Если операций нет — 200 с пустым `items` (сервис не проверяет существование кошелька,
он владеет только read model).

## Таблица кодов ошибок

| code                    | HTTP | где                     | смысл                          |
|-------------------------|------|-------------------------|--------------------------------|
| VALIDATION_ERROR        | 400  | все                     | невалидный запрос              |
| WALLET_NOT_FOUND        | 404  | ledger, payment         | кошелек не найден              |
| PAYMENT_NOT_FOUND       | 404  | payment                 | платеж не найден               |
| IDEMPOTENCY_KEY_CONFLICT| 409  | ledger, payment         | ключ использован с другим телом|
| INSUFFICIENT_FUNDS      | 422  | ledger (в payment — в теле) | недостаточно средств       |
| WALLET_BLOCKED          | 422  | ledger (в payment — в теле) | кошелек заблокирован       |
| LEDGER_UNAVAILABLE      | 503  | payment                 | ledger недоступен, retry       |