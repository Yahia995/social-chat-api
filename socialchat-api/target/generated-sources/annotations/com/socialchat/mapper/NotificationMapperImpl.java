package com.socialchat.mapper;

import com.socialchat.dto.notification.NotificationResponse;
import com.socialchat.dto.websocket.NotificationEvent;
import com.socialchat.entity.Notification;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T10:18:08+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse.NotificationResponseBuilder notificationResponse = NotificationResponse.builder();

        notificationResponse.id( notification.getId() );
        notificationResponse.type( notification.getType() );
        notificationResponse.title( notification.getTitle() );
        notificationResponse.message( notification.getMessage() );
        notificationResponse.data( notification.getData() );
        notificationResponse.isRead( notification.getIsRead() );
        notificationResponse.createdAt( notification.getCreatedAt() );

        return notificationResponse.build();
    }

    @Override
    public NotificationEvent toEvent(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationEvent.NotificationEventBuilder notificationEvent = NotificationEvent.builder();

        notificationEvent.id( notification.getId() );
        notificationEvent.type( notification.getType() );
        notificationEvent.title( notification.getTitle() );
        notificationEvent.message( notification.getMessage() );
        notificationEvent.data( notification.getData() );
        notificationEvent.createdAt( notification.getCreatedAt() );

        return notificationEvent.build();
    }
}
