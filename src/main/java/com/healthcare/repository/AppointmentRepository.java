package com.healthcare.repository;

import com.healthcare.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientUserIdOrderByAppointmentDateDesc(Long userId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.user.id = :userId AND a.appointmentDate = :date ORDER BY a.appointmentTime")
    List<Appointment> findByDoctorUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(Long doctorId, LocalDate date, LocalTime time);

    @Query("SELECT a.appointmentTime FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status != 'CANCELLED'")
    List<LocalTime> findBookedTimesByDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status IN ('SCHEDULED','CONFIRMED')
        AND CAST(CONCAT(a.appointmentDate, ' ', a.appointmentTime) AS java.time.LocalDateTime)
            BETWEEN :from AND :to
        """)
    List<Appointment> findAppointmentsDueForReminder(@Param("from") LocalDateTime from, @Param("to")   LocalDateTime to);
}