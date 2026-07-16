package vn.vnpost.lunchorder.system.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;

import java.time.Duration;

/**
 * Chặn brute-force / credential stuffing trên endpoint đăng nhập bằng cách đếm
 * số lần đăng nhập sai theo cặp (IP, username) trong một cửa sổ thời gian, lưu
 * trạng thái đếm trên Redis để hoạt động đúng khi có nhiều instance backend.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "login:attempts:";

    private final StringRedisTemplate redisTemplate;

    public void checkAllowed(String clientIp, String username) {
        String value = redisTemplate.opsForValue().get(buildKey(clientIp, username));
        int attempts = value != null ? Integer.parseInt(value) : 0;
        if (attempts >= MAX_ATTEMPTS) {
            throw new AppException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    public void recordFailure(String clientIp, String username) {
        String key = buildKey(clientIp, username);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    public void recordSuccess(String clientIp, String username) {
        redisTemplate.delete(buildKey(clientIp, username));
    }

    private String buildKey(String clientIp, String username) {
        return KEY_PREFIX + clientIp + ":" + username;
    }
}
