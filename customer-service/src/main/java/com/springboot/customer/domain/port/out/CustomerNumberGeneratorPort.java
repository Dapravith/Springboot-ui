package com.springboot.customer.domain.port.out;

import com.springboot.customer.domain.model.CustomerNumber;

/**
 * Outbound port for allocating customer numbers.
 *
 * <p>A port rather than a static helper so tests can supply a deterministic
 * generator, and so the numbering scheme (random, sequence-backed, or supplied
 * by a core banking system) can change without touching the use case.
 */
public interface CustomerNumberGeneratorPort {

    CustomerNumber next();
}
