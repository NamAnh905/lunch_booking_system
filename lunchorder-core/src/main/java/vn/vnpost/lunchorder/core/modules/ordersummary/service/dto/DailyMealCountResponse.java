package vn.vnpost.lunchorder.core.modules.ordersummary.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMealCountResponse {
    private LocalDate date;
    private Integer totalMeals;
}
