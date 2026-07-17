package vn.vnpost.lunchorder.system.modules.auth.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.system.modules.auth.repository.InvalidatedTokenRepository;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidatedTokenCleanUpScheduler {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanUpExpiredTokens() {
        log.info("Starting clean up of expired invalidated tokens...");
        try {
            Instant now = Instant.now();
            invalidatedTokenRepository.deleteByExpiryTimeBefore(now);
            log.info("Expired invalidated tokens cleaned up successfully.");
        } catch (Exception e) {
            log.error("Failed to clean up expired invalidated tokens", e);
        }
    }
}
