package vn.vnpost.lunchorder.system.modules.audit.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.audit.service.AuditLogService;
import vn.vnpost.lunchorder.system.modules.audit.service.dto.AuditLogResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")
    public ApiResponse<PageResponse<AuditLogResponse>> findAll(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "action", required = false, defaultValue = "") String action,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size) {
        return ApiResponse.<PageResponse<AuditLogResponse>>builder()
                .result(auditLogService.findAll(keyword, action, startDate, endDate, page, size))
                .build();
    }

    @GetMapping("/actions")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")
    public ApiResponse<List<String>> getActions() {
        return ApiResponse.<List<String>>builder()
                .result(auditLogService.getActions())
                .build();
    }
}
