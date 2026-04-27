package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.AgentChatRequest;
import com.vanilo.psych.agent.dto.AgentChatResponse;
import com.vanilo.psych.agent.dto.ToolCallRequest;
import com.vanilo.psych.agent.dto.ToolDecisionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AgentService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ToolCallService toolCallService;

    public AgentService(ChatClient.Builder clientBuilder, ObjectMapper objectMapper, ToolCallService toolCallService) {
        this.chatClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.toolCallService = toolCallService;
    }
    public AgentChatResponse chat(AgentChatRequest request,
                                  String username) {
        String message = request.getMessage();
        ToolDecisionResponse response=decideTool(message);
        if (response != null && !response.isNeedTool()) {
            String reply = response.getReply();

            if (reply == null || reply.isBlank()) {
                reply = generatePlainReply(message);
            }

            return new AgentChatResponse(
                    reply,
                    false,
                    null
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
            if("get_dashboard".equals(response.getTool())){
                response.getArguments().put("username",username);
            }
            toolResult = toolCallService.call(
                    new ToolCallRequest(
                            response.getTool(),
                            response.getArguments()
                    )
            );

        }
        return new AgentChatResponse(
                generateFinalReply(message,toolResult),
                true,
                response.getTool()
        );
    }
    private ToolDecisionResponse decideTool(String message){
        String result=chatClient.prompt()
                .system("""
你是一个工具选择助手。你需要判断用户的问题是否需要调用工具。

当前可用工具：
1. search_knowledge：用于搜索心理知识库
2. get_dashboard：用于获取用户 dashboard 信息

要求：
1. 只返回 JSON
2. 不要返回任何解释、说明、Markdown 或代码块
3. 返回格式必须是：
{
  "needTool": true/false,
  "tool": "search_knowledge|get_dashboard|null",
  "arguments": {...},
  "reply": "..."
}

规则：
- 如果用户在问知识库类问题，例如“焦虑的原因是什么”“怎么缓解焦虑”，优先用 search_knowledge
- 如果用户在问 dashboard、最近报告、风险统计等，优先用 get_dashboard
- 如果只是普通聊天，不需要工具，needTool=false，并直接给出 reply
- 如果 needTool=true，则 reply 可以为空字符串

search_knowledge 参数：
- query: string，必填
- category: string，可选，如果明显是焦虑相关可填 anxiety

get_dashboard 参数：
- recentLimit: integer，可选
- topRiskLimit: integer，可选
get_dashboard 的 username 会由后端根据登录态自动注入，模型不需要生成 username。
                        """)
                .user(message)
                .call()
                .content();
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
    private String generateFinalReply(String message,Object toolResult){
        String toolResultJson;
        try {
            toolResultJson=objectMapper.writeValueAsString(toolResult);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tool result");
        }
        return chatClient.prompt()
                .system("""
你是一个心理支持助手。
你需要基于工具返回结果，给用户一个自然、清晰、简洁的中文回答。
不要编造工具结果中不存在的信息。
如果工具结果是知识检索结果，请优先总结其中最相关的信息。
如果工具结果是 dashboard 数据，请概括最近报告和高风险统计情况。
                        """)
                .user(
                        """
                                用户问题：
                                %s
                                
                                工具结果：
                                %s
                                """.formatted(message,toolResultJson)
                )
                .call()
                .content();
    }
    private String generatePlainReply(String message){
        return chatClient.prompt()
                .system("""
                        你是一个温和、自然、有一点幽默感的聊天助手。
                        请直接回复用户，不要调用工具。
                        回答要简洁、友好
                        """)
                .user(message)
                .call()
                .content();
    }
}
