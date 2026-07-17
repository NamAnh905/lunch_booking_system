package vn.vnpost.lunchorder.system.security.ratelimit;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.exception.TooManyLoginAttemptsException;

import java.util.concurrent.TimeUnit;

/**
 * Chặn brute-force / credential stuffing trên endpoint đăng nhập bằng cách đếm
 * số lần đăng nhập sai theo cặp (IP, username) trong một cửa sổ thời gian, lưu
 * trạng thái đếm trên Redis để hoạt động đúng khi có nhiều instance backend.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    @Value("${security.login-rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${security.login-rate-limit.window-seconds}")
    private int windowSeconds;

    private static final String KEY_PREFIX = "login:attempts:";

    private final StringRedisTemplate redisTemplate;

    public void checkAllowed(String clientIp, String username) {
        String key = buildKey(clientIp, username);
        String value = redisTemplate.opsForValue().get(key);
        int attempts = value != null ? Integer.parseInt(value) : 0;
        if (attempts >= maxAttempts) {
            throw new TooManyLoginAttemptsException(resolveRetryAfterSeconds(key));
        }
    }

    private long resolveRetryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : windowSeconds;
    }

    public void recordFailure(String clientIp, String username) {
        String key = buildKey(clientIp, username);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
    }

    public void recordSuccess(String clientIp, String username) {
        redisTemplate.delete(buildKey(clientIp, username));
    }

    private String buildKey(String clientIp, String username) {
        return KEY_PREFIX + clientIp + ":" + username;
    }
}
