package com.healthcare.notification;

import com.healthcare.entity.Notification;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Long referenceId;
    private String referenceType;
    private String imageUrl;
    private String clickAction;
    private boolean sendEmail;
    private boolean broadcast;
    private String broadcastRole;
}
