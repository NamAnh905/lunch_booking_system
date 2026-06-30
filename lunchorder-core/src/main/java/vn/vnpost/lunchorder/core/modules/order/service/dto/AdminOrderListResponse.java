package vn.vnpost.lunchorder.core.modules.order.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderListResponse {
    private long totalCount;
    private List<OrderResponse> orders;
}
