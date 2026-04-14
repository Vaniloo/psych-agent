package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.dto.RerankRequest;
import com.vanilo.psych.agent.dto.RerankRequestItem;
import com.vanilo.psych.agent.dto.RerankResponseItem;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class LocalModelRerankService implements RerankService {
    private static final String RERANK_URL="http://localhost:9001/rerank";
    private final RestTemplate restTemplate=new RestTemplate();
    @Override
    public List<KnowledgeSearchResponse> rerank(
            String query,
            String category,
            List<KnowledgeSearchResponse> candidates
    ){
        if(candidates==null || candidates.isEmpty()){
            return candidates;
        }
        try {
            List<RerankRequestItem> items=candidates.stream().map(candidate-> new RerankRequestItem(
                    candidate.getId(),
                    candidate.getContent(),
                    candidate.getCategory(),
                    candidate.getSource()
            )).toList();
            RerankRequest request=new RerankRequest(query,items);
            RerankResponseItem[] response=restTemplate.postForObject(
                    RERANK_URL,
                    request,
                    RerankResponseItem[].class

            );
            if(response==null || response.length==0){
                throw new RuntimeException("No response from Rerank");
            }
            Map<String,Double> scoreMap=new HashMap<>();
            for(RerankResponseItem item:response){
                scoreMap.put(item.getId(), item.getScore());
            }
            return candidates.stream().sorted((a,b)->{
                Double aScore=scoreMap.get(a.getId());
                Double bScore=scoreMap.get(b.getId());
                return Double.compare(bScore,aScore);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new UnsupportedOperationException("Local model reranker is not integrated yet",e);
        }

    }
}
