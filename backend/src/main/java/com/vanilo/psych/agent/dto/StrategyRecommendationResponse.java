package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyRecommendationResponse {
    private AnalysisResult analysis;
    private String recommendedTone;
    private String memoryFocus;
    private boolean crisisEscalation;
    private List<String> suggestedActions;
    private List<KnowledgeSearchResponse> references;
}
