package vn.vnpost.lunchorder.core.modules.ordersummary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/order-summary")
public class OrderSummaryController {

    private final OrderSummaryService orderSummaryService;

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ApiResponse<DailyOrderSummaryResponse> getDailySummary(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        return ApiResponse.<DailyOrderSummaryResponse>builder()
                .result(orderSummaryService.getDailySummary(date, departmentId))
                .build();
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ApiResponse<MonthlyOrderSummaryResponse> getMonthlySummary(
            @RequestParam("month") int month,
            @RequestParam("year") int year,
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
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/monthly/export")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    public ResponseEntity<byte[]> exportMonthlyMatrixExcel(
            @RequestParam("month") int month,
            @RequestParam("year") int year,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        byte[] excelData = orderSummaryService.exportMonthlyMatrixExcel(month, year, departmentId);

        String filename = "theo_doi_dat_com_thang_" + month + "_" + year + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @PostMapping("/daily/send-email")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    public ApiResponse<Void> triggerSendEmail(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        orderSummaryService.sendDailyReportEmail(date);
        return ApiResponse.<Void>builder()
                .message("Email triggers successfully")
                .build();
    }

    @PostMapping("/monthly/send-email")
    @PreAuthorize("hasAuthority('EXPORT_REPORTS')")
    public ApiResponse<Void> triggerSendMonthlyEmail(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {
        orderSummaryService.sendMonthlyReportEmail(month, year);
        return ApiResponse.<Void>builder()
                .message("Email triggers successfully")
                .build();
    }
}
