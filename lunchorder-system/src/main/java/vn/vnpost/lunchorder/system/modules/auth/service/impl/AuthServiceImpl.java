package vn.vnpost.lunchorder.system.modules.auth.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.entity.InvalidatedToken;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.auth.repository.InvalidatedTokenRepository;
import vn.vnpost.lunchorder.system.modules.auth.service.AuthService;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.IntrospectResponse;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LoginRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.LogoutRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.RefreshRequest;
import vn.vnpost.lunchorder.system.modules.auth.service.dto.TokenResponse;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean isValid = true;
        try {
            verifyToken(request.getToken(), false);
        } catch (Exception e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Instant refreshExpiry = Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS);
        String token = generateToken(user, refreshExpiry);
        return TokenResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        try {
            SignedJWT signedJWT = verifyToken(request.getToken(), true);
            String jit = signedJWT.getJWTClaimsSet().getJWTID();
            
            Long refreshExpiryMillis = signedJWT.getJWTClaimsSet().getLongClaim("refreshExpiry");
            Instant expiryTime = refreshExpiryMillis != null
                    ? Instant.ofEpochMilli(refreshExpiryMillis)
                    : signedJWT.getJWTClaimsSet().getIssueTime().toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS);

            InvalidatedToken invalidatedToken = new InvalidatedToken();
            invalidatedToken.setToken(jit);
            invalidatedToken.setExpiryTime(expiryTime);

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired or invalid");
        } catch (Exception e) {
            log.error("Error during logout", e);
        }
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshRequest request) {
        try {
            SignedJWT signedJWT = verifyToken(request.getToken(), true);
            String jit = signedJWT.getJWTClaimsSet().getJWTID();

            Long refreshExpiryMillis = signedJWT.getJWTClaimsSet().getLongClaim("refreshExpiry");
            Instant oldRefreshExpiry = refreshExpiryMillis != null
                    ? Instant.ofEpochMilli(refreshExpiryMillis)
                    : signedJWT.getJWTClaimsSet().getIssueTime().toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS);

            // Blacklist the old token; keep the blacklist row alive until the old token's
            // own refresh window would have expired so it can never be reused.
            InvalidatedToken invalidatedToken = new InvalidatedToken();
            invalidatedToken.setToken(jit);
            invalidatedToken.setExpiryTime(oldRefreshExpiry);
            invalidatedTokenRepository.save(invalidatedToken);

            String emailOrUsername = signedJWT.getJWTClaimsSet().getSubject();
            User user = userRepository.findByUsername(emailOrUsername)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new AppException(ErrorCode.USER_LOCKED);
            }

            // Sliding session: the new token gets a fresh refresh window from now,
            // so an actively-used session is not force-logged-out on a fixed 1h boundary.
            Instant newRefreshExpiry = Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS);
            String token = generateToken(user, newRefreshExpiry);
            return TokenResponse.builder()
                    .token(token)
                    .authenticated(true)
                    .build();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String generateToken(User user, Instant refreshExpiry) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("vnpost.vn")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .claim("scope", buildScope(user))
                .claim("refreshExpiry", refreshExpiry.toEpochMilli())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Long refreshExpiryMillis = signedJWT.getJWTClaimsSet().getLongClaim("refreshExpiry");
        Date expiryTime = isRefresh
                ? (refreshExpiryMillis != null
                    ? new Date(refreshExpiryMillis)
                    : new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);
        if (!(verified && expiryTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String jit = signedJWT.getJWTClaimsSet().getJWTID();
        if (invalidatedTokenRepository.existsByToken(jit)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getCode());
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> {
                        stringJoiner.add(permission.getAction());
                    });
                }
            });
        }
        return stringJoiner.toString();
    }
}
