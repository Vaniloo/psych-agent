package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.AnalysisResult;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.service.PsychologicalService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RiskScanTool implements ToolExecutor {
    private final PsychologicalService psychologicalService;

    public RiskScanTool(PsychologicalService psychologicalService) {
        this.psychologicalService = psychologicalService;
    }

    @Override
    public String getName() {
        return "risk_scan";
    }

    @Override
    public ToolInfoResponse getToolInfo() {
        return new ToolInfoResponse(
                getName(),
                "扫描用户消息的心理风险等级与主要情绪，不写入正式报告",
                List.of(
                        new ToolParameterInfo("message", "string", true, "待分析的用户消息")
                )
        );
    }

    @Override
    public AnalysisResult execute(Map<String, Object> arguments) {
        String message = arguments.get("message") == null ? null : arguments.get("message").toString();
        if (message == null || message.isBlank()) {
            throw new RuntimeException("message不能为空");
        }
        return psychologicalService.scan(message);
    }
}
