package vn.vnpost.lunchorder.core.modules.order.service.dto;

public record DepartmentMemberOrderResponse(
        String fullName,
        String departmentName,
        boolean hasOrdered,
        boolean isSpecial) {
}
