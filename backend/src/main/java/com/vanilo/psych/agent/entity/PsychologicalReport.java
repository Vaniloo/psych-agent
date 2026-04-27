package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "psychological_reports")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PsychologicalReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String message;
    @Column(nullable = false)
    private String risk;
    @Column(nullable = false)
    private String emotion;
    @Column(nullable = false)
    private Double confidence;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
