package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.DashboardResponse;
import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ToolCallService {
    private final KnowledgeService knowledgeService;
    private final UserRepository userRepository;
    private final PsychologicalService psychologicalService;

    public ToolCallService(KnowledgeService knowledgeService,
                           UserRepository userRepository,
                           PsychologicalService psychologicalService) {
        this.knowledgeService = knowledgeService;
        this.userRepository = userRepository;
        this.psychologicalService = psychologicalService;
    }

    public Object call(ToolCallRequest request) {
        if (request == null) {
            throw new RuntimeException("request不能为空");
        }

        String tool = request.getTool();
        Map<String, Object> arguments = request.getArguments();

        if (tool == null || tool.isBlank()) {
            throw new RuntimeException("tool不能为空");
        }
        if (arguments == null) {
            throw new RuntimeException("arguments不能为空");
        }

        return switch (tool) {
            case "search_knowledge" -> callSearchKnowledge(arguments);
            case "get_dashboard" -> callGetDashboard(arguments);
            default -> throw new RuntimeException("未知工具: " + tool);
        };
    }

    private List<KnowledgeSearchResponse> callSearchKnowledge(Map<String, Object> arguments) {
        String query = arguments.get("query") == null ? null : arguments.get("query").toString();
        String category = arguments.get("category") == null ? null : arguments.get("category").toString();

        if (query == null || query.isBlank()) {
            throw new RuntimeException("query不能为空");
        }

        return knowledgeService.searchKnowledge(query, category);
    }

    private DashboardResponse callGetDashboard(Map<String, Object> arguments) {
        String username = arguments.get("username") == null ? null : arguments.get("username").toString();

        if (username == null || username.isBlank()) {
            throw new RuntimeException("username不能为空");
        }

        Object recentLimitObject = arguments.get("recentLimit");
        Object topRiskLimitObject = arguments.get("topRiskLimit");

        int recentLimit = recentLimitObject == null ? 20 : ((Number) recentLimitObject).intValue();
        int topRiskLimit = topRiskLimitObject == null ? 10 : ((Number) topRiskLimitObject).intValue();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Long userId = user.getId();
        return new DashboardResponse(
                psychologicalService.getRecentReports(userId, recentLimit),
                psychologicalService.getTopRiskUsers(topRiskLimit)
        );
    }
}