package com.springboot.common.audit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable entry in the audit trail: who did what, to which resource, when,
 * and whether it worked.
 *
 * <p>Framework-free by design - this is shared-kernel domain, so it can be
 * constructed and asserted on in a plain unit test.
 *
 * @param eventId      unique id for this entry, so a specific line can be quoted
 * @param occurredAt   when the audited action happened
 * @param service      which service produced the entry
 * @param action       what was attempted, UPPER_SNAKE_CASE (e.g. {@code ACCOUNT_OPENED})
 * @param resourceType the kind of thing acted on (e.g. {@code Account})
 * @param resourceId   the business identifier of that thing, never a surrogate key
 * @param outcome      success or failure
 * @param actor        who performed it; see {@link #actor()} for the trust caveat
 * @param traceId      correlates this entry with logs and traces for the same request
 * @param reason       why a failure happened; null on success
 * @param attributes   extra context, flattened to strings so the line stays greppable
 */
public record AuditEvent(
        String eventId,
        Instant occurredAt,
        String service,
        String action,
        String resourceType,
        String resourceId,
        AuditOutcome outcome,
        String actor,
        String traceId,
        String reason,
        Map<String, String> attributes) {

    public AuditEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(outcome, "outcome");
        attributes = (attributes == null) ? Map.of() : Map.copyOf(new LinkedHashMap<>(attributes));
    }

    /**
     * The actor recorded against this entry.
     *
     * <p><strong>This value is only as trustworthy as the authentication in front
     * of it.</strong> This system currently has none, so the actor is taken from a
     * request header and must be treated as a claim, not as proof. See
     * {@code docs/audit-logging.md}.
     */
    @Override
    public String actor() {
        return actor;
    }
}
