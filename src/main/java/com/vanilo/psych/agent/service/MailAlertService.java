package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.AnalyzeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailAlertService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;
    public MailAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    public void sendHighRiskEmail(String username, String message, AnalyzeResponse response) {
        SimpleMailMessage mail=new SimpleMailMessage();
        mail.setTo(from);
        mail.setFrom(from);
        mail.setSubject("警告");
        mail.setText(
                "用户：" + username + "\n" +
                        "内容：" + message + "\n" +
                        "风险等级：" + response.getRisk() + "\n" +
                        "情绪：" + response.getEmotion() + "\n" +
                        "置信度：" + response.getConfidence()
        );
        mailSender.send(mail);
    }


}
