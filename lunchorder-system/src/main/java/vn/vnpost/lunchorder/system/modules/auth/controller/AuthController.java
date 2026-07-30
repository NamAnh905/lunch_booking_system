package vn.vnpost.lunchorder.system.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import vn.vnpost.lunchorder.system.security.cookie.AuthCookieManager;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody @Valid IntrospectRequest request) {
        return ApiResponse.<IntrospectResponse>builder()
                .result(authService.introspect(request))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.findByUsername(requireAuthenticated(principal).getUsername()))
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ProfileUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateProfile(requireAuthenticated(principal).getUsername(), request))
                .build();
    }

    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(requireAuthenticated(principal).getUsername(), request);
        return ApiResponse.<Void>builder()
                .message("Đổi mật khẩu thành công")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        TokenResponse response = authService.login(request, httpServletRequest.getRemoteAddr());
        authCookieManager.write(httpServletRequest, httpServletResponse, response.getToken(), response.isRememberMe());

        return ApiResponse.<TokenResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        resolveToken(request == null ? null : request.getToken(), httpServletRequest)
                .ifPresent(authService::logout);
        authCookieManager.clear(httpServletRequest, httpServletResponse);

        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        String token = resolveToken(request == null ? null : request.getToken(), httpServletRequest)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        TokenResponse response = authService.refreshToken(token);
        authCookieManager.write(httpServletRequest, httpServletResponse, response.getToken(), response.isRememberMe());

        return ApiResponse.<TokenResponse>builder()
                .result(response)
                .build();
    }

    private Optional<String> resolveToken(String bodyToken, HttpServletRequest request) {
        if (StringUtils.hasText(bodyToken)) {
            return Optional.of(bodyToken);
        }
        return authCookieManager.readToken(request).filter(StringUtils::hasText);
    }

    private UserPrincipal requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return principal;
    }
}
