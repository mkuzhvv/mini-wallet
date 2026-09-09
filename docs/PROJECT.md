# mini-wallet — проектная документация и передача контекста

Документ для разработчиков и AI-агентов. Описывает, ЧТО построено, ПОЧЕМУ так, и ЧТО планируется дальше.
Читай целиком перед любыми правками. Дополнительные контракты: `docs/domain.md`, `docs/api.md`, `docs/events.md`.

---

## 1. Что это за проект

пет-проект «мини-кошелек»: микросервисный бэкенд на Spring Boot, реализующий
пополнения и переводы между кошельками с двойной записью, идемпотентностью,
событийной архитектурой (Kafka + outbox) и CQRS (read model выписки).

Цель проекта — освоить backend-разработку: Spring Boot, JPA/Postgres, Flyway,
транзакции и блокировки, идемпотентность, HTTP-клиенты, Kafka, outbox, CQRS,
а далее — security (JWT), Redis, MongoDB, gateway и load balancing.
так же на проекте будут проектиковаться различные новые технологии - тем самым расширяя его функционал путес внедрения новых фич

---

## 2. Технологический стек

- Java 21, Spring Boot 4.1.0 (Spring Framework 7)
- Spring Web (RestClient), Spring Data JPA, Bean Validation
- PostgreSQL 16 (Docker), Flyway (миграции, режим `validate`)
- Apache Kafka (Docker, образ `apache/kafka`, KRaft, без Zookeeper)
- Lombok, Jackson
- Планируется: Spring Security + JWT, Redis, MongoDB, Spring Cloud Gateway, nginx, springdoc-openapi(также думаю в будущем сделать общение микросервисов по gRPC)

---

## 3. Архитектура: сервисы и связи

```
                        ┌──────────┐
                        │  CLIENT  │
                        └────┬─────┘
   POST /api/v1/payments │   │ GET /api/v1/wallets   │ GET /api/v1/statement
                         v   v                       v
                  ┌───────────┐   ┌───────────┐  ┌──────────────┐
                  │ payment   │   │ ledger    │  │ statement    │
                  │ :8082     │   │ :8081     │  │ :8083        │
                  └─────┬─────┘   └─────┬─────┘  └──────▲───────┘
                        │ REST (команда, синхронно)      │
                        └──────────────>│               │
                                        │ outbox → Kafka (topic: ledger-transactions)
                                        └───────────────────────────────┘
                  [payment]        [ledger]         [statement]   ← свои схемы Postgres
```

- **Команды** (payment → ledger): синхронный REST `POST /internal/v1/operations`.
- **События** (ledger → Kafka → statement): асинхронные, через outbox.
- Каждый сервис владеет своей схемой Postgres. **Между схемами НЕТ foreign keys.**

Инфраструктура (Docker Compose, папка `docker/`):
- `miniwallet-postgres` :5432, БД `miniwallet`, user/pass `wallet`/`wallet`
- `miniwallet-kafka` :9092 (KRaft single-node)

---

## 4. Модули репозитория

- `ledger-service` — денежное ядро (кошельки, операции, двойная запись, outbox+publisher)
- `payment-service` — оркестратор платежей (клиентская идемпотентность, вызов ledger, маппинг статусов)
- `statement-service` — read model выписки (Kafka consumer, API выписки)
- `docker/` — docker-compose (postgres, kafka)
- `docs/` — документация (domain, api, events, этот файл)

---

## 5. Доменные правила (инварианты)

### Ledger (L-…)
- L-01 Двойная запись: у каждой операции ровно две проводки (DEBIT source, CREDIT target) на одинаковую сумму; сумма со знаком = 0.
- L-02 Баланс обычного кошелька не может стать отрицательным; SYSTEM-кошелек может уходить в минус (эмитент).
- L-03 Идемпотентность операции по `idempotency_key` (UNIQUE): повтор возвращает существующую транзакцию, деньги не двигаются.
- L-04 Блокировки кошельков `SELECT ... FOR UPDATE` в фиксированном порядке (сначала меньший UUID) — защита от deadlock.
- DEPOSIT допустим только из SYSTEM; TRANSFER не допустим из SYSTEM.

### Payment (P-…)
- P-01 Заголовок `Idempotency-Key` обязателен (иначе 400).
- P-02 Повтор с тем же ключом и тем же payload возвращает сохраненный платеж (200), ledger не вызывается.
- P-03 Терминальные статусы SUCCESS/REJECTED кэшируются; FAILED — нетерминальный, retry с тем же ключом безопасен.
- P-04 Тот же ключ, другой payload → 409 IDEMPOTENCY_KEY_CONFLICT.
- P-05 Маппинг результата ledger: 2xx→SUCCESS; 422/404→REJECTED(code); 409→409; 5xx/таймаут→FAILED(LEDGER_UNAVAILABLE).
- P-06 Таймаут вызова ledger (~3 c): защита от «повисшего» ledger.

### Два уровня идемпотентности (важно!)
- payment: клиентский `Idempotency-Key` защищает переход клиент→payment (схлопывает retry клиента в один Payment).
- ledger: ключ команды = `payment.id` защищает переход payment→ledger (схлопывает повторные вызовы одного Payment; гарантирует отсутствие double spend при retry после таймаута).

---

## 6. Схемы БД

### ledger (схема `ledger`)
- `wallets(id, user_id, status, currency, balance, created_at, updated_at)`; SYSTEM = `00000000-0000-0000-0000-000000000000`
- `ledger_transactions(id, type, status, amount, currency, source_wallet_id, target_wallet_id, external_ref, idempotency_key UNIQUE, created_at)`
- `ledger_entries(id, transaction_id, wallet_id, direction DEBIT|CREDIT, amount, created_at)`
- `outbox(id, event_type, payload, created_at, sent_at)` — sent_at NULL = не отправлено

### payment (схема `payment`)
- `payments(id, idempotency_key UNIQUE, type, source_wallet_id, target_wallet_id, amount, currency, status NEW|PROCESSING|SUCCESS|REJECTED|FAILED, failure_reason, description, created_at, updated_at)`

### statement (схема `statement`)
- `statement_entries(id, transaction_id, wallet_id, direction IN|OUT, amount, currency, counterparty_wallet_id, type, created_at)`; UNIQUE(transaction_id, wallet_id) — идемпотентность консьюмера

---

## 7. API-контракты (кратко)

Формат ошибки везде: RFC 7807 ProblemDetail + поле `code`.

### ledger :8081
- `POST /api/v1/wallets` → 201 (создание кошелька)
- `GET /api/v1/wallets/{id}` → 200 / 404 WALLET_NOT_FOUND
- `POST /internal/v1/operations` → 201 POSTED; идемпотентна по ключу; 422 INSUFFICIENT_FUNDS / WALLET_BLOCKED; 400 VALIDATION_ERROR

### payment :8082
- `POST /api/v1/payments` + `Idempotency-Key` → 201 (создан) / 200 (повтор) / 409 (конфликт ключа); тело = платеж со статусом
- `GET /api/v1/payments/{id}` → 200 / 404 PAYMENT_NOT_FOUND

### statement :8083
- `GET /api/v1/statement?walletId=&from=&to=&limit=` → 200, список записей выписки (свежие первыми)

Коды ошибок: WALLET_NOT_FOUND, PAYMENT_NOT_FOUND, NOT_FOUND, VALIDATION_ERROR, INSUFFICIENT_FUNDS, WALLET_BLOCKED, IDEMPOTENCY_KEY_CONFLICT, INTERNAL_ERROR.

---

## 8. События и outbox

- Топик: `ledger-transactions` (3 партиции). Ключ партиционирования = `transactionId`.
- Событие `TRANSACTION_POSTED`, payload JSON:
  `{transactionId, type, amount, currency, sourceWalletId, targetWalletId, createdAt}`
- Публикация: outbox пишется в той же БД-транзакции, что и операция; relay (`OutboxPublisher`, `@Scheduled` 1 c) читает `sent_at IS NULL`, шлет в Kafka, ставит `sent_at`. Семантика at-least-once → консьюмеры идемпотентны.
- Консьюмеры: `statement-service` (group `statement-service`), строит read model (2 строки на событие: OUT для source, IN для target).

---

## 9. Что уже реализовано (Этапы 1–5)

- **Этап 1:** документация и контракты (domain/api/events), правила L/P, диаграммы.
- **Этап 2:** Docker Postgres + Flyway + схема ledger + JPA Wallet + ручки кошельков + ProblemDetail.
- **Этап 3:** денежное ядро: двойная запись, блокировки FOR UPDATE + фикс. порядок, идемпотентность, статусы, `@RestControllerAdvice` с кодами.
- **Этап 4:** payment-оркестратор: RestClient, синхронная команда в ledger, маппинг P-05, клиентская идемпотентность, статусы платежа.
- **Этап 5:** Kafka + outbox + publisher; statement-service consumer + read model + API выписки; eventual consistency.

---

## 10. Ключевые проектные решения (и почему)

- Команды синхронные (REST), события асинхронные (Kafka): клиенту нужен результат сразу; асинхронные команды отложены.
- Два уровня идемпотентности (см. §5) — защита от double spend на обоих переходах.
- Outbox вместо прямой отправки в Kafka: атомарность «операция + событие» одной БД-транзакцией.
- CQRS: ledger = нормализованная write model (корректность записи); statement = денормализованная read model (скорость чтения).
- Нет FK между схемами: микросервисные границы; целостность кошельков проверяет ledger.
- Слоистость: сервис возвращает доменные сущности, контроллер маппит DTO; сервис не зависит от web-DTO.
- Централизованные ошибки: `@RestControllerAdvice` + ProblemDetail + `code`.

### Известные особенности / заметки
- На Spring Boot 4.1 автоконфигурация Kafka в этом окружении НЕ создала `KafkaTemplate`/`kafkaListenerContainerFactory`; поэтому в ledger и statement есть РУЧНЫЕ `KafkaConfig`-классы. НЕ удалять их.
- Relay outbox — polling (1 c): известен trade-off (лаг, холостые тики); в бою заменяется на CDC/LISTEN-NOTIFY.

---

## 11. Как запустить

```bash
cd docker && docker compose up -d     # postgres + kafka
# затем запустить ledger-service (8081), payment-service (8082), statement-service (8083)
```

Топик создается один раз:
```bash
docker exec -it miniwallet-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic ledger-transactions --partitions 3 --replication-factor 1
```

---

## 12. Дорожная карта (порядок выполнения)

Порядок: **Этап 7 → Этап 8 → Этап 6 → Этап 9 → Этап 10.**

### Этап 7 — Аутентификация и JWT
Цель: пользователи, регистрация/логин, stateless-аутентификация, защита ручек.
- Новый user/auth-сервис: регистрация (email+пароль, BCrypt), логин → JWT (access; опц. refresh); таблица пользователей в своей схеме.
- SecurityFilterChain + JWT-фильтр: валидация токена, извлечение userId в SecurityContext.
- Защита payment/wallet/statement: без токена 401; кошельки привязываются к userId из токена (вместо свободной строки).
- Концепции: stateless auth, структура/подпись JWT, BCrypt, filter chain, роли.
- Критерий: register/login выдают JWT; защищенная ручка без токена → 401, с токеном → 200; кошелек создается на пользователя из токена.

### Этап 8 — Notification-сервис + Telegram + MongoDB
Цель: уведомления о операциях в Telegram; хранение в MongoDB (документная модель).
- Новый notification-service: Kafka-consumer топика `ledger-transactions` (своя группа).
- На `TRANSACTION_POSTED`: найти chat_id пользователя (документ предпочтений в Mongo), отправить сообщение через Telegram Bot API (`sendMessage`), сохранить документ уведомления в Mongo; дедуп по transactionId.
- MongoDB в Docker + spring-data-mongodb; документы NotificationDoc / UserPrefsDoc.
- Концепции: интеграция внешнего API (BotFather, токен бота), документная модель, когда NoSQL уместен.
- Критерий: после платежа пользователь получает сообщение в Telegram; уведомление есть в Mongo; повторы не дублируются.

### Этап 6 — Rate limiter на Redis
Цель: защита публичных ручек от злоупотреблений.
- Redis в Docker; interceptor/filter на публичных эндпоинтах (login, POST payments) с окном/лимитом (sliding window или token bucket, можно Lua-скриптом).
- Ключи per-user / per-IP; при превышении → 429 + Retry-After.
- Концепции: Redis, алгоритмы rate limiting.
- Критерий: N+1-й запрос в окне возвращает 429.

### Этап 9 — API Gateway + nginx (load balancing)
Цель: единая точка входа, маршрутизация, балансировка.
- Spring Cloud Gateway: маршруты /api/v1/payments→payment, /api/v1/wallets→ledger, /api/v1/statement→statement, /auth→user-service.
- nginx в Docker как reverse proxy/LB; демонстрация round-robin на 2 инстансах одного сервиса (statelessness).
- Концепции: routing, cross-cutting concerns, LB, statelessness.
- Критерий: весь трафик идет через gateway/nginx; LB распределяет между инстансами.

### Этап 10 — Полировка
- Тесты (unit + integration, Testcontainers), springdoc-openapi (Swagger), полный docker-compose всех сервисов, README, опц. мониторинг (actuator/Prometheus).

---

## 13. Соглашения по коду (для агентов)

- Слоистость: controller → service → repository; сервис не импортирует web-DTO.
- Ошибки: доменные исключения + `@RestControllerAdvice` → ProblemDetail с `code`.
- Миграции Flyway иммутабельны: новые изменения — только новые файлы V2, V3…
- Идемпотентность обязательна для денежных операций и консьюмеров.
- Логи: бизнес-отказы WARN в сервисе (богатый контекст), технические сбои ERROR централизованно.
- Коммиты: `feat: …`, `fix: …`, `docs: …` с кратким описанием.