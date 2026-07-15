package vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection;

import java.time.LocalDate;

/**
 * Typed projection for the per-day meal count, computed with a database-side
 * {@code GROUP BY} instead of grouping every order in application memory.
 */
public interface DailyMealCount {
    LocalDate getDate();

    Long getTotalMeals();
}
