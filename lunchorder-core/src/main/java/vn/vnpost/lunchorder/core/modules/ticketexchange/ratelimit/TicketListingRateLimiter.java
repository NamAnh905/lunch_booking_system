package vn.vnpost.lunchorder.core.modules.ticketexchange.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Throttles listing mutations (post to market / withdraw from market) per user.
 * Both operations share one bucket so a post-withdraw-post toggle loop is capped
 * as a whole instead of each direction getting its own budget.
 */
@Component
@RequiredArgsConstructor
public class TicketListingRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:ticket-listing:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.ticket-listing-rate-limit.max-requests}")
    private int maxRequests;

    @Value("${security.ticket-listing-rate-limit.window-seconds}")
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
