# Запуск mini-wallet в Docker

Все команды выполняются из корня репозитория. Нужен Docker Engine с Compose
(например, Docker Desktop с Linux containers). Для контейнерного запуска
устанавливать Java и Maven на компьютер не требуется.

## Запуск и управление

```shell
docker compose up --build -d --wait --wait-timeout 300
docker compose ps -a
docker compose logs -f ledger-service payment-service statement-service
```

Первая сборка скачивает базовые образы и Maven-зависимости. Повторная использует
кэш. Приложения доступны на `localhost:8081`–`localhost:8084`.
Проверка готовности: `GET /actuator/health` на каждом из этих портов.

```shell
docker compose stop
docker compose start
docker compose down
```

`stop` останавливает контейнеры; `start` запускает существующие контейнеры.
`down` удаляет контейнеры и сеть, сохраняя named volumes. Для запуска после `down`
снова выполните `docker compose up -d --wait`.
Ключ `down -v` удаляет управляемые Compose тома с данными; для обычной остановки
он не нужен.

## Что находится в Dockerfile

В `dockerfiles/` лежат четыре отдельных Dockerfile и общая конфигурация Maven.
Контекст сборки — корень репозитория, поэтому пути в `COPY` начинаются с имени
каталога сервиса, например `ledger-service/src`.

Каждый образ собирается в два этапа:

1. `build`: Maven 3.9.16 и JDK 21 компилируют исходники и упаковывают Spring Boot JAR.
2. Финальный образ: JRE 21 запускает готовый JAR от пользователя `wallet`, без root.

В финальный образ не копируются Maven, исходники и кэш зависимостей.
BuildKit хранит общий кэш Maven между сборками. Доступ к нему сериализован, чтобы
четыре параллельные сборки не записывали одни и те же файлы одновременно.
`.dockerignore` исключает локальные `target/`, `.git`, `.idea` и локальные настройки.

Сборка использует `dockerfiles/maven-settings.xml` с HTTPS-зеркалом Maven Central
у Google. Настройки `~/.m2/settings.xml` с компьютера внутрь сборки не передаются.
На стадии упаковки тесты не выполняются: существующие `@SpringBootTest` требуют
работающих PostgreSQL и Kafka. Их можно запускать отдельно после подготовки инфраструктуры.

## Сеть и адреса

Compose создаёт сеть проекта `mini-wallet`. Контейнеры обращаются друг к другу
по именам сервисов; внешний IP контейнера знать не нужно.

| Соединение | Адрес внутри Docker | Адрес с компьютера |
| --- | --- | --- |
| PostgreSQL | `postgres:5432` | `localhost:5432` |
| Kafka | `kafka:9092` | `localhost:9092` |
| Ledger | `ledger-service:8081` | `localhost:8081` |
| Payment | `payment-service:8082` | `localhost:8082` |
| Statement | `statement-service:8083` | `localhost:8083` |
| User | `user-service:8084` | `localhost:8084` |

Адреса PostgreSQL, Kafka и ledger переопределены через environment в Compose.
Значения `localhost` в `application.yaml` остаются пригодны для запуска из IDE.
Порты опубликованы только на `127.0.0.1`, для локального доступа.

У Kafka два клиентских listener: `INTERNAL` для контейнеров и `EXTERNAL` для
приложений на компьютере. Внешний порт `9092` перенаправляется на порт `19092`
контейнера. В `advertised.listeners` брокер возвращает клиентам адрес,
доступный с соответствующей стороны сети.

## Готовность и топик

PostgreSQL проверяется через `pg_isready`, Kafka — через запрос списка топиков.
Каждый Java-сервис имеет healthcheck на `/actuator/health`.

`kafka-init` ждёт готовности Kafka, создаёт `ledger-transactions` с тремя
партициями и replication factor 1, затем завершается. `Exited (0)` для этого
контейнера означает успешную инициализацию. Ledger и statement ждут её завершения,
а payment дополнительно ждёт готовности ledger.

Команда создания использует `--if-not-exists`: повтор безопасен, топик не
удаляется и не пересоздаётся. Эта команда не меняет параметры существующего
топика. Автоматическое создание произвольных топиков брокером отключено.

```shell
docker compose logs kafka-init
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic ledger-transactions
```

## Хранение данных

По умолчанию Compose создаёт тома `mini-wallet_pgdata` и `mini-wallet_kafkadata`.
В Kafka явно настроен `log.dirs=/var/lib/kafka/data`, связанный с named volume:
после пересоздания контейнера сохраняются сообщения, топики и consumer offsets.

PostgreSQL использует `mini-wallet_pgdata`. Дополнительные Compose-файлы для
подключения данных не требуются. Обычный `docker compose down` сохраняет оба тома,
а `docker compose down -v` удаляет их вместе с данными.

## Проверка приложения

После готовности контейнеров используйте полный сценарий из README:
регистрация → вход → создание кошельков → пополнение → перевод → выписка.
Изменение баланса и появление записей выписки проверяют также соединение
payment → ledger и цепочку outbox → Kafka → statement.

Интеграционная регрессия выписки проверяет запросы без дат, с одной границей и с
обеими, включительность границ, сортировку, лимит и изоляцию владельцев / кошельков.
Она работает с PostgreSQL из Compose и откатывает созданные строки:

```powershell
.\statement_service\mvnw.cmd -f statement_service/pom.xml test
```

Для разработки запускайте Maven с JDK 21. Полный контейнерный запуск этого не требует.
