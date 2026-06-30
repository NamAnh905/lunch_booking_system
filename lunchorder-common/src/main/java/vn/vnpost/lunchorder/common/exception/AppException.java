package vn.vnpost.lunchorder.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}