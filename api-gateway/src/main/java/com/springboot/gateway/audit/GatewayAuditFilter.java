package com.springboot.gateway.audit;

import com.springboot.common.audit.AuditLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records every API call that passes through the gateway.
 *
 * <p>This is the front-door entry in the trail: it captures what was requested
 * and what status came back, which is the only place that sees a call the
 * downstream service rejected or never received.
 *
 * <p>Runs just after the actor filter so the caller's claimed identity is
 * already in the MDC. Non-API paths - actuator, and the gateway's own BootUI
 * console - are skipped, since health polling every few seconds would bury the
 * entries that matter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewayAuditFilter extends OncePerRequestFilter {

    private static final String RESOURCE = "ApiRequest";
    private static final String ACTION = "API_CALL_ROUTED";

    private final AuditLogger audit;

    public GatewayAuditFilter(AuditLogger audit) {
        this.audit = audit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();

            Map<String, String> context = new LinkedHashMap<>();
            context.put("method", request.getMethod());
            context.put("status", Integer.toString(status));
            context.put("durationMs", Long.toString(millis));
            context.put("remoteAddr", request.getRemoteAddr());

            // 4xx and 5xx are recorded as failures so a spike of refused calls is
            // greppable without parsing status codes out of a success stream.
            if (status >= 400) {
                audit.failure(ACTION, RESOURCE, request.getRequestURI(), "HTTP_" + status, context);
            } else {
                audit.success(ACTION, RESOURCE, request.getRequestURI(), context);
            }
        }
    }
}
