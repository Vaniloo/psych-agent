package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeResponse {
    private Long reportId;
    private String risk;
    private String emotion;
    private Double confidence;
}
