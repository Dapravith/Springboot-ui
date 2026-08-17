# BootUI Development Guide

## Purpose

BootUI is a **developer diagnostics console**. It is embedded in each Spring Boot
application during local development so you can inspect the running process —
beans, mappings, configuration, SQL traces, exceptions, HTTP exchanges, memory —
without adding print statements or attaching a debugger.

BootUI is **not** the production observability platform. Production monitoring
stays with Micrometer, Prometheus, Grafana, OpenTelemetry and centralized
logging. Those systems are unaffected by anything in this document.

| | Development | Production |
|---|---|---|
| Tool | BootUI, per service, on loopback | Prometheus / Grafana / OTel / central logs |
| Lifetime | While a developer is debugging | Always on |
| Present in artifact | Only in `-Pdev` builds | Never |

## Architecture

BootUI is embedded **inside each microservice**. There is no central BootUI
instance, because each service owns its own JVM, its own bean graph and its own
connection pool — only an in-process console can see those.

```
                            Developer (localhost)
                                     │
        ┌──────────────┬─────────────┼──────────────┬──────────────┐
        ▼              ▼             ▼              ▼              ▼
   :8080/bootui   :8081/bootui  :8082/bootui   :8083/bootui  :8084/bootui
   api-gateway    customer-svc  account-svc    transfer-svc  notification-svc
        │              │             │              │              │
     BootUI         BootUI        BootUI         BootUI         BootUI
```

The API gateway deliberately has **no route** for `/bootui/**`. Its own console
at `:8080/bootui` belongs to the gateway process itself; it does not and must
not proxy the other services' consoles.

## Versions

| Component | Version | Notes |
|---|---|---|
| BootUI | 1.13.1 | Servlet starter `com.julien-dubois.bootui:bootui-spring-boot-starter` |
| Java | 25 | Pinned via Gradle toolchain, not `JAVA_HOME` |
| Spring Boot | 4.1.0 | |
| Spring Cloud | 2025.1.2 | Gateway (WebMVC/servlet variant) |
| Gradle | 9.7.0 | Groovy DSL. Wrapper pinned with a SHA-256 checksum. Java 25 needs Gradle ≥ 9.1 |
| Base package | `com.springboot` | Also the Gradle `group` |

Every service is on the **servlet** stack, so all of them use the servlet
starter. If a service is ever migrated to WebFlux it must instead use
`bootui-spring-boot-starter-reactive` — never both.

## Service matrix

| Service | Port | BootUI URL | MCP URL | DB | Kafka | Redis | BootUI |
|---|---|---|---|---|---|---|---|
| api-gateway | 8080 | http://localhost:8080/bootui | http://127.0.0.1:8080/bootui/api/mcp | – | – | – | Yes (dev) |
| customer-service | 8081 | http://localhost:8081/bootui | http://127.0.0.1:8081/bootui/api/mcp | `customer_db` | Producer | – | Yes (dev) |
| account-service | 8082 | http://localhost:8082/bootui | http://127.0.0.1:8082/bootui/api/mcp | `account_db` | Producer | Yes | Yes (dev) |
| transfer-service | 8083 | http://localhost:8083/bootui | http://127.0.0.1:8083/bootui/api/mcp | – | – | – | Yes (dev) |
| notification-service | 8084 | http://localhost:8084/bootui | http://127.0.0.1:8084/bootui/api/mcp | – | Consumer | – | Yes (dev) |
| **common-domain** | – | – | – | – | – | – | **No — shared kernel** |
| **common-web** | – | – | – | – | – | – | **No — shared kernel** |

### Kafka topics

| Topic | Producer | Consumer |
|---|---|---|
| `customer-registered-topic` | customer-service | notification-service |
| `account-opened-topic` | account-service | notification-service |

## How to enable BootUI

BootUI is attached by a single build flag:

```bash
./gradlew :customer-service:bootRun -Pdev
```

`-Pdev` does two things: it adds BootUI to the runtime classpath, and it sets
`spring.profiles.active=dev` so `application-dev.yml` is loaded.

Check which mode a build is in at any time:

```bash
./gradlew bootuiStatus -Pdev
```

## How to disable BootUI

Omit the flag. That is the whole mechanism:

```bash
./gradlew clean build
```

The resulting jars contain no BootUI classes. This is stronger than a
configuration switch — there is nothing present to re-enable. Verify it:

```bash
unzip -l customer-service/build/libs/customer-service-0.0.1-SNAPSHOT.jar | grep -c bootui
```

`0` is the expected answer for a production build.

## Configuration

Each service carries its own `src/main/resources/application-dev.yml`. The
configuration is intentionally duplicated per service rather than shared, so
changing one service's diagnostics cannot affect another.

```yaml
bootui:
  enabled: AUTO              # activates only for the profiles below
  enabled-profiles: dev,local
  path: /bootui
  allow-non-localhost: false # loopback only
  mask-secrets: true         # redact secret-like values
  read-only: true            # deliberate: BootUI's own default is false
  trust-container-gateway: AUTO
  mcp:
    enabled: OFF             # opt-in per debugging session
```

Two of these are worth calling out:

- **`read-only: true` is not BootUI's default.** BootUI ships with
  `read-only: false`. It is set explicitly here because this environment may be
  pointed at shared development or staging databases.
- **`enabled: AUTO` plus `-Pdev` are independent barriers.** Even if the profile
  were set wrongly, a production build has no BootUI on the classpath at all.

## How the Gradle isolation works

`build.gradle` (root):

```groovy
def developerMode = providers.gradleProperty('dev').present

subprojects {
    plugins.withId('org.springframework.boot') {   // runnable apps only
        if (developerMode) {
            dependencies { runtimeOnly bootuiCoordinates }
            tasks.named('bootRun') {
                systemProperty 'spring.profiles.active', 'dev'
            }
        }
    }
}
```

`plugins.withId` is the isolation mechanism. It fires only for modules applying
the Spring Boot plugin — the runnable applications. `common-domain` / `common-web` applies
`java-library` instead, so it can never receive BootUI, without needing to opt
out by hand. `runtimeOnly` keeps BootUI invisible to application source code.

## Running the system

Start infrastructure first (loopback-bound, non-standard ports so it will not
collide with other local Postgres/Kafka/Redis containers):

```bash
docker compose -f compose.yml up -d
```

| Service | Host port | Note |
|---|---|---|
| PostgreSQL | 127.0.0.1:55432 | databases `customer_db`, `account_db` |
| Redis | 127.0.0.1:56379 | |
| Kafka | 127.0.0.1:59092 | |

Then run whichever services you need, each in its own terminal:

```bash
./gradlew :api-gateway:bootRun -Pdev
```
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

Stop the infrastructure with:

```bash
docker compose -f compose.yml down
```

### Smoke test

```bash
curl -s -X POST -H 'Content-Type: application/json' -d '{"fullName":"Ada Lovelace","email":"ada@example.com"}' http://localhost:8081/api/v1/customers
```

### Port conflicts

Ports 8080–8084 are the documented allocation. If something else on your machine
already uses one of them, override it per run rather than editing the committed
configuration:

```bash
./gradlew :customer-service:bootRun -Pdev --args='--server.port=18081'
```

BootUI follows the server port automatically, so the console moves with it.

## Docker development

To run the services themselves in containers:

```bash
./gradlew bootJar -Pdev
```
```bash
docker compose -f compose.yml -f compose.dev.yml up --build
```

Two details make BootUI work in containers without weakening it:

- Every service port is published as `127.0.0.1:<port>:<port>`, so the consoles
  are reachable from this machine only, not from the local network.
- `BOOTUI_TRUST_CONTAINER_GATEWAY=AUTO` lets BootUI recognise the Docker bridge
  gateway as local. This is deliberately used **instead of**
  `bootui.allow-non-localhost=true`, which would switch off the source-address
  check entirely.

Because `docker/Dockerfile` copies a jar built on the host, a production image is
simply the same build without `-Pdev`.

## Claude Code MCP usage

BootUI can expose a local MCP endpoint so Claude Code can query the running
application directly. It is **off by default** and should be switched on only
while you are actively using it.

1. Start the service with MCP enabled:

```bash
./gradlew :customer-service:bootRun -Pdev --args='--bootui.mcp.enabled=ON'
```

2. Copy `.mcp.json.example` to `.mcp.json` and keep only the services you are
   debugging. `.mcp.json` is gitignored.

```json
{
  "servers": {
    "bootui-customer-service": {
      "type": "http",
      "url": "http://127.0.0.1:8081/bootui/api/mcp"
    }
  }
}
```

Each service is a separate MCP server on its own port; there is no aggregated
endpoint. Loopback clients need no token, so never put credentials in this file.

With MCP on, 21 tools are available, including `get_sql_traces`,
`get_exceptions`, `get_live_activity`, `get_mappings` and the advisor scans.

### Advisor scans and read-only

The advisor scans (`architecture_scan`, `spring_scan`, `hibernate_scan`,
`rest_api_scan`, `pentest_scan`, `memory_scan`, `graalvm_scan`, `crac_scan`) are
classified by BootUI as **actions**, not reads. With `read-only: true` they are
refused:

```
BootUI is read-only via bootui.read-only=true
```

This is the one place where the safe default gets in the way. Treat it as a
documented, temporary exception:

| | |
|---|---|
| **Action** | Run BootUI advisor scans |
| **Reason** | Scans are registered as action-capable operations |
| **Risk** | Low. The scans analyse the running context and do not write to the database. However, disabling read-only also unblocks every other action-capable panel for that process |
| **Scope** | One service, one process, one debugging session. Never a shared or staging deployment |
| **How to enable** | `./gradlew :customer-service:bootRun -Pdev --args='--bootui.mcp.enabled=ON --bootui.read-only=false'` |
| **How to disable** | Stop the process. Nothing is persisted; the committed default stays `read-only: true` |

Do not set `read-only: false` in `application-dev.yml`.

Advisor output is a set of **review prompts, not verdicts** — BootUI says so
itself in every scan payload. Verify each finding against the code before acting
on it, and never apply a suggested Hibernate or schema change blindly.

## Security considerations

- **Loopback only.** `allow-non-localhost: false`. A request from the machine's
  LAN address is rejected with HTTP 403; the business API is unaffected.
- **Read-only by default.** Mutating panel actions return HTTP 403.
- **Secrets masked.** `mask-secrets: true`. `spring.datasource.password` and
  similar keys render as `******`.
- **MCP off by default.** Disabled endpoints answer with JSON-RPC error -32000.
- **No gateway exposure.** No `/bootui/**` route exists in the gateway.
- **Bearer token.** BootUI prints a token at startup for non-loopback API access.
  It is regenerated each run. Never commit it, and never use it to open BootUI
  to a network.
- **Actuator untouched.** Exposure stays `health,info,metrics`. BootUI does not
  need broader actuator exposure and none was granted.

### Known gap: no Spring Security

This project has **no authentication layer**. BootUI's `pentest_scan` reports
this as HIGH (`PT-A07-001`), plus two MEDIUM findings about unauthenticated
actuator mappings. Those findings are correct. Adding Spring Security is an
architectural decision that has not been taken here; do not deploy these
services beyond a developer machine until it is.

## Production restrictions

- BootUI must never appear in a production artifact. The `-Pdev` mechanism is
  the control; the jar audit above is the check.
- `application-prod.yml` additionally pins `bootui.enabled: OFF` as a backstop.
- Never set `allow-non-localhost: true` or `trusted-proxies` on a deployed
  environment.
- Never expose `/bootui` or `/bootui/api/mcp` through ingress.
- A production build that contains BootUI classes is a release blocker.

## Troubleshooting

**`/bootui` returns 404 in development.** The build was run without `-Pdev`, so
the dependency is absent. Confirm with `./gradlew bootuiStatus -Pdev`.

**`/bootui` returns 403.** You are reaching the service over a non-loopback
address. Use `localhost`/`127.0.0.1`. In Docker, ensure
`BOOTUI_TRUST_CONTAINER_GATEWAY=AUTO` is set rather than relaxing
`allow-non-localhost`.

**A BootUI action returns 403 `"BootUI is read-only"`.** Expected. See the
advisor-scan exception above.

**MCP returns `-32000 "BootUI MCP server is disabled"`.** Start the service with
`--bootui.mcp.enabled=ON`.

**`Port 8081 was already in use`.** Another process holds the port. Override with
`--args='--server.port=18081'`.

**`Unable to determine Dialect without JDBC metadata`.** The datasource could not
connect — usually PostgreSQL is not running. Start it with
`docker compose -f compose.yml up -d`.

**No `KafkaTemplate` bean.** Spring Boot 4 moved Kafka auto-configuration into
`spring-boot-starter-kafka`. Depending on `org.springframework.kafka:spring-kafka`
alone gives the library without the auto-configuration.
