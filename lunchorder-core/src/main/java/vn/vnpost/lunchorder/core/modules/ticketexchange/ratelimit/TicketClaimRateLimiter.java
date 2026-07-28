package vn.vnpost.lunchorder.core.modules.ticketexchange.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Throttles ticket-claim attempts per user to absorb spam-clicking on the claim
 * button. The pessimistic lock in {@code findByIdForUpdate} already guarantees a
 * ticket can't be double-claimed; this only cuts down on redundant transactions/locks.
 */
@Component
@RequiredArgsConstructor
public class TicketClaimRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:ticket-claim:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.ticket-claim-rate-limit.max-requests}")
    private int maxRequests;

    @Value("${security.ticket-claim-rate-limit.window-seconds}")
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
