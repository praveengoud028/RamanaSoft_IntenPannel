package com.ramanasoft.www.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "attendance")
@NoArgsConstructor
@AllArgsConstructor
public class InternModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String batchNO;
    private String designation;
    private String department;

    @Lob
    private byte[] profileImage;

    
}
