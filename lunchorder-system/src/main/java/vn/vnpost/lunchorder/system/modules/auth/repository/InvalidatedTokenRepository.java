package vn.vnpost.lunchorder.system.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vnpost.lunchorder.common.entity.InvalidatedToken;

import java.time.Instant;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, Long> {
    boolean existsByToken(String token);

    void deleteByExpiryTimeBefore(Instant expiryTime);
}
