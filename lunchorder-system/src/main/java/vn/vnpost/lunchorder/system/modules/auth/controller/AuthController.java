package vn.vnpost.lunchorder.system.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.AuthService;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LoginRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LogoutRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.RefreshRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.TokenResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody @Valid IntrospectRequest request) {
        IntrospectResponse response = authService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ApiResponse.<TokenResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ApiResponse.<TokenResponse>builder()
                .result(response)
                .build();
    }
}
