package vn.vnpost.lunchorder.common.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends AppException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
