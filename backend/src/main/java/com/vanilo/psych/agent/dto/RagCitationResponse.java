package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagCitationResponse {
    private int rank;
    private String id;
    private String category;
    private String source;
    private Double relevanceScore;
    private String confidenceLabel;
    private String matchReason;
    private String excerpt;
}
