package com.springboot.common.audit;

/**
 * Whether the audited attempt succeeded.
 *
 * <p>Refused attempts are recorded, not discarded. An audit trail that contains
 * only successes cannot answer the question auditors actually ask, which is who
 * tried to do something they were not allowed to do.
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE
}
