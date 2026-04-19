package com.healthcare.controller;

import com.healthcare.dto.AppointmentRequest;
import com.healthcare.dto.AppointmentResponse;
import com.healthcare.entity.Appointment;
import com.healthcare.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /** Book a new appointment */
    @PostMapping("/book")	
	public ResponseEntity<AppointmentResponse> book(@RequestBody AppointmentRequest request) {
	  return ResponseEntity.ok(appointmentService.bookAppointment(request, 1L)); 
    }
	 
    /** Get appointments for logged-in patient */
    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> myAppointments() {

        return ResponseEntity.ok(appointmentService.getPatientAppointments(1L));
    }

    /** Get doctor's schedule for a specific date */
    @GetMapping("/doctor/{doctorId}/{date}/{userId}")
    public ResponseEntity<List<AppointmentResponse>> doctorSchedule(@PathVariable Long doctorId,
    		@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
    		@PathVariable Long userId) {
	  
	  return
	  ResponseEntity.ok(appointmentService.getDoctorAppointments(userId, date)); }
	 

    /** Get available time slots for a doctor on a date */
    @GetMapping("/available-slots/{doctorId}")
    public ResponseEntity<List<LocalTime>> availableSlots(@PathVariable Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, LocalDate.of(2024, 6, 30)));
    }

    /** Update appointment status (confirm / cancel / complete) */
	
	  @PatchMapping("/{id}/{userId}/{status}")
	  public ResponseEntity<AppointmentResponse> updateStatus(@PathVariable Long id,	  
			  @PathVariable Long userId, @PathVariable Appointment.Status status) {
	  
	  return ResponseEntity.ok( appointmentService.updateStatus(id, status,userId)); }
	 

    /** Patient cancels their own appointment */
	  
	  @DeleteMapping("/{id}/{userId}") 
	  ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id, @PathVariable Long userId) {
		  return ResponseEntity.ok( appointmentService.updateStatus(id,
				  Appointment.Status.CANCELLED, userId)); 
	  }
}
