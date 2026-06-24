package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserMemoryToolResponse {
    private String username;
    private String summary;
    private String longTermMemory;
    private LocalDateTime updatedAt;
    private UserProfileResponse profile;
    private List<ConversationSessionResponse> sessions;
}
