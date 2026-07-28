package vn.vnpost.lunchorder.system.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.exception.ErrorCode;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> READ_METHODS = Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name());
    private static final List<String> EXCLUDED_PATH_PREFIXES =
            List.of("/v3/api-docs", "/swagger-ui");

    private final GlobalRateLimiter globalRateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${security.global-rate-limit.enabled}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod()) || isExcluded(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        boolean isRead = READ_METHODS.contains(request.getMethod());
        if (globalRateLimiter.tryAcquire(clientIp, isRead)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimitExceeded(response, globalRateLimiter.resolveRetryAfterSeconds(clientIp, isRead));
    }

    private boolean isExcluded(HttpServletRequest request) {
        String path = request.getServletPath();
        return EXCLUDED_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void writeRateLimitExceeded(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.flushBuffer();
    }
}
