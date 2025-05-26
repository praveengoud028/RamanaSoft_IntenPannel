package com.ramanasoft.www.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String internMobile;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate requestDate = LocalDate.now(); // default to today

    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING; // default status

    public enum LeaveStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
