package com.vanilo.psych.agent.dto;

import com.vanilo.psych.agent.enums.IntentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse {
    private IntentType intent;
    private String reply;
    private AnalyzeResponse analyzeResponse;
}
