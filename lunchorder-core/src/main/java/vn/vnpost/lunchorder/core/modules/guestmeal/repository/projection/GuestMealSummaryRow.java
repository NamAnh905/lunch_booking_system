package vn.vnpost.lunchorder.core.modules.guestmeal.repository.projection;

import java.math.BigDecimal;

/**
 * Typed projection for the per-department guest meal aggregation. Field names
 * must match the query aliases.
 */
public interface GuestMealSummaryRow {

    Long getDepartmentId();

    String getDepartmentName();

    String getRequestedByName();

    Long getNormalMealCount();

    Long getSpecialMealCount();

    BigDecimal getTotalAmount();
}
