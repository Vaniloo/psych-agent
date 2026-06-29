package com.vanilo.psych.agent.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskDetectionService {
    private static final List<String> HIGH_RISK_PHRASES = List.of(
            "想死", "不想活", "自杀", "轻生", "结束自己", "活着没意义",
            "伤害自己", "割腕", "跳楼", "服药自杀", "准备去死"
    );

    public boolean isHighRisk(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return HIGH_RISK_PHRASES.stream().anyMatch(message::contains);
    }
}
