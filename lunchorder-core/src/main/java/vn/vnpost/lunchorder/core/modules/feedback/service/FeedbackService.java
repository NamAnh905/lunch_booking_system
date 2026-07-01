package vn.vnpost.lunchorder.core.modules.feedback.service;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackCreateRequest;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackResponse;

public interface FeedbackService {
    FeedbackResponse createFeedback(Long userId, FeedbackCreateRequest request);
    PageResponse<FeedbackResponse> getFeedbacks(int page);
    void deleteFeedback(Long id);
}
