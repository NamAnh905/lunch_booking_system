package vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection;

import java.math.BigDecimal;

/**
 * Typed projection for the per-user order aggregation, replacing positional
 * {@code Object[]} access. Field names must match the query aliases.
 */
public interface OrderSummaryRow {
    Long getUserId();

    String getFullName();

    String getDepartmentName();

    Long getNormalMealCount();

    Long getSpecialMealCount();

    BigDecimal getTotalAmount();
}
