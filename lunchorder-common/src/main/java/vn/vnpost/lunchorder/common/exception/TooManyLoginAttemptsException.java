package vn.vnpost.lunchorder.common.exception;

import lombok.Getter;

@Getter
public class TooManyLoginAttemptsException extends AppException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
