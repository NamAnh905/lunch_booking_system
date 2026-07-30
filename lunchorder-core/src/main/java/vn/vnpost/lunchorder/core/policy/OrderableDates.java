package vn.vnpost.lunchorder.core.policy;

import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;

import java.time.LocalDate;
import java.util.Set;

public record OrderableDates(CutOffPolicy cutOffPolicy, Set<LocalDate> holidays, LocalDate maxOrderableDate) {

    public static OrderableDates snapshot(CutOffPolicy cutOffPolicy) {
        return new OrderableDates(cutOffPolicy, cutOffPolicy.getHolidayDates(), cutOffPolicy.getMaxOrderableDate());
    }

    public void assertOrderable(LocalDate orderDate) {
        if (cutOffPolicy.isWeekend(orderDate) || holidays.contains(orderDate)) {
            throw new AppException(ErrorCode.ORDER_DATE_NOT_ALLOWED);
        }

        if (orderDate.isAfter(maxOrderableDate)) {
            throw new AppException(ErrorCode.ORDER_DATE_TOO_FAR);
        }

        if (cutOffPolicy.isCutOffReached(orderDate)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }
    }
}
