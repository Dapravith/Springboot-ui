package com.springboot.transfer.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Built from the auto-configured, observation-enabled builder so trace
     * context propagates to account-service.
     *
     * <p>Timeouts are deliberately configured through {@code spring.http.client.*}
     * rather than in code: they are an operational concern that should be tunable
     * per environment without a rebuild. They are not optional - without them a
     * stalled ledger would pin threads here and turn one slow dependency into an
     * outage in this service.
     */
    @Bean
    RestClient accountServiceRestClient(RestClient.Builder builder,
                                        @Value("${transfer.account-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
