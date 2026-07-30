package vn.vnpost.lunchorder.system.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.system.modules.auth.repository.InvalidatedTokenRepository;
import vn.vnpost.lunchorder.system.modules.user.entity.User;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_FULL_NAME = "fullName";
    private static final String CLAIM_SCOPE = "scope";
    private static final String CLAIM_REFRESH_EXPIRY = "refreshExpiry";
    private static final String CLAIM_REMEMBER_ME = "rememberMe";
    private static final String ISSUER = "vnpost.vn";

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Value("${jwt.valid-duration}")
    private long validDuration;

    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    @Value("${jwt.remember-me-duration:2592000}")
    private long rememberMeDuration;

    public long refreshWindow(boolean rememberMe) {
        return rememberMe ? rememberMeDuration : refreshableDuration;
    }

    public String generate(User user, Instant refreshExpiry, boolean rememberMe) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(ISSUER)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(validDuration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_FULL_NAME, user.getFullName())
                .claim(CLAIM_SCOPE, buildScope(user))
                .claim(CLAIM_REFRESH_EXPIRY, refreshExpiry.toEpochMilli())
                .claim(CLAIM_REMEMBER_ME, rememberMe)
                .build();

        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS512), new Payload(claims.toJSONObject()));
        try {
            jwsObject.sign(new MACSigner(signerKeyBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    public SignedJWT verify(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(signerKeyBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        Date expiryTime = isRefresh
                ? Date.from(resolveRefreshExpiry(claims))
                : claims.getExpirationTime();

        if (!(signedJWT.verify(verifier) && expiryTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String jit = claims.getJWTID();
        if (jit != null && invalidatedTokenRepository.existsByToken(jit)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    public boolean validateToken(String token) {
        try {
            verify(token, false);
            return true;
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return false;
        }
    }

    public Instant resolveRefreshExpiry(JWTClaimsSet claims) throws ParseException {
        Long refreshExpiryMillis = claims.getLongClaim(CLAIM_REFRESH_EXPIRY);
        return refreshExpiryMillis != null
                ? Instant.ofEpochMilli(refreshExpiryMillis)
                : claims.getIssueTime().toInstant().plus(refreshableDuration, ChronoUnit.SECONDS);
    }

    public boolean isRememberMe(JWTClaimsSet claims) throws ParseException {
        return Boolean.TRUE.equals(claims.getBooleanClaim(CLAIM_REMEMBER_ME));
    }

    public Authentication getAuthentication(String token) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            String username = claims.getSubject();
            Long userId = claims.getLongClaim(CLAIM_USER_ID);
            String scope = claims.getStringClaim(CLAIM_SCOPE);

            Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
            if (scope != null && !scope.isEmpty()) {
                authorities = Arrays.stream(scope.split(" "))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UserPrincipal principal = new UserPrincipal(userId, username, authorities);
            return new UsernamePasswordAuthenticationToken(principal, token, authorities);
        } catch (Exception e) {
            log.error("Failed to parse authentication from JWT", e);
            return null;
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getCode());
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getAction()));
                }
            });
        }
        return stringJoiner.toString();
    }

    private byte[] signerKeyBytes() {
        return signerKey.getBytes(StandardCharsets.UTF_8);
    }
}
