package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.AdminMemoryResponse;
import com.vanilo.psych.agent.dto.ConversationSessionResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.dto.UserMemoryToolResponse;
import com.vanilo.psych.agent.service.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GetUserMemoryTool implements ToolExecutor {
    private final ConversationMemoryService conversationMemoryService;

    public GetUserMemoryTool(ConversationMemoryService conversationMemoryService) {
        this.conversationMemoryService = conversationMemoryService;
    }

    @Override
    public String getName() {
        return "get_user_memory";
    }

    @Override
    public ToolInfoResponse getToolInfo() {
        return new ToolInfoResponse(
                getName(),
                "获取用户的中长期记忆摘要与画像信息，可选返回最近会话概览",
                List.of(
                        new ToolParameterInfo("username", "string", true, "要查询的用户名"),
                        new ToolParameterInfo("includeSessions", "boolean", false, "是否包含最近会话概览"),
                        new ToolParameterInfo("sessionLimit", "integer", false, "会话概览数量，默认3，最大10")
                )
        );
    }

    @Override
    public UserMemoryToolResponse execute(Map<String, Object> arguments) {
        String username = arguments.get("username") == null ? null : arguments.get("username").toString();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("username不能为空");
        }
        boolean includeSessions = arguments.get("includeSessions") instanceof Boolean value && value;
        int sessionLimit = arguments.get("sessionLimit") instanceof Number number ? number.intValue() : 3;
        if (sessionLimit <= 0 || sessionLimit > 10) {
            sessionLimit = 3;
        }

        AdminMemoryResponse memory = conversationMemoryService.getAdminMemory(username);
        List<ConversationSessionResponse> sessions = includeSessions
                ? memory.getSessions().stream().limit(sessionLimit).toList()
                : List.of();

        return new UserMemoryToolResponse(
                memory.getUsername(),
                memory.getSummary(),
                memory.getLongTermMemory(),
                memory.getUpdatedAt(),
                memory.getProfile(),
                sessions
        );
    }
}
