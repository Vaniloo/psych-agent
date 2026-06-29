package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.dto.IntentResult;
import com.vanilo.psych.agent.dto.MessageResponse;
import org.springframework.stereotype.Service;

@Service
public class MessageRouterService {
    private final IntentService intentService;
    private final ChatService chatService;
    private final PsychologicalService psychologicalService;
    private final UserMessageLockService userMessageLockService;
    private final ConversationMemoryService conversationMemoryService;
    private final CrisisSupportService crisisSupportService;
    private final CrisisWorkflowService crisisWorkflowService;
    private final RoleCardService roleCardService;

    public MessageRouterService(IntentService intentService,
                                ChatService chatService,
                                PsychologicalService psychologicalService,
                                UserMessageLockService userMessageLockService,
                                ConversationMemoryService conversationMemoryService,
                                CrisisSupportService crisisSupportService,
                                CrisisWorkflowService crisisWorkflowService,
                                RoleCardService roleCardService) {
        this.intentService = intentService;
        this.chatService = chatService;
        this.psychologicalService = psychologicalService;
        this.userMessageLockService = userMessageLockService;
        this.conversationMemoryService = conversationMemoryService;
        this.crisisSupportService = crisisSupportService;
        this.crisisWorkflowService = crisisWorkflowService;
        this.roleCardService = roleCardService;
    }
    public MessageResponse route(String message,String username){
        if(message==null||username==null||message.isBlank()||username.isBlank()){
            throw new RuntimeException("信息和用户名不能为空");
        }
        boolean isLocked=userMessageLockService.tryUserLock(username,message);
        if(!isLocked){
            throw new RuntimeException("请勿频繁输入相同内容");
        }
        Long sessionId = conversationMemoryService.resolveSessionId(username, null, message);
        String memoryContext = conversationMemoryService.buildMemoryContext(username, sessionId);
        String rolePrompt = roleCardService.buildRolePrompt(username);
        IntentResult result = intentService.classify(message);
        switch (result.getIntent()){
            case CHAT:{
                String reply = chatService.plainChat(message, memoryContext, rolePrompt);
                conversationMemoryService.rememberTurn(username, sessionId, message, reply);
                return new MessageResponse(
                        result.getIntent(),
                        reply,
                        null
                );
            }


            case CONSULT: {
                AnalyzeResponse response = psychologicalService.analyze(message,username);
                String reply = chatService.ragChat(message, memoryContext, rolePrompt);
                conversationMemoryService.rememberTurn(username, sessionId, message, reply);
                return  new MessageResponse(
                        result.getIntent(),
                        reply,
                        response
                );

            }

            case RISK: {
                AnalyzeResponse response = crisisWorkflowService.handle(username, message);
                String reply = crisisSupportService.crisisReply();
                conversationMemoryService.rememberTurn(username, sessionId, message, reply);
                return new MessageResponse(
                        result.getIntent(),
                        reply,
                        response,
                        true,
                        crisisSupportService.helpCenterUrl(),
                        crisisSupportService.resources()
                );
            }

        }
        throw new RuntimeException("解析错误");


    }

}
