package com.springboot.common.audit;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * Makes the audit trail tell the truth about transactions.
 *
 * <p>Writing to a log file is not transactional. A naive audit call inside a
 * {@code @Transactional} method records {@code SUCCESS} the moment it runs - and
 * if the transaction later rolls back (an optimistic-lock clash, a constraint
 * violation at flush, a failure in a subsequent step) the entry survives and
 * claims something happened that did not. For a banking trail that is worse than
 * having no entry at all.
 *
 * <p>So the two outcomes are treated differently, on purpose:
 *
 * <ul>
 *   <li><strong>SUCCESS</strong> is deferred to after commit. If the transaction
 *       rolls back, no success is ever claimed.</li>
 *   <li><strong>FAILURE</strong> is written immediately. A refused attempt
 *       usually rolls its transaction back, and that is precisely the attempt an
 *       investigator needs to see.</li>
 * </ul>
 *
 * <p>With no active transaction, both are written straight through.
 *
 * <p>One consequence worth knowing: a deferred entry is stamped at commit time
 * rather than at call time. The gap is milliseconds, and commit time is arguably
 * the more accurate answer to "when did this become true".
 */
public class TransactionAwareAuditLogger implements AuditLogger {

    private final AuditLogger delegate;

    public TransactionAwareAuditLogger(AuditLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void record(String action, String resourceType, String resourceId, AuditOutcome outcome,
                       String reason, Map<String, String> attributes) {

        boolean deferrable = outcome == AuditOutcome.SUCCESS
                && TransactionSynchronizationManager.isSynchronizationActive();

        if (!deferrable) {
            delegate.record(action, resourceType, resourceId, outcome, reason, attributes);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Same thread as the original call, so the MDC actor and trace id
                // are still in place.
                delegate.record(action, resourceType, resourceId, outcome, reason, attributes);
            }
        });
    }
}
