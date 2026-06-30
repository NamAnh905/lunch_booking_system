package vn.vnpost.lunchorder.common.base;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseResponse {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
