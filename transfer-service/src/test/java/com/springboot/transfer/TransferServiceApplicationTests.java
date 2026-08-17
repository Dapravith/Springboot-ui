package com.springboot.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full context load. transfer-service has no external infrastructure
 * dependency, so this runs hermetically in CI and catches wiring mistakes that
 * unit tests cannot.
 */
@SpringBootTest
class TransferServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
