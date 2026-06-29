package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "psychological_reports", indexes = {
        @Index(name = "idx_reports_user_created_at", columnList = "user_id,created_at"),
        @Index(name = "idx_reports_risk_created_at", columnList = "risk,created_at")
})
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
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
