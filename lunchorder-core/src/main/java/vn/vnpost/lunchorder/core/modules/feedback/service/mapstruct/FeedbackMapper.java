package vn.vnpost.lunchorder.core.modules.feedback.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.common.entity.Feedback;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackCreateRequest;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FeedbackMapper extends BaseMapper<FeedbackCreateRequest, FeedbackResponse, Feedback> {

    @Override
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(source = "menu.id", target = "menuId")
    @Mapping(source = "menu.menuDate", target = "menuDate")
    FeedbackResponse toDto(Feedback entity);
}
