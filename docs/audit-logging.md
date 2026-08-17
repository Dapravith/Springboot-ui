# Audit Logging

## What this is

An append-only record of who did what, to which resource, when, and whether it
worked — written by **every service** into one shared directory, and readable as
a single live stream across the whole system.

It is deliberately **not** the application log. Application logs are for
debugging and get rotated aggressively; the audit trail answers "what happened
to this account, and who did it" months later.

| | Application log | Audit trail |
|---|---|---|
| Destination | console / platform collector | `logs/audit-<service>.log` |
| Format | human-readable text | JSON Lines, one object per line |
| Contents | whatever helps debug | significant business actions only |
| Retention | short | 30 days rolling, 2 GB cap — raise for your regulator |
| Failures included | incidentally | **always**, refused attempts included |

## Tail every service at once

```bash
./scripts/tail-audit.py
```

That follows every `logs/audit-*.log`, colour-codes by service, and merges the
entries in **timestamp order** rather than file order — the whole point being to
see the sequence across services.

```
03:13:02.770  transfer-service  OK    TRANSFER_SUBMITTED  Transfer:85f75fa4…  actor=order-test  amount=11.00 currency=USD  trace=fce52383…
03:13:02.781  transfer-service  FAIL  TRANSFER_POSTED     Transfer:85f75fa4…  actor=order-test  reason=Ledger is unreachable  trace=fce52383…
03:13:02.787  api-gateway       OK    API_CALL_ROUTED     ApiRequest:/api/v1/transfers  status=201 method=POST durationMs=55  trace=fce52383…
```

The shared `trace` value is what ties those three lines to one request.

### Options

| Command | Does |
|---|---|
| `./scripts/tail-audit.py` | follow everything, after replaying the last 20 entries |
| `./scripts/tail-audit.py -n 200` | replay more history first |
| `./scripts/tail-audit.py -n 0` | live entries only, no replay |
| `./scripts/tail-audit.py --no-follow` | print and exit (scripting, CI) |
| `./scripts/tail-audit.py --failures` | only refused or failed attempts |
| `./scripts/tail-audit.py -s account-service` | one service |
| `./scripts/tail-audit.py -a FUNDS_TRANSFERRED` | one action |
| `./scripts/tail-audit.py --actor teller-42` | one actor |
| `./scripts/tail-audit.py --raw` | original JSON lines, for piping into `jq` |
| `./scripts/tail-audit.py -d /path/to/logs` | a different directory |

Standard library Python only — nothing to install. It survives log rotation and
in-place truncation, and prints unparseable lines rather than hiding them,
because a corrupt entry in an audit trail is itself worth seeing.

## Why all services share one directory

Every service writes to `${AUDIT_LOG_DIR:-./logs}/audit-<service>.log`, and the
root build sets `bootRun`'s working directory to the repository root:

```groovy
tasks.named('bootRun') {
    workingDir = rootProject.projectDir
}
```

Without that, each service would write `logs/` beside its own module and there
would be no single place to tail from. For jars or containers, set
`AUDIT_LOG_DIR` to a shared volume.

## Entry format

```json
{
  "eventId": "0d29d477-f07c-4eb0-abd7-a5047189b91a",
  "occurredAt": "2026-08-17T03:11:25.137956Z",
  "service": "transfer-service",
  "action": "TRANSFER_SUBMITTED",
  "resourceType": "Transfer",
  "resourceId": "36d3ec8b-f72f-41ec-91a1-b45c02d810da",
  "outcome": "SUCCESS",
  "actor": "teller-42",
  "traceId": "b56cb42de1d171bad707f38da4a701bd",
  "reason": null,
  "attributes": { "from": "ACC…001", "to": "ACC…002", "amount": "250.00", "currency": "USD" }
}
```

`resourceId` is always the **business** identifier (account number, customer
number), never a database surrogate key — a trail keyed on primary keys is
useless after a migration.

Serialisation goes through Jackson, not string concatenation. A value containing
a quote or a newline therefore cannot split one entry into two: in a
line-delimited append-only trail that would be a tampering vector, and there is
a test asserting it.

## What each service records

| Service | Action | When |
|---|---|---|
| api-gateway | `API_CALL_ROUTED` | every `/api/**` call, with method, status, duration; 4xx/5xx recorded as FAILURE |
| customer-service | `CUSTOMER_REGISTERED` | success, and refusal on duplicate email |
| account-service | `ACCOUNT_OPENED` | account opened |
| account-service | `FUNDS_TRANSFERRED` | success, and every domain refusal with its code |
| transfer-service | `TRANSFER_SUBMITTED` | accepted by policy, or refused with the reason |
| transfer-service | `TRANSFER_POSTED` | ledger accepted, or failed with the ledger's message |
| notification-service | `EVENT_CONSUMED` | each Kafka event received |

Actuator, health checks and the gateway's own BootUI console are **not** audited.
Health polling every few seconds would bury the entries that matter.

## Design decisions

### Failures are recorded, not just successes

A trail containing only successes cannot answer the question auditors actually
ask: who *tried* to do something they were not allowed to do. Every refusal —
over-limit transfer, duplicate email, insufficient funds — produces a `FAILURE`
entry carrying the stable domain code as its `reason`.

### A SUCCESS entry never outlives a rollback

Writing to a log file is not transactional. An audit call inside a
`@Transactional` method records `SUCCESS` immediately, and if that transaction
later rolls back — an optimistic-lock clash, a constraint violation at flush —
the entry survives and claims something happened that did not.

`TransactionAwareAuditLogger` fixes that by treating the two outcomes
differently:

- **SUCCESS** is deferred to after commit. No commit, no claim.
- **FAILURE** is written immediately, because a refused attempt normally rolls
  its transaction back and is exactly what an investigator needs.

Services with no transaction manager on the classpath (api-gateway,
transfer-service, notification-service) write directly, which is correct for
them: there is no commit that could later be undone.

### Audit lives in the application layer

Not in the controller (which does not know the outcome) and not in the domain
(which must stay framework-free). The application service knows what was
attempted and how it ended, so that is where the call belongs.

### An audit write never fails a business operation

`Slf4jAuditLogger` catches serialisation errors and logs them at `ERROR` rather
than rethrowing. Losing an entry is an incident; failing a completed banking
operation because of a logging problem would be worse.

## Actor: read this before trusting it

The actor comes from the `X-Actor-Id` request header.

> **This system has no authentication. The actor is an unverified claim, not
> proof of identity.** Anyone who can reach the API can send any value.

It is recorded anyway, because a claimed actor plus a trace id is far more useful
during an investigation than nothing at all — but it must not be treated as
identity, and this trail is **not** sufficient for a compliance regime that
requires attributable actions.

When Spring Security is added, `AuditActorFilter` should read the authenticated
principal and ignore the header. That is the one change needed to make this trail
trustworthy.

Requests with no header are recorded as `anonymous`.

## Configuration

```yaml
audit:
  enabled: ${AUDIT_ENABLED:true}
  actor-header: ${AUDIT_ACTOR_HEADER:X-Actor-Id}
```

| Setting | Default | Notes |
|---|---|---|
| `audit.enabled` | `true` | Turning the trail off is a deliberate, visible act |
| `audit.actor-header` | `X-Actor-Id` | See the caveat above |
| `AUDIT_LOG_DIR` | `./logs` | Point at a shared volume in Docker |

Rotation lives in `common-audit/src/main/resources/com/springboot/common/audit/audit-appender.xml`
— 50 MB per file, 30 days, 2 GB cap, gzipped into `logs/archive/`. Defined once
so services cannot drift apart on retention.

## Adding an audited action to a service

1. Inject `AuditLogger` into the application service.
2. Call `audit.success(...)` / `audit.failure(...)` at the point the outcome is
   known.
3. Use `UPPER_SNAKE_CASE` for the action and a business identifier for
   `resourceId`.

```java
audit.success(ACTION_OPEN, "Account", saved.accountNumber().value(), Map.of(
        "customerNumber", saved.customerNumber(),
        "openingBalance", saved.balance().amount().toPlainString()));
```

In tests, use the shared fixture rather than a new fake:

```groovy
testImplementation testFixtures(project(':common-audit'))
```

```java
private final RecordingAuditLogger audit = new RecordingAuditLogger();
...
assertEquals(1, audit.failures().size());
assertEquals("DUPLICATE_EMAIL", audit.failures().getFirst().reason());
```

## Known limitations

- **The actor is unverified.** See above. This is the big one.
- **The trail is local files.** Nothing ships it off the machine, so it is as
  durable as the host. Production would forward it to append-only storage the
  services themselves cannot rewrite.
- **Not tamper-evident.** No hash chaining or signing; anyone with file access
  can edit history.
- **Cross-service ordering is exact within a 250 ms poll window.** An entry
  flushed late by one service can appear after a newer entry from another. Sort
  by `occurredAt` if you need strict order.
- **`notification-service` audits every consumed event**, which is fine at
  development volume and would need sampling or removal under real load.
