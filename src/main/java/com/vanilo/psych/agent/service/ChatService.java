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


    public String chat(String message){
        List<KnowledgeSearchResponse> knowledges = knowledgeService.searchKnowledge(message);

        String knowledgeContext = knowledges.isEmpty()?"暂无相关知识": IntStream.range(0, knowledges.size())
                                                                       .mapToObj(i->{
                                                                           KnowledgeSearchResponse k = knowledges.get(i);
                                                                           return """
                                                                                   参考知识%d（分类：%s，来源：%s）:
                                                                                   %s
                                                                                   """.formatted(i+1,k.getCategory(),k.getSource(),k.getContent());
                                                                       }).collect(Collectors.joining("\n"));
        return chatClient
                .prompt()
                .system("""
你是一名心理支持助手，请优先参考提供的知识库内容回答用户问题。
如果知识库内容不足，可以结合常识进行温和、谨慎的补充。
回答时不要编造具体医学结论，不要做确定性诊断。
                        """)
                .user("""
                    参考知识：
                    %s

                    用户问题：
                    %s
                    """.formatted(knowledgeContext, message))
                .call()
                .content();
    }

}
