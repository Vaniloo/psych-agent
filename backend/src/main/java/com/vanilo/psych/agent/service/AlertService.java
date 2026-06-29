package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.entity.PsychologicalReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    public void sendHighRiskAlert(PsychologicalReport report) {
        Long userId = report.getUser() == null ? null : report.getUser().getId();
        log.warn("High-risk report created: reportId={}, userId={}, risk={}, confidence={}",
                report.getId(), userId, report.getRisk(), report.getConfidence());
    }
}
