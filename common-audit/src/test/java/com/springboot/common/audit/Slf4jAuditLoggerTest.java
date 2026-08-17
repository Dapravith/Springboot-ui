package com.springboot.common.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts the on-disk contract of the audit trail. These are not cosmetic
 * checks: the tail tooling and any downstream ingestion both parse this exact
 * shape, one JSON object per line.
 */
class Slf4jAuditLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger auditLogger;

    @BeforeEach
    void attachAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        auditLogger = context.getLogger(Slf4jAuditLogger.AUDIT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        auditLogger.addAppender(appender);
        auditLogger.setLevel(Level.INFO);
        AuditContext.clear();
    }

    @AfterEach
    void detachAppender() {
        auditLogger.detachAppender(appender);
        AuditContext.clear();
    }

    private JsonNode onlyEntry() {
        assertEquals(1, appender.list.size(), "expected exactly one audit line");
        return JsonMapper.builder().build().readTree(appender.list.getFirst().getFormattedMessage());
    }

    @Test
    void writesOneJsonObjectPerEntry() {
        new Slf4jAuditLogger("customer-service", true)
                .success("CUSTOMER_REGISTERED", "Customer", "CUS0000000001");

        JsonNode entry = onlyEntry();
        assertEquals("customer-service", entry.get("service").asString());
        assertEquals("CUSTOMER_REGISTERED", entry.get("action").asString());
        assertEquals("Customer", entry.get("resourceType").asString());
        assertEquals("CUS0000000001", entry.get("resourceId").asString());
        assertEquals("SUCCESS", entry.get("outcome").asString());
        assertNotNull(entry.get("eventId").asString());
        assertNotNull(entry.get("occurredAt"));
    }

    @Test
    void recordsFailuresWithReason() {
        new Slf4jAuditLogger("account-service", true)
                .failure("FUNDS_TRANSFERRED", "Account", "ACC000000000001", "INSUFFICIENT_FUNDS");

        JsonNode entry = onlyEntry();
        assertEquals("FAILURE", entry.get("outcome").asString());
        assertEquals("INSUFFICIENT_FUNDS", entry.get("reason").asString());
    }

    @Test
    void usesActorFromContext() {
        AuditContext.setActor("teller-42");

        new Slf4jAuditLogger("account-service", true).success("ACCOUNT_OPENED", "Account", "ACC000000000001");

        assertEquals("teller-42", onlyEntry().get("actor").asString());
    }

    @Test
    void fallsBackToAnonymousWhenNoActorIsSet() {
        new Slf4jAuditLogger("account-service", true).success("ACCOUNT_OPENED", "Account", "ACC000000000001");

        assertEquals(AuditContext.UNKNOWN_ACTOR, onlyEntry().get("actor").asString());
    }

    @Test
    void carriesAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("amount", "250.00");
        attributes.put("currency", "USD");

        new Slf4jAuditLogger("transfer-service", true)
                .success("TRANSFER_POSTED", "Transfer", "ref-1", attributes);

        JsonNode entry = onlyEntry();
        assertEquals("250.00", entry.get("attributes").get("amount").asString());
        assertEquals("USD", entry.get("attributes").get("currency").asString());
    }

    @Test
    void valuesContainingQuotesOrNewlinesCannotBreakTheLine() {
        // An audit trail parsed line-by-line is corruptible by an unescaped
        // newline, so this is a tampering concern rather than a formatting nit.
        new Slf4jAuditLogger("customer-service", true).failure(
                "CUSTOMER_REGISTERED", "Customer", "CUS0000000001",
                "bad \"input\"\nFAKE {\"outcome\":\"SUCCESS\"}");

        String line = appender.list.getFirst().getFormattedMessage();
        assertFalse(line.contains("\n"), "a single entry must stay on a single line");

        JsonNode entry = JsonMapper.builder().build().readTree(line);
        assertEquals("FAILURE", entry.get("outcome").asString(), "the injected outcome must not win");
        assertTrue(entry.get("reason").asString().contains("FAKE"));
    }

    @Test
    void writesNothingWhenDisabled() {
        new Slf4jAuditLogger("customer-service", false)
                .success("CUSTOMER_REGISTERED", "Customer", "CUS0000000001");

        assertTrue(appender.list.isEmpty());
    }
}
