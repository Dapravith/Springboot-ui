package com.springboot.common.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates the audit actor for the duration of a request.
 *
 * <p>The actor is read from a request header. <strong>With no authentication in
 * front of it, that header is an unverified claim.</strong> It is recorded anyway
 * because a claimed actor plus a trace id is still far more useful during an
 * investigation than nothing - but the value must not be treated as identity.
 * When Spring Security is introduced, this filter should read the authenticated
 * principal instead, and the header should be ignored.
 *
 * <p>The MDC entry is always cleared in a finally block: servlet threads are
 * pooled, and a leaked actor would be attributed to the next unrelated request.
 */
public class AuditActorFilter extends OncePerRequestFilter {

    private final String actorHeader;

    public AuditActorFilter(String actorHeader) {
        this.actorHeader = actorHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            AuditContext.setActor(request.getHeader(actorHeader));
            chain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }
}
