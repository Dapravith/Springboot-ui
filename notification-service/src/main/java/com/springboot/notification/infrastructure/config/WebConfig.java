package com.springboot.notification.infrastructure.config;

import com.springboot.common.audit.AuditConfig;
import com.springboot.common.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the shared error handling and audit wiring explicitly, since component
 * scanning covers only this service's own package.
 */
@Configuration
@Import({GlobalExceptionHandler.class, AuditConfig.class})
public class WebConfig {
}
