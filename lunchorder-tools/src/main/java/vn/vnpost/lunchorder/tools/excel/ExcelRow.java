package vn.vnpost.lunchorder.tools.excel;

import java.util.List;

public record ExcelRow(int rowNumber, List<String> cells) {

    public String cell(int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        String value = cells.get(index);
        return value == null ? "" : value;
    }

    public boolean isEmpty() {
        return cells.stream().allMatch(cell -> cell == null || cell.isEmpty());
    }
}
