package vn.vnpost.lunchorder.common.base;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class ExcelDownload {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ExcelDownload() {
    }

    public static ResponseEntity<byte[]> of(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(XLSX_MEDIA_TYPE)
                .body(data);
    }
}
