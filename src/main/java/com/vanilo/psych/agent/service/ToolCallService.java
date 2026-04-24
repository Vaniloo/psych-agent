package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.*;
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
    public  List<ToolInfoResponse> listTools(){
        ToolInfoResponse searchKnowledge=new ToolInfoResponse(
                "search_knowledge",
                "在心理知识库中搜索相关内容",
                List.of(
                        new ToolParameterInfo("query","string",true,"用户的问题"),
                        new ToolParameterInfo("category","string",false,"知识分类，如anxiety")
                )
        );
        ToolInfoResponse getDashboard=new ToolInfoResponse(
                "get_dashboard",
                "获取用户心理报告和风险统计",
                List.of (new ToolParameterInfo("username","string",true,"用户名"),
                new ToolParameterInfo("recentLimit","integer",false,"最近报告数量"),
                new ToolParameterInfo("topRiskLimit","integer",false,"高风险用户数量")
                )
        );
        return List.of(searchKnowledge,getDashboard);
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