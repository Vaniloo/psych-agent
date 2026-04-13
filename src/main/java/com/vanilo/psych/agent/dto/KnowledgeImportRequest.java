package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeImportRequest {
    private String content;
    private String category;
    private String source;
    private Integer chunkSize;
    private Integer overlap;
}
