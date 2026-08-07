package vn.vnpost.lunchorder.tools.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelImportServiceTest {

    private final ExcelImportService service = new ExcelImportService();

    @Test
    void readRowsSkipsHeaderAndKeepsExcelRowNumbers() throws IOException {
        byte[] file = buildWorkbook(sheet -> {
            writeTextRow(sheet, 0, "Tài khoản", "Họ tên");
            writeTextRow(sheet, 1, "0912345678", "Nguyễn Văn A");
            writeTextRow(sheet, 2, "0912345679", "Trần Thị B");
        });

        List<ExcelRow> rows = service.readRows(new ByteArrayInputStream(file), 2, 1);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        assertThat(rows.get(0).cell(0)).isEqualTo("0912345678");
        assertThat(rows.get(1).rowNumber()).isEqualTo(3);
        assertThat(rows.get(1).cell(1)).isEqualTo("Trần Thị B");
    }

    @Test
    void readRowsKeepsNumericAccountsAsPlainDigits() throws IOException {
        byte[] file = buildWorkbook(sheet -> {
            writeTextRow(sheet, 0, "Tài khoản");
            sheet.createRow(1).createCell(0).setCellValue(912345678901d);
        });

        List<ExcelRow> rows = service.readRows(new ByteArrayInputStream(file), 1, 1);

        assertThat(rows.get(0).cell(0)).isEqualTo("912345678901");
    }

    @Test
    void readRowsKeepsLeadingZeroWhenCellIsTextFormatted() throws IOException {
        byte[] file = buildWorkbook(sheet -> {
            writeTextRow(sheet, 0, "Tài khoản");
            writeTextRow(sheet, 1, "0123456789");
        });

        List<ExcelRow> rows = service.readRows(new ByteArrayInputStream(file), 1, 1);

        assertThat(rows.get(0).cell(0)).isEqualTo("0123456789");
    }

    @Test
    void readRowsSkipsBlankRowsAndPadsMissingCells() throws IOException {
        byte[] file = buildWorkbook(sheet -> {
            writeTextRow(sheet, 0, "Tài khoản", "Họ tên", "Phòng ban");
            writeTextRow(sheet, 1, "0912345678");
            sheet.createRow(2);
            writeTextRow(sheet, 3, "   ", "", "");
        });

        List<ExcelRow> rows = service.readRows(new ByteArrayInputStream(file), 3, 1);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).cells()).containsExactly("0912345678", "", "");
    }

    @Test
    void readRowsReturnsEmptyListWhenOnlyHeaderIsPresent() throws IOException {
        byte[] file = buildWorkbook(sheet -> writeTextRow(sheet, 0, "Tài khoản"));

        assertThat(service.readRows(new ByteArrayInputStream(file), 1, 1)).isEmpty();
    }

    private void writeTextRow(Sheet sheet, int rowIdx, String... values) {
        CellStyle textStyle = sheet.getWorkbook().createCellStyle();
        textStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("@"));

        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(textStyle);
            cell.setCellValue(values[i]);
        }
    }

    private byte[] buildWorkbook(SheetWriter writer) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.write(workbook.createSheet("data"));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @FunctionalInterface
    private interface SheetWriter {
        void write(Sheet sheet);
    }
}
