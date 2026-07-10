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
    USER_USERNAME_EXISTS(2003, "Username already exists", HttpStatus.BAD_REQUEST),
    USER_EMAIL_EXISTS(2004, "Email already exists", HttpStatus.BAD_REQUEST),
    USER_EMPLOYEE_CODE_EXISTS(2005, "Employee code already exists", HttpStatus.BAD_REQUEST),

    // 3xxx: Role & Permission
    ROLE_NOT_FOUND(3001, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(3002, "Role already exists", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_FOUND(3501, "Permission not found", HttpStatus.NOT_FOUND),

    // 4xxx: Department Management
    DEPARTMENT_NOT_FOUND(4001, "Department not found", HttpStatus.NOT_FOUND),

    // 5xxx: Dish Management
    DISH_NOT_FOUND(5001, "Dish not found", HttpStatus.NOT_FOUND),
    DISH_ALREADY_EXISTS(5002, "Dish already exists", HttpStatus.BAD_REQUEST),
    DISH_OUT_OF_STOCK(5003, "Dish is out of stock", HttpStatus.BAD_REQUEST),

    // 6xxx: Menu Management
    MENU_NOT_FOUND(6001, "Menu not found", HttpStatus.NOT_FOUND),
    MENU_ALREADY_EXISTS(6002, "Menu already exists for this date", HttpStatus.BAD_REQUEST),

    // 7xxx: Order Management
    ORDER_NOT_FOUND(7001, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_CUTOFF_REACHED(7002, "Order or cancellation cutoff time has been reached", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_EXISTS(7003, "You have already ordered a meal for this date", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL(7004, "This order cannot be cancelled", HttpStatus.BAD_REQUEST),
    ORDER_IN_MARKET(7005, "Order is currently listed in the market", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_PASS(7006, "Order cannot be passed", HttpStatus.BAD_REQUEST),

    // 8xxx: Feedback
    FEEDBACK_NOT_FOUND(8001, "Feedback not found", HttpStatus.NOT_FOUND),
    FEEDBACK_CANNOT_CREATE(8002, "You must order this meal before giving feedback", HttpStatus.BAD_REQUEST),
    FEEDBACK_ALREADY_EXISTS(8003, "You have already submitted feedback for this menu", HttpStatus.BAD_REQUEST),

    // 9xxx: Ticket Management
    TICKET_NOT_FOUND(9001, "Ticket not found", HttpStatus.NOT_FOUND),

    // 10xxx: Ticket Exchange Management
    EXCHANGE_NOT_FOUND(10001, "Exchange not found", HttpStatus.NOT_FOUND),
    EXCHANGE_NOT_OPEN(10002, "Exchange is not open", HttpStatus.BAD_REQUEST),
    CANNOT_CLAIM_OWN_TICKET(10003, "Cannot claim your own ticket", HttpStatus.BAD_REQUEST),

    // 11xxx: Notification Management
    NOTIFICATION_NOT_FOUND(11001, "Notification not found", HttpStatus.NOT_FOUND),

    // 12xxx: Payment Management
    PAYMENT_NOT_FOUND(12001, "Payment not found", HttpStatus.NOT_FOUND),
    INVALID_ENUM_VALUE(12002, "Invalid enum value", HttpStatus.BAD_REQUEST),

    // 13xxx: Price Management
    PRICE_NOT_FOUND(13001, "Price not found", HttpStatus.NOT_FOUND),
    PRICE_ALREADY_EXISTS(13002, "Price already exists", HttpStatus.BAD_REQUEST),
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
