package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.IntentResult;
import com.vanilo.psych.agent.enums.IntentType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntentService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    public IntentService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    public IntentResult classify(String message) {
        if (message == null || message.isBlank()) {
            throw new RuntimeException("信息不能为空");
        }
        if(isHighrisk(message)){
            return new IntentResult(IntentType.RISK,1.0,"命中风险词");
        }
        String result=chatClient.prompt()
                .system("""
你是一个意图分类器
要求：
1. 只返回 JSON
2. 不要返回任何解释、说明、前后缀、Markdown 标记或代码块
3. intent 只能是 CHAT、CONSULT、RISK 三者之一
判断规则：
- 普通聊天 → CHAT
- 寻求建议/倾诉/心理困扰 → CONSULT
- 明显自伤/轻生/极端绝望 → RISK
4. confidence 是 0 到 1 之间的小数
5. reason用简洁的语言说明原因
返回格式：
{
  "intent": "CHAT|CONSULT|RISK",
  "confidence": 0.xx,
  "reason": "..."
}
                        """)
                .user(message)
                .call()
                .content();
        int start = result.indexOf("{");
        int end = result.lastIndexOf("}");

        if (start != -1 && end != -1) {
            result = result.substring(start, end + 1);
        }
        else{
            throw new RuntimeException("未返回合法json"+result);
        }
        try {
            return objectMapper.readValue(result,IntentResult.class);
        }
        catch (Exception e) {
            throw new RuntimeException("意图分类解析失败："+result,e);
        }


    }
    public Boolean isHighrisk(String message) {
        List<String> riskWords=List.of(
                "想死",
                "不想活",
                "自杀",
                "结束自己",
                "活着没意义"
        );
        return riskWords.stream().anyMatch(message::contains);
    }
}
