package com.vanilo.psych.agent.dto;

import java.time.LocalDateTime;

public record FeedbackMessageResponse(
        Long id,
        String username,
        String content,
        String status,
        String reply,
        LocalDateTime repliedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}