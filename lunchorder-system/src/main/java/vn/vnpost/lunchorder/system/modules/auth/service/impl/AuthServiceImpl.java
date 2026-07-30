package vn.vnpost.lunchorder.system.modules.auth.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.system.modules.auth.entity.InvalidatedToken;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.auth.repository.InvalidatedTokenRepository;
import vn.vnpost.lunchorder.system.modules.auth.service.AuthService;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LoginRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.TokenResponse;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;
import vn.vnpost.lunchorder.system.security.jwt.JwtTokenProvider;
import vn.vnpost.lunchorder.system.security.ratelimit.LoginAttemptLimiter;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    PasswordEncoder passwordEncoder;
    LoginAttemptLimiter loginAttemptLimiter;
    JwtTokenProvider jwtTokenProvider;

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean isValid = true;
        try {
            jwtTokenProvider.verify(request.getToken(), false);
        } catch (Exception e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    @Override
    public TokenResponse login(LoginRequest request, String clientIp) {
        loginAttemptLimiter.checkAllowed(clientIp, request.getUsername());

        TokenResponse response;
        try {
            response = issueTokenFor(authenticate(request), request.isRememberMe());
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.UNAUTHENTICATED) {
                loginAttemptLimiter.recordFailure(clientIp, request.getUsername());
            }
            throw e;
        }

        loginAttemptLimiter.recordSuccess(clientIp, request.getUsername());
        return response;
    }

    @Override
    @Transactional
    public void logout(String token) {
        SignedJWT signedJWT;
        try {
            signedJWT = jwtTokenProvider.verify(token, true);
        } catch (AppException | JOSEException | ParseException e) {
            log.info("Token already expired or invalid");
            return;
        }

        try {
            invalidate(signedJWT);
        } catch (ParseException e) {
            log.error("Cannot read claims of a verified token during logout", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(String token) {
        try {
            SignedJWT signedJWT = jwtTokenProvider.verify(token, true);
            invalidate(signedJWT);

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            User user = userRepository.findByUsername(claims.getSubject())
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new AppException(ErrorCode.USER_LOCKED);
            }

            return issueTokenFor(user, jwtTokenProvider.isRememberMe(claims));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private User authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }

    private TokenResponse issueTokenFor(User user, boolean rememberMe) {
        Instant refreshExpiry = Instant.now()
                .plus(jwtTokenProvider.refreshWindow(rememberMe), ChronoUnit.SECONDS);

        return TokenResponse.builder()
                .token(jwtTokenProvider.generate(user, refreshExpiry, rememberMe))
                .authenticated(true)
                .rememberMe(rememberMe)
                .build();
    }

    private void invalidate(SignedJWT signedJWT) throws ParseException {
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        InvalidatedToken invalidatedToken = new InvalidatedToken();
        invalidatedToken.setToken(claims.getJWTID());
        invalidatedToken.setExpiryTime(jwtTokenProvider.resolveRefreshExpiry(claims));
        invalidatedTokenRepository.save(invalidatedToken);
    }
}
