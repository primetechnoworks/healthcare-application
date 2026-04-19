// NotificationController.java
package com.healthcare.controller;
import com.healthcare.entity.Notification;
import com.healthcare.dto.PatientDetails;
import com.healthcare.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @MessageMapping("/sendMessage") // Endpoint matching the JavaScript destination
    @SendTo("/topic/notifications") // Broadcast to subscribers of this topic
    public PatientDetails sendMessage(@RequestBody PatientDetails patientDetails) {
        System.out.println("Received message: " + patientDetails); // Debugging log
        return patientDetails; // Broadcast the message
    }

    /** Get all notifications for the current user */

    @GetMapping("/{id}/{userId}")
    public ResponseEntity<Notification> getNotification(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotifications(id, userId).get());
    }


    /** Get unread notification count */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {

        return ResponseEntity.ok( Map.of("count",
                notificationService.getUnreadCount(1L)));
    }
}
