package com.springboot.transfer.infrastructure.config;

import com.springboot.common.domain.Money;
import com.springboot.transfer.domain.model.TransferPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Builds the domain policy from configuration.
 *
 * <p>The policy object itself has no Spring annotations, so it can be
 * constructed directly in a unit test. Wiring it here is what keeps the domain
 * free of the framework while still letting operators tune the limit.
 */
@Configuration
public class TransferPolicyConfig {

    @Bean
    TransferPolicy transferPolicy(@Value("${transfer.limits.single-transfer-max}") BigDecimal limit,
                                  @Value("${transfer.limits.currency}") String currency) {
        return new TransferPolicy(Money.of(limit, currency));
    }
}
