package vn.vnpost.lunchorder.core.modules.guestmeal.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.GuestMealService;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealCreateRequest;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealResponse;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealUpdateRequest;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/guest-meals")
public class GuestMealController {

    private final GuestMealService guestMealService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_GUEST_MEALS')")
    public ApiResponse<GuestMealResponse> create(@RequestBody @Valid GuestMealCreateRequest request) {
        return ApiResponse.<GuestMealResponse>builder()
                .result(guestMealService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_GUEST_MEALS')")
    public ApiResponse<GuestMealResponse> update(@PathVariable Long id,
            @RequestBody @Valid GuestMealUpdateRequest request) {
        return ApiResponse.<GuestMealResponse>builder()
                .result(guestMealService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_GUEST_MEALS')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        guestMealService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_GUEST_MEALS')")
    public ApiResponse<PageResponse<GuestMealResponse>> findAll(
            @RequestParam(value = "page", defaultValue = PaginationConstants.DEFAULT_PAGE) @Min(1) int page,
            @RequestParam(value = "size", defaultValue = PaginationConstants.DEFAULT_SIZE) @Min(1) int size,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "requestedByUserId", required = false) Long requestedByUserId) {
        return ApiResponse.<PageResponse<GuestMealResponse>>builder()
                .result(guestMealService.findAll(page, size, startDate, endDate, departmentId, requestedByUserId))
                .build();
    }
}
