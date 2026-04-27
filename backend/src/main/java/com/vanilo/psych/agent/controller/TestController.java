package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.service.MailAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    private final MailAlertService mailAlertService;
    public TestController(MailAlertService mailAlertService) {
        this.mailAlertService = mailAlertService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
    @PostMapping("/mail")
    public String testMail() {
        AnalyzeResponse response = new AnalyzeResponse();
        response.setRisk("high");
        response.setEmotion("低落");
        response.setConfidence(1.0);

        mailAlertService.sendHighRiskEmail("testUser", "测试高风险消息", response);

        return "ok";
    }
}
