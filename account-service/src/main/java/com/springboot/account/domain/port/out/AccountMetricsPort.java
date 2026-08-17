package com.springboot.account.domain.port.out;

/**
 * Outbound port for lightweight operational counters.
 *
 * <p>A port, not a direct Redis call, so the use case does not depend on a
 * cache being present. Implementations must be non-fatal: losing a counter is
 * never a reason to fail a banking operation.
 */
public interface AccountMetricsPort {

    void accountOpened();

    long openedCount();
}
