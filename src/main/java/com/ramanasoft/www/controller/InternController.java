package com.ramanasoft.www.controller;

import com.ramanasoft.www.model.AttendanceLog;
import com.ramanasoft.www.model.InternModel;
import com.ramanasoft.www.model.LeaveRequest;
import com.ramanasoft.www.model.LeaveRequest.LeaveStatus;
import com.ramanasoft.www.repository.AttendanceLogRepository;
import com.ramanasoft.www.repository.LeaveRequestRepository;
import com.ramanasoft.www.service.InternService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class InternController {

	@Autowired
	private InternService internService;

	@Autowired
	private AttendanceLogRepository logRepository;

	// Create new intern
	@PostMapping
	public ResponseEntity<InternModel> create(@RequestBody InternModel model) {
		InternModel saved = internService.createIntern(model);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	// Get all interns
	@GetMapping
	public ResponseEntity<List<InternModel>> getAll() {
		List<InternModel> interns = internService.getAllInterns();
		return ResponseEntity.ok(interns);
	}

	// Get intern by ID
	@GetMapping("/{id}")
	public ResponseEntity<InternModel> getById(@PathVariable Long id) {
		InternModel intern = internService.getInternById(id);
		return intern != null ? ResponseEntity.ok(intern) : ResponseEntity.notFound().build();
	}


	@PutMapping("/{id}")
	public ResponseEntity<InternModel> update(@PathVariable Long id, @RequestBody InternModel model) {
		InternModel updated = internService.updateIntern(id, model);
		return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
	}


	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		internService.deleteIntern(id);
		return ResponseEntity.noContent().build();
	}


	@GetMapping("/by-mobile/{mobile}")
	public ResponseEntity<InternModel> getByMobile(@PathVariable String mobile) {
		InternModel intern = internService.getInternByMobile(mobile);
		return intern != null ? ResponseEntity.ok(intern) : ResponseEntity.notFound().build();
	}


	@PostMapping("/upload-image/{mobile}")
	public ResponseEntity<String> uploadProfileImage(@PathVariable String mobile,
			@RequestParam("image") MultipartFile imageFile) {
		try {
			if (imageFile.isEmpty()) {
				return ResponseEntity.badRequest().body("No file selected");
			}
			internService.saveProfileImage(mobile, imageFile.getBytes());
			return ResponseEntity.ok("Image uploaded successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to upload image: " + e.getMessage());
		}
	}


	@GetMapping(value = "/image/{mobile}", produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<byte[]> getProfileImage(@PathVariable String mobile) {
		try {
			byte[] image = internService.getProfileImageByMobile(mobile);
			if (image != null) {
				return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}


	@PostMapping("/check-in/{mobile}")
	public ResponseEntity<String> checkIn(@PathVariable String mobile) {
		Optional<AttendanceLog> lastLog = logRepository.findTopByMobileOrderByCheckInTimeDesc(mobile);
		if (lastLog.isPresent() && lastLog.get().getCheckOutTime() == null) {
			return ResponseEntity.badRequest().body("Already checked in");
		}

		AttendanceLog log = new AttendanceLog();
		log.setMobile(mobile);
		log.setCheckInTime(LocalDateTime.now());
		log.setDate(LocalDate.now());

		logRepository.save(log);
		return ResponseEntity.ok("Checked in successfully");
	}


	@PostMapping("/check-out/{mobile}")
	public ResponseEntity<String> checkOut(@PathVariable String mobile) {
		Optional<AttendanceLog> lastLog = logRepository.findTopByMobileOrderByCheckInTimeDesc(mobile);
		if (lastLog.isEmpty() || lastLog.get().getCheckOutTime() != null) {
			return ResponseEntity.badRequest().body("No active check-in to check out from");
		}

		AttendanceLog log = lastLog.get();
		log.setCheckOutTime(LocalDateTime.now());
		log.calculateDuration();

		logRepository.save(log);
		return ResponseEntity.ok("Checked out successfully");
	}


	@GetMapping("/productive-time/{mobile}")
	public ResponseEntity<Map<String, Object>> getProductiveTime(@PathVariable String mobile) {
		LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
		LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

		List<AttendanceLog> logs = logRepository.findByMobileAndCheckInTimeBetween(mobile, startOfDay, endOfDay);

		long totalSeconds = 0;
		for (AttendanceLog log : logs) {
			if (log.getCheckInTime() != null && log.getCheckOutTime() != null) {
				totalSeconds += Duration.between(log.getCheckInTime(), log.getCheckOutTime()).getSeconds();
			}
		}

		Duration duration = Duration.ofSeconds(totalSeconds);
		String formatted = String.format("%02d:%02d:%02d",
				duration.toHours(),
				duration.toMinutes() % 60,
				duration.getSeconds() % 60);

		Map<String, Object> response = new HashMap<>();
		response.put("seconds", totalSeconds);
		response.put("formatted", formatted);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/daily-logs/{mobile}")
	public ResponseEntity<List<AttendanceLog>> getLogsByDateQueryParam(
			@PathVariable String mobile,
			@RequestParam String date) {
		try {
			LocalDate localDate = LocalDate.parse(date);
			List<AttendanceLog> logs = logRepository.findByMobileAndDate(mobile, localDate);
			return ResponseEntity.ok(logs);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}


	@GetMapping("/logs/{mobile}/{date}")
	public ResponseEntity<List<AttendanceLog>> getLogsByDate(@PathVariable String mobile, @PathVariable String date) {
		LocalDate localDate = LocalDate.parse(date);
		List<AttendanceLog> logs = logRepository.findByMobileAndDate(mobile, localDate);
		return ResponseEntity.ok(logs);
	}

	@Autowired
	private LeaveRequestRepository leaveRequestRepository;

	@PostMapping("/leave-request/{mobile}")
	public ResponseEntity<String> submitLeaveRequest(@PathVariable String mobile, @RequestBody Map<String, Object> requestBody) {
		try {
			// Parse request data
			LocalDate startDate = LocalDate.parse((String) requestBody.get("startDate"));
			LocalDate endDate = LocalDate.parse((String) requestBody.get("endDate"));
			String reason = (String) requestBody.get("reason");

			// Create leave request object
			LeaveRequest leaveRequest = new LeaveRequest();
			leaveRequest.setStartDate(startDate);
			leaveRequest.setEndDate(endDate);
			leaveRequest.setReason(reason);

			// Use service to save leave request
			internService.submitLeaveRequest(mobile, leaveRequest);

			return ResponseEntity.ok("Leave request submitted successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to submit leave request: " + e.getMessage());
		}
	}

	@GetMapping("/leave-requests/{mobile}")
	public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByMobile(@PathVariable String mobile) {
		try {
			List<LeaveRequest> requests = internService.getLeaveRequestsByMobile(mobile);
			return ResponseEntity.ok(requests);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
	}




}
