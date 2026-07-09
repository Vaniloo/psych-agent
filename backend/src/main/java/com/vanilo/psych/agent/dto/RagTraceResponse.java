package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagTraceResponse {
    private String requestId;
    private String tool;
    private String query;
    private String category;
    private int citationCount;
    private String status;
    private LocalDateTime executedAt;
    private List<RagCitationResponse> citations;
}
