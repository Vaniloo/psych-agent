package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentChatResponse {
    private String reply;
    private boolean usedTool;
    private String toolName;
    private Long sessionId;
    private boolean crisis;
    private String helpCenterUrl;
    private List<HelpResourceResponse> resources;
    private AnalyzeResponse analyzeResponse;
    private RagTraceResponse ragTrace;

    public AgentChatResponse(String reply, boolean usedTool, String toolName, Long sessionId) {
        this.reply = reply;
        this.usedTool = usedTool;
        this.toolName = toolName;
        this.sessionId = sessionId;
        this.resources = List.of();
    }

    public AgentChatResponse(String reply,
                             boolean usedTool,
                             String toolName,
                             Long sessionId,
                             RagTraceResponse ragTrace) {
        this(reply, usedTool, toolName, sessionId);
        this.ragTrace = ragTrace;
    }
}
