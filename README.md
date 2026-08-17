# Banking Microservices with BootUI

A local banking microservices environment built with Java 25, Spring Boot 4,
Gradle, PostgreSQL, Redis, Kafka, and Spring Cloud Gateway. Each runnable service
includes a development-only [BootUI](https://www.julien-dubois.com/boot-ui/)
diagnostics console.

> This project currently has no authentication layer. It is intended for local
> development only and must not be exposed to a public or shared network.

## Contents

- [System overview](#system-overview)
- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Gradle commands](#gradle-commands)
- [Manual startup](#manual-startup)
- [Containerized startup](#containerized-startup)
- [Service URLs](#service-urls)
- [API smoke test](#api-smoke-test)
- [Configuration](#configuration)
- [Development workflow](#development-workflow)
- [Implementation guidelines](#implementation-guidelines)
- [Adding a new service](#adding-a-new-service)
- [Testing](#testing)
- [Production build](#production-build)
- [Stopping and cleaning](#stopping-and-cleaning)
- [Troubleshooting](#troubleshooting)
- [Further documentation](#further-documentation)
- [References](#references)

## System overview

The repository is a Gradle multi-project build. The Gradle wrapper exists only
at the repository root; individual service directories do not contain their own
`gradlew` files.

| Module | Port | Responsibility | Runtime dependencies |
|---|---:|---|---|
| `api-gateway` | 8080 | Routes business APIs to services | All application services |
| `customer-service` | 8081 | Customer registration and queries | PostgreSQL, Kafka |
| `account-service` | 8082 | Accounts, balances, and ledger updates | PostgreSQL, Redis, Kafka |
| `transfer-service` | 8083 | Transfer policy and orchestration | Account service |
| `notification-service` | 8084 | Consumes domain events and exposes recent notifications | Kafka |
| `common-domain` | — | Shared domain primitives and errors | None |
| `common-web` | — | API envelope and centralized error handling | `common-domain` |
| `common-audit` | — | Shared structured audit logging | `common-domain` |

Local infrastructure uses deliberately non-standard, loopback-only ports:

| Infrastructure | Host address | Purpose |
|---|---|---|
| PostgreSQL | `127.0.0.1:55432` | `customer_db` and `account_db` |
| Redis | `127.0.0.1:56379` | Non-critical account metrics |
| Kafka | `127.0.0.1:59092` | Customer and account domain events |

Repository layout:

```text
.
├── api-gateway/
├── customer-service/
├── account-service/
├── transfer-service/
├── notification-service/
├── common-domain/
├── common-web/
├── common-audit/
├── docker/
├── docs/
├── compose.yml            # PostgreSQL, Redis, and Kafka
├── compose.dev.yml        # Optional containerized application services
├── build.gradle           # Shared Gradle conventions
├── settings.gradle        # Module registration
├── gradlew                 # Run this wrapper from the repository root
└── run-all.sh              # Build and run the complete local system
```

## Prerequisites

Install the following before starting:

1. **JDK 25**
2. **Docker Desktop** or Docker Engine with the Compose plugin
3. **curl** for health checks and examples
4. At least **2 GB of free JVM heap** for the Gradle build

Verify the required tools:

```bash
java -version
docker --version
docker compose version
curl --version
```

The Java version should report 25. Start Docker Desktop before running the
project. On macOS:

```bash
open -a Docker
```

Wait until Docker reports that its engine is running:

```bash
docker info
```

## Quick start

Open a terminal and move to the repository root:

```bash
cd /path/to/BootUI
```

Run the complete system:

```bash
./run-all.sh
```

The script performs these steps:

1. Checks Java, curl, Docker, the Gradle wrapper, and ports 8080–8084.
2. Starts PostgreSQL, Redis, and Kafka from `compose.yml`.
3. Waits for the infrastructure health checks.
4. Runs `./gradlew clean build -Pdev` to compile and test every module.
5. Starts the five application JARs with the `dev` Spring profile.
6. Waits for every `/actuator/health` endpoint.
7. Keeps the services running until you press `Ctrl+C`.

Application logs are written separately under:

```text
logs/run-all/
```

Useful script options:

```bash
./run-all.sh --help
./run-all.sh --skip-build
./run-all.sh --skip-infra
./run-all.sh --stop-infra-on-exit
```

- `--skip-build` reuses existing JARs in each module's `build/libs` directory.
  Build those JARs with `-Pdev` first if BootUI is required.
- `--skip-infra` is only appropriate when compatible PostgreSQL, Redis, and
  Kafka instances are already available.
- `--stop-infra-on-exit` stops the Compose infrastructure when the script exits.

Increase the service startup timeout when working on a slower machine:

```bash
STARTUP_TIMEOUT=180 ./run-all.sh
```

## Gradle commands

Always run the wrapper from the repository root:

```bash
./gradlew tasks
./gradlew clean build
./gradlew clean build -Pdev
./gradlew test
```

Build or test one service:

```bash
./gradlew :customer-service:build
./gradlew :account-service:test
./gradlew :transfer-service:bootJar -Pdev
```

Build all runnable JARs:

```bash
./gradlew bootJar -Pdev
```

If the terminal is already inside a service directory, call the root wrapper
through the parent directory:

```bash
../gradlew build
```

Prefer running from the root with an explicit task path because it clearly
identifies the target module:

```bash
./gradlew :account-service:build
```

## Manual startup

Use this workflow when debugging services in separate terminals.

### 1. Start local infrastructure

```bash
docker compose -f compose.yml up -d --wait
```

Check its state:

```bash
docker compose -f compose.yml ps
```

### 2. Build the project

```bash
./gradlew clean build -Pdev
```

### 3. Run each service

Open a separate terminal for each command and execute it from the repository
root:

```bash
./gradlew :customer-service:bootRun -Pdev
```

```bash
./gradlew :account-service:bootRun -Pdev
```

```bash
./gradlew :transfer-service:bootRun -Pdev
```

```bash
./gradlew :notification-service:bootRun -Pdev
```

```bash
./gradlew :api-gateway:bootRun -Pdev
```

The `-Pdev` Gradle property attaches the BootUI runtime dependency and causes
`bootRun` to activate the `dev` Spring profile.

To run only one service, start its required infrastructure and execute only its
`bootRun` task. For example:

```bash
docker compose -f compose.yml up -d postgres kafka
./gradlew :customer-service:bootRun -Pdev
```

## Containerized startup

To run the application services as containers instead of host JVM processes:

```bash
./gradlew clean bootJar -Pdev
docker compose -f compose.yml -f compose.dev.yml up --build
```

The Docker images copy the JARs built by Gradle. The development overlay binds
all application ports to `127.0.0.1`, activates the `dev` profile, and connects
the services to their containerized dependencies.

Stop this stack with:

```bash
docker compose -f compose.yml -f compose.dev.yml down
```

## Service URLs

| Service | Business API | Health | BootUI |
|---|---|---|---|
| API gateway | `http://localhost:8080` | `http://localhost:8080/actuator/health` | `http://localhost:8080/bootui` |
| Customer | `http://localhost:8081/api/v1/customers` | `http://localhost:8081/actuator/health` | `http://localhost:8081/bootui` |
| Account | `http://localhost:8082/api/v1/accounts` | `http://localhost:8082/actuator/health` | `http://localhost:8082/bootui` |
| Transfer | `http://localhost:8083/api/v1/transfers` | `http://localhost:8083/actuator/health` | `http://localhost:8083/bootui` |
| Notification | `http://localhost:8084/api/v1/notifications` | `http://localhost:8084/actuator/health` | `http://localhost:8084/bootui` |

Business APIs can be called through the gateway on port 8080. Health and BootUI
URLs belong to the individual services and are intentionally not proxied by the
gateway.

Check all health endpoints:

```bash
for port in 8080 8081 8082 8083 8084; do
  curl --fail "http://localhost:${port}/actuator/health"
  echo
done
```

## API smoke test

The following examples use the gateway at `http://localhost:8080`.

### 1. Register a customer

```bash
curl --request POST \
  --header 'Content-Type: application/json' \
  --data '{"fullName":"Ada Lovelace","email":"ada@example.com"}' \
  http://localhost:8080/api/v1/customers
```

Copy `data.customerNumber` from the response. It has a format such as
`CUS0000000001`.

### 2. Open two accounts

Replace `CUS0000000001` with the customer number returned above:

```bash
curl --request POST \
  --header 'Content-Type: application/json' \
  --data '{"customerNumber":"CUS0000000001","openingBalance":1000.00,"currency":"USD"}' \
  http://localhost:8080/api/v1/accounts
```

```bash
curl --request POST \
  --header 'Content-Type: application/json' \
  --data '{"customerNumber":"CUS0000000001","openingBalance":100.00,"currency":"USD"}' \
  http://localhost:8080/api/v1/accounts
```

Copy each returned `data.accountNumber`.

### 3. Submit a transfer

Replace the sample account numbers with the two values returned above:

```bash
curl --request POST \
  --header 'Content-Type: application/json' \
  --data '{"fromAccountNumber":"ACC000000000001","toAccountNumber":"ACC000000000002","amount":25.00,"currency":"USD"}' \
  http://localhost:8080/api/v1/transfers
```

### 4. Inspect the result

```bash
curl http://localhost:8080/api/v1/accounts
curl http://localhost:8080/api/v1/transfers
curl http://localhost:8080/api/v1/notifications
```

Kafka delivery is asynchronous, so a notification may appear shortly after the
customer or account request completes.

## Configuration

Configuration is stored in each service under `src/main/resources`:

- `application.yml` contains shared defaults and local dependency addresses.
- `application-dev.yml` enables the local BootUI configuration.
- `application-prod.yml` explicitly disables BootUI as a safety backstop.

Important environment overrides:

| Variable | Used by | Default |
|---|---|---|
| `DB_URL` | Customer, account | Service-specific PostgreSQL JDBC URL |
| `DB_USERNAME` | Customer, account | `dev_user` |
| `DB_PASSWORD` | Customer, account | `dev_password` |
| `KAFKA_BOOTSTRAP_SERVERS` | Customer, account, notification | `localhost:59092` |
| `REDIS_HOST` | Account | `localhost` |
| `REDIS_PORT` | Account | `56379` |
| `ACCOUNT_SERVICE_URL` | Transfer, gateway | `http://localhost:8082` |
| `CUSTOMER_SERVICE_URL` | Gateway | `http://localhost:8081` |
| `TRANSFER_SERVICE_URL` | Gateway | `http://localhost:8083` |
| `NOTIFICATION_SERVICE_URL` | Gateway | `http://localhost:8084` |
| `AUDIT_ENABLED` | All services | `true` |

Override a value for one run without editing committed configuration:

```bash
DB_URL='jdbc:postgresql://localhost:55432/customer_db' \
  ./gradlew :customer-service:bootRun -Pdev
```

Override a Spring property with `bootRun` arguments:

```bash
./gradlew :customer-service:bootRun -Pdev \
  --args='--server.port=18081'
```

Do not commit real credentials. Local `.env` files are ignored by Git.

## Development workflow

Use this sequence for normal feature work:

1. Start infrastructure with `docker compose -f compose.yml up -d --wait`.
2. Run the affected module's tests.
3. Implement the smallest complete change through the required architecture
   layers.
4. Run the affected service with `bootRun -Pdev`.
5. Exercise the endpoint through the gateway and inspect the service's BootUI.
6. Run `./gradlew test` to catch cross-module regressions.
7. Run `./gradlew clean build` before producing a production artifact.

Example inner loop for account-service:

```bash
./gradlew :account-service:test
./gradlew :account-service:bootRun -Pdev
```

Tail the structured audit logs while exercising APIs:

```bash
python3 scripts/tail-audit.py
```

See `docs/audit-logging.md` for filtering options and the event format.

## Implementation guidelines

Each business service follows Clean Architecture. Dependencies point inward:

```text
interfaces/rest ───────┐
                      v
infrastructure ──> application ──> domain
```

The domain must not depend on Spring, JPA, Kafka, Redis, Jackson, HTTP, or other
framework details.

### Implementing a new feature

Follow these steps in order:

1. **Define the domain behavior.** Add or update domain models, value objects,
   invariants, and domain exceptions under `domain/model`.
2. **Define inbound ports.** Add a use-case interface under `domain/port/in`.
   Commands should contain the values required by the use case, not web DTOs.
3. **Define outbound ports.** Add repository, event, clock, identity, or client
   interfaces under `domain/port/out` only when the use case needs external I/O.
4. **Implement orchestration.** Add an application service under `application`.
   It coordinates domain behavior and ports and owns transaction boundaries.
5. **Implement driven adapters.** Put JPA, Kafka, Redis, and HTTP client
   implementations under `infrastructure`. Keep framework annotations here.
6. **Implement the driving adapter.** Add the controller and request/response
   DTOs under `interfaces/rest`. Controllers translate wire formats and call
   inbound ports; they must not contain business rules or access repositories.
7. **Add configuration.** Register adapters through the service's configuration
   package and use environment-driven properties for external addresses and
   credentials.
8. **Add database migrations.** Put Flyway scripts under
   `src/main/resources/db/migration` using names such as
   `V2__add_transfer_status.sql`. Never use Hibernate schema updates.
9. **Add audit events.** Record important attempts, successes, and failures at
   the application boundary. Follow `docs/audit-logging.md`.
10. **Test from the inside out.** Test domain invariants first, then application
    behavior with fakes, adapter mapping, validation, and architecture rules.
11. **Run the service locally.** Use `bootRun -Pdev`, verify its health endpoint,
    call the API, and inspect BootUI and logs.
12. **Run the complete build.** Execute `./gradlew clean build` before handing
    off the change.

### Layer rules

- Domain classes must remain plain Java.
- Application services depend on port interfaces, not concrete adapters.
- Controllers depend on inbound ports, never persistence implementations.
- Persistence entities stay inside their infrastructure package and are mapped
  explicitly to domain objects.
- Request and response DTOs are separate from domain models.
- Shared code belongs in a common module only when multiple services genuinely
  share the same stable concept.
- A service owns its data. Do not query another service's database directly.
- Cross-service synchronous calls require explicit timeouts.
- Kafka consumers must be idempotent because messages can be delivered again.
- Never log passwords, tokens, full credentials, or unmasked personal data.

### Database changes

Flyway owns relational schemas and Hibernate validates them at startup.

1. Add a new forward-only migration instead of editing a migration already used
   by another developer or environment.
2. Keep a service's schema changes within that service.
3. Update the JPA entity and mapper explicitly.
4. Add repository or integration coverage for the new behavior.
5. Start against an existing local database to confirm that migration from the
   previous schema succeeds.

### API changes

- Keep endpoints under `/api/v1/<resource>`.
- Use Jakarta validation on HTTP request DTOs.
- Return the shared `ApiResponse<T>` envelope.
- Let `GlobalExceptionHandler` produce consistent errors; do not duplicate
  exception mapping in controllers.
- Add the gateway route when introducing a new business API prefix.
- Treat incompatible request or response changes as a new API version.

## Adding a new service

Use the following checklist when creating another deployable service:

1. Create `<name>-service/` at the repository root.
2. Add `include '<name>-service'` to `settings.gradle`.
3. Add a module `build.gradle` applying the Spring Boot and dependency-management
   plugins.
4. Add explicit dependencies on only the common modules the service needs.
5. Create the package root `com.springboot.<name>` with `domain`, `application`,
   `infrastructure`, and `interfaces/rest` packages.
6. Add a Spring Boot application class.
7. Add `application.yml`, `application-dev.yml`, and `application-prod.yml`.
8. Allocate a unique server port and document it in this README.
9. Copy an existing `ArchitectureTest`, change its base package, and keep the
   test non-vacuous.
10. If persistence is needed, add a service-owned database, Flyway migrations,
    Compose configuration, and environment variables.
11. If messaging is needed, document the topic, payload contract, producer, and
    consumer ownership.
12. Add the service's route to `api-gateway/src/main/resources/application.yml`.
13. Add the service and port to the `SERVICES` and `PORTS` arrays in
    `run-all.sh`.
14. Add the container definition to `compose.dev.yml` when containerized local
    execution is required.
15. Run the module tests, the architecture test, and the complete build.

The root build automatically attaches BootUI to modules applying the Spring
Boot plugin, but only when Gradle is invoked with `-Pdev`.

## Testing

Run the complete test suite:

```bash
./gradlew test
```

Run one module:

```bash
./gradlew :customer-service:test
```

Run one test class:

```bash
./gradlew :account-service:test --tests '*AccountTest'
```

Run architecture enforcement:

```bash
./gradlew :account-service:test --tests '*ArchitectureTest'
./gradlew :customer-service:test --tests '*ArchitectureTest'
```

Run a clean verification build:

```bash
./gradlew clean build
```

Gradle parallel execution and build caching are enabled in `gradle.properties`.

## Production build

Developer build with BootUI attached:

```bash
./gradlew clean build -Pdev
```

Production build without BootUI:

```bash
./gradlew clean build
```

Confirm the current Gradle invocation mode:

```bash
./gradlew bootuiStatus
./gradlew bootuiStatus -Pdev
```

Verify that a production JAR does not contain BootUI:

```bash
unzip -l account-service/build/libs/account-service-0.0.1-SNAPSHOT.jar \
  | grep -c bootui
```

The expected count is `0`. A production artifact containing BootUI is a release
blocker.

## Stopping and cleaning

When using `run-all.sh`, press `Ctrl+C` to stop all application JVMs.

Stop infrastructure while preserving database and Kafka volumes:

```bash
docker compose -f compose.yml stop
```

Stop and remove infrastructure containers while preserving named volumes:

```bash
docker compose -f compose.yml down
```

Remove Gradle build outputs:

```bash
./gradlew clean
```

To completely reset local PostgreSQL and Kafka data, remove the named volumes:

```bash
docker compose -f compose.yml down --volumes
```

> Warning: `down --volumes` permanently deletes the local databases and Kafka
> data for this Compose project.

## Troubleshooting

### `zsh: no such file or directory: ./gradlew`

The command is being run outside the repository root or from inside a service
directory. Use:

```bash
cd /path/to/BootUI
./gradlew build
```

From a service directory, use `../gradlew`, although root task paths are clearer.

### `Permission denied: ./gradlew`

Restore executable permission:

```bash
chmod +x gradlew run-all.sh
```

### `Docker is not running`

Start Docker Desktop, wait for its engine, and retry:

```bash
open -a Docker
docker info
./run-all.sh
```

### A port is already in use

Identify the process on a service port:

```bash
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

Stop the existing process or override the port for a manual run:

```bash
./gradlew :customer-service:bootRun -Pdev \
  --args='--server.port=18081'
```

When changing a service port, also update the corresponding gateway URL for
that run.

### PostgreSQL connection or Hibernate dialect error

Confirm PostgreSQL is healthy:

```bash
docker compose -f compose.yml ps postgres
docker compose -f compose.yml logs postgres
```

The default host URL is `jdbc:postgresql://localhost:55432/<database>`.

### Service does not become healthy

Inspect its dedicated startup log:

```bash
tail -n 100 logs/run-all/customer-service.log
```

Also check infrastructure:

```bash
docker compose -f compose.yml ps
docker compose -f compose.yml logs --tail=100
```

### `/bootui` returns 404

The service was built or started without `-Pdev`. Verify developer mode:

```bash
./gradlew bootuiStatus -Pdev
```

Then rebuild and run the service with `-Pdev`.

### `/bootui` returns 403

Use `localhost` or `127.0.0.1`. BootUI intentionally rejects non-loopback
clients by default.

### BootUI reports read-only mode

This is the expected safe default. See `docs/bootui-development.md` before
temporarily enabling an action-capable feature. Do not commit
`bootui.read-only=false`.

### Gradle cannot find Java 25

Confirm the active JDK:

```bash
java -version
/usr/libexec/java_home -V
```

On macOS, select JDK 25 for the current terminal:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
```

## Further documentation

- [`docs/architecture.md`](docs/architecture.md) — dependency rules, service
  structure, shared modules, and reliability decisions.
- [`docs/bootui-development.md`](docs/bootui-development.md) — BootUI profiles,
  Docker behavior, MCP usage, security controls, and production restrictions.
- [`docs/audit-logging.md`](docs/audit-logging.md) — audit format, configuration,
  actor limitations, and implementation guidance.

## References

- [BootUI — Spring Boot application management and monitoring](https://www.julien-dubois.com/boot-ui/)

## Security status

There is no Spring Security configuration in this project. Business endpoints
and exposed actuator endpoints are unauthenticated. Keep all ports bound to
localhost, never route BootUI through an ingress or public gateway, never commit
credentials, and do not deploy this system beyond a developer machine until an
authentication and authorization design has been implemented.
