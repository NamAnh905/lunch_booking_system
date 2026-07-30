package vn.vnpost.lunchorder.system.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieManager {

    private static final String TOKEN_COOKIE = "token";
    private static final long SESSION_COOKIE_MAX_AGE = -1;

    @Value("${jwt.remember-me-duration:2592000}")
    private long rememberMeDuration;

    public Optional<String> readToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> TOKEN_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void write(HttpServletRequest request, HttpServletResponse response, String token, boolean rememberMe) {
        addCookie(request, response, token, rememberMe ? rememberMeDuration : SESSION_COOKIE_MAX_AGE);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        addCookie(request, response, "", 0);
    }

    private void addCookie(HttpServletRequest request, HttpServletResponse response, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
