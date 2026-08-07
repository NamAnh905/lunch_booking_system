package vn.vnpost.lunchorder.tools.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<ExcelRow> readRows(InputStream inputStream, int columnCount, int headerRowCount) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            List<ExcelRow> rows = new ArrayList<>();
            if (workbook.getNumberOfSheets() == 0) {
                return rows;
            }

            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIdx = headerRowCount; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                List<String> cells = new ArrayList<>(columnCount);
                for (int colIdx = 0; colIdx < columnCount; colIdx++) {
                    cells.add(row == null ? "" : toStringValue(row.getCell(colIdx)));
                }

                ExcelRow excelRow = new ExcelRow(rowIdx + 1, cells);
                if (!excelRow.isEmpty()) {
                    rows.add(excelRow);
                }
            }
            return rows;
        }
    }

    private String toStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();

        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> numericToString(cell);
            default -> "";
        };
    }

    private String numericToString(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER);
        }
        return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    }
}
