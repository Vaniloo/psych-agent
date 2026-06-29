package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.entity.RiskAlertEvent;
import com.vanilo.psych.agent.repository.RiskAlertEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CrisisWorkflowService {
    private final PsychologicalService psychologicalService;
    private final RiskAlertLockService riskAlertLockService;
    private final RiskAlertEventRepository riskAlertEventRepository;
    private final RiskAlertQueueService riskAlertQueueService;

    public CrisisWorkflowService(PsychologicalService psychologicalService,
                                 RiskAlertLockService riskAlertLockService,
                                 RiskAlertEventRepository riskAlertEventRepository,
                                 RiskAlertQueueService riskAlertQueueService) {
        this.psychologicalService = psychologicalService;
        this.riskAlertLockService = riskAlertLockService;
        this.riskAlertEventRepository = riskAlertEventRepository;
        this.riskAlertQueueService = riskAlertQueueService;
    }

    public AnalyzeResponse handle(String username, String message) {
        AnalyzeResponse response = psychologicalService.analyzeHighRisk(message, username);
        if (!riskAlertLockService.tryLock(username, message)) {
            return response;
        }
        RiskAlertEvent event = new RiskAlertEvent();
        LocalDateTime now = LocalDateTime.now();
        event.setReportId(response.getReportId());
        event.setUsername(username);
        event.setMessage(message);
        event.setRisk(response.getRisk());
        event.setEmotion(response.getEmotion());
        event.setConfidence(response.getConfidence());
        event.setStatus("PENDING");
        event.setAttempts(0);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event = riskAlertEventRepository.save(event);
        riskAlertQueueService.publish(event.getId());
        return response;
    }
}
