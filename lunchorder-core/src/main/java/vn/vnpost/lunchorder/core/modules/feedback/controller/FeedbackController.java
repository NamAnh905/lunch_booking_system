package vn.vnpost.lunchorder.core.modules.feedback.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.feedback.service.FeedbackService;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackCreateRequest;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackResponse;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_OWN_FEEDBACK')")
    public ApiResponse<FeedbackResponse> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid FeedbackCreateRequest request) {
        return ApiResponse.<FeedbackResponse>builder()
                .result(feedbackService.createFeedback(userPrincipal.getUserId(), request))
                .build();
    }
}
