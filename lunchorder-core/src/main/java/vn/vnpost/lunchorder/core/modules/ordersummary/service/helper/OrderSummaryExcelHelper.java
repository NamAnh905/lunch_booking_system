package vn.vnpost.lunchorder.core.modules.ordersummary.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.systemconfig.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.MonthlyOrderDetail;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.OrderSummaryItemResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

            ExcelStyles styles = buildStyles(workbook);

            Sheet sheet = workbook
                    .createSheet("Tổng hợp suất ăn " + date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            sheet.setZoom(150); // Set zoom level to 150% so it looks larger and fills the screen

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(40);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("TỔNG HỢP SUẤT ĂN NGÀY " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            titleCell.setCellStyle(styles.title);

            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(28);
            String[] headers = { "STT", "Họ tên", "Phòng ban", "Suất thường", "Suất đặc biệt", "Thành tiền" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styles.header);
            }

            int rowIdx = 3;
            for (int i = 0; i < summary.getItems().size(); i++) {
                OrderSummaryItemResponse item = summary.getItems().get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                Cell cellStt = row.createCell(0);
                cellStt.setCellValue(i + 1);
                cellStt.setCellStyle(styles.center);

                Cell cellName = row.createCell(1);
                cellName.setCellValue(item.getFullName());
                cellName.setCellStyle(styles.data);

                Cell cellDept = row.createCell(2);
                cellDept.setCellValue(item.getDepartmentName());
                cellDept.setCellStyle(styles.data);

                Cell cellNormal = row.createCell(3);
                cellNormal.setCellValue(item.getNormalMealCount());
                cellNormal.setCellStyle(styles.center);

                Cell cellSpecial = row.createCell(4);
                cellSpecial.setCellValue(item.getSpecialMealCount());
                cellSpecial.setCellStyle(styles.center);

                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(item.getTotalAmount().doubleValue());
                amountCell.setCellStyle(styles.money);
            }

            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.setHeightInPoints(24);

            for (int i = 0; i < 2; i++) {
                Cell cell = summaryRow.createCell(i);
                cell.setCellStyle(styles.base);
            }

            Cell summaryLabel = summaryRow.createCell(2);
            summaryLabel.setCellValue("TỔNG CỘNG");
            summaryLabel.setCellStyle(styles.header);

            Cell sumNormal = summaryRow.createCell(3);
            sumNormal.setCellValue(summary.getTotalNormalMeals());
            sumNormal.setCellStyle(styles.header);

            Cell sumSpecial = summaryRow.createCell(4);
            sumSpecial.setCellValue(summary.getTotalSpecialMeals());
            sumSpecial.setCellStyle(styles.header);

            Cell totalCell = summaryRow.createCell(5);
            totalCell.setCellValue(summary.getTotalAmount().doubleValue());
            totalCell.setCellStyle(styles.money);

            int[] columnWidths = { 8, 25, 20, 15, 15, 15 };
            for (int i = 0; i < columnWidths.length; i++) {
                sheet.setColumnWidth(i, columnWidths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export daily Excel for date {}", date, e);
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    public byte[] exportMonthlyMatrixExcel(int month, int year, MonthlyOrderSummaryResponse summary,
            List<MonthlyOrderDetail> detailRecords, BigDecimal normalPrice, BigDecimal specialPrice) {
        Map<Long, Map<Integer, String>> userDayMealMap = new HashMap<>();
        Map<Integer, Integer> dayNormalCountMap = new HashMap<>();
        Map<Integer, Integer> daySpecialCountMap = new HashMap<>();

        for (MonthlyOrderDetail record : detailRecords) {
            if (record.getUserId() == null || record.getOrderDate() == null) {
                continue;
            }
            Long userId = record.getUserId();
            LocalDate date = record.getOrderDate();

            BigDecimal price = record.getPrice() != null ? record.getPrice() : BigDecimal.ZERO;
            boolean isSpecial = price.compareTo(normalPrice) > 0;

            int day = date.getDayOfMonth();
            userDayMealMap
                    .computeIfAbsent(userId, k -> new HashMap<>())
                    .put(day, isSpecial ? "XX" : "X");

            if (isSpecial) {
                daySpecialCountMap.put(day, daySpecialCountMap.getOrDefault(day, 0) + 1);
            } else {
                dayNormalCountMap.put(day, dayNormalCountMap.getOrDefault(day, 0) + 1);
            }
        }
        
        Map<Integer, List<Integer>> dayUserIndexMap = new HashMap<>();
        for (int uIdx = 0; uIdx < summary.getItems().size(); uIdx++) {
            Long userId = summary.getItems().get(uIdx).getUserId();
            for (Integer day : userDayMealMap.getOrDefault(userId, Collections.emptyMap()).keySet()) {
                dayUserIndexMap.computeIfAbsent(day, k -> new ArrayList<>()).add(uIdx);
            }
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            ExcelStyles styles = buildStyles(workbook);

            Sheet mainSheet = workbook.createSheet("Cơm trưa tháng " + month + "-" + year);
            mainSheet.setZoom(120); // Zoom 120% for matrix view

            Row titleRow = mainSheet.createRow(0);
            titleRow.setHeightInPoints(40);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG THEO DÕI ĐẶT CƠM - THÁNG " + month + "/" + year);
            titleCell.setCellStyle(styles.title);

            YearMonth yearMonth = YearMonth.of(year, month);
            int totalDays = yearMonth.lengthOfMonth();

            Row dowRow = mainSheet.createRow(2);
            dowRow.setHeightInPoints(28);

            Cell cellSttDow = dowRow.createCell(0);
            cellSttDow.setCellValue("STT");
            cellSttDow.setCellStyle(styles.header);

            Cell cellNameDow = dowRow.createCell(1);
            cellNameDow.setCellValue("Họ và tên");
            cellNameDow.setCellStyle(styles.header);

            Cell cellDeptDow = dowRow.createCell(2);
            cellDeptDow.setCellValue("Bộ phận");
            cellDeptDow.setCellStyle(styles.header);

            String[] weekdayNames = { "CN", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy" };
            for (int day = 1; day <= totalDays; day++) {
                LocalDate date = yearMonth.atDay(day);
                int dowIndex = date.getDayOfWeek().getValue() % 7;
                Cell cell = dowRow.createCell(3 + day - 1);
                cell.setCellValue(weekdayNames[dowIndex]);
                cell.setCellStyle(styles.header);
            }

            int nextColIdx = 3 + totalDays;
            String[] summaryHeaders = { "Tổng thường", "Tổng đặc biệt", "Tiền cần TT", "Tiền đã TT", "Còn lại" };
            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = dowRow.createCell(nextColIdx + i);
                cell.setCellValue(summaryHeaders[i]);
                cell.setCellStyle(styles.header);
            }

            Row dayNumRow = mainSheet.createRow(3);
            dayNumRow.setHeightInPoints(24);

            Cell cellSttDayNum = dayNumRow.createCell(0);
            cellSttDayNum.setCellStyle(styles.header);

            Cell cellNameDayNum = dayNumRow.createCell(1);
            cellNameDayNum.setCellStyle(styles.header);

            Cell cellDeptDayNum = dayNumRow.createCell(2);
            cellDeptDayNum.setCellStyle(styles.header);

            for (int day = 1; day <= totalDays; day++) {
                Cell cell = dayNumRow.createCell(3 + day - 1);
                cell.setCellValue(day);
                cell.setCellStyle(styles.header);
            }

            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = dayNumRow.createCell(nextColIdx + i);
                cell.setCellStyle(styles.header);
            }

            int rowIdx = 4;
            for (int i = 0; i < summary.getItems().size(); i++) {
                OrderSummaryItemResponse item = summary.getItems().get(i);
                Row row = mainSheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                Cell cellStt = row.createCell(0);
                cellStt.setCellValue(i + 1);
                cellStt.setCellStyle(styles.center);

                Cell cellName = row.createCell(1);
                cellName.setCellValue(item.getFullName() != null ? item.getFullName() : "");
                cellName.setCellStyle(styles.data);

                Cell cellDept = row.createCell(2);
                cellDept.setCellValue(item.getDepartmentName() != null ? item.getDepartmentName() : "");
                cellDept.setCellStyle(styles.data);

                Map<Integer, String> dayMealMap = userDayMealMap.getOrDefault(item.getUserId(), Collections.emptyMap());
                for (int day = 1; day <= totalDays; day++) {
                    Cell cell = row.createCell(3 + day - 1);
                    String mealType = dayMealMap.get(day);
                    cell.setCellValue(mealType != null ? mealType : "");
                    cell.setCellStyle(styles.center);
                }

                Cell cellNormal = row.createCell(nextColIdx);
                cellNormal.setCellValue(item.getNormalMealCount());
                cellNormal.setCellStyle(styles.center);

                Cell cellSpecial = row.createCell(nextColIdx + 1);
                cellSpecial.setCellValue(item.getSpecialMealCount());
                cellSpecial.setCellStyle(styles.center);

                Cell cellAmount = row.createCell(nextColIdx + 2);
                cellAmount.setCellValue(item.getTotalAmount() != null ? item.getTotalAmount().doubleValue() : 0.0);
                cellAmount.setCellStyle(styles.money);

                Cell cellPaid = row.createCell(nextColIdx + 3);
                cellPaid.setCellValue(item.getTotalPaid() != null ? item.getTotalPaid().doubleValue() : 0.0);
                cellPaid.setCellStyle(styles.money);

                Cell cellRemaining = row.createCell(nextColIdx + 4);
                cellRemaining.setCellValue(
                        item.getRemainingAmount() != null ? item.getRemainingAmount().doubleValue() : 0.0);
                cellRemaining.setCellStyle(styles.money);
            }

            Row totalRow = mainSheet.createRow(rowIdx + 1);
            totalRow.setHeightInPoints(24);

            for (int col = 0; col < nextColIdx; col++) {
                Cell cell = totalRow.createCell(col);
                cell.setCellStyle(styles.base);
            }
            Cell totalLabelCell = totalRow.createCell(1);
            totalLabelCell.setCellValue("TỔNG CỘNG");
            totalLabelCell.setCellStyle(styles.header);

            Cell totalNormalCell = totalRow.createCell(nextColIdx);
            totalNormalCell.setCellValue(summary.getTotalNormalMeals());
            totalNormalCell.setCellStyle(styles.header);

            Cell totalSpecialCell = totalRow.createCell(nextColIdx + 1);
            totalSpecialCell.setCellValue(summary.getTotalSpecialMeals());
            totalSpecialCell.setCellStyle(styles.header);

            Cell totalAmountCell = totalRow.createCell(nextColIdx + 2);
            totalAmountCell
                    .setCellValue(summary.getTotalAmount() != null ? summary.getTotalAmount().doubleValue() : 0.0);
            totalAmountCell.setCellStyle(styles.money);

            Cell totalPaidCell = totalRow.createCell(nextColIdx + 3);
            totalPaidCell.setCellValue(summary.getTotalPaid() != null ? summary.getTotalPaid().doubleValue() : 0.0);
            totalPaidCell.setCellStyle(styles.money);

            Cell totalRemainingCell = totalRow.createCell(nextColIdx + 4);
            totalRemainingCell.setCellValue(
                    summary.getTotalRemaining() != null ? summary.getTotalRemaining().doubleValue() : 0.0);
            totalRemainingCell.setCellStyle(styles.money);

            for (int col = 0; col < nextColIdx + 5; col++) {
                int width;
                if (col == 1) {
                    width = 25 * 256; // Name
                } else if (col == 2) {
                    width = 20 * 256; // Department
                } else if (col < nextColIdx) {
                    width = 8 * 256; // STT / day columns
                } else {
                    width = 15 * 256; // Summary columns
                }
                mainSheet.setColumnWidth(col, width);
            }

            Sheet pmcSheet = workbook.createSheet("Đặt cơm PMC");
            pmcSheet.setZoom(120);

            Row pmcTitleRow = pmcSheet.createRow(0);
            pmcTitleRow.setHeightInPoints(40);
            Cell pmcTitleCell = pmcTitleRow.createCell(0);
            pmcTitleCell.setCellValue("ĐĂNG KÝ CƠM - PMC");
            pmcTitleCell.setCellStyle(styles.title);

            Row pmcHeaderRow = pmcSheet.createRow(1);
            pmcHeaderRow.setHeightInPoints(28);
            String[] pmcHeaders = { "Ngày", "Suất thường", "Suất tăng cường", "TỔNG", "Số tiền cần TT cho PMC",
                    "Ghi chú" };
            for (int i = 0; i < pmcHeaders.length; i++) {
                Cell cell = pmcHeaderRow.createCell(i);
                cell.setCellValue(pmcHeaders[i]);
                cell.setCellStyle(styles.header);
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
                cellDate.setCellStyle(styles.date);

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
                cellNormal.setCellStyle(styles.center);

                Cell cellSpecial = row.createCell(2);
                if (specialCount > 0) {
                    cellSpecial.setCellValue(specialCount);
                }
                cellSpecial.setCellStyle(styles.center);

                Cell cellTotal = row.createCell(3);
                cellTotal.setCellValue(totalCount);
                cellTotal.setCellStyle(styles.center);

                Cell cellAmount = row.createCell(4);
                cellAmount.setCellValue(amount.doubleValue());
                cellAmount.setCellStyle(styles.money);

                Cell cellNote = row.createCell(5);
                cellNote.setCellValue("");
                cellNote.setCellStyle(styles.base);
            }

            Row pmcTotalRow = pmcSheet.createRow(pmcRowIdx++);
            pmcTotalRow.setHeightInPoints(24);

            Cell cellTotalLabel = pmcTotalRow.createCell(0);
            cellTotalLabel.setCellValue("TỔNG");
            cellTotalLabel.setCellStyle(styles.header);

            Cell cellTotalNormalVal = pmcTotalRow.createCell(1);
            cellTotalNormalVal.setCellValue(pmcTotalNormal);
            cellTotalNormalVal.setCellStyle(styles.header);

            Cell cellTotalSpecialVal = pmcTotalRow.createCell(2);
            cellTotalSpecialVal.setCellValue(pmcTotalSpecial);
            cellTotalSpecialVal.setCellStyle(styles.header);

            Cell cellTotalAllCount = pmcTotalRow.createCell(3);
            cellTotalAllCount.setCellValue(pmcTotalNormal + pmcTotalSpecial);
            cellTotalAllCount.setCellStyle(styles.header);

            Cell cellTotalAmountVal = pmcTotalRow.createCell(4);
            cellTotalAmountVal.setCellValue(pmcTotalAmount.doubleValue());
            cellTotalAmountVal.setCellStyle(styles.money);

            Cell cellTotalNote = pmcTotalRow.createCell(5);
            cellTotalNote.setCellStyle(styles.header);

            Row signatureRow = pmcSheet.createRow(pmcRowIdx++);
            signatureRow.setHeightInPoints(22);
            Cell sigCell = signatureRow.createCell(0);
            sigCell.setCellValue("PMC");
            sigCell.setCellStyle(styles.base);
            for (int i = 1; i < 6; i++) {
                signatureRow.createCell(i).setCellStyle(styles.base);
            }

            for (int col = 0; col < 6; col++) {
                int width = (col == 4) ? 25 * 256 : 12 * 256;
                pmcSheet.setColumnWidth(col, width);
            }

            for (int day = 1; day <= totalDays; day++) {
                String daySheetName = String.valueOf(day);
                Sheet daySheet = workbook.createSheet(daySheetName);
                daySheet.setZoom(120);

                Row r0 = daySheet.createRow(0);
                r0.setHeightInPoints(24);

                Cell c0Title = r0.createCell(0);
                c0Title.setCellValue("ĐĂNG KÝ VÉ CƠM NGÀY " + String.format("%02d", day));
                c0Title.setCellStyle(styles.title);

                Cell c0Normal = r0.createCell(4);
                c0Normal.setCellValue("Thường");
                c0Normal.setCellStyle(styles.header);

                Cell c0Special = r0.createCell(5);
                c0Special.setCellValue("Đặc biệt");
                c0Special.setCellStyle(styles.header);

                Row r1 = daySheet.createRow(1);
                r1.setHeightInPoints(24);

                Cell c1Stt = r1.createCell(0);
                c1Stt.setCellValue("STT");
                c1Stt.setCellStyle(styles.header);

                Cell c1Name = r1.createCell(1);
                c1Name.setCellValue("Họ và tên");
                c1Name.setCellStyle(styles.header);

                Cell c1Reg = r1.createCell(2);
                c1Reg.setCellValue("Đăng ký ");
                c1Reg.setCellStyle(styles.header);

                Cell c1Note = r1.createCell(3);
                c1Note.setCellValue("Ghi chú");
                c1Note.setCellStyle(styles.header);

                int normalCount = dayNormalCountMap.getOrDefault(day, 0);
                int specialCount = daySpecialCountMap.getOrDefault(day, 0);

                Cell c1NormalVal = r1.createCell(4);
                c1NormalVal.setCellValue(normalCount);
                c1NormalVal.setCellStyle(styles.header);

                Cell c1SpecialVal = r1.createCell(5);
                c1SpecialVal.setCellValue(specialCount);
                c1SpecialVal.setCellStyle(styles.header);

                // Populate users who registered for this day (pre-computed, O(records) overall)
                int dayUserRowIdx = 2;
                for (int uIdx : dayUserIndexMap.getOrDefault(day, Collections.emptyList())) {
                    OrderSummaryItemResponse item = summary.getItems().get(uIdx);
                    String mealType = userDayMealMap.get(item.getUserId()).get(day);

                    Row userRow = daySheet.createRow(dayUserRowIdx++);
                    userRow.setHeightInPoints(22);

                    Cell cellStt = userRow.createCell(0);
                    cellStt.setCellValue(uIdx + 1);
                    cellStt.setCellStyle(styles.center);

                    Cell cellName = userRow.createCell(1);
                    cellName.setCellValue(item.getFullName() != null ? item.getFullName() : "");
                    cellName.setCellStyle(styles.data);

                    Cell cellRegVal = userRow.createCell(2);
                    cellRegVal.setCellValue(mealType.toLowerCase()); // "x" or "xx"
                    cellRegVal.setCellStyle(styles.center);

                    Cell cellNoteVal = userRow.createCell(3);
                    cellNoteVal.setCellValue("");
                    cellNoteVal.setCellStyle(styles.base);

                    userRow.createCell(4).setCellStyle(styles.base);
                    userRow.createCell(5).setCellStyle(styles.base);
                }

                for (int col = 0; col < 6; col++) {
                    int width = (col == 1) ? 25 * 256 : 10 * 256;
                    daySheet.setColumnWidth(col, width);
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export monthly matrix Excel for month {} year {}", month, year, e);
            throw new RuntimeException("Failed to export monthly Excel", e);
        }
    }

    private ExcelStyles buildStyles(Workbook workbook) {
        CellStyle baseStyle = workbook.createCellStyle();
        baseStyle.setBorderBottom(BorderStyle.THIN);
        baseStyle.setBorderTop(BorderStyle.THIN);
        baseStyle.setBorderLeft(BorderStyle.THIN);
        baseStyle.setBorderRight(BorderStyle.THIN);
        baseStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.cloneStyleFrom(baseStyle);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.cloneStyleFrom(baseStyle);
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.cloneStyleFrom(baseStyle);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        DataFormat format = workbook.createDataFormat();

        CellStyle moneyStyle = workbook.createCellStyle();
        moneyStyle.cloneStyleFrom(baseStyle);
        moneyStyle.setDataFormat(format.getFormat("#,##0"));
        moneyStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(baseStyle);
        dateStyle.setDataFormat(format.getFormat("dd/MM/yyyy"));
        dateStyle.setAlignment(HorizontalAlignment.CENTER);

        return new ExcelStyles(baseStyle, headerStyle, titleStyle, dataStyle, centerStyle, moneyStyle, dateStyle);
    }

    private static final class ExcelStyles {
        final CellStyle base;
        final CellStyle header;
        final CellStyle title;
        final CellStyle data;
        final CellStyle center;
        final CellStyle money;
        final CellStyle date;

        private ExcelStyles(CellStyle base, CellStyle header, CellStyle title, CellStyle data,
                CellStyle center, CellStyle money, CellStyle date) {
            this.base = base;
            this.header = header;
            this.title = title;
            this.data = data;
            this.center = center;
            this.money = money;
            this.date = date;
        }
    }
}
