package com.springboot.common.audit;

import org.slf4j.MDC;

/**
 * Carries the actor for the current thread via SLF4J's MDC.
 *
 * <p>MDC is used rather than a bespoke ThreadLocal so the actor also appears in
 * ordinary application logs, and so Spring's existing MDC propagation applies.
 */
public final class AuditContext {

    /** MDC key holding the actor for the in-flight request. */
    public static final String ACTOR_KEY = "auditActor";

    /** Recorded when no actor could be determined. */
    public static final String UNKNOWN_ACTOR = "anonymous";

    /** MDC key Spring Boot's tracing support populates, when tracing is on the classpath. */
    static final String TRACE_ID_KEY = "traceId";

    private AuditContext() {
    }

    public static void setActor(String actor) {
        MDC.put(ACTOR_KEY, (actor == null || actor.isBlank()) ? UNKNOWN_ACTOR : actor.trim());
    }

    public static String actor() {
        String actor = MDC.get(ACTOR_KEY);
        return (actor == null || actor.isBlank()) ? UNKNOWN_ACTOR : actor;
    }

    public static String traceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void clear() {
        MDC.remove(ACTOR_KEY);
    }
}
