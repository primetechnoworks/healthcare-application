package com.healthcare.dto;

import com.healthcare.entity.Appointment;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Integer durationMinutes;
    private String status;
    private String reason;
    private String notes;
    private String meetingLink;

    public static AppointmentResponse from(Appointment a) {
        return AppointmentResponse.builder()
            .id(a.getId())
            .patientId(a.getPatient().getId())
            .patientName(a.getPatient().getUser().getFullName())
            .doctorId(a.getDoctor().getId())
            .doctorName(a.getDoctor().getUser().getFullName())
            .specialization(a.getDoctor().getSpecialization())
            .appointmentDate(a.getAppointmentDate())
            .appointmentTime(a.getAppointmentTime())
            .durationMinutes(a.getDurationMinutes())
            .status(a.getStatus().name())
            .reason(a.getReason())
            .build();
    }
}
