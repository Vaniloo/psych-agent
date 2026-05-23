package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import com.vanilo.psych.agent.repository.UserRepository;
import com.vanilo.psych.agent.service.MailAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    private final MailAlertService mailAlertService;
    private final UserRepository userRepository;

    public TestController(MailAlertService mailAlertService, UserRepository userRepository) {
        this.mailAlertService = mailAlertService;
        this.userRepository = userRepository;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
    @GetMapping("/db")
    public String db() {
        return "db ok, users=" + userRepository.count();
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
