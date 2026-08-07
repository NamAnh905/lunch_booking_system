package vn.vnpost.lunchorder.core.modules.order.service.dto;

import java.util.List;

public record DepartmentMealListResponse(
        List<DepartmentMemberOrderResponse> members,
        int guestNormalQuantity,
        int guestSpecialQuantity) {
}
