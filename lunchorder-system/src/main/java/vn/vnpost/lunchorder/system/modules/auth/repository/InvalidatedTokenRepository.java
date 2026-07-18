package vn.vnpost.lunchorder.system.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.vnpost.lunchorder.system.modules.auth.entity.InvalidatedToken;

import java.time.Instant;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, Long> {
    boolean existsByToken(String token);

    void deleteByExpiryTimeBefore(Instant expiryTime);
}
