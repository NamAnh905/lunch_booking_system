package vn.vnpost.lunchorder.system.modules.user.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class UserImportExcelHelper {

    public static final int COLUMN_USERNAME = 0;
    public static final int COLUMN_PASSWORD = 1;
    public static final int COLUMN_FULL_NAME = 2;
    public static final int COLUMN_DEPARTMENT = 3;
    public static final int COLUMN_ROLES = 4;

    public static final int COLUMN_COUNT = 5;
    public static final int HEADER_ROW_COUNT = 1;

    private static final String DATA_SHEET_NAME = "Danh sách người dùng";
    private static final String GUIDE_SHEET_NAME = "Hướng dẫn";

    private static final String[] HEADERS = { "Tài khoản", "Mật khẩu", "Họ tên", "Phòng ban", "Vai trò" };
    private static final int[] COLUMN_WIDTHS = { 20, 20, 30, 30, 20 };

    private static final String[] GUIDE_LINES = {
            "HƯỚNG DẪN NHẬP DANH SÁCH NGƯỜI DÙNG",
            "",
            "Điền dữ liệu vào sheet \"" + DATA_SHEET_NAME + "\", bắt đầu từ dòng thứ 2. Không sửa dòng tiêu đề.",
            "",
            "Tài khoản: bắt buộc, chỉ gồm chữ số, độ dài 10 - 50 ký tự, không được trùng với tài khoản đã có.",
            "Mật khẩu: bắt buộc, độ dài 8 - 255 ký tự, không chứa khoảng trắng.",
            "Họ tên: bắt buộc, tối đa 255 ký tự, chỉ gồm chữ cái và khoảng trắng.",
            "Phòng ban: bắt buộc, nhập đúng tên hoặc mã phòng ban đã có trong hệ thống.",
            "Vai trò: có thể bỏ trống (mặc định là USER). Nhiều vai trò thì cách nhau bởi dấu phẩy.",
            "",
            "Ví dụ một dòng dữ liệu:",
            "0123456789 | Abc@12345 | Nguyễn Văn A | Phòng Kỹ thuật | USER"
    };

    public byte[] buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildDataSheet(workbook);
            buildGuideSheet(workbook);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to build the user import template", e);
            throw new AppException(ErrorCode.EXPORT_FAILED);
        }
    }

    private void buildDataSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(DATA_SHEET_NAME);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
        }

        sheet.setDefaultColumnStyle(COLUMN_USERNAME, textStyle);
        sheet.setDefaultColumnStyle(COLUMN_PASSWORD, textStyle);
    }

    private void buildGuideSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(GUIDE_SHEET_NAME);
        sheet.setColumnWidth(0, 120 * 256);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        for (int i = 0; i < GUIDE_LINES.length; i++) {
            Cell cell = sheet.createRow(i).createCell(0);
            cell.setCellValue(GUIDE_LINES[i]);
            if (i == 0) {
                cell.setCellStyle(titleStyle);
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return style;
    }
}
