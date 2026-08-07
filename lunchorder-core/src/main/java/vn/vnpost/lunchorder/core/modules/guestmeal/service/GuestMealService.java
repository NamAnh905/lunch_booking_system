package vn.vnpost.lunchorder.core.modules.guestmeal.service;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealCreateRequest;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealResponse;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealUpdateRequest;

import java.time.LocalDate;

public interface GuestMealService {

    GuestMealResponse create(GuestMealCreateRequest request);

    GuestMealResponse update(Long id, GuestMealUpdateRequest request);

    void delete(Long id);

    PageResponse<GuestMealResponse> findAll(int page, int size, LocalDate startDate, LocalDate endDate,
            Long departmentId, Long requestedByUserId);
}
