package com.ramanasoft.www.repository;


import com.ramanasoft.www.model.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    
	List<AttendanceLog> findByMobileAndCheckInTimeBetween(String mobile, LocalDateTime start, LocalDateTime end);


    Optional<AttendanceLog> findTopByMobileAndDateAndCheckOutTimeIsNullOrderByIdDesc(String mobile, LocalDate date);

    List<AttendanceLog> findByMobileAndDate(String mobile, LocalDate date);

	Optional<AttendanceLog> findTopByMobileOrderByCheckInTimeDesc(String mobile);
}