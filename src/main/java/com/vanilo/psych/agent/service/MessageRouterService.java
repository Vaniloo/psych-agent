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
    private final MailAlertService mailAlertService;
    private final RiskAlertLockService riskAlertLockService;
    private final UserMessageLockService userMessageLockService;

    public MessageRouterService(IntentService intentService, ChatService chatService, PsychologicalService psychologicalService, ExcelRecordService excelRecordService, MailAlertService mailAlertService, RiskAlertLockService riskAlertLockService, UserMessageLockService userMessageLockService) {
        this.intentService = intentService;
        this.chatService = chatService;
        this.psychologicalService = psychologicalService;
        this.excelRecordService = excelRecordService;
        this.mailAlertService = mailAlertService;
        this.riskAlertLockService = riskAlertLockService;
        this.userMessageLockService = userMessageLockService;
    }
    public MessageResponse route(String message,String username){
        if(message==null||username==null||message.isBlank()||username.isBlank()){
            throw new RuntimeException("信息和用户名不能为空");
        }
        boolean isLocked=userMessageLockService.tryUserLock(username,message);
        if(!isLocked){
            throw new RuntimeException("请勿频繁输入相同内容");
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
                    boolean locked=riskAlertLockService.tryLock(username,message);
                    if(locked){
                        excelRecordService.appendHighRiskRecord(username,message,response);
                        try {
                            mailAlertService.sendHighRiskEmail(username,message,response);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                        System.out.println("Locked");
                    }
                }
                return  new MessageResponse(
                        result.getIntent(),
                        chatService.ragChat(message),
                        response
                );

            }

        }
        throw new RuntimeException("解析错误");


    }
}
