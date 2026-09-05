# mini-wallet

Микросервисная платежная система — учебный backend-проект на Spring Boot, который моделирует ключевые механизмы настоящих финансовых систем: двойную запись, идемпотентность, транзакционность, синхронное и асинхронное межсервисное взаимодействие.

## Архитектура

Три микросервиса, у каждого — своя схема PostgreSQL:

| Сервис | Порт | Роль |
|---|---|---|
| `payment-service` | 8082 | Оркестратор платежей: принимает запросы клиента, синхронно вызывает ledger, маппит результаты в статусы |
| `ledger-service` | 8081 | Денежное ядро (source of truth): кошельки, операции двойной записи, балансы |
| `statement-service` | 8083 | Read model (CQRS): потребляет события из Kafka и строит выписки |

```
            [Клиент]
               │ REST (Idempotency-Key)
               ▼
      payment-service :8082      — оркестрация, клиентская идемпотентность
               │ REST (команды, синхронно)
               ▼
      ledger-service  :8081      — двойная запись, блокировки, идемпотентность
               │ outbox → Kafka (события, асинхронно)
               ▼
      statement-service :8083    — read model, выписки (CQRS)
```

**Принципы взаимодействия:**
- **Команды** — синхронный REST (`payment → ledger`): плательщик сразу получает результат.
- **События** — асинхронная Kafka (`ledger → statement`): слабая связанность, надежность, масштабируемость.

## Ключевые механизмы

### Двойная запись (double-entry)
Каждая операция — это `ledger_transaction` + две проводки `ledger_entries` (DEBIT и CREDIT) на одинаковую сумму. Инвариант: сумма всех балансов системы всегда равна 0. Деньги не создаются и не исчезают.

### Идемпотентность (два уровня)
- **Клиент → payment**: заголовок `Idempotency-Key`; повтор с тем же ключом и payload возвращает сохраненный результат, с другим payload — 409.
- **payment → ledger**: ключ команды = id платежа; повторный вызов не проводит деньги дважды.

Ретраи, таймауты и «потерянные» ответы не приводят к двойному списанию.

### Параллелизм и блокировки
Пессимистические блокировки (`SELECT ... FOR UPDATE`) с **фиксированным порядком** захвата кошельков (по UUID) — защита от lost update и deadlock.

### Outbox pattern
Событие записывается в таблицу `outbox` **в той же транзакции**, что и операция; отдельный publisher отправляет накопленные события в Kafka. Атомарность «операция + событие», доставка at-least-once с идемпотентным консьюмером.

### CQRS и eventual consistency
`ledger` — сторона записи (нормализованные таблицы, source of truth), `statement` — сторона чтения (денормализованная read model). Выписка строится из событий с небольшой задержкой.

### Единый формат ошибок
RFC 7807 (ProblemDetail) + бизнес-поле `code` (`INSUFFICIENT_FUNDS`, `WALLET_NOT_FOUND`, `IDEMPOTENCY_KEY_CONFLICT` и др.) через централизованный `@RestControllerAdvice`.

## Технологический стек

- **Java 21**, **Spring Boot 4** (Spring MVC, Spring Data JPA / Hibernate 7, RestClient, spring-kafka)
- **PostgreSQL 16** (схемы `ledger`, `payment`, `statement`), **Flyway** (миграции)
- **Kafka** (брокер событий), **Docker / docker-compose**
- **Maven**, **Lombok**, **Bean Validation**

## Структура репозитория

```
mini-wallet/
├── docker/              # docker-compose: PostgreSQL + Kafka
├── docs/                # документация: domain.md, api.md, rules.md, events.md
├── ledger-service/      # денежное ядро
├── payment-service/     # оркестратор платежей
└── statement-service/   # read model, выписки
```

## Запуск

Требования: Java 21, Maven, Docker.

```bash
cd docker
docker compose up -d        # поднимает PostgreSQL и Kafka

# затем запустить сервисы (ledger → payment → statement),
# например из IDE или:
mvn -pl ledger-service spring-boot:run
```

Flyway сам накатит миграции при старте каждого сервиса.

## API (основное)

- `POST /api/v1/wallets`, `GET /api/v1/wallets/{id}` — кошельки (ledger)
- `POST /internal/v1/operations` — внутренняя денежная команда (ledger)
- `POST /api/v1/payments` (заголовок `Idempotency-Key`), `GET /api/v1/payments/{id}` — платежи (payment)
- `GET /api/v1/statement?walletId=...&from=...&to=...` — выписка (statement)

Полный контракт — в [`docs/api.md`](docs/api.md).

## Документация

- [`docs/domain.md`](docs/domain.md) — доменная модель и границы сервисов
- [`docs/rules.md`](docs/rules.md) — бизнес-правила (W-*, L-*, P-*)
- [`docs/api.md`](docs/api.md) — HTTP-контракты и коды ошибок
- [`docs/events.md`](docs/events.md) — схема событий Kafka

## Roadmap (в разработке)

- **notification-service** — уведомления на основе событий Kafka;
- **JWT-авторизация** — аутентификация и разграничение доступа;
- **rate limiter** (Redis) — защита публичных endpoint'ов от флуда;
- **OpenAPI gateway** — единая точка входа и автодокументация API.