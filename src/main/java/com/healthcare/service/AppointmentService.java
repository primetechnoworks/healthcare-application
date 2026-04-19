package com.healthcare.service;

import com.healthcare.dto.AppointmentRequest;
import com.healthcare.dto.AppointmentResponse;
import com.healthcare.entity.Appointment;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.Notification;
import com.healthcare.entity.Patient;
import com.healthcare.notification.NotificationRequest;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, Long patientUserId) {
        List<Patient> patients = patientRepository.findByUserId(patientUserId);
        if (patients.isEmpty()) {
            throw new RuntimeException("Patient profile not found for user id: " + patientUserId);
        }
        if (patients.size() > 1) {
            throw new RuntimeException("Multiple patient profiles found for user id: " + patientUserId);
        }
        Patient patient = patients.get(0);

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
            .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Check slot availability
        boolean slotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTime(
            doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime());

        if (slotTaken) {
            throw new RuntimeException("Time slot is already booked");
        }

        Appointment appointment = Appointment.builder()
            .patient(patient)
            .doctor(doctor)
            .appointmentDate(request.getAppointmentDate())
            .appointmentTime(request.getAppointmentTime())
            .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30)
            .reason(request.getReason())
            .status(Appointment.Status.SCHEDULED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        appointment = appointmentRepository.save(appointment);

        // Notify patient
        notificationService.sendNotification(NotificationRequest.builder()
            .userId(patient.getUser().getId())
            .title("Appointment Booked")
            .message(String.format("Your appointment with Dr. %s on %s at %s has been booked.",
                doctor.getUser().getFullName(),
                request.getAppointmentDate(),
                request.getAppointmentTime()))
            .type(Notification.NotificationType.APPOINTMENT_CONFIRMED)
            .referenceId(appointment.getId())
            .referenceType("APPOINTMENT")
            .sendEmail(true)
            .clickAction("/appointments/" + appointment.getId())
            .build());

        // Notify doctor
        notificationService.sendNotification(NotificationRequest.builder()
            .userId(doctor.getUser().getId())
            .title("New Appointment")
            .message(String.format("New appointment scheduled: %s on %s at %s",
                patient.getUser().getFullName(),
                request.getAppointmentDate(),
                request.getAppointmentTime()))
            .type(Notification.NotificationType.APPOINTMENT_CONFIRMED)
            .referenceId(appointment.getId())
            .referenceType("APPOINTMENT")
            .sendEmail(false)
            .build());

        log.info("Appointment {} booked for patient {} with doctor {}",
            appointment.getId(), patient.getId(), doctor.getId());

        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long appointmentId,
                                             Appointment.Status newStatus,
                                             Long requestingUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Appointment not found"));

        Appointment.Status oldStatus = appointment.getStatus();
        appointment.setStatus(newStatus);
        appointment = appointmentRepository.save(appointment);

        // Fire notifications based on new status
        triggerStatusChangeNotifications(appointment, oldStatus, newStatus);

        return AppointmentResponse.from(appointment);
    }

    private void triggerStatusChangeNotifications(Appointment appointment,
                                                   Appointment.Status oldStatus,
                                                   Appointment.Status newStatus) {
        Long patientUserId = appointment.getPatient().getUser().getId();
        String doctorName  = appointment.getDoctor().getUser().getFullName();

        switch (newStatus) {
            case CONFIRMED -> notificationService.sendNotification(NotificationRequest.builder()
                .userId(patientUserId)
                .title("Appointment Confirmed")
                .message("Dr. " + doctorName + " has confirmed your appointment on "
                    + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime())
                .type(Notification.NotificationType.APPOINTMENT_CONFIRMED)
                .referenceId(appointment.getId())
                .referenceType("APPOINTMENT")
                .sendEmail(true)
                .clickAction("/appointments/" + appointment.getId())
                .build());

            case CANCELLED -> notificationService.sendNotification(NotificationRequest.builder()
                .userId(patientUserId)
                .title("Appointment Cancelled")
                .message("Your appointment with Dr. " + doctorName + " on "
                    + appointment.getAppointmentDate() + " has been cancelled.")
                .type(Notification.NotificationType.APPOINTMENT_CANCELLED)
                .referenceId(appointment.getId())
                .referenceType("APPOINTMENT")
                .sendEmail(true)
                .clickAction("/appointments")
                .build());

            case COMPLETED -> log.info("Appointment {} marked as completed", appointment.getId());
            default -> log.debug("No notification for status transition {} -> {}", oldStatus, newStatus);
        }
    }

    public List<AppointmentResponse> getPatientAppointments(Long patientUserId) {
        return appointmentRepository.findByPatientUserIdOrderByAppointmentDateDesc(patientUserId)
            .stream().map(AppointmentResponse::from).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getDoctorAppointments(Long doctorUserId, LocalDate date) {
        return appointmentRepository.findByDoctorUserIdAndDate(doctorUserId, date)
            .stream().map(AppointmentResponse::from).collect(Collectors.toList());
    }

    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new RuntimeException("Doctor not found"));

        List<LocalTime> bookedTimes = appointmentRepository
            .findBookedTimesByDoctorAndDate(doctorId, date);

        // Generate 30-min slots within doctor's available hours
        LocalTime from = doctor.getAvailableFrom() != null
            ? doctor.getAvailableFrom() : LocalTime.of(9, 0);
        LocalTime to = doctor.getAvailableTo() != null
            ? doctor.getAvailableTo() : LocalTime.of(17, 0);

        List<LocalTime> slots = new java.util.ArrayList<>();
        LocalTime slot = from;
        while (slot.isBefore(to)) {
            if (!bookedTimes.contains(slot)) {
                slots.add(slot);
            }
            slot = slot.plusMinutes(30);
        }
        return slots;
    }
}
