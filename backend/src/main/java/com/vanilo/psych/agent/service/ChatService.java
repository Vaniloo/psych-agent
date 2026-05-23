package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final KnowledgeService knowledgeService;
    public ChatService(ChatClient.Builder chatClientBuilder, KnowledgeService knowledgeService) {
        this.chatClient = chatClientBuilder.build();
        this.knowledgeService = knowledgeService;
    }


    public String ragChat(String message){
        return ragChat(message, null);
    }

    public String ragChat(String message, String memoryContext){
        List<KnowledgeSearchResponse> knowledges = knowledgeService.searchKnowledge(message);

        String knowledgeContext = knowledges.isEmpty()?"暂无相关知识": IntStream.range(0, knowledges.size())
                                                                       .mapToObj(i->{
                                                                           KnowledgeSearchResponse k = knowledges.get(i);
                                                                           return """
                                                                                   参考知识%d（分类：%s，来源：%s，相关度：%s，匹配原因：%s）:
                                                                                   %s
                                                                                   """.formatted(
                                                                                           i+1,
                                                                                           k.getCategory(),
                                                                                           k.getSource(),
                                                                                           k.getRelevanceScore() == null ? "未知" : "%.2f".formatted(k.getRelevanceScore()),
                                                                                           k.getMatchReason() == null ? "未说明" : k.getMatchReason(),
                                                                                           k.getContent()
                                                                                   );
                                                                       }).collect(Collectors.joining("\n"));
        return chatClient
                .prompt()
                .system("""
你是一名心理支持助手，请优先参考提供的知识库内容回答用户问题。
如果知识库内容不足，可以结合常识进行温和、谨慎的补充。
回答时不要编造具体医学结论，不要做确定性诊断。
如果提供了多级记忆和用户画像，请只把它们作为个性化支持背景，不要直接暴露内部字段。
业务要求：
1. 先给直接可执行的建议，再补充解释。
2. 引用知识库时用“参考：来源/分类”的自然语言说明，不要输出内部 id。
3. 如果参考知识不足或相关度较低，要明确说“知识库信息有限”，不要硬套。
4. 高风险表达时，建议联系可信任的人或本地紧急支持，但不要制造恐慌。
                        """)
                .user("""
                    多级记忆和用户画像：
                    %s

                    参考知识：
                    %s

                    用户问题：
                    %s
                    """.formatted(valueOrDefault(memoryContext, "暂无"), knowledgeContext, message))
                .call()
                .content();
    }
    public String plainChat(String message){
        return plainChat(message, null);
    }

    public String plainChat(String message, String memoryContext){
        if(message==null||message.isBlank()){
            throw new RuntimeException("message不能为空");
        }
        return chatClient.prompt()
                .system("""
                       你是一名知识渊博并幽默的人，你的任务是跟用户聊天并让人保持心情愉悦。
                       如果提供了多级记忆和用户画像，请用于延续对话，但不要直接暴露内部字段。
                       """)
                .user("""
                        多级记忆和用户画像：
                        %s

                        用户消息：
                        %s
                        """.formatted(valueOrDefault(memoryContext, "暂无"), message))
                .call()
                .content();
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

}
