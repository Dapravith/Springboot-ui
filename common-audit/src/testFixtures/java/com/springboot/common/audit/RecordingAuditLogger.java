package com.springboot.common.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An {@link AuditLogger} that keeps entries in memory so tests can assert on the
 * audit trail.
 *
 * <p>Published as a test fixture rather than copied into each service, so all
 * services assert against the same fake and a change to the audit contract shows
 * up in one place.
 */
public class RecordingAuditLogger implements AuditLogger {

    /** One recorded call, with the fields a test is likely to assert on. */
    public record Entry(String action, String resourceType, String resourceId, AuditOutcome outcome,
                        String reason, Map<String, String> attributes) {
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void record(String action, String resourceType, String resourceId, AuditOutcome outcome,
                       String reason, Map<String, String> attributes) {
        entries.add(new Entry(action, resourceType, resourceId, outcome, reason,
                attributes == null ? Map.of() : Map.copyOf(attributes)));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public List<Entry> withAction(String action) {
        return entries.stream().filter(e -> e.action().equals(action)).toList();
    }

    public List<Entry> successes() {
        return entries.stream().filter(e -> e.outcome() == AuditOutcome.SUCCESS).toList();
    }

    public List<Entry> failures() {
        return entries.stream().filter(e -> e.outcome() == AuditOutcome.FAILURE).toList();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }
}
