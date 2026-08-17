package com.springboot.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Writes each audit entry as one line of JSON to a dedicated {@code AUDIT} logger.
 *
 * <p>One JSON object per line (JSONL) is the format on purpose: it stays
 * greppable and tailable with ordinary tools, while remaining machine-readable
 * for whatever ingests it later. Logback routes the {@code AUDIT} logger to its
 * own rolling file, separate from the application log, so the trail is not
 * interleaved with debug noise.
 *
 * <p>Serialisation goes through Jackson rather than string concatenation, so a
 * value containing a quote or a newline cannot break the line structure - which
 * for an append-only audit trail would be a tampering vector, not just a bug.
 */
public class Slf4jAuditLogger implements AuditLogger {

    /** Logger name that {@code audit-appender.xml} binds to the audit file. */
    public static final String AUDIT_LOGGER_NAME = "AUDIT";

    private static final Logger auditLog = LoggerFactory.getLogger(AUDIT_LOGGER_NAME);
    private static final Logger log = LoggerFactory.getLogger(Slf4jAuditLogger.class);

    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final boolean enabled;

    public Slf4jAuditLogger(String serviceName, boolean enabled) {
        this(serviceName, enabled, JsonMapper.builder().build());
    }

    public Slf4jAuditLogger(String serviceName, boolean enabled, ObjectMapper objectMapper) {
        this.serviceName = serviceName;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(String action, String resourceType, String resourceId, AuditOutcome outcome,
                       String reason, Map<String, String> attributes) {
        if (!enabled) {
            return;
        }

        AuditEvent event = new AuditEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                serviceName,
                action,
                resourceType,
                resourceId,
                outcome,
                AuditContext.actor(),
                AuditContext.traceId(),
                reason,
                attributes);

        write(event);
    }

    private void write(AuditEvent event) {
        try {
            auditLog.info(objectMapper.writeValueAsString(event));
        } catch (JacksonException ex) {
            // Never rethrow: the audited business operation has already happened,
            // and failing it now would be worse than a gap in the trail. But the
            // gap itself is an incident, so it is logged at error.
            log.error("AUDIT WRITE FAILED for action={} resource={}:{} - entry lost",
                    event.action(), event.resourceType(), event.resourceId(), ex);
        }
    }
}
