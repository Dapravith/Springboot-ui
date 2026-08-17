package com.springboot.account.infrastructure.cache;

import com.springboot.account.domain.port.out.AccountMetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed operational counter.
 *
 * <p>Deliberately failure-tolerant. Redis here is a convenience, never a source
 * of truth, so every call is wrapped: a Redis outage degrades the counter and
 * nothing else. Losing a metric must never fail an account opening.
 */
@Component
class RedisAccountMetrics implements AccountMetricsPort {

    private static final Logger log = LoggerFactory.getLogger(RedisAccountMetrics.class);
    private static final String OPENED_COUNT_KEY = "account-service:accounts:opened";

    private final ObjectProvider<StringRedisTemplate> redisTemplate;

    RedisAccountMetrics(ObjectProvider<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void accountOpened() {
        StringRedisTemplate template = redisTemplate.getIfAvailable();
        if (template == null) {
            return;
        }
        try {
            template.opsForValue().increment(OPENED_COUNT_KEY);
        } catch (RuntimeException ex) {
            log.warn("Could not increment Redis opened-account counter", ex);
            log.info("Redis is a convenience, not a source of truth, so this failure is ignored");
        }
    }

    @Override
    public long openedCount() {
        StringRedisTemplate template = redisTemplate.getIfAvailable();
        if (template == null) {
            return 0L;
        }
        try {
            String value = template.opsForValue().get(OPENED_COUNT_KEY);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException ex) {
            log.warn("Could not read Redis opened-account counter", ex);
            return 0L;
        }
    }
}
