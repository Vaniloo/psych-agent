package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 32)
    private String status = "PENDING";

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "replied_by")
    private Long repliedBy;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}