package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationMessageResponse {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
