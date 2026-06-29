package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor

@NoArgsConstructor
public class DashboardResponse {
    private List<ReportSummaryResponse> reportSummaryResponse;
    private List<TopRiskUserResponse> topRiskUserResponse;
    private List<RiskCountResponse> riskDistribution;
    private List<EmotionTrendResponse> emotionTrend;

    public DashboardResponse(List<ReportSummaryResponse> reportSummaryResponse,
                             List<TopRiskUserResponse> topRiskUserResponse) {
        this.reportSummaryResponse = reportSummaryResponse;
        this.topRiskUserResponse = topRiskUserResponse;
        this.riskDistribution = List.of();
        this.emotionTrend = List.of();
    }
}
