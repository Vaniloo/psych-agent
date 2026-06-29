package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.entity.RiskAlertEvent;
import com.vanilo.psych.agent.repository.RiskAlertEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RiskAlertDispatcher {
    private final RiskAlertEventRepository repository;
    private final ExcelRecordService excelRecordService;
    private final MailAlertService mailAlertService;

    public RiskAlertDispatcher(RiskAlertEventRepository repository,
                               ExcelRecordService excelRecordService,
                               MailAlertService mailAlertService) {
        this.repository = repository;
        this.excelRecordService = excelRecordService;
        this.mailAlertService = mailAlertService;
    }

    @Async("alertExecutor")
    public void dispatch(Long eventId) {
        RiskAlertEvent event = repository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }
        AnalyzeResponse response = new AnalyzeResponse(
                event.getReportId(), event.getRisk(), event.getEmotion(), event.getConfidence()
        );
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                event.setStatus("PROCESSING");
                event.setAttempts(attempt);
                event.setUpdatedAt(LocalDateTime.now());
                repository.save(event);

                excelRecordService.appendHighRiskRecord(event.getUsername(), event.getMessage(), response);
                mailAlertService.sendHighRiskEmail(event.getUsername(), event.getMessage(), response);

                event.setStatus("SENT");
                event.setLastError(null);
                event.setUpdatedAt(LocalDateTime.now());
                repository.save(event);
                return;
            } catch (Exception e) {
                event.setStatus(attempt == 3 ? "FAILED" : "PENDING");
                event.setLastError(e.getMessage());
                event.setUpdatedAt(LocalDateTime.now());
                repository.save(event);
                if (attempt < 3) {
                    try {
                        Thread.sleep(attempt * 500L);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
