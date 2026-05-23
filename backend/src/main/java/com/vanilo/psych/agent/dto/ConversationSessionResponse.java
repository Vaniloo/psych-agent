package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSessionResponse {
    private Long id;
    private String title;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
