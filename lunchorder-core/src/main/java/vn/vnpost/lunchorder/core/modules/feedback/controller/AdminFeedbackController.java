package vn.vnpost.lunchorder.core.modules.feedback.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.feedback.service.FeedbackService;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/feedbacks")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    @PreAuthorize("hasAuthority('DELETE_ANY_FEEDBACK')")
    public ApiResponse<PageResponse<FeedbackResponse>> getFeedbacks(
            @RequestParam(name = "page", defaultValue = "1") int page) {
        return ApiResponse.<PageResponse<FeedbackResponse>>builder()
                .result(feedbackService.getFeedbacks(page))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_ANY_FEEDBACK')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ApiResponse.<Void>builder().build();
    }
}
