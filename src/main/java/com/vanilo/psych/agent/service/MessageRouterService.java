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
    private final ExcelRecordService excelRecordService;

    public MessageRouterService(IntentService intentService, ChatService chatService, PsychologicalService psychologicalService, ExcelRecordService excelRecordService) {
        this.intentService = intentService;
        this.chatService = chatService;
        this.psychologicalService = psychologicalService;
        this.excelRecordService = excelRecordService;
    }
    public MessageResponse route(String message,String username){
        if(message==null||username==null||message.isBlank()||username.isBlank()){
            throw new RuntimeException("信息和用户名不能为空");
        }
        IntentResult result = intentService.classify(message);
        switch (result.getIntent()){
            case CHAT:return new MessageResponse(
                    result.getIntent(),
                    chatService.plainChat(message),
                    null
            );


            case CONSULT, RISK: {
                AnalyzeResponse response = psychologicalService.analyze(message,username);
                if("high".equalsIgnoreCase(response.getRisk())){
                    excelRecordService.appendHighRiskRecord(username,message,response);
                }
                return  new MessageResponse(
                        result.getIntent(),
                        chatService.ragChat(message),
                        response
                );

            }

        };
        throw new RuntimeException("解析错误");


    }
}
