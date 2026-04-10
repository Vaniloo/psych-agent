package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.IntentResult;
import com.vanilo.psych.agent.dto.MessageResponse;
import org.springframework.stereotype.Service;

@Service
public class MessageRouterService {
    private final IntentService intentService;
    private final ChatService chatService;
    private final PsychologicalService psychologicalService;
    public MessageRouterService(IntentService intentService, ChatService chatService, PsychologicalService psychologicalService) {
        this.intentService = intentService;
        this.chatService = chatService;
        this.psychologicalService = psychologicalService;
    }
    public MessageResponse route(String message,String username){
        if(message==null||username==null||message.isBlank()||username.isBlank()){
            throw new RuntimeException("信息和用户名不能为空");
        }
        IntentResult result = intentService.classify(message);
        return switch (result.getIntent()){
            case CHAT ->
                    new MessageResponse(
                        result.getIntent(),
                        chatService.chat(message),
                        null
                );

            case CONSULT, RISK ->
                        new MessageResponse(
                        result.getIntent(),
                        chatService.chat(message),
                        psychologicalService.analyze(message, username)
                );

        };


    }
}
