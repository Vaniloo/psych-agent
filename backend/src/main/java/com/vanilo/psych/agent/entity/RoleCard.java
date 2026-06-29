package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_cards")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String tone;

    @Column(nullable = false)
    private String responseStyle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String customInstructions;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String forbiddenExpressions;

    @Column(nullable = false)
    private boolean preset;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
