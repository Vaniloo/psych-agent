package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class FallbackRerankService implements RerankService {

    private final LocalModelRerankService localModelRerankService;
    private final RuleBasedRerankService ruleBasedRerankService;

    public FallbackRerankService(LocalModelRerankService localModelRerankService, RuleBasedRerankService ruleBasedRerankService) {
        this.localModelRerankService = localModelRerankService;
        this.ruleBasedRerankService = ruleBasedRerankService;
    }

    @Override
    public List<KnowledgeSearchResponse> rerank(
            String query,
            String category,
            List<KnowledgeSearchResponse> candidates
    ){
        try{
            List<KnowledgeSearchResponse> rerankedCandidates = localModelRerankService.rerank(query, category, candidates);
            return rerankedCandidates;
        }
        catch(Exception e){
            try {
                List<KnowledgeSearchResponse> rerankedCandidates=ruleBasedRerankService.rerank(query, category, candidates);
                return rerankedCandidates;
            } catch (Exception ex) {
                throw new RuntimeException("Fallback Rerank Service failed",ex);
            }
        }
    }
}
