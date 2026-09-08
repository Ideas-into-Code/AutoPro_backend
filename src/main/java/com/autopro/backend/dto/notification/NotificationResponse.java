package com.autopro.backend.dto.notification;

import com.autopro.backend.entity.Notification;
import com.autopro.backend.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .link(n.getLink())
                .read(Boolean.TRUE.equals(n.getRead()))
                .createdAt(n.getCreatedAt())
                .build();
    }
}
