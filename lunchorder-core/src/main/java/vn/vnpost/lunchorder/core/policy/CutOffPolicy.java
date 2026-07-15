package vn.vnpost.lunchorder.core.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Shared ordering cut-off rules, previously duplicated inside
 * {@code OrderServiceImpl} and {@code TicketExchangeServiceImpl}.
 *
 * <p>The cut-off time is read from the {@code CUT_OFF_TIME} system config and
 * defaults to 14:45 when missing or unparseable. The ticket-exchange lock time
 * is read from {@code TICKET_LOCK_TIME} and defaults to 10:30.</p>
 *
 * <p>Not cached: {@link LocalTime} is a final JDK type, and the Redis
 * serializer here only embeds type info for non-final classes
 * ({@code DefaultTyping.NON_FINAL} in {@code RedisConfig}), so a cache hit
 * deserializes to a plain {@code String} and throws a {@code ClassCastException}
 * on return. The underlying read is a single indexed lookup by unique
 * {@code config_key}, so it is not worth caching.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CutOffPolicy {

    private static final String CUT_OFF_TIME_KEY = "CUT_OFF_TIME";
    private static final LocalTime DEFAULT_CUT_OFF_TIME = LocalTime.of(14, 45);

    private static final String TICKET_LOCK_TIME_KEY = "TICKET_LOCK_TIME";
    private static final LocalTime DEFAULT_TICKET_LOCK_TIME = LocalTime.of(10, 30);

    private final SystemConfigRepository systemConfigRepository;

    public LocalTime getCutOffTime() {
        return readTime(CUT_OFF_TIME_KEY, DEFAULT_CUT_OFF_TIME);
    }

    /**
     * The daily time on the menu date after which an open market ticket for
     * that date is locked (no longer claimable) and reverts to its original
     * owner.
     */
    public LocalTime getTicketLockTime() {
        return readTime(TICKET_LOCK_TIME_KEY, DEFAULT_TICKET_LOCK_TIME);
    }

    private LocalTime readTime(String configKey, LocalTime defaultValue) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(config -> {
                    try {
                        return LocalTime.parse(config.getConfigValue());
                    } catch (Exception e) {
                        log.error("Failed to parse {} configuration: {}", configKey, config.getConfigValue(), e);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Whether the ordering/cancellation cut-off for the given menu date has passed.
     * Cut-off is the configured time on the day before the menu date.
     */
    public boolean isCutOffReached(LocalDate menuDate) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = menuDate.minusDays(1);
        if (today.isAfter(cutoffDate)) {
            return true;
        }
        if (today.isEqual(cutoffDate)) {
            return LocalTime.now().isAfter(getCutOffTime());
        }
        return false;
    }

    /**
     * Whether a market ticket for the given menu date can currently be
     * posted/claimed: from {@code CUT_OFF_TIME} on the day before the menu
     * date until {@code TICKET_LOCK_TIME} on the menu date itself.
     */
    public boolean isWithinExchangeWindow(LocalDate menuDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = menuDate.minusDays(1).atTime(getCutOffTime());
        LocalDateTime windowEnd = menuDate.atTime(getTicketLockTime());
        return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
    }
}
