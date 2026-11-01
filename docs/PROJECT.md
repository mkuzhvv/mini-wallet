# mini-wallet — проектная документация и передача контекста

Документ для разработчиков и AI-агентов. Описывает, ЧТО построено, ПОЧЕМУ так, и ЧТО планируется дальше.
Читай целиком перед любыми правками. Дополнительные контракты: `docs/domain.md`, `docs/api.md`, `docs/events.md`.

---

## 1. Что это за проект

пет-проект «мини-кошелек»: микросервисный бэкенд на Spring Boot, реализующий
пополнения и переводы между кошельками с двойной записью, идемпотентностью,
событийной архитектурой (Kafka + outbox) и CQRS (read model выписки).

Цель проекта — освоить backend-разработку: Spring Boot, JPA/Postgres, Flyway,
транзакции и блокировки, идемпотентность, HTTP-клиенты, Kafka, outbox и CQRS.
JWT уже реализован. Итоговый объём: Docker Compose, OpenAPI, Spring Cloud Gateway, nginx и возможный gRPC для payment → ledger.

---

## 2. Технологический стек

- Java 21, Spring Boot 4.1.0 (Spring Framework 7)
- Spring Web (RestClient), Spring Data JPA, Bean Validation
- PostgreSQL 16 (Docker), Flyway (миграции, режим `validate`)
- Apache Kafka (Docker, образ `apache/kafka`, KRaft, без Zookeeper)
- Lombok, Jackson
- Реализовано: Spring Security / JWT, Dockerfile для четырёх сервисов, полный Compose, kafka-init. Далее: OpenAPI, Spring Cloud Gateway, nginx; gRPC рассматривается для payment → ledger.

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

Инфраструктура и приложения описаны в корневом `compose.yaml`; Dockerfile и настройки Maven — в `dockerfiles/`:
- `miniwallet-postgres` :5432, БД `miniwallet`, user/pass `wallet`/`wallet`
- `miniwallet-kafka` :9092 (KRaft single-node)

---

## 4. Модули репозитория

- `ledger-service` — денежное ядро (кошельки, операции, двойная запись, outbox+publisher)
- `payment-service` — оркестратор платежей (клиентская идемпотентность, вызов ledger, маппинг статусов)
- `statement-service` — read model выписки (Kafka consumer, API выписки)
- `user_service` — регистрация пользователей и выдача JWT
- `compose.yaml` — четыре приложения, Postgres, Kafka, kafka-init
- `dockerfiles/` — Dockerfile и настройки Maven
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

Из корня репозитория:

```shell
docker compose up --build -d --wait --wait-timeout 300
docker compose ps -a
```

Сборка JAR выполняется внутри Docker. Готовность приложений проверяется через `/actuator/health`. Kafka использует `kafka:9092` внутри Docker и `localhost:9092` с компьютера. Топик `ledger-transactions` (3 партиции, replication factor 1) создаёт одноразовый сервис `kafka-init`. PostgreSQL и Kafka сохраняют данные в named volumes. Остановка: `docker compose down` без `-v`. Подробнее: `docs/docker.md`. Поддерживается запуск приложений из IDE с контейнерной инфраструктурой.

---

## 12. Итоговая дорожная карта

Объём согласован 1 ноября 2026 года. Прежние планы Redis, MongoDB и уведомлений исключены.

1. Docker: отдельные Dockerfile, корневой Compose, проверки готовности и kafka-init — реализовано.
2. OpenAPI / Swagger UI — описание и демонстрация публичного HTTP API.
3. Spring Cloud Gateway и nginx — единый внешний вход с разделением обязанностей.
4. gRPC для payment → ledger — отдельный рассматриваемый этап; Kafka остаётся каналом событий.
5. Итоговая проверка основных сценариев и документация для портфолио.

---

## 13. Соглашения по коду (для агентов)

- Слоистость: controller → service → repository; сервис не импортирует web-DTO.
- Ошибки: доменные исключения + `@RestControllerAdvice` → ProblemDetail с `code`.
- Миграции Flyway иммутабельны: новые изменения — только новые файлы V2, V3…
- Идемпотентность обязательна для денежных операций и консьюмеров.
- Логи: бизнес-отказы WARN в сервисе (богатый контекст), технические сбои ERROR централизованно.
- Коммиты: `feat: …`, `fix: …`, `docs: …` с кратким описанием.
