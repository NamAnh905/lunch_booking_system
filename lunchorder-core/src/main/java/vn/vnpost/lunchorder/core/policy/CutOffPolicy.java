package vn.vnpost.lunchorder.core.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.systemconfig.repository.SystemConfigRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CutOffPolicy {

    private static final String CUT_OFF_TIME_KEY = "CUT_OFF_TIME";
    private static final LocalTime DEFAULT_CUT_OFF_TIME = LocalTime.of(14, 45);

    private static final String TICKET_LOCK_TIME_KEY = "TICKET_LOCK_TIME";
    private static final LocalTime DEFAULT_TICKET_LOCK_TIME = LocalTime.of(11, 00);

    private final SystemConfigRepository systemConfigRepository;

    public LocalTime getCutOffTime() {
        return readTime(CUT_OFF_TIME_KEY, DEFAULT_CUT_OFF_TIME);
    }

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
 
    public boolean isWithinExchangeWindow(LocalDate menuDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = menuDate.minusDays(1).atTime(getCutOffTime());
        LocalDateTime windowEnd = menuDate.atTime(getTicketLockTime());
        return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
    }
}
