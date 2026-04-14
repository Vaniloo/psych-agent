package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RuleBasedRerankService implements RerankService {
    @Override
    public List<KnowledgeSearchResponse> rerank(
            String query,
            String category,
            List<KnowledgeSearchResponse> results
    ){
        if(results==null||results.isEmpty()){
            return results;
        }
        return results.stream().sorted(
                (a,b)->{
                    int scoreA=calculateRerankScore(query,category,a);
                    int scoreB=calculateRerankScore(query,category,b);
                    return Integer.compare(scoreB,scoreA);
                }

        ).collect(Collectors.toList());
    }
    private int calculateRerankScore(
            String query,
            String category,
            KnowledgeSearchResponse item
    ){
        int score=0;
        if(category!=null&&!category.isBlank()){
            if(item.getCategory()!=null&&!item.getCategory().isBlank()){
                if(item.getCategory().equals(category)){
                    score+=20;
                }
            }
        }
        String content=item.getContent();
        if(content!=null&&!content.isBlank()){
            if(content.contains(query)){
                score+=50;
            }
            for(int i=0;i<query.length()-1;i++){
                if(content.contains(query.substring(i,i+2))){
                    score+=2;
                }
            }
        }

        return score;
    }
}
