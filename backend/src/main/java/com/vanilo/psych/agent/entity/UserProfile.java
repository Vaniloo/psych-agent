package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String profileSummary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String concerns;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String preferences;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String copingStrategies;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String riskSignals;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String supportGoals;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
