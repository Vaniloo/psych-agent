package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.AnalysisResult;
import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.enums.RiskLevel;
import com.vanilo.psych.agent.repository.PsychologicalReportRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PsychologicalService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper ;
    private final PsychologicalReportRepository psychologicalReportRepository;
    private final AlertService alertService;
    private final UserRepository userRepository;

    public PsychologicalService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, PsychologicalReportRepository psychologicalReportRepository, AlertService alertService, UserRepository userRepository) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.psychologicalReportRepository = psychologicalReportRepository;
        this.alertService = alertService;
        this.userRepository = userRepository;
    }
    public AnalyzeResponse analyze(String message,String username){
    String response=chatClient
                .prompt()
                .system("""
你是一个心理状态分析助手。
请根据用户输入分析其心理风险等级和主要情绪。

要求：
1. 只返回 JSON
2. 不要返回任何解释、说明、前后缀、Markdown 标记或代码块
3. risk 只能是 high、medium、low 三者之一
4. emotion 使用简洁中文词语，如 焦虑、低落、抑郁、平静 等
5. confidence 是 0 到 1 之间的小数

返回格式：
{
  "risk": "high|medium|low",
  "emotion": "...",
  "confidence": 0.xx
}
""")
                .user(message)
                .call()
                .content();
        System.out.println("原始返回："+response);
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");

        if (start != -1 && end != -1) {
            response = response.substring(start, end + 1);
        }
        else{
            throw new RuntimeException("未返回合法json"+response);
        }
        User user = userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("用户不存在！"));
        try{
             AnalysisResult analysisResult=objectMapper.readValue(response, AnalysisResult.class);
            PsychologicalReport psychologicalReport=new PsychologicalReport();
            psychologicalReport.setUser(user);
            psychologicalReport.setRisk(analysisResult.getRisk());
            psychologicalReport.setEmotion(analysisResult.getEmotion());
            psychologicalReport.setConfidence(analysisResult.getConfidence());
            psychologicalReport.setMessage(message);
            psychologicalReport.setCreatedAt(LocalDateTime.now());
            psychologicalReportRepository.save(psychologicalReport);
            RiskLevel level=RiskLevel.fromString(analysisResult.getRisk());
            if(level==RiskLevel.HIGH){
                alertService.sendHighRiskAlert(psychologicalReport);
            }
            return new AnalyzeResponse(
                    psychologicalReport.getId(),
                    analysisResult.getRisk(),
                    analysisResult.getEmotion(),
                    analysisResult.getConfidence()
            );
        }
        catch(Exception e){
            throw new RuntimeException("分析结果解析失败: " + response, e);
        }
    }
}
