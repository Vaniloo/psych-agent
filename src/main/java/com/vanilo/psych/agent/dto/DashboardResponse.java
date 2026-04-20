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
}
