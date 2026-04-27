package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportSummaryResponse {
    private Long id;
    private String message;
    private String risk;
    private String emotion;
    private Double confidence;
    private LocalDateTime createdAt;
}
