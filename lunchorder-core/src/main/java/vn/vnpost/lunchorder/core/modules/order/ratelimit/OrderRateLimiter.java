package vn.vnpost.lunchorder.core.modules.order.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OrderRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:order-create:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.order-rate-limit.max-requests}")
    private int maxRequests;

    @Value("${security.order-rate-limit.window-seconds}")
    private int windowSeconds;

    public boolean tryAcquire(Long userId) {
        String key = KEY_PREFIX + userId;
        Long requests = redisTemplate.opsForValue().increment(key);
        if (requests != null && requests == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return requests != null && requests <= maxRequests;
    }

    public long resolveRetryAfterSeconds(Long userId) {
        Long ttl = redisTemplate.getExpire(KEY_PREFIX + userId, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : windowSeconds;
    }
}
