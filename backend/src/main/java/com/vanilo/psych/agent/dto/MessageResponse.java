package com.vanilo.psych.agent.dto;

import com.vanilo.psych.agent.enums.IntentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse {
    private IntentType intent;
    private String reply;
    private AnalyzeResponse analyzeResponse;
    private boolean crisis;
    private String helpCenterUrl;
    private List<HelpResourceResponse> resources;

    public MessageResponse(IntentType intent, String reply, AnalyzeResponse analyzeResponse) {
        this.intent = intent;
        this.reply = reply;
        this.analyzeResponse = analyzeResponse;
        this.crisis = false;
        this.resources = List.of();
    }
}
