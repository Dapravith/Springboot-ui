package com.springboot.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

/**
 * Wires the audit trail into a service.
 *
 * <p>Imported explicitly by each service rather than auto-configured, following
 * the same convention as {@code GlobalExceptionHandler}: a reader of the
 * service's configuration can see that it audits, and where that comes from.
 */
@Configuration
public class AuditConfig {

    private static final String TX_MANAGER_CLASS =
            "org.springframework.transaction.support.TransactionSynchronizationManager";

    private static final Logger log = LoggerFactory.getLogger(AuditConfig.class);

    /**
     * Builds the audit writer, wrapping it so that a SUCCESS entry is only written
     * once its transaction actually commits.
     *
     * <p>That wrapper needs Spring's transaction support, which is only on the
     * classpath of services that actually use a database. Rather than forcing
     * {@code spring-tx} onto services that have no transactions, the wrapper is
     * applied only when it is available - and for a service with no transactions,
     * writing directly is the correct behaviour anyway, because there is no commit
     * that could later be rolled back.
     */
    @Bean
    public AuditLogger auditLogger(@Value("${spring.application.name:unknown-service}") String serviceName,
                                   @Value("${audit.enabled:true}") boolean enabled) {
        AuditLogger writer = new Slf4jAuditLogger(serviceName, enabled);

        if (!ClassUtils.isPresent(TX_MANAGER_CLASS, AuditConfig.class.getClassLoader())) {
            log.debug("Spring transaction support not present; audit entries are written immediately");
            return writer;
        }

        return new TransactionAwareAuditLogger(writer);
    }

    /**
     * Spring Boot registers any {@code Filter} bean across all URLs, so returning
     * the filter directly avoids depending on Boot's servlet registration types
     * from this shared module.
     *
     * <p>Highest precedence: the actor must be in the MDC before any other filter,
     * controller or audit call runs.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public AuditActorFilter auditActorFilter(@Value("${audit.actor-header:X-Actor-Id}") String actorHeader) {
        return new AuditActorFilter(actorHeader);
    }
}
