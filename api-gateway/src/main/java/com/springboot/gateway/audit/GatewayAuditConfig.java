package com.springboot.gateway.audit;

import com.springboot.common.audit.AuditConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the shared audit wiring explicitly, since component scanning covers
 * only this service's own package.
 */
@Configuration
@Import(AuditConfig.class)
public class GatewayAuditConfig {
}
