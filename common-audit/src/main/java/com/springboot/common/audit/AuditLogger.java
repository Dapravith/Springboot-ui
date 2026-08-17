package com.springboot.common.audit;

import java.util.Map;

/**
 * Records audit entries.
 *
 * <p>Callers supply only what they know - the action, the resource and the
 * outcome. Service name, actor, trace id, event id and timestamp are filled in
 * by the implementation, so those can never be forgotten or spelled differently
 * at one of the call sites.
 *
 * <p>Implementations must never throw. A failure to write the audit trail is
 * serious and must be logged loudly, but it must not roll back or fail the
 * business operation that was being audited.
 */
public interface AuditLogger {

    void record(String action, String resourceType, String resourceId, AuditOutcome outcome,
                String reason, Map<String, String> attributes);

    default void success(String action, String resourceType, String resourceId) {
        record(action, resourceType, resourceId, AuditOutcome.SUCCESS, null, Map.of());
    }

    default void success(String action, String resourceType, String resourceId, Map<String, String> attributes) {
        record(action, resourceType, resourceId, AuditOutcome.SUCCESS, null, attributes);
    }

    default void failure(String action, String resourceType, String resourceId, String reason) {
        record(action, resourceType, resourceId, AuditOutcome.FAILURE, reason, Map.of());
    }

    default void failure(String action, String resourceType, String resourceId, String reason,
                         Map<String, String> attributes) {
        record(action, resourceType, resourceId, AuditOutcome.FAILURE, reason, attributes);
    }
}
