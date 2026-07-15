package vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Typed projection for a single order row used to build the monthly matrix.
 * Using a typed {@link LocalDate} getter removes the fragile runtime date-type
 * conversions that positional {@code Object[]} access previously required.
 */
public interface MonthlyOrderDetail {
    Long getUserId();

    LocalDate getOrderDate();

    BigDecimal getPrice();
}
