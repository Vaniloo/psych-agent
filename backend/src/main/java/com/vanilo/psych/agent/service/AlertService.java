package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.entity.PsychologicalReport;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    public void sendHighRiskAlert(PsychologicalReport report){
        System.out.println("风险："+ report.getRisk());
        System.out.println("情感："+ report.getEmotion());
        System.out.println("信息："+ report.getMessage());
        System.out.println("置信度："+report.getConfidence().toString());
        System.out.println("创建时间："+ report.getCreatedAt().toString());
    }
}
