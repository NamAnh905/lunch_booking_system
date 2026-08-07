package vn.vnpost.lunchorder.core.modules.ordersummary.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.OrderSummaryItemResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class OrderSummaryExcelHelper {

    private static final String[] HEADERS = { "STT", "Họ tên", "Phòng ban", "Suất thường", "Suất tăng cường",
            "Thành tiền", "Ghi chú" };
    private static final Set<Integer> AUTO_SIZED_COLUMNS = Set.of(0, 1, 2, 6);

    private static final int CHAR_WIDTH = 256;
    private static final int FIXED_COLUMN_CHARS = 15;
    private static final int MIN_AUTO_SIZED_CHARS = 8;
    private static final int MAX_AUTO_SIZED_CHARS = 50;
    private static final int AUTO_SIZED_PADDING_CHARS = 2;

    public byte[] exportDailyExcel(LocalDate date, DailyOrderSummaryResponse summary) {
        String sheetName = "Tổng hợp suất ăn " + date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        return buildSummaryWorkbook(sheetName, summary.getItems(), summary.getTotalNormalMeals(),
                summary.getTotalSpecialMeals(), summary.getTotalAmount());
    }

    public byte[] exportMonthlyExcel(int month, int year, MonthlyOrderSummaryResponse summary) {
        String sheetName = "Tổng hợp suất ăn " + month + "-" + year;

        return buildSummaryWorkbook(sheetName, summary.getItems(), summary.getTotalNormalMeals(),
                summary.getTotalSpecialMeals(), summary.getTotalAmount());
    }

    private byte[] buildSummaryWorkbook(String sheetName, List<OrderSummaryItemResponse> items,
            int totalNormalMeals, int totalSpecialMeals, BigDecimal totalAmount) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            ExcelStyles styles = buildStyles(workbook);

            Sheet sheet = workbook.createSheet(sheetName);
            sheet.setZoom(100); // Set zoom level to 150% so it looks larger and fills the screen

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(styles.header());
            }

            int rowIdx = 1;
            for (int i = 0; i < items.size(); i++) {
                OrderSummaryItemResponse item = items.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                Cell cellStt = row.createCell(0);
                cellStt.setCellValue(i + 1);
                cellStt.setCellStyle(styles.center());

                Cell cellName = row.createCell(1);
                cellName.setCellValue(item.getFullName() != null ? item.getFullName() : "");
                cellName.setCellStyle(styles.data());

                Cell cellDept = row.createCell(2);
                cellDept.setCellValue(item.getDepartmentName() != null ? item.getDepartmentName() : "");
                cellDept.setCellStyle(styles.data());

                Cell cellNormal = row.createCell(3);
                cellNormal.setCellValue(item.getNormalMealCount());
                cellNormal.setCellStyle(styles.center());

                Cell cellSpecial = row.createCell(4);
                cellSpecial.setCellValue(item.getSpecialMealCount());
                cellSpecial.setCellStyle(styles.center());

                Cell cellAmount = row.createCell(5);
                cellAmount.setCellValue(item.getTotalAmount() != null ? item.getTotalAmount().doubleValue() : 0.0);
                cellAmount.setCellStyle(styles.money());

                Cell cellNote = row.createCell(6);
                cellNote.setCellValue(item.getNote() != null ? item.getNote() : "");
                cellNote.setCellStyle(styles.data());
            }

            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.setHeightInPoints(24);

            for (int i = 0; i < 2; i++) {
                Cell cell = summaryRow.createCell(i);
                cell.setCellStyle(styles.base());
            }

            Cell summaryLabel = summaryRow.createCell(2);
            summaryLabel.setCellValue("TỔNG CỘNG");
            summaryLabel.setCellStyle(styles.header());

            Cell sumNormal = summaryRow.createCell(3);
            sumNormal.setCellValue(totalNormalMeals);
            sumNormal.setCellStyle(styles.header());

            Cell sumSpecial = summaryRow.createCell(4);
            sumSpecial.setCellValue(totalSpecialMeals);
            sumSpecial.setCellStyle(styles.header());

            Cell totalCell = summaryRow.createCell(5);
            totalCell.setCellValue(totalAmount != null ? totalAmount.doubleValue() : 0.0);
            totalCell.setCellStyle(styles.money());

            summaryRow.createCell(6).setCellStyle(styles.base());

            applyColumnWidths(sheet);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export Excel for sheet {}", sheetName, e);
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    private void applyColumnWidths(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (!AUTO_SIZED_COLUMNS.contains(i)) {
                sheet.setColumnWidth(i, FIXED_COLUMN_CHARS * CHAR_WIDTH);
                continue;
            }

            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i) + AUTO_SIZED_PADDING_CHARS * CHAR_WIDTH;
            sheet.setColumnWidth(i, Math.clamp(width, MIN_AUTO_SIZED_CHARS * CHAR_WIDTH,
                    MAX_AUTO_SIZED_CHARS * CHAR_WIDTH));
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

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.cloneStyleFrom(baseStyle);
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.cloneStyleFrom(baseStyle);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle moneyStyle = workbook.createCellStyle();
        moneyStyle.cloneStyleFrom(baseStyle);
        moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        moneyStyle.setAlignment(HorizontalAlignment.RIGHT);

        return new ExcelStyles(baseStyle, headerStyle, dataStyle, centerStyle, moneyStyle);
    }

    private record ExcelStyles(CellStyle base, CellStyle header, CellStyle data,
            CellStyle center, CellStyle money) {
    }
}
