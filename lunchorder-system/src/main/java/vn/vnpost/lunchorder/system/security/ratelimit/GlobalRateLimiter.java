package vn.vnpost.lunchorder.system.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GlobalRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:global:";

    private final StringRedisTemplate redisTemplate;

    @Value("${security.global-rate-limit.read.max-requests}")
    private int readMaxRequests;

    @Value("${security.global-rate-limit.read.window-seconds}")
    private int readWindowSeconds;

    @Value("${security.global-rate-limit.write.max-requests}")
    private int writeMaxRequests;

    @Value("${security.global-rate-limit.write.window-seconds}")
    private int writeWindowSeconds;

    public boolean tryAcquire(String clientIp, boolean isRead) {
        String key = buildKey(clientIp, isRead);
        int maxRequests = isRead ? readMaxRequests : writeMaxRequests;
        int windowSeconds = isRead ? readWindowSeconds : writeWindowSeconds;

        Long requests = redisTemplate.opsForValue().increment(key);
        if (requests != null && requests == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return requests != null && requests <= maxRequests;
    }

    public long resolveRetryAfterSeconds(String clientIp, boolean isRead) {
        Long ttl = redisTemplate.getExpire(buildKey(clientIp, isRead), TimeUnit.SECONDS);
        int windowSeconds = isRead ? readWindowSeconds : writeWindowSeconds;
        return ttl != null && ttl > 0 ? ttl : windowSeconds;
    }

    private String buildKey(String clientIp, boolean isRead) {
        return KEY_PREFIX + (isRead ? "read:" : "write:") + clientIp;
    }
}
