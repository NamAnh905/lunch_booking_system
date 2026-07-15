package vn.vnpost.lunchorder.system.modules.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.auth.service.AuthService;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.ChangePasswordRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LoginRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LogoutRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.ProfileUpdateRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.RefreshRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.TokenResponse;
import vn.vnpost.lunchorder.system.modules.user.service.UserService;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody @Valid IntrospectRequest request) {
        IntrospectResponse response = authService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return ApiResponse.<UserResponse>builder()
                .result(userService.findByUsername(principal.getUsername()))
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ProfileUpdateRequest request) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateProfile(principal.getUsername(), request))
                .build();
    }

    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ChangePasswordRequest request) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        userService.changePassword(principal.getUsername(), request);
        return ApiResponse.<Void>builder()
                .message("Đổi mật khẩu thành công")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        TokenResponse response = authService.login(request);
        setCookie(httpServletRequest, httpServletResponse, response.getToken(), refreshableDuration);
        return ApiResponse.<TokenResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        
        String token = null;
        if (request != null && StringUtils.hasText(request.getToken())) {
            token = request.getToken();
        } else {
            token = getCookieValue(httpServletRequest, "token");
        }

        if (token != null) {
            LogoutRequest serviceRequest = new LogoutRequest();
            serviceRequest.setToken(token);
            authService.logout(serviceRequest);
        }

        setCookie(httpServletRequest, httpServletResponse, null, 0);

        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        
        String token = null;
        if (request != null && StringUtils.hasText(request.getToken())) {
            token = request.getToken();
        } else {
            token = getCookieValue(httpServletRequest, "token");
        }

        if (!StringUtils.hasText(token)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        RefreshRequest serviceRequest = new RefreshRequest();
        serviceRequest.setToken(token);

        TokenResponse response = authService.refreshToken(serviceRequest);
        setCookie(httpServletRequest, httpServletResponse, response.getToken(), refreshableDuration);

        return ApiResponse.<TokenResponse>builder()
                .result(response)
                .build();
    }

    private void setCookie(HttpServletRequest request, HttpServletResponse response, String token, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from("token", token != null ? token : "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(token != null ? maxAge : 0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
