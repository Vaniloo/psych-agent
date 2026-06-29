package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_alert_events", indexes = {
        @Index(name = "idx_risk_alert_status_created", columnList = "status,created_at")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskAlertEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reportId;

    @Column(nullable = false)
    private String username;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String risk;

    @Column(nullable = false)
    private String emotion;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Integer attempts;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
