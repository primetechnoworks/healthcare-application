// ============================================================
// AppointmentRequest.java
// ============================================================
package com.healthcare.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentRequest {
    @NotNull private Long doctorId;
    @NotNull private LocalDate appointmentDate;
    @NotNull private LocalTime appointmentTime;
    private Integer durationMinutes;
    private String reason;
}