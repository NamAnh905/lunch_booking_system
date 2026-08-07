package vn.vnpost.lunchorder.system.modules.user.service.dto;

import java.util.List;

public record UserImportResultResponse(int totalRows, int successCount, int failureCount,
        List<UserImportErrorResponse> errors) {
}
