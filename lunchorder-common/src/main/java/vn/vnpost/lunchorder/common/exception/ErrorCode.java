package vn.vnpost.lunchorder.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter

public enum ErrorCode {
    // 99xx: System & Generic Errors
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9901, "Invalid key", HttpStatus.BAD_REQUEST),

    // 1xxx: Authentication & Authorization
    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "Unauthorized access", HttpStatus.FORBIDDEN),

    // 2xxx: User Management
    USER_NOT_FOUND(2001, "User not found", HttpStatus.NOT_FOUND),
    USER_LOCKED(2002, "User account is locked", HttpStatus.FORBIDDEN),

    // 3xxx: Role & Permission
    ROLE_NOT_FOUND(3001, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(3501, "Permission not found", HttpStatus.NOT_FOUND),

    // 4xxx: Department Management
    DEPARTMENT_NOT_FOUND(4001, "Department not found", HttpStatus.NOT_FOUND),

    // 5xxx: Dish Management
    DISH_NOT_FOUND(5001, "Dish not found", HttpStatus.NOT_FOUND),

    // 6xxx: Menu Management
    MENU_NOT_FOUND(6001, "Menu not found", HttpStatus.NOT_FOUND),
    MENU_ALREADY_EXISTS(6002, "Menu already exists for this date", HttpStatus.BAD_REQUEST),

    // 7xxx: Order Management
    ORDER_NOT_FOUND(7001, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_CUTOFF_REACHED(7002, "Order or cancellation cutoff time has been reached", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_EXISTS(7003, "You have already ordered a meal for this date", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL(7004, "This order cannot be cancelled", HttpStatus.BAD_REQUEST),

    // 8xxx: Rating & Feedback
    RATING_NOT_FOUND(8001, "Rating not found", HttpStatus.NOT_FOUND),

    // 9xxx: Ticket Management
    TICKET_NOT_FOUND(9001, "Ticket not found", HttpStatus.NOT_FOUND),

    // 11xxx: Notification Management
    NOTIFICATION_NOT_FOUND(11001, "Notification not found", HttpStatus.NOT_FOUND),
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
