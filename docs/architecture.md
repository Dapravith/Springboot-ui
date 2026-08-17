# Architecture

## Repository layout

Every deployable unit is a top-level directory. There is no `services/` wrapper:
the repository root *is* the service list, so a newcomer sees the system's shape
without opening anything.

```
banking-microservices/
├── common-domain/          shared kernel - zero dependencies
├── common-web/             shared web edge - API envelope + error handling
│
├── api-gateway/            :8080  Spring Cloud Gateway (WebMVC)
├── customer-service/       :8081  PostgreSQL, Kafka producer
├── account-service/        :8082  PostgreSQL, Redis, Kafka producer
├── transfer-service/       :8083  transfer policy, calls the ledger
├── notification-service/   :8084  Kafka consumer
│
├── docker/                 Dockerfile + database bootstrap
├── docs/
├── compose.yml             infrastructure
├── compose.dev.yml         containerised services (dev only)
├── build.gradle            one convention for every module
└── settings.gradle
```

## Clean Architecture inside a service

Each service is organised in four layers. Dependencies point **inward only**:

```
        ┌──────────────────────────────────────────────┐
        │  interfaces/rest        infrastructure/       │   frameworks,
        │  controllers, DTOs      JPA, Kafka, Redis     │   I/O, drivers
        └───────────┬──────────────────────┬───────────┘
                    │                      │
                    ▼                      ▼
        ┌──────────────────────────────────────────────┐
        │  application            use-case services,   │   orchestration,
        │                         transaction bounds   │   no business rules
        └───────────────────────┬──────────────────────┘
                                ▼
        ┌──────────────────────────────────────────────┐
        │  domain     model + ports (in / out)         │   the rules.
        │             no Spring, no JPA, no Jackson    │   knows nothing
        └──────────────────────────────────────────────┘
```

Concretely, for `account-service`:

```
com.springboot.account
├── domain
│   ├── model/            Account, AccountNumber, AccountStatus, exceptions
│   └── port
│       ├── in/           OpenAccountUseCase, TransferFundsUseCase, QueryAccountUseCase
│       └── out/          AccountRepositoryPort, AccountEventPublisherPort,
│                         AccountNumberGeneratorPort, AccountMetricsPort
├── application/          AccountApplicationService (implements the inbound ports)
├── infrastructure
│   ├── persistence/      AccountJpaEntity, AccountJpaRepository,
│   │                     AccountPersistenceAdapter, AccountJpaMapper
│   ├── messaging/        KafkaAccountEventPublisher
│   ├── cache/            RedisAccountMetrics
│   ├── identity/         RandomAccountNumberGenerator
│   └── config/           WebConfig
└── interfaces/rest/      AccountController + dto/
```

### Why the ports exist

The outbound ports are declared in `domain` and implemented in `infrastructure`.
That inversion buys three concrete things:

1. **The domain is testable without a container.** `AccountTest` runs ten
   invariant checks in milliseconds — no Spring, no database, no Testcontainers.
2. **The application layer is testable with fakes.**
   `TransferApplicationServiceTest` simulates a ledger outage in one line, which
   would otherwise need a stubbed HTTP server.
3. **Technology is replaceable.** `InMemoryTransferRepository` can become a JPA
   adapter by adding one class; no domain or application code changes.

### Why the domain model is separate from the JPA entity

`Account` and `AccountJpaEntity` are deliberately two classes joined by a mapper.
The alternative — annotating the domain model with `@Entity` — makes the database
schema and the business model the same thing, so neither can change without the
other. The mapper is the small, explicit price for that independence, and it is
the single place a schema change surfaces as a compile error.

## The rules are enforced, not just described

`ArchitectureTest` in `account-service` and `customer-service` runs as a normal
unit test and fails the build on:

- any dependency from `domain` on `org.springframework..`
- any dependency from `domain` on JPA, Hibernate, Kafka or Jackson
- any dependency from `domain` on the web layer
- controllers reaching directly into `infrastructure.persistence`
- the JPA entity escaping its own package
- layer violations of the dependency rule generally

It also asserts that classes were actually imported, so the suite cannot pass
vacuously if the package is ever renamed.

```bash
./gradlew :account-service:test --tests '*ArchitectureTest'
```

When you add a service, copy that test and change the base package. It is the
cheapest way to keep the architecture true a year from now.

## The shared kernel

Two modules, ordered by dependency:

| Module | Depends on | Contains |
|---|---|---|
| `common-domain` | **nothing** | `Money`, the `DomainException` hierarchy |
| `common-web` | `common-domain`, Spring MVC | `ApiResponse`, `ApiError`, `GlobalExceptionHandler` |

`common-domain` having zero dependencies is the point: it is the innermost ring,
and an empty dependency block is a rule that cannot be violated by accident.

`Money` lives here because "an amount without a currency" is the classic banking
defect. It normalises scale to the currency's minor units and refuses arithmetic
across currencies, so `USD + EUR` is a compile-time-shaped runtime failure rather
than a silently wrong balance.

`common-web` is imported explicitly by each service through its `WebConfig`
rather than component-scanned, because services scan only their own package. The
service therefore states which cross-cutting behaviour it opts into.

## Reliability decisions

These were driven by BootUI's own advisor findings plus normal review.

| Decision | Why |
|---|---|
| **Flyway owns the schema**, `ddl-auto: validate` | `ddl-auto: update` silently alters live databases. Validate turns drift into a startup failure. Fixes advisor `HIB-CONFIG-002` |
| **`SEQUENCE` ids with `allocationSize = 50`** | `IDENTITY` forces a flush per insert and disables JDBC batching entirely. Fixes `HIB-ID-001` |
| **JDBC batching + ordered inserts** | Fixes `HIB-CONFIG-004` |
| **Centralized `GlobalExceptionHandler`** | Fixes `RAPI-ERR-001`; also gives every error a `traceId` and a stable `code` |
| **Extends `ResponseEntityExceptionHandler`** | Without it, a plain `Exception` catch-all reports unknown paths as 500 and logs every stray probe at ERROR |
| **Pessimistic lock + deterministic lock order on transfer** | Two concurrent transfers on one account must serialise; ordering the locks by account number prevents A→B / B→A deadlock |
| **`CHECK (balance_amount >= 0)`** | The domain enforces it; the database enforces it again, so a future code path still cannot persist an overdrawn account |
| **Redis failures swallowed; Redis health check disabled** | The counter is a convenience, not a dependency. A Redis outage must not fail an account opening or get the service drained |
| **Kafka publish failures logged, not rethrown** | A broker outage must not fail customer registration |
| **Explicit HTTP client timeouts** | Without them a stalled ledger pins threads and turns one slow dependency into an outage |
| **Graceful shutdown, 20s drain** | In-flight requests finish during a rolling restart |
| **Bounded notification store** | The service consumes an unbounded stream; an unbounded store is a memory leak with a delay fuse |

## Known gap: no authentication

There is **no Spring Security** in this system. BootUI's `pentest_scan` reports
this as HIGH (`PT-A07-001`), plus two MEDIUM findings about unauthenticated
actuator mappings. Those findings are correct and have not been addressed —
adding an auth layer is an architectural decision, not a detail to slip in.

Do not deploy these services beyond a developer machine until it is taken.

## Adding a new service

1. Create `<name>-service/` at the repository root with a `build.gradle` that
   applies the two Spring plugins and depends on `:common-domain` and
   `:common-web`.
2. Add `include '<name>-service'` to `settings.gradle`.
3. Create the four layers under `com.springboot.<name>`.
4. Add a `WebConfig` that imports `GlobalExceptionHandler`.
5. Copy `ArchitectureTest` and change `BASE`.
6. Add `application-dev.yml` and `application-prod.yml` with the BootUI blocks
   from an existing service.

BootUI attaches itself: the root build script gives it to any module that applies
the Spring Boot plugin, and only under `-Pdev`. Nothing else to wire.
