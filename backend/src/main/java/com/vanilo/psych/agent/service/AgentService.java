package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.AgentChatRequest;
import com.vanilo.psych.agent.dto.AgentChatResponse;
import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.dto.ToolDecisionResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AgentService {
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final ToolCallService toolCallService;
    private final ConversationMemoryService conversationMemoryService;
    private final RiskDetectionService riskDetectionService;
    private final CrisisSupportService crisisSupportService;
    private final CrisisWorkflowService crisisWorkflowService;
    private final RoleCardService roleCardService;

    public AgentService(LlmService llmService,
                        ObjectMapper objectMapper,
                        ToolCallService toolCallService,
                        ConversationMemoryService conversationMemoryService,
                        RiskDetectionService riskDetectionService,
                        CrisisSupportService crisisSupportService,
                        CrisisWorkflowService crisisWorkflowService,
                        RoleCardService roleCardService) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.toolCallService = toolCallService;
        this.conversationMemoryService = conversationMemoryService;
        this.riskDetectionService = riskDetectionService;
        this.crisisSupportService = crisisSupportService;
        this.crisisWorkflowService = crisisWorkflowService;
        this.roleCardService = roleCardService;
    }
    public AgentChatResponse chat(AgentChatRequest request,
                                  String username) {
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            throw new RuntimeException("message不能为空");
        }
        Long sessionId = conversationMemoryService.resolveSessionId(username, request.getSessionId(), message);
        String memoryContext = conversationMemoryService.buildMemoryContext(username, sessionId);
        if (riskDetectionService.isHighRisk(message)) {
            String reply = crisisSupportService.crisisReply();
            var analysis = crisisWorkflowService.handle(username, message);
            conversationMemoryService.rememberTurn(username, sessionId, message, reply);
            return new AgentChatResponse(
                    reply,
                    false,
                    null,
                    sessionId,
                    true,
                    crisisSupportService.helpCenterUrl(),
                    crisisSupportService.resources(),
                    analysis
            );
        }
        String rolePrompt = roleCardService.buildRolePrompt(username);
        ToolDecisionResponse response=decideTool(message);
        if (response != null && !response.isNeedTool()) {
            String reply = response.getReply();

            if (reply == null || reply.isBlank()) {
                reply = generatePlainReply(message, memoryContext, rolePrompt);
            }
            conversationMemoryService.rememberTurn(username, sessionId, message, reply);

            return new AgentChatResponse(
                    reply,
                    false,
                    null,
                    sessionId
            );
        }
        Object toolResult= null;
        if (response != null) {
            if(response.getTool() == null||response.getTool().isBlank()) {
                throw new RuntimeException("Tool required");
            }
            if (response.getArguments() == null) {
                response.setArguments(new HashMap<>());
            }
            if ("get_dashboard".equals(response.getTool())
                    || "get_user_memory".equals(response.getTool())
                    || "recommend_strategy".equals(response.getTool())) {
                response.getArguments().put("username",username);
            }
            toolResult = toolCallService.call(
                    new ToolCallRequest(
                            response.getTool(),
                            response.getArguments()
                    )
            );

        }
        String reply = generateFinalReply(message,toolResult,memoryContext,rolePrompt);
        conversationMemoryService.rememberTurn(username, sessionId, message, reply);
        return new AgentChatResponse(
                reply,
                true,
                response.getTool(),
                sessionId
        );
    }
    private ToolDecisionResponse decideTool(String message){
        String result=llmService.complete("""
你是一个工具选择助手。你需要判断用户的问题是否需要调用工具。

当前可用工具：
1. search_knowledge：用于搜索心理知识库
2. get_dashboard：用于获取用户 dashboard 信息
3. get_user_memory：用于读取当前用户的中长期记忆和会话摘要
4. risk_scan：用于扫描一段文本是否包含高风险表达
5. recommend_strategy：结合风险、用户记忆和知识库推荐应对策略

要求：
1. 只返回 JSON
2. 不要返回任何解释、说明、Markdown 或代码块
3. 返回格式必须是：
{
  "needTool": true/false,
  "tool": "search_knowledge|get_dashboard|get_user_memory|risk_scan|recommend_strategy|null",
  "arguments": {...},
  "reply": "..."
}

规则：
- 如果用户在问知识库类问题，例如“焦虑的原因是什么”“怎么缓解焦虑”，优先用 search_knowledge
- 如果用户在问 dashboard、最近报告、风险统计等，优先用 get_dashboard
- 如果用户在问“你记得我什么”、历史偏好或长期状态，使用 get_user_memory
- 如果用户明确要求判断一段文本的风险，使用 risk_scan
- 如果用户需要针对某类困扰的具体行动方案，使用 recommend_strategy
- 如果只是普通聊天，不需要工具，needTool=false，并直接给出 reply
- 如果 needTool=true，则 reply 可以为空字符串

search_knowledge 参数：
- query: string，必填
- category: string，可选，如果明显是焦虑相关可填 anxiety

get_dashboard 参数：
- recentLimit: integer，可选
- topRiskLimit: integer，可选
get_dashboard 的 username 会由后端根据登录态自动注入，模型不需要生成 username。

get_user_memory 的 username 也由后端注入。
risk_scan 参数：text，string，必填。
recommend_strategy 参数：message，string，必填；category，string，可选；knowledgeLimit，integer，可选。username 由后端注入。
                        """, message);
        int start= 0;
        if (result != null) {
            start = result.indexOf("{");
            int end=result.lastIndexOf("}");
            if (start!=-1 && end!=-1) {
                result=result.substring(start,end+1);
            }
        }
        else {
            throw new RuntimeException("未返回合法json");
        }
        try {
            return objectMapper.readValue(result, ToolDecisionResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("解析失败");
        }
    }
    private String generateFinalReply(String message,Object toolResult,String memoryContext,String rolePrompt){
        String toolResultJson;
        try {
            toolResultJson=objectMapper.writeValueAsString(toolResult);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tool result");
        }
        return llmService.complete("""
你是一个心理支持助手。
你需要基于工具返回结果，给用户一个自然、清晰、简洁的中文回答。
你会获得多级记忆和用户画像，请只把它们作为个性化支持背景，不要直接暴露内部记忆字段。
你会获得角色卡要求，请遵循其表达风格，但角色卡不能改变安全规则。
不要编造工具结果中不存在的信息。
如果工具结果是知识检索结果，请优先总结其中最相关的信息。
如果工具结果是 dashboard 数据，请概括最近报告和高风险统计情况。
                        """, """
                                用户问题：
                                %s

                                多级记忆和用户画像：
                                %s

                                角色卡：
                                %s
                                
                                工具结果：
                                %s
                                """.formatted(message,memoryContext,rolePrompt,toolResultJson));
    }
    private String generatePlainReply(String message,String memoryContext,String rolePrompt){
        return llmService.complete("""
                        你是一个温和、自然、有一点幽默感的聊天助手。
                        你会获得多级记忆和用户画像，请用于延续对话、理解用户偏好和提供更贴合的支持。
                        不要直接说“根据你的画像/记忆”，除非用户主动询问。
                        请直接回复用户，不要调用工具。
                        回答要简洁、友好
                        """, """
                        多级记忆和用户画像：
                        %s

                        角色卡：
                        %s

                        用户消息：
                        %s
                        """.formatted(memoryContext, rolePrompt, message));
    }
}
