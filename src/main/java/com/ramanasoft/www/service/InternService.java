package com.ramanasoft.www.service;

import com.ramanasoft.www.model.AttendanceLog;
import com.ramanasoft.www.model.InternModel;
import com.ramanasoft.www.model.LeaveRequest;
import com.ramanasoft.www.model.LeaveRequest.LeaveStatus;
import com.ramanasoft.www.repository.AttendanceLogRepository;
import com.ramanasoft.www.repository.InternRepository;
import com.ramanasoft.www.repository.LeaveRequestRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InternService {

    @Autowired
    private InternRepository internRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    // ✅ Create Intern
    public InternModel createIntern(InternModel model) {
        return internRepository.save(model);
    }

    // ✅ Get All Interns
    public List<InternModel> getAllInterns() {
        return internRepository.findAll();
    }

    // ✅ Get Intern By ID
    public InternModel getInternById(Long id) {
        return internRepository.findById(id).orElse(null);
    }

    // ✅ Update Intern
    public InternModel updateIntern(Long id, InternModel model) {
        InternModel existing = internRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setFirstName(model.getFirstName());
            existing.setLastName(model.getLastName());
            existing.setMobile(model.getMobile());
            existing.setBatchNO(model.getBatchNO());
            existing.setDesignation(model.getDesignation());
            existing.setDepartment(model.getDepartment());
            return internRepository.save(existing);
        }
        return null;
    }

    // ✅ Delete Intern
    public void deleteIntern(Long id) {
        internRepository.deleteById(id);
    }

    // ✅ Get Intern by Mobile
    public InternModel getInternByMobile(String mobile) {
        return internRepository.findByMobile(mobile);
    }

    // ✅ Save Profile Image
    public void saveProfileImage(String mobile, byte[] imageBytes) {
        InternModel intern = internRepository.findByMobile(mobile);
        if (intern != null) {
            intern.setProfileImage(imageBytes);
            internRepository.save(intern);
        } else {
            throw new RuntimeException("Intern not found for mobile: " + mobile);
        }
    }

    // ✅ Get Profile Image
    public byte[] getProfileImageByMobile(String mobile) {
        InternModel intern = internRepository.findByMobile(mobile);
        return (intern != null) ? intern.getProfileImage() : null;
    }

    // ✅ Check-In
    public void checkIn(String mobile) {
        InternModel intern = internRepository.findByMobile(mobile);
        if (intern == null) {
            throw new RuntimeException("Intern not found for mobile: " + mobile);
        }

        // Create new log entry
        AttendanceLog log = new AttendanceLog();
        log.setMobile(mobile);
        log.setDate(LocalDate.now());
        log.setCheckInTime(LocalDateTime.now());
        log.setDuration("00:00:00");

        attendanceLogRepository.save(log);
    }

    // ✅ Check-Out
    public void checkout(String mobile) {
        AttendanceLog log = attendanceLogRepository
            .findTopByMobileAndDateAndCheckOutTimeIsNullOrderByIdDesc(mobile, LocalDate.now())
            .orElse(null);

        if (log != null) {
            // safe to use log
            log.setCheckOutTime(LocalDateTime.now());
            log.calculateDuration();
            attendanceLogRepository.save(log);
        } else {
            // no ongoing session found
            System.out.println("No active session to check out.");
        }
    }
 // ✅ Get logs by mobile and date
    public List<AttendanceLog> getLogsByMobileAndDate(String mobile, LocalDate date) {
        return attendanceLogRepository.findByMobileAndDate(mobile, date);
    }


    // ✅ Total Time for Today (seconds)
    public long getTotalProductiveTimeForToday(String mobile) 
    {
        List<AttendanceLog> logs = attendanceLogRepository
                .findByMobileAndDate(mobile, LocalDate.now());

        long total = 0;
        for (AttendanceLog log : logs) 
        {
            if (log.getCheckInTime() != null && log.getCheckOutTime() != null) {
                Duration dur = Duration.between(log.getCheckInTime(), log.getCheckOutTime());
                total += dur.getSeconds();
            }
        }
        return total;
    }

    // ✅ Total Time for Today (formatted HH:mm:ss)
    public String getFormattedProductiveTimeForToday(String mobile) {
        long totalSeconds = getTotalProductiveTimeForToday(mobile);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    
    
    public LeaveRequest submitLeaveRequest(String mobile, LeaveRequest leaveRequest) {
        // Verify intern exists
        InternModel intern = internRepository.findByMobile(mobile);
        if (intern == null) {
            throw new RuntimeException("Intern not found for mobile: " + mobile);
        }
        
        // Set additional fields
        leaveRequest.setInternMobile(mobile);
        leaveRequest.setRequestDate(LocalDate.now());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        
        // Save and return the leave request
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public List<LeaveRequest> getLeaveRequestsByMobile(String mobile) {
        InternModel intern = internRepository.findByMobile(mobile);
        if (intern == null) {
            throw new RuntimeException("Intern not found for mobile: " + mobile);
        }
        return leaveRequestRepository.findByInternMobile(mobile);
    }

    
    
}
