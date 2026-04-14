package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;


import java.util.List;

public interface RerankService {
    List<KnowledgeSearchResponse> rerank(
            String query,
            String category,
            List<KnowledgeSearchResponse> candidates
    );
}
