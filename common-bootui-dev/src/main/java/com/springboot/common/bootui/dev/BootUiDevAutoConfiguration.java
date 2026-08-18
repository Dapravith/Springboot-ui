package com.springboot.common.bootui.dev;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

import tools.jackson.databind.ObjectMapper;

/**
 * Registers the developer-only BootUI workarounds.
 *
 * <p>Auto-configured rather than {@code @Import}ed by each service, which is the
 * convention used for {@code AuditConfig} and {@code GlobalExceptionHandler}. The
 * convention cannot apply here: this module is on the classpath only in
 * {@code -Pdev} builds, and a service cannot name a class in {@code @Import} that
 * is absent from its production build.
 *
 * <p>{@code @Profile("dev")} is a second barrier behind the {@code -Pdev} Gradle
 * guard, matching how BootUI's own activation is gated twice.
 */
@AutoConfiguration
@Profile("dev")
public class BootUiDevAutoConfiguration {

    /** The single BootUI endpoint whose payload needs correcting. */
    private static final String SCAN_ENDPOINT = "/bootui/api/vulnerabilities/scan";

    @Bean
    public FilterRegistrationBean<VulnerabilityScanSeverityFilter> vulnerabilityScanSeverityFilter(
            ObjectMapper objectMapper) {

        FilterRegistrationBean<VulnerabilityScanSeverityFilter> registration =
                new FilterRegistrationBean<>(new VulnerabilityScanSeverityFilter(objectMapper));

        // Scoped to one exact path so nothing else in the application can be
        // affected by the response buffering this filter performs.
        registration.addUrlPatterns(SCAN_ENDPOINT);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        registration.setName("bootUiVulnerabilityScanSeverityFilter");
        return registration;
    }
}
