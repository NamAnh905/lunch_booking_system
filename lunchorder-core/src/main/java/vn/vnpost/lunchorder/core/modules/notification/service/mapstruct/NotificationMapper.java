package vn.vnpost.lunchorder.core.modules.notification.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import vn.vnpost.lunchorder.core.modules.notification.entity.Notification;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "user.id", target = "userId")
    NotificationResponse toDto(Notification notification);

    List<NotificationResponse> toDtoList(List<Notification> notifications);

    default Page<NotificationResponse> toDtoPage(Page<Notification> page) {
        return page.map(this::toDto);
    }
}
