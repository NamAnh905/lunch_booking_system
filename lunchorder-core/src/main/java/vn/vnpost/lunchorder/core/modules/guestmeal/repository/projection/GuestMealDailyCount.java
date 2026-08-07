package vn.vnpost.lunchorder.core.modules.guestmeal.repository.projection;

import java.time.LocalDate;

/**
 * Typed projection for the per-day guest meal count, computed with a
 * database-side {@code GROUP BY}.
 */
public interface GuestMealDailyCount {

    LocalDate getDate();

    Long getTotalMeals();
}
