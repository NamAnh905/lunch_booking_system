package vn.vnpost.lunchorder.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9901, "Invalid key", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(9902, "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(9903, "HTTP method not supported for this endpoint", HttpStatus.METHOD_NOT_ALLOWED),
    MALFORMED_REQUEST(9904, "Malformed request", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_MEDIA_TYPE(9905, "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "Unauthorized access", HttpStatus.FORBIDDEN),
    TOKEN_GENERATION_FAILED(1003, "Unable to generate authentication token", HttpStatus.INTERNAL_SERVER_ERROR),
    TOO_MANY_LOGIN_ATTEMPTS(1004, "Too many login attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),

    USER_NOT_FOUND(2001, "User not found", HttpStatus.NOT_FOUND),
    USER_LOCKED(2002, "User account is locked", HttpStatus.FORBIDDEN),
    USER_USERNAME_EXISTS(2003, "Username already exists", HttpStatus.BAD_REQUEST),
    USER_EMAIL_EXISTS(2004, "Email already exists", HttpStatus.BAD_REQUEST),
    USER_EMPLOYEE_CODE_EXISTS(2005, "Employee code already exists", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(2006, "Current password is incorrect", HttpStatus.BAD_REQUEST),

    ROLE_NOT_FOUND(3001, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(3002, "Role already exists", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_FOUND(3501, "Permission not found", HttpStatus.NOT_FOUND),
    PERMISSION_ALREADY_EXISTS(3502, "Permission already exists", HttpStatus.BAD_REQUEST),

    DEPARTMENT_NOT_FOUND(4001, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_CODE_EXISTS(4002, "Department code already exists", HttpStatus.BAD_REQUEST),

    DISH_NOT_FOUND(5001, "Dish not found", HttpStatus.NOT_FOUND),
    DISH_ALREADY_EXISTS(5002, "Dish already exists", HttpStatus.BAD_REQUEST),
    DISH_OUT_OF_STOCK(5003, "Dish is out of stock", HttpStatus.BAD_REQUEST),

    MENU_NOT_FOUND(6001, "Menu not found", HttpStatus.NOT_FOUND),
    MENU_ALREADY_EXISTS(6002, "Menu already exists for this date", HttpStatus.BAD_REQUEST),

    ORDER_NOT_FOUND(7001, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_CUTOFF_REACHED(7002, "Order or cancellation cutoff time has been reached", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_EXISTS(7003, "You have already ordered a meal for this date", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL(7004, "This order cannot be cancelled", HttpStatus.BAD_REQUEST),
    ORDER_IN_MARKET(7005, "Order is currently listed in the market", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_PASS(7006, "Order cannot be passed", HttpStatus.BAD_REQUEST),
    ORDER_CLAIMED_CANNOT_PASS(7007, "A ticket claimed from the market cannot be passed again", HttpStatus.BAD_REQUEST),

    TICKET_NOT_FOUND(9001, "Ticket not found", HttpStatus.NOT_FOUND),

    EXCHANGE_NOT_FOUND(10001, "Exchange not found", HttpStatus.NOT_FOUND),
    EXCHANGE_NOT_OPEN(10002, "Exchange is not open", HttpStatus.BAD_REQUEST),
    CANNOT_CLAIM_OWN_TICKET(10003, "Cannot claim your own ticket", HttpStatus.BAD_REQUEST),

    NOTIFICATION_NOT_FOUND(11001, "Notification not found", HttpStatus.NOT_FOUND),

    INVALID_ENUM_VALUE(12002, "Invalid enum value", HttpStatus.BAD_REQUEST),

    PRICE_NOT_FOUND(13001, "Price not found", HttpStatus.NOT_FOUND),
    PRICE_ALREADY_EXISTS(13002, "Price already exists", HttpStatus.BAD_REQUEST),

    ADMIN_REPORT_EMAIL_NOT_CONFIGURED(14001, "Admin report email is not configured in system settings", HttpStatus.INTERNAL_SERVER_ERROR),
    EXPORT_FAILED(14002, "Failed to export data to Excel", HttpStatus.INTERNAL_SERVER_ERROR),

    IMAGE_INVALID(15001, "Image file is empty or invalid", HttpStatus.BAD_REQUEST),
    IMAGE_UPLOAD_FAILED(15002, "Failed to upload image", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_TYPE_NOT_ALLOWED(15003, "Image type is not allowed", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE(15004, "Image file exceeds the maximum allowed size", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
