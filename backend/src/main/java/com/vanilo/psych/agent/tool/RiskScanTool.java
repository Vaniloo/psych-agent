package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.service.RiskDetectionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RiskScanTool implements ToolExecutor {
    private final RiskDetectionService riskDetectionService;

    public RiskScanTool(RiskDetectionService riskDetectionService) {
        this.riskDetectionService = riskDetectionService;
    }

    @Override
    public String getName() {
        return "risk_scan";
    }

    @Override
    public ToolInfoResponse getToolInfo() {
        return new ToolInfoResponse(
                getName(),
                "使用确定性规则快速扫描文本中的高风险表达，不写入正式报告",
                List.of(new ToolParameterInfo("text", "string", true, "待扫描文本"))
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        Object rawText = arguments.containsKey("text") ? arguments.get("text") : arguments.get("message");
        String text = rawText == null ? "" : rawText.toString();
        boolean highRisk = riskDetectionService.isHighRisk(text);
        return Map.of(
                "highRisk", highRisk,
                "risk", highRisk ? "high" : "not_high",
                "action", highRisk ? "trigger_crisis_workflow" : "continue_normal_flow"
        );
    }
}
