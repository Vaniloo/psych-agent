package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.AdminMemoryResponse;
import com.vanilo.psych.agent.dto.StrategyRecommendationResponse;
import com.vanilo.psych.agent.service.ConversationMemoryService;
import com.vanilo.psych.agent.service.KnowledgeService;
import com.vanilo.psych.agent.service.PsychologicalService;
import com.vanilo.psych.agent.service.RiskDetectionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendStrategyToolTests {
    @Test
    void deterministicRiskCheckBypassesModelScan() {
        PsychologicalService psychologicalService = mock(PsychologicalService.class);
        ConversationMemoryService memoryService = mock(ConversationMemoryService.class);
        KnowledgeService knowledgeService = mock(KnowledgeService.class);
        RiskDetectionService riskDetectionService = mock(RiskDetectionService.class);
        AdminMemoryResponse memory = new AdminMemoryResponse();
        memory.setSessions(List.of());

        when(riskDetectionService.isHighRisk("我想结束生命")).thenReturn(true);
        when(memoryService.getAdminMemory("alice")).thenReturn(memory);
        when(knowledgeService.searchKnowledge("我想结束生命", null, 3)).thenReturn(List.of());

        RecommendStrategyTool tool = new RecommendStrategyTool(
                psychologicalService, memoryService, knowledgeService, riskDetectionService
        );
        StrategyRecommendationResponse result = tool.execute(Map.of(
                "username", "alice",
                "message", "我想结束生命"
        ));

        assertTrue(result.isCrisisEscalation());
        verifyNoInteractions(psychologicalService);
    }
}
