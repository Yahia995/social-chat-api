package com.socialchat.mapper;

import com.socialchat.dto.notification.NotificationResponse;
import com.socialchat.dto.websocket.NotificationEvent;
import com.socialchat.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    NotificationEvent toEvent(Notification notification);
}
