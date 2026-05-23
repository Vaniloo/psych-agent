package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSearchResponse {
    private String content;
    private String category;
    private String source;
    private String id;
    private Double relevanceScore;
    private String matchReason;

    public KnowledgeSearchResponse(String content, String category, String source, String id) {
        this.content = content;
        this.category = category;
        this.source = source;
        this.id = id;
    }
}
