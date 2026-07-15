package vn.vnpost.lunchorder.tools.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelExportService {

    public <T> ByteArrayInputStream exportToExcel(List<T> data, String sheetName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            if (data.isEmpty()) {
                workbook.write(out);
                return new ByteArrayInputStream(out.toByteArray());
            }

            Class<?> clazz = data.get(0).getClass();
            List<Object> exportableItems = new ArrayList<>();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    field.setAccessible(true);
                    exportableItems.add(field);
                }
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ExcelColumn.class)) {
                    method.setAccessible(true);
                    exportableItems.add(method);
                }
            }

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            int colIdx = 0;
            for (Object item : exportableItems) {
                Cell cell = headerRow.createCell(colIdx);
                cell.setCellStyle(headerStyle);

                ExcelColumn annotation;
                if (item instanceof Field) {
                    annotation = ((Field) item).getAnnotation(ExcelColumn.class);
                } else {
                    annotation = ((Method) item).getAnnotation(ExcelColumn.class);
                }

                cell.setCellValue(annotation.name());
                sheet.setColumnWidth(colIdx, annotation.width());
                colIdx++;
            }

            int rowIdx = 1;
            for (T dataItem : data) {
                Row row = sheet.createRow(rowIdx++);
                int cellIdx = 0;

                for (Object item : exportableItems) {
                    Cell cell = row.createCell(cellIdx++);
                    Object value = null;

                    try {
                        if (item instanceof Field) {
                            value = ((Field) item).get(dataItem);
                        } else if (item instanceof Method) {
                            value = ((Method) item).invoke(dataItem);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                }
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        return style;
    }
}
