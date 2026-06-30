package vn.vnpost.lunchorder.system.modules.auth.service;

import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LoginRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LogoutRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.RefreshRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    TokenResponse refreshToken(RefreshRequest request);

    IntrospectResponse introspect(IntrospectRequest request);
}
