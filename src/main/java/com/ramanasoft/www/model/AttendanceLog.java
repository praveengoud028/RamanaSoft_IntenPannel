package com.ramanasoft.www.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance_logs")
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mobile;

    private LocalDate date;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private String duration = "00:00:00";

    public void calculateDuration() {
        if (checkInTime != null && checkOutTime != null) {
            Duration dur = Duration.between(checkInTime, checkOutTime);
            long hours = dur.toHours();
            long minutes = dur.toMinutes() % 60;
            long seconds = dur.getSeconds() % 60;

            this.duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }
}
