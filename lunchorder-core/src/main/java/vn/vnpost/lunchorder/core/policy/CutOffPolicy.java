package vn.vnpost.lunchorder.core.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.systemconfig.repository.SystemConfigRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CutOffPolicy {

    private static final String CUT_OFF_TIME_KEY = "CUT_OFF_TIME";
    private static final LocalTime DEFAULT_CUT_OFF_TIME = LocalTime.of(14, 45);

    private static final String TICKET_LOCK_TIME_KEY = "TICKET_LOCK_TIME";
    private static final LocalTime DEFAULT_TICKET_LOCK_TIME = LocalTime.of(11, 00);

    private static final String HOLIDAYS_KEY = "HOLIDAYS";

    private static final int MAX_ADVANCE_MONTHS = 3;

    private final SystemConfigRepository systemConfigRepository;
    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

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
        LocalDate today = today();
        LocalDate cutoffDate = menuDate.minusDays(1);
        if (today.isAfter(cutoffDate)) {
            return true;
        }
        if (today.isEqual(cutoffDate)) {
            return LocalTime.now(clock).isAfter(getCutOffTime());
        }
        return false;
    }

    public boolean isWithinExchangeWindow(LocalDate menuDate) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = menuDate.minusDays(1).atTime(getCutOffTime());
        LocalDateTime windowEnd = menuDate.atTime(getTicketLockTime());
        return !now.isBefore(windowStart) && !now.isAfter(windowEnd);
    }

    public List<String> getHolidays() {
        return systemConfigRepository.findByConfigKey(HOLIDAYS_KEY)
                .map(config -> Arrays.stream(config.getConfigValue().split(","))
                        .map(String::trim)
                        .filter(date -> !date.isEmpty())
                        .toList())
                .orElse(List.of());
    }

    public Set<LocalDate> getHolidayDates() {
        return getHolidays().stream()
                .map(this::parseHolidayOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private LocalDate parseHolidayOrNull(String rawValue) {
        try {
            return LocalDate.parse(rawValue);
        } catch (DateTimeParseException e) {
            log.error("Bỏ qua giá trị ngày lễ không hợp lệ trong cấu hình {}: {}", HOLIDAYS_KEY, rawValue);
            return null;
        }
    }

    public boolean isWeekend(LocalDate menuDate) {
        DayOfWeek dayOfWeek = menuDate.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public LocalDate getMaxOrderableDate() {
        return YearMonth.now(clock).plusMonths(MAX_ADVANCE_MONTHS).atEndOfMonth();
    }
}
