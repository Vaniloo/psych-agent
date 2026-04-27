package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.AnalysisResult;
import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.dto.ReportSummaryResponse;
import com.vanilo.psych.agent.dto.TopRiskUserResponse;
import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.enums.RiskLevel;
import com.vanilo.psych.agent.repository.PsychologicalReportRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
risk 判定规则（非常重要）：
- high：只有在用户明确表达自杀、自伤、轻生意图、极端绝望（如“活着没有意义”、“我想结束生命”）时，才能判定为 high
- medium：用户存在明显焦虑、低落、压力大、失眠、情绪困扰，但未表达自伤或轻生倾向
- low：普通聊天、轻度情绪波动、日常抱怨、兴趣表达等

严格限制：
- 仅表达“焦虑、压力大、失眠、难过”等，必须判定为 medium，而不是 high
- 普通聊天内容（如兴趣、娱乐、日常交流）必须判定为 low
- 不要因为语气消极就提高 risk 等级
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
    public List<ReportSummaryResponse> getRecentReports(Long userId,int limit) {
        if(limit <= 0||limit>50){
            limit=20;
        }
        List<Object[]> rows=psychologicalReportRepository.findRecentReportsByUserId(userId,limit);
        if(rows==null || rows.isEmpty()){
            throw new RuntimeException("rows are empty");
        }
        return rows.stream().map(
                row->new ReportSummaryResponse(
                        ((Number)row[0]).longValue(),
                        (String)row[1],
                        (String)row[2],
                        (String) row[3],
                        ((Number)row[4]).doubleValue(),
                        ((java.sql.Timestamp)row[5]).toLocalDateTime()
                )
        ).collect(Collectors.toList());
    }
    public List<TopRiskUserResponse> getTopRiskUsers(int limit) {
        if(limit <= 0||limit>50){
            limit=20;
        }
        List<Object[]> rows=psychologicalReportRepository.findTopRiskUsers(limit);
        if(rows==null || rows.isEmpty()){
            return Collections.emptyList();
        }
        return rows.stream().map(
                row->new TopRiskUserResponse(
                        row[0]==null?null:((Number)row[0]).longValue(),
                        ((Number)row[1]).longValue()
                )
        ).toList();
    }
}
