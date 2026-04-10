package com.vanilo.psych.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_document")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocument {
    @Id
    private String id;
    @Column(nullable = false,columnDefinition = "TEXT")
    private String content;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private String source;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
}
