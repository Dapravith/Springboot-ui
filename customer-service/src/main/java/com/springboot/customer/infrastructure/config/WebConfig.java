package com.springboot.customer.infrastructure.config;

import com.springboot.common.audit.AuditConfig;
import com.springboot.common.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the shared error handling explicitly.
 *
 * <p>Component scanning covers only this service's own package, so the shared
 * handler is wired in by hand. That is deliberate: the service states which
 * cross-cutting behaviour it opts into, rather than inheriting it invisibly.
 */
@Configuration
@Import({GlobalExceptionHandler.class, AuditConfig.class})
public class WebConfig {
}
