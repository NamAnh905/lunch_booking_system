package vn.vnpost.lunchorder.system.modules.user.service.dto;

public record UserImportErrorResponse(int rowNumber, String username, String message) {
}
