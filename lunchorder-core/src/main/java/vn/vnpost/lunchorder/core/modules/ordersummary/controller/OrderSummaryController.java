package vn.vnpost.lunchorder.core.modules.ordersummary.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.ExcelDownload;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderReportMailService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/order-summary")
public class OrderSummaryController {

    private final OrderSummaryService orderSummaryService;
    private final OrderReportMailService orderReportMailService;

    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ApiResponse<MonthlyOrderSummaryResponse> getMonthlySummary(
            @RequestParam("month") @Min(1) @Max(12) int month,
            @RequestParam("year") @Min(2000) @Max(2100) int year,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        return ApiResponse.<MonthlyOrderSummaryResponse>builder()
                .result(orderSummaryService.getMonthlySummary(month, year, departmentId))
                .build();
    }

    @GetMapping("/daily/export")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    public ResponseEntity<byte[]> exportDailyExcel(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        byte[] excelData = orderSummaryService.exportDailyExcel(date, departmentId);
        String filename = "tong_hop_suat_an_" + date.format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";

        return ExcelDownload.of(excelData, filename);
    }

    @GetMapping("/monthly/export")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    public ResponseEntity<byte[]> exportMonthlyMatrixExcel(
            @RequestParam("month") @Min(1) @Max(12) int month,
            @RequestParam("year") @Min(2000) @Max(2100) int year,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        byte[] excelData = orderSummaryService.exportMonthlyMatrixExcel(month, year, departmentId);
        String filename = "theo_doi_dat_com_thang_" + month + "_" + year + ".xlsx";

        return ExcelDownload.of(excelData, filename);
    }

    @PostMapping("/daily/send-email")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> triggerSendEmail(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        orderReportMailService.sendDailyReportEmail(date);
        return ApiResponse.<Void>builder()
                .message("Email report has been queued for sending")
                .build();
    }

    @PostMapping("/monthly/send-email")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> triggerSendMonthlyEmail(
            @RequestParam("month") @Min(1) @Max(12) int month,
            @RequestParam("year") @Min(2000) @Max(2100) int year) {
        orderReportMailService.sendMonthlyReportEmail(month, year);
        return ApiResponse.<Void>builder()
                .message("Email report has been queued for sending")
                .build();
    }
}
