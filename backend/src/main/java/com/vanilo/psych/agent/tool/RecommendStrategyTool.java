package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.AdminMemoryResponse;
import com.vanilo.psych.agent.dto.AnalysisResult;
import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.dto.StrategyRecommendationResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.service.ConversationMemoryService;
import com.vanilo.psych.agent.service.KnowledgeService;
import com.vanilo.psych.agent.service.PsychologicalService;
import com.vanilo.psych.agent.service.RiskDetectionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RecommendStrategyTool implements ToolExecutor {
    private final PsychologicalService psychologicalService;
    private final ConversationMemoryService conversationMemoryService;
    private final KnowledgeService knowledgeService;
    private final RiskDetectionService riskDetectionService;

    public RecommendStrategyTool(PsychologicalService psychologicalService,
                                 ConversationMemoryService conversationMemoryService,
                                 KnowledgeService knowledgeService,
                                 RiskDetectionService riskDetectionService) {
        this.psychologicalService = psychologicalService;
        this.conversationMemoryService = conversationMemoryService;
        this.knowledgeService = knowledgeService;
        this.riskDetectionService = riskDetectionService;
    }

    @Override
    public String getName() {
        return "recommend_strategy";
    }

    @Override
    public ToolInfoResponse getToolInfo() {
        return new ToolInfoResponse(
                getName(),
                "结合风险扫描、用户记忆和知识库检索，生成当前轮次的陪伴与干预策略",
                List.of(
                        new ToolParameterInfo("username", "string", true, "当前用户的用户名"),
                        new ToolParameterInfo("message", "string", true, "当前用户消息"),
                        new ToolParameterInfo("category", "string", false, "知识检索分类，如sleep或anxiety"),
                        new ToolParameterInfo("knowledgeLimit", "integer", false, "参考知识数量，默认3，最大5")
                )
        );
    }

    @Override
    public StrategyRecommendationResponse execute(Map<String, Object> arguments) {
        String username = arguments.get("username") == null ? null : arguments.get("username").toString();
        String message = arguments.get("message") == null ? null : arguments.get("message").toString();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("username不能为空");
        }
        if (message == null || message.isBlank()) {
            throw new RuntimeException("message不能为空");
        }

        String category = arguments.get("category") == null ? null : arguments.get("category").toString();
        int knowledgeLimit = arguments.get("knowledgeLimit") instanceof Number number ? number.intValue() : 3;
        if (knowledgeLimit <= 0 || knowledgeLimit > 5) {
            knowledgeLimit = 3;
        }

        AnalysisResult analysis = riskDetectionService.isHighRisk(message)
                ? new AnalysisResult("high", "危机", 1.0)
                : psychologicalService.scan(message);
        AdminMemoryResponse memory = conversationMemoryService.getAdminMemory(username);
        List<KnowledgeSearchResponse> references = knowledgeService.searchKnowledge(message, category, knowledgeLimit);

        return new StrategyRecommendationResponse(
                analysis,
                recommendedTone(analysis.getRisk()),
                buildMemoryFocus(memory),
                "high".equalsIgnoreCase(analysis.getRisk()),
                suggestedActions(analysis.getRisk(), analysis.getEmotion(), memory),
                references
        );
    }

    private String recommendedTone(String risk) {
        if ("high".equalsIgnoreCase(risk)) {
            return "冷静、直接、强支持，优先确认安全并提供立即求助指引";
        }
        if ("medium".equalsIgnoreCase(risk)) {
            return "温和、结构化、共情，帮助用户拆解压力并给出小步建议";
        }
        return "轻松、稳定、陪伴感强，先理解用户再给出自然回应";
    }

    private String buildMemoryFocus(AdminMemoryResponse memory) {
        String profileSummary = memory.getProfile() == null ? "" : memory.getProfile().getProfileSummary();
        String concerns = memory.getProfile() == null ? "" : memory.getProfile().getConcerns();
        String preferences = memory.getProfile() == null ? "" : memory.getProfile().getPreferences();
        return "画像概览：" + blankDefault(profileSummary, "暂无")
                + "；当前主要困扰：" + blankDefault(concerns, "暂无")
                + "；沟通偏好：" + blankDefault(preferences, "暂无");
    }

    private List<String> suggestedActions(String risk, String emotion, AdminMemoryResponse memory) {
        if ("high".equalsIgnoreCase(risk)) {
            return List.of(
                    "立即切换到高风险固定兜底回复，避免开放式长建议",
                    "明确建议联系身边可信任的人、热线或线下急救资源",
                    "同步记录风险事件并触发人工跟进"
            );
        }
        if ("medium".equalsIgnoreCase(risk)) {
            return List.of(
                    "先复述并确认用户的主要情绪：" + blankDefault(emotion, "压力或低落"),
                    "围绕用户画像中的困扰提供1到3条可执行建议",
                    "在回复末尾加入可继续追问的支持性问题"
            );
        }
        return List.of(
                "保持自然陪伴式回复，避免过度医疗化表达",
                "结合用户偏好延续熟悉的沟通风格",
                "如适合，可顺带提供轻量知识或自助练习"
        );
    }

    private String blankDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
