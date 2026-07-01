package vn.vnpost.lunchorder.core.modules.feedback.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private Long menuId;
    private LocalDate menuDate;
    private String comment;
    private Instant createdAt;
}
