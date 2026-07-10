package vn.vnpost.lunchorder.core.modules.ordersummary.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.OrderSummaryItemResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSummaryExcelHelper {

    private final SystemConfigRepository systemConfigRepository;

    public byte[] exportDailyExcel(LocalDate date, DailyOrderSummaryResponse summary) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook
                    .createSheet("Tổng hợp suất ăn " + date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            sheet.setZoom(150); // Set zoom level to 150% so it looks larger and fills the screen

            // Define borders & alignment styles
            CellStyle baseStyle = workbook.createCellStyle();
            baseStyle.setBorderBottom(BorderStyle.THIN);
            baseStyle.setBorderTop(BorderStyle.THIN);
            baseStyle.setBorderLeft(BorderStyle.THIN);
            baseStyle.setBorderRight(BorderStyle.THIN);
            baseStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.cloneStyleFrom(baseStyle);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title style
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Title row
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(40);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("TỔNG HỢP SUẤT ĂN NGÀY " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            titleCell.setCellStyle(titleStyle);

            // Header row
            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(28);
            String[] headers = { "STT", "Họ tên", "Phòng ban", "Suất thường", "Suất đặc biệt", "Thành tiền" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data styles
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.cloneStyleFrom(baseStyle);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(baseStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(baseStyle);
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("#,##0"));
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);

            int rowIdx = 3;
            for (int i = 0; i < summary.getItems().size(); i++) {
                OrderSummaryItemResponse item = summary.getItems().get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                Cell cellStt = row.createCell(0);
                cellStt.setCellValue(i + 1);
                cellStt.setCellStyle(centerStyle);

                Cell cellName = row.createCell(1);
                cellName.setCellValue(item.getFullName());
                cellName.setCellStyle(dataStyle);

                Cell cellDept = row.createCell(2);
                cellDept.setCellValue(item.getDepartmentName());
                cellDept.setCellStyle(dataStyle);

                Cell cellNormal = row.createCell(3);
                cellNormal.setCellValue(item.getNormalMealCount());
                cellNormal.setCellStyle(centerStyle);

                Cell cellSpecial = row.createCell(4);
                cellSpecial.setCellValue(item.getSpecialMealCount());
                cellSpecial.setCellStyle(centerStyle);

                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(item.getTotalAmount().doubleValue());
                amountCell.setCellStyle(moneyStyle);
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.setHeightInPoints(24);

            // Empty cells in summary row with border
            for (int i = 0; i < 2; i++) {
                Cell cell = summaryRow.createCell(i);
                cell.setCellStyle(baseStyle);
            }

            Cell summaryLabel = summaryRow.createCell(2);
            summaryLabel.setCellValue("TỔNG CỘNG");
            summaryLabel.setCellStyle(headerStyle);

            Cell sumNormal = summaryRow.createCell(3);
            sumNormal.setCellValue(summary.getTotalNormalMeals());
            sumNormal.setCellStyle(headerStyle);

            Cell sumSpecial = summaryRow.createCell(4);
            sumSpecial.setCellValue(summary.getTotalSpecialMeals());
            sumSpecial.setCellStyle(headerStyle);

            Cell totalCell = summaryRow.createCell(5);
            totalCell.setCellValue(summary.getTotalAmount().doubleValue());
            totalCell.setCellStyle(moneyStyle);

            // Auto-size columns and set a minimum width
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                int minWidth = 15 * 256; // 15 characters min width
                if (i == 2)
                    minWidth = 25 * 256; // Họ tên
                if (i == 3)
                    minWidth = 25 * 256; // Phòng ban
                if (currentWidth < minWidth) {
                    sheet.setColumnWidth(i, minWidth);
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export daily Excel for date {}", date, e);
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    public byte[] exportMonthlyMatrixExcel(int month, int year, MonthlyOrderSummaryResponse summary,
            List<Object[]> detailRecords, BigDecimal normalPrice, BigDecimal specialPrice) {
        // Map from userId -> Map<Integer (day), String ("X" or "XX")>
        Map<Long, Map<Integer, String>> userDayMealMap = new HashMap<>();
        Map<Integer, Integer> dayNormalCountMap = new HashMap<>();
        Map<Integer, Integer> daySpecialCountMap = new HashMap<>();

        for (Object[] row : detailRecords) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            Long userId = ((Number) row[0]).longValue();

            // Safe LocalDate conversion
            Object dateObj = row[1];
            LocalDate date = null;
            if (dateObj instanceof LocalDate) {
                date = (LocalDate) dateObj;
            } else if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof java.util.Date) {
                date = ((java.util.Date) dateObj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            } else {
                date = LocalDate.parse(dateObj.toString());
            }

            BigDecimal priceObj = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            Boolean isSpecial = priceObj.compareTo(normalPrice) > 0;

            if (userId != null && date != null) {
                int day = date.getDayOfMonth();
                userDayMealMap
                        .computeIfAbsent(userId, k -> new HashMap<>())
                        .put(day, Boolean.TRUE.equals(isSpecial) ? "XX" : "X");

                if (Boolean.TRUE.equals(isSpecial)) {
                    daySpecialCountMap.put(day, daySpecialCountMap.getOrDefault(day, 0) + 1);
                } else {
                    dayNormalCountMap.put(day, dayNormalCountMap.getOrDefault(day, 0) + 1);
                }
            }
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet mainSheet = workbook.createSheet("Cơm trưa tháng " + month + "-" + year);
            mainSheet.setZoom(120); // Zoom 120% for matrix view

            // Base style with borders
            CellStyle baseStyle = workbook.createCellStyle();
            baseStyle.setBorderBottom(BorderStyle.THIN);
            baseStyle.setBorderTop(BorderStyle.THIN);
            baseStyle.setBorderLeft(BorderStyle.THIN);
            baseStyle.setBorderRight(BorderStyle.THIN);
            baseStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.cloneStyleFrom(baseStyle);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title style
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Title row
            Row titleRow = mainSheet.createRow(0);
            titleRow.setHeightInPoints(40);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG THEO DÕI ĐẶT CƠM - THÁNG " + month + "/" + year);
            titleCell.setCellStyle(titleStyle);

            YearMonth yearMonth = YearMonth.of(year, month);
            int totalDays = yearMonth.lengthOfMonth();

            // Construct headers
            // Row 2: Headers Day of week
            Row dowRow = mainSheet.createRow(2);
            dowRow.setHeightInPoints(28);

            Cell cellSttDow = dowRow.createCell(0);
            cellSttDow.setCellValue("STT");
            cellSttDow.setCellStyle(headerStyle);

            Cell cellNameDow = dowRow.createCell(1);
            cellNameDow.setCellValue("Họ và tên");
            cellNameDow.setCellStyle(headerStyle);

            Cell cellDeptDow = dowRow.createCell(2);
            cellDeptDow.setCellValue("Bộ phận");
            cellDeptDow.setCellStyle(headerStyle);

            String[] weekdayNames = { "CN", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy" };
            for (int day = 1; day <= totalDays; day++) {
                LocalDate date = yearMonth.atDay(day);
                int dowIndex = date.getDayOfWeek().getValue() % 7;
                Cell cell = dowRow.createCell(3 + day - 1);
                cell.setCellValue(weekdayNames[dowIndex]);
                cell.setCellStyle(headerStyle);
            }

            int nextColIdx = 3 + totalDays;
            String[] summaryHeaders = { "Tổng thường", "Tổng đặc biệt", "Tiền cần TT", "Tiền đã TT", "Còn lại" };
            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = dowRow.createCell(nextColIdx + i);
                cell.setCellValue(summaryHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Row 3: Day numbers
            Row dayNumRow = mainSheet.createRow(3);
            dayNumRow.setHeightInPoints(24);

            Cell cellSttDayNum = dayNumRow.createCell(0);
            cellSttDayNum.setCellStyle(headerStyle);

            Cell cellNameDayNum = dayNumRow.createCell(1);
            cellNameDayNum.setCellStyle(headerStyle);

            Cell cellDeptDayNum = dayNumRow.createCell(2);
            cellDeptDayNum.setCellStyle(headerStyle);

            for (int day = 1; day <= totalDays; day++) {
                Cell cell = dayNumRow.createCell(3 + day - 1);
                cell.setCellValue(day);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = dayNumRow.createCell(nextColIdx + i);
                cell.setCellStyle(headerStyle);
            }

            // Data styles
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.cloneStyleFrom(baseStyle);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(baseStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.cloneStyleFrom(baseStyle);
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("#,##0"));
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Date style for PMC sheet
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(baseStyle);
            dateStyle.setDataFormat(format.getFormat("dd/MM/yyyy"));
            dateStyle.setAlignment(HorizontalAlignment.CENTER);

            // Populate data
            int rowIdx = 4;
            for (int i = 0; i < summary.getItems().size(); i++) {
                OrderSummaryItemResponse item = summary.getItems().get(i);
                Row row = mainSheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                Cell cellStt = row.createCell(0);
                cellStt.setCellValue(i + 1);
                cellStt.setCellStyle(centerStyle);

                Cell cellName = row.createCell(1);
                cellName.setCellValue(item.getFullName() != null ? item.getFullName() : "");
                cellName.setCellStyle(dataStyle);

                Cell cellDept = row.createCell(2);
                cellDept.setCellValue(item.getDepartmentName() != null ? item.getDepartmentName() : "");
                cellDept.setCellStyle(dataStyle);

                // Populate day registrations
                Map<Integer, String> dayMealMap = userDayMealMap.getOrDefault(item.getUserId(), new HashMap<>());
                for (int day = 1; day <= totalDays; day++) {
                    Cell cell = row.createCell(3 + day - 1);
                    String mealType = dayMealMap.get(day);
                    cell.setCellValue(mealType != null ? mealType : "");
                    cell.setCellStyle(centerStyle);
                }

                // Populate summary columns
                Cell cellNormal = row.createCell(nextColIdx);
                cellNormal.setCellValue(item.getNormalMealCount());
                cellNormal.setCellStyle(centerStyle);

                Cell cellSpecial = row.createCell(nextColIdx + 1);
                cellSpecial.setCellValue(item.getSpecialMealCount());
                cellSpecial.setCellStyle(centerStyle);

                Cell cellAmount = row.createCell(nextColIdx + 2);
                cellAmount.setCellValue(item.getTotalAmount() != null ? item.getTotalAmount().doubleValue() : 0.0);
                cellAmount.setCellStyle(moneyStyle);

                Cell cellPaid = row.createCell(nextColIdx + 3);
                cellPaid.setCellValue(item.getTotalPaid() != null ? item.getTotalPaid().doubleValue() : 0.0);
                cellPaid.setCellStyle(moneyStyle);

                Cell cellRemaining = row.createCell(nextColIdx + 4);
                cellRemaining.setCellValue(
                        item.getRemainingAmount() != null ? item.getRemainingAmount().doubleValue() : 0.0);
                cellRemaining.setCellStyle(moneyStyle);
            }

            // Summary row at the bottom
            Row totalRow = mainSheet.createRow(rowIdx + 1);
            totalRow.setHeightInPoints(24);

            for (int col = 0; col < nextColIdx; col++) {
                Cell cell = totalRow.createCell(col);
                cell.setCellStyle(baseStyle);
            }
            Cell totalLabelCell = totalRow.createCell(1);
            totalLabelCell.setCellValue("TỔNG CỘNG");
            totalLabelCell.setCellStyle(headerStyle);

            Cell totalNormalCell = totalRow.createCell(nextColIdx);
            totalNormalCell.setCellValue(summary.getTotalNormalMeals());
            totalNormalCell.setCellStyle(headerStyle);

            Cell totalSpecialCell = totalRow.createCell(nextColIdx + 1);
            totalSpecialCell.setCellValue(summary.getTotalSpecialMeals());
            totalSpecialCell.setCellStyle(headerStyle);

            Cell totalAmountCell = totalRow.createCell(nextColIdx + 2);
            totalAmountCell
                    .setCellValue(summary.getTotalAmount() != null ? summary.getTotalAmount().doubleValue() : 0.0);
            totalAmountCell.setCellStyle(moneyStyle);

            Cell totalPaidCell = totalRow.createCell(nextColIdx + 3);
            totalPaidCell.setCellValue(summary.getTotalPaid() != null ? summary.getTotalPaid().doubleValue() : 0.0);
            totalPaidCell.setCellStyle(moneyStyle);

            Cell totalRemainingCell = totalRow.createCell(nextColIdx + 4);
            totalRemainingCell.setCellValue(
                    summary.getTotalRemaining() != null ? summary.getTotalRemaining().doubleValue() : 0.0);
            totalRemainingCell.setCellStyle(moneyStyle);

            // Auto-size columns and set min/max widths
            for (int col = 0; col < nextColIdx + 5; col++) {
                mainSheet.autoSizeColumn(col);
                int currentWidth = mainSheet.getColumnWidth(col);
                int minWidth = 8 * 256; // 8 chars minimum
                if (col == 1)
                    minWidth = 25 * 256; // Name
                if (col == 2)
                    minWidth = 20 * 256; // Department
                if (currentWidth < minWidth) {
                    mainSheet.setColumnWidth(col, minWidth);
                }
            }

            // ================== SHEET 2: ĐẶT CƠM PMC ==================
            Sheet pmcSheet = workbook.createSheet("Đặt cơm PMC");
            pmcSheet.setZoom(120);

            // Title
            Row pmcTitleRow = pmcSheet.createRow(0);
            pmcTitleRow.setHeightInPoints(40);
            Cell pmcTitleCell = pmcTitleRow.createCell(0);
            pmcTitleCell.setCellValue("ĐĂNG KÝ CƠM - PMC");
            pmcTitleCell.setCellStyle(titleStyle);

            // Headers
            Row pmcHeaderRow = pmcSheet.createRow(1);
            pmcHeaderRow.setHeightInPoints(28);
            String[] pmcHeaders = { "Ngày", "Suất thường", "Suất tăng cường", "TỔNG", "Số tiền cần TT cho PMC",
                    "Ghi chú" };
            for (int i = 0; i < pmcHeaders.length; i++) {
                Cell cell = pmcHeaderRow.createCell(i);
                cell.setCellValue(pmcHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal pmcNormalPrice = systemConfigRepository.findByConfigKey("PMC_NORMAL_MEAL_PRICE")
                    .or(() -> systemConfigRepository.findByConfigKey("PMC_MEAL_PRICE"))
                    .map(config -> {
                        try {
                            return new BigDecimal(config.getConfigValue().trim());
                        } catch (Exception e) {
                            return normalPrice;
                        }
                    })
                    .orElse(normalPrice);

            BigDecimal pmcSpecialPrice = systemConfigRepository.findByConfigKey("PMC_SPECIAL_MEAL_PRICE")
                    .map(config -> {
                        try {
                            return new BigDecimal(config.getConfigValue().trim());
                        } catch (Exception e) {
                            return specialPrice;
                        }
                    })
                    .orElse(specialPrice);

            int pmcRowIdx = 2;
            int pmcTotalNormal = 0;
            int pmcTotalSpecial = 0;
            BigDecimal pmcTotalAmount = BigDecimal.ZERO;

            for (int day = 1; day <= totalDays; day++) {
                Row row = pmcSheet.createRow(pmcRowIdx++);
                row.setHeightInPoints(22);

                LocalDate date = yearMonth.atDay(day);
                Cell cellDate = row.createCell(0);
                cellDate.setCellValue(date);
                cellDate.setCellStyle(dateStyle);

                int normalCount = dayNormalCountMap.getOrDefault(day, 0);
                int specialCount = daySpecialCountMap.getOrDefault(day, 0);
                int totalCount = normalCount + specialCount;
                BigDecimal amount = pmcNormalPrice.multiply(BigDecimal.valueOf(normalCount))
                        .add(pmcSpecialPrice.multiply(BigDecimal.valueOf(specialCount)));

                pmcTotalNormal += normalCount;
                pmcTotalSpecial += specialCount;
                pmcTotalAmount = pmcTotalAmount.add(amount);

                Cell cellNormal = row.createCell(1);
                if (normalCount > 0) {
                    cellNormal.setCellValue(normalCount);
                }
                cellNormal.setCellStyle(centerStyle);

                Cell cellSpecial = row.createCell(2);
                if (specialCount > 0) {
                    cellSpecial.setCellValue(specialCount);
                }
                cellSpecial.setCellStyle(centerStyle);

                Cell cellTotal = row.createCell(3);
                cellTotal.setCellValue(totalCount);
                cellTotal.setCellStyle(centerStyle);

                Cell cellAmount = row.createCell(4);
                cellAmount.setCellValue(amount.doubleValue());
                cellAmount.setCellStyle(moneyStyle);

                Cell cellNote = row.createCell(5);
                cellNote.setCellValue("");
                cellNote.setCellStyle(baseStyle);
            }

            // Total row
            Row pmcTotalRow = pmcSheet.createRow(pmcRowIdx++);
            pmcTotalRow.setHeightInPoints(24);

            Cell cellTotalLabel = pmcTotalRow.createCell(0);
            cellTotalLabel.setCellValue("TỔNG");
            cellTotalLabel.setCellStyle(headerStyle);

            Cell cellTotalNormalVal = pmcTotalRow.createCell(1);
            cellTotalNormalVal.setCellValue(pmcTotalNormal);
            cellTotalNormalVal.setCellStyle(headerStyle);

            Cell cellTotalSpecialVal = pmcTotalRow.createCell(2);
            cellTotalSpecialVal.setCellValue(pmcTotalSpecial);
            cellTotalSpecialVal.setCellStyle(headerStyle);

            Cell cellTotalAllCount = pmcTotalRow.createCell(3);
            cellTotalAllCount.setCellValue(pmcTotalNormal + pmcTotalSpecial);
            cellTotalAllCount.setCellStyle(headerStyle);

            Cell cellTotalAmountVal = pmcTotalRow.createCell(4);
            cellTotalAmountVal.setCellValue(pmcTotalAmount.doubleValue());
            cellTotalAmountVal.setCellStyle(moneyStyle);

            Cell cellTotalNote = pmcTotalRow.createCell(5);
            cellTotalNote.setCellStyle(headerStyle);

            // PMC row
            Row signatureRow = pmcSheet.createRow(pmcRowIdx++);
            signatureRow.setHeightInPoints(22);
            Cell sigCell = signatureRow.createCell(0);
            sigCell.setCellValue("PMC");
            sigCell.setCellStyle(baseStyle);
            for (int i = 1; i < 6; i++) {
                signatureRow.createCell(i).setCellStyle(baseStyle);
            }

            // Auto-size columns
            for (int col = 0; col < 6; col++) {
                pmcSheet.autoSizeColumn(col);
                int currentWidth = pmcSheet.getColumnWidth(col);
                int minWidth = 12 * 256;
                if (col == 4)
                    minWidth = 25 * 256;
                if (currentWidth < minWidth) {
                    pmcSheet.setColumnWidth(col, minWidth);
                }
            }

            // ================== SHEETS 3+: DAILY SHEETS 1 TO N ==================
            for (int day = 1; day <= totalDays; day++) {
                String daySheetName = String.valueOf(day);
                Sheet daySheet = workbook.createSheet(daySheetName);
                daySheet.setZoom(120);

                // Row 0
                Row r0 = daySheet.createRow(0);
                r0.setHeightInPoints(24);

                Cell c0Title = r0.createCell(0);
                c0Title.setCellValue("ĐĂNG KÝ VÉ CƠM NGÀY " + String.format("%02d", day));
                c0Title.setCellStyle(titleStyle);

                Cell c0Normal = r0.createCell(4);
                c0Normal.setCellValue("Thường");
                c0Normal.setCellStyle(headerStyle);

                Cell c0Special = r0.createCell(5);
                c0Special.setCellValue("Đặc biệt");
                c0Special.setCellStyle(headerStyle);

                // Row 1
                Row r1 = daySheet.createRow(1);
                r1.setHeightInPoints(24);

                Cell c1Stt = r1.createCell(0);
                c1Stt.setCellValue("STT");
                c1Stt.setCellStyle(headerStyle);

                Cell c1Name = r1.createCell(1);
                c1Name.setCellValue("Họ và tên");
                c1Name.setCellStyle(headerStyle);

                Cell c1Reg = r1.createCell(2);
                c1Reg.setCellValue("Đăng ký ");
                c1Reg.setCellStyle(headerStyle);

                Cell c1Note = r1.createCell(3);
                c1Note.setCellValue("Ghi chú");
                c1Note.setCellStyle(headerStyle);

                int normalCount = dayNormalCountMap.getOrDefault(day, 0);
                int specialCount = daySpecialCountMap.getOrDefault(day, 0);

                Cell c1NormalVal = r1.createCell(4);
                c1NormalVal.setCellValue(normalCount);
                c1NormalVal.setCellStyle(headerStyle);

                Cell c1SpecialVal = r1.createCell(5);
                c1SpecialVal.setCellValue(specialCount);
                c1SpecialVal.setCellStyle(headerStyle);

                // Populate users who registered for this day
                int dayUserRowIdx = 2;
                for (int uIdx = 0; uIdx < summary.getItems().size(); uIdx++) {
                    OrderSummaryItemResponse item = summary.getItems().get(uIdx);
                    Map<Integer, String> dayMealMap = userDayMealMap.getOrDefault(item.getUserId(), new HashMap<>());
                    String mealType = dayMealMap.get(day);
                    if (mealType != null) {
                        Row userRow = daySheet.createRow(dayUserRowIdx++);
                        userRow.setHeightInPoints(22);

                        Cell cellStt = userRow.createCell(0);
                        cellStt.setCellValue(uIdx + 1);
                        cellStt.setCellStyle(centerStyle);

                        Cell cellName = userRow.createCell(1);
                        cellName.setCellValue(item.getFullName() != null ? item.getFullName() : "");
                        cellName.setCellStyle(dataStyle);

                        Cell cellRegVal = userRow.createCell(2);
                        cellRegVal.setCellValue(mealType.toLowerCase()); // "x" or "xx"
                        cellRegVal.setCellStyle(centerStyle);

                        Cell cellNoteVal = userRow.createCell(3);
                        cellNoteVal.setCellValue("");
                        cellNoteVal.setCellStyle(baseStyle);

                        userRow.createCell(4).setCellStyle(baseStyle);
                        userRow.createCell(5).setCellStyle(baseStyle);
                    }
                }

                // Auto-size columns
                for (int col = 0; col < 6; col++) {
                    daySheet.autoSizeColumn(col);
                    int currentWidth = daySheet.getColumnWidth(col);
                    int minWidth = 10 * 256;
                    if (col == 1)
                        minWidth = 25 * 256;
                    if (currentWidth < minWidth) {
                        daySheet.setColumnWidth(col, minWidth);
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export monthly matrix Excel for month {} year {}", month, year, e);
            throw new RuntimeException("Failed to export monthly Excel", e);
        }
    }
}
