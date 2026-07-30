package vn.vnpost.lunchorder.system.modules.auth.service;

import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LoginRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request, String clientIp);

    void logout(String token);

    TokenResponse refreshToken(String token);

    IntrospectResponse introspect(IntrospectRequest request);
}
