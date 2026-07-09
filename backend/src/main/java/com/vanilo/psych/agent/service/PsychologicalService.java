package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.AnalysisResult;
import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.dto.EmotionTrendResponse;
import com.vanilo.psych.agent.dto.ReportSummaryResponse;
import com.vanilo.psych.agent.dto.RiskCountResponse;
import com.vanilo.psych.agent.dto.TopRiskUserResponse;
import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import com.vanilo.psych.agent.enums.RiskLevel;
import com.vanilo.psych.agent.repository.PsychologicalReportRepository;
import com.vanilo.psych.agent.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class PsychologicalService {
    private static final String ANALYSIS_PROMPT = """
            你是一个心理状态分析助手。请根据用户输入分析其心理风险等级和主要情绪。

            要求：
            1. 只返回 JSON，不要返回解释、前后缀、Markdown 或代码块
            2. risk 只能是 high、medium、low
            3. emotion 使用简洁中文词语
            4. confidence 是 0 到 1 之间的小数
            5. 只有明确自杀、自伤、轻生意图或极端绝望才能判定 high
            6. 焦虑、压力、失眠、低落但无自伤倾向判定 medium
            7. 普通聊天、轻度波动和日常抱怨判定 low

            返回格式：
            {"risk":"high|medium|low","emotion":"...","confidence":0.xx}
            """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final PsychologicalReportRepository reportRepository;
    private final AlertService alertService;
    private final UserRepository userRepository;

    public PsychologicalService(LlmService llmService,
                                ObjectMapper objectMapper,
                                PsychologicalReportRepository reportRepository,
                                AlertService alertService,
                                UserRepository userRepository) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.reportRepository = reportRepository;
        this.alertService = alertService;
        this.userRepository = userRepository;
    }

    public AnalysisResult scan(String message) {
        String response = llmService.complete(ANALYSIS_PROMPT, message);
        int start = response == null ? -1 : response.indexOf('{');
        int end = response == null ? -1 : response.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new RuntimeException("未返回合法JSON");
        }
        String json = response.substring(start, end + 1);
        try {
            return objectMapper.readValue(json, AnalysisResult.class);
        } catch (Exception exception) {
            throw new RuntimeException("分析结果解析失败", exception);
        }
    }

    public AnalyzeResponse analyze(String message, String username) {
        return saveAnalysis(message, username, scan(message), true);
    }

    public AnalyzeResponse analyzeHighRisk(String message, String username) {
        return saveAnalysis(message, username, new AnalysisResult("high", "危机", 1.0), false);
    }

    private AnalyzeResponse saveAnalysis(String message,
                                         String username,
                                         AnalysisResult analysis,
                                         boolean sendLegacyAlert) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        PsychologicalReport report = new PsychologicalReport();
        report.setUser(user);
        report.setRisk(analysis.getRisk());
        report.setEmotion(analysis.getEmotion());
        report.setConfidence(analysis.getConfidence());
        report.setMessage(message);
        report.setCreatedAt(LocalDateTime.now());
        reportRepository.save(report);

        if (sendLegacyAlert && RiskLevel.fromString(analysis.getRisk()) == RiskLevel.HIGH) {
            alertService.sendHighRiskAlert(report);
        }
        return new AnalyzeResponse(
                report.getId(), analysis.getRisk(), analysis.getEmotion(), analysis.getConfidence()
        );
    }

    public List<ReportSummaryResponse> getRecentReports(Long userId, int limit) {
        int safeLimit = limit <= 0 || limit > 50 ? 20 : limit;
        List<Object[]> rows = reportRepository.findRecentReportsByUserId(userId, safeLimit);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> new ReportSummaryResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                ((Number) row[4]).doubleValue(),
                ((java.sql.Timestamp) row[5]).toLocalDateTime()
        )).toList();
    }

    public List<TopRiskUserResponse> getTopRiskUsers(int limit) {
        int safeLimit = limit <= 0 || limit > 50 ? 20 : limit;
        List<Object[]> rows = reportRepository.findTopRiskUsers(safeLimit);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> new TopRiskUserResponse(
                row[0] == null ? null : ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue()
        )).toList();
    }

    public List<RiskCountResponse> getRiskDistribution(Long userId) {
        return reportRepository.findRiskDistributionByUserId(userId)
                .stream()
                .map(row -> new RiskCountResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public List<RiskCountResponse> getRiskDistribution() {
        return reportRepository.findRiskDistribution()
                .stream()
                .map(row -> new RiskCountResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public List<EmotionTrendResponse> getEmotionTrend(Long userId) {
        return reportRepository.findEmotionTrendByUserId(userId)
                .stream()
                .map(row -> new EmotionTrendResponse(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue()
                ))
                .toList();
    }

    public List<EmotionTrendResponse> getEmotionTrend() {
        return reportRepository.findEmotionTrend()
                .stream()
                .map(row -> new EmotionTrendResponse(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue()
                ))
                .toList();
    }
}
