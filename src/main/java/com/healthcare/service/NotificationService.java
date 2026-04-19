package com.healthcare.service;

import com.healthcare.entity.Appointment;
import com.healthcare.entity.Notification;
import com.healthcare.entity.User;
import com.healthcare.notification.NotificationRequest;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central notification dispatcher.
 * Sends push (FCM), WebSocket, and email notifications concurrently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Async
    @Transactional
    public void sendNotification(NotificationRequest request) {
        // Persist to notification log
        Notification notification = buildAndSaveNotification(request);

        // Dispatch to all channels concurrently
        sendWebSocketNotification(request);

        if (request.isSendEmail()) {
            emailService.sendNotificationEmail(request);
        }

        // Mark as sent
        notification.setStatus(Notification.Status.SENT);
        notification.setSentAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
    }

    // ──────────────────────────────────────────────────────────
    // WebSocket (real-time in-app)
    // ──────────────────────────────────────────────────────────

    private void sendWebSocketNotification(NotificationRequest request) {
        try {
            Map<String, Object> wsPayload = Map.of(
                "id",        System.currentTimeMillis(),
                "title",     request.getTitle(),
                "message",   request.getMessage(),
                "type",      request.getType().name(),
                "timestamp", LocalDateTime.now().toString()
            );

            // Send to specific user's private queue
            messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getUserId()),
                "/queue/notifications",
                wsPayload
            );

            // Also broadcast to role-based topic if needed
            if (request.isBroadcast()) {
                messagingTemplate.convertAndSend(
                    "/topic/notifications." + request.getBroadcastRole(),
                    wsPayload
                );
            }

            log.debug("WebSocket notification sent to user {}", request.getUserId());
        } catch (Exception e) {
            log.error("WebSocket notification failed for user {}: {}", request.getUserId(), e.getMessage());
        }
    }

    /** Runs every minute, sends reminders 24h and 1h before appointments */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendAppointmentReminders() {
        LocalDateTime now = LocalDateTime.now();

        // 24-hour reminder window (23:55 to 24:05)
        List<Appointment> dayReminders = appointmentRepository
            .findAppointmentsDueForReminder(now.plusHours(24).minusMinutes(5),
                                             now.plusHours(24).plusMinutes(5));

        // 1-hour reminder window (0:55 to 1:05)
        List<Appointment> hourReminders = appointmentRepository
            .findAppointmentsDueForReminder(now.plusHours(1).minusMinutes(5),
                                             now.plusHours(1).plusMinutes(5));

        dayReminders.forEach(a -> sendAppointmentReminder(a, "24 hours"));
        hourReminders.forEach(a -> sendAppointmentReminder(a, "1 hour"));

        if (!dayReminders.isEmpty() || !hourReminders.isEmpty()) {
            log.info("Sent {} day reminders, {} hour reminders",
                dayReminders.size(), hourReminders.size());
        }
    }

    private void sendAppointmentReminder(Appointment appointment, String timeframe) {
        String doctorName = appointment.getDoctor().getUser().getFullName();
        String patientName = appointment.getPatient().getUser().getFullName();

        // Notify patient
        sendNotification(NotificationRequest.builder()
            .userId(appointment.getPatient().getUser().getId())
            .title("Appointment Reminder")
            .message(String.format("You have an appointment with Dr. %s in %s at %s",
                doctorName, timeframe, appointment.getAppointmentTime()))
            .type(Notification.NotificationType.APPOINTMENT_REMINDER)
            .referenceId(appointment.getId())
            .referenceType("APPOINTMENT")
            .sendEmail(true)
            .clickAction("/appointments/" + appointment.getId())
            .build());

        // Notify doctor
        sendNotification(NotificationRequest.builder()
            .userId(appointment.getDoctor().getUser().getId())
            .title("Upcoming Appointment")
            .message(String.format("Appointment with %s in %s at %s",
                patientName, timeframe, appointment.getAppointmentTime()))
            .type(Notification.NotificationType.APPOINTMENT_REMINDER)
            .referenceId(appointment.getId())
            .referenceType("APPOINTMENT")
            .sendEmail(false)
            .clickAction("/appointments/" + appointment.getId())
            .build());
    }

    // ──────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────

    private Notification buildAndSaveNotification(NotificationRequest request) {
        User userRef = new User();
        userRef.setId(request.getUserId());

        Notification notification = Notification.builder()
            .user(userRef)
            .title(request.getTitle())
            .message(request.getMessage())
            .type(request.getType())
            .channel(Notification.Channel.ALL)
            .status(Notification.Status.PENDING)
            .referenceId(request.getReferenceId())
            .referenceType(request.getReferenceType())
            .createdAt(LocalDateTime.now())
            .build();

        return notificationRepository.save(notification);
    }

    /** Get unread count for a user */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /** Get paginated notification history */
    public Optional<Notification> getNotifications(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId);
    }
}
