# HTTP API mini-wallet

Основной адрес контейнерного окружения — `http://localhost:8080`. Запрос проходит nginx → Spring Cloud Gateway → сервис. JWT и права владельца проверяет целевой сервис.

## Интерактивная документация

[Swagger UI](http://localhost:8080/swagger-ui.html) содержит четыре контракта: User, Ledger, Payment, Statement.

| Контракт | JSON |
| --- | --- |
| User | `/openapi/user` |
| Ledger | `/openapi/ledger` |
| Payment | `/openapi/payment` |
| Statement | `/openapi/statement` |

Каждый сервис формирует JSON через springdoc на своём `/v3/api-docs`. Gateway переписывает `/openapi/<service>` в этот путь. Общая UI размещена на gateway, в четырёх бизнес-сервисах подключена только генерация спецификации.

## Маршруты

| Метод | Путь | Сервис | Доступ / результат |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | user | Без JWT; 201 |
| POST | `/api/v1/auth/login` | user | Без JWT; 200, accessToken |
| GET | `/api/v1/users/me` | user | JWT; идентификатор и роль |
| POST | `/api/v1/wallets` | ledger | JWT; 201, кошелёк с нулевым балансом |
| GET | `/api/v1/wallets/{id}` | ledger | JWT; собственный кошелёк и баланс |
| POST | `/api/v1/payments` | payment | JWT + Idempotency-Key; 201 новый / 200 повтор |
| GET | `/api/v1/payments/{id}` | payment | JWT; собственный платёж |
| GET | `/api/v1/statement?walletId=...` | statement | JWT; выписка владельца |

Query-параметры выписки: `walletId` обязателен; `from` / `to` — необязательные ISO-8601 UTC instant, обе границы включительны; `limit` по умолчанию 50. Свежие записи возвращаются первыми.

## JWT и идемпотентность

В Swagger UI выполните login, затем вставьте полученный `accessToken` в **Authorize** без слова `Bearer`. UI сформирует `Authorization: Bearer <token>` для защищённых операций. Регистрация и login остаются публичными. Swagger UI сохраняет JWT в локальном хранилище браузера для переключения контрактов; Remove authorization удаляет его.

Для POST payments задайте уникальный `Idempotency-Key`, сохраните его вместе с исходным запросом и используйте тот же ключ при повторной отправке. Nginx и gateway передают этот заголовок и Authorization целевому сервису.

Пример пополнения (UUID нужно заменить на кошелёк текущего пользователя):

```json
{"type":"DEPOSIT","amount":"100.00","currency":"RUB","targetWalletId":"22222222-2222-4222-8222-222222222222"}
```

Для TRANSFER дополнительно передайте `sourceWalletId`; для DEPOSIT это поле нужно опустить. Примеры обоих запросов доступны в Swagger UI.

## Результаты и ошибки

У платежа бизнес-результат находится в `status`: `SUCCESS`, `REJECTED` или `FAILED`. Например, недостаток средств возвращает созданный платёж со статусом `REJECTED` и `failureReason=INSUFFICIENT_FUNDS`, а не HTTP 422 от публичного payment API.

HTTP-ошибки бизнес-сервисов описаны через `ProblemDetail` и дополнительное поле `code`. Например, отсутствие JWT даёт 401 / UNAUTHORIZED; чужой кошелёк или платёж — 404; конфликт ключа платежа — 409 / IDEMPOTENCY_KEY_CONFLICT. Чужая выписка возвращает пустой список. Поле code заполняют обработчики сервисов; стандартные ошибки Spring MVC (например, валидация DTO) могут возвращать ProblemDetail без code. Прокси передают ответ сервиса без замены его бизнес-кода.

## Внутренний вызов ledger

`POST /internal/v1/operations` вызывается payment напрямую по `http://ledger-service:8081` внутри Docker. Этот маршрут не включён в публичный OpenAPI и не настроен на gateway. Бизнес-сервисы и gateway не публикуют HTTP-порты на компьютере; внешним входом является nginx на 8080.

## Локальная разработка

Если приложения запущены из IDE, используйте gateway на `http://localhost:8085`. Адреса backend-сервисов задаются переменными `USER_SERVICE_URL`, `LEDGER_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `STATEMENT_SERVICE_URL`; их значения по умолчанию указывают на localhost и порты 8084, 8081, 8082, 8083 соответственно.

## Проверка маршрутизации

Тесты gateway поднимают настоящий локальный HTTP backend и проверяют маршруты, query-параметры, передачу Authorization / Idempotency-Key, тела запросов и HTTP-статусов, переписывание путей документации и закрытость internal API. Они не требуют PostgreSQL или Kafka.

```powershell
.\gateway-service\mvnw.cmd -f gateway-service/pom.xml test
```
