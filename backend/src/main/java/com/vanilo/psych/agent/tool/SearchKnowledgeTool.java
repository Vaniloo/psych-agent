package com.vanilo.psych.agent.tool;

import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.dto.ToolInfoResponse;
import com.vanilo.psych.agent.dto.ToolParameterInfo;
import com.vanilo.psych.agent.service.KnowledgeService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SearchKnowledgeTool implements ToolExecutor{
    private final KnowledgeService knowledgeService;

    public SearchKnowledgeTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Override
    public String getName(){
        return "search_knowledge";
    }
    @Override
    public ToolInfoResponse getToolInfo(){
        return new ToolInfoResponse(
                getName(),
                "在心理知识库中搜索相关内容",
                List.of(
                        new ToolParameterInfo("query","string",true,"用户的问题"),
                        new ToolParameterInfo("category","string",false,"知识分类，如anxiety")
                )
        );
    }
    @Override
    public List<KnowledgeSearchResponse> execute(Map<String, Object> arguments){
        String query = arguments.get("query")==null?null:arguments.get("query").toString();
        if(query == null||query.isBlank()){
            throw new RuntimeException("query不能为空");
        }
        String category = arguments.get("category")==null?null:arguments.get("category").toString();
        return knowledgeService.searchKnowledge(query,category);
    }

}
