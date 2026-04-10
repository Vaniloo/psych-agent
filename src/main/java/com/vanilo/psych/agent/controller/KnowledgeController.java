package com.vanilo.psych.agent.controller;

import com.vanilo.psych.agent.dto.KnowledgeAddRequest;
import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.entity.KnowledgeDocument;
import com.vanilo.psych.agent.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {
    private KnowledgeService knowledgeService;
    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }
    @PostMapping("/add")
    public String addKnowledge(@RequestBody KnowledgeAddRequest knowledgeAddRequest){
        knowledgeService.addKnowledge(knowledgeAddRequest);
        return "ok";
    }
    @GetMapping("/search")
    public List<KnowledgeSearchResponse> searchKnowledge(@RequestParam("query") String query){
        return knowledgeService.searchKnowledge(query);
    }
    @DeleteMapping("/{id}")
    public String deleteKnowledge(@PathVariable String id){
        knowledgeService.deleteKnowledge(id);
        return "ok";
    }
    @GetMapping("/all")
    public List<KnowledgeDocument> listAllDocuments(){
        return knowledgeService.listAllDocuments();
    }
    @GetMapping("/page")
    public Page<KnowledgeDocument> listDocumentsByPage(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ){
        return knowledgeService.listDocumentsByPage(page, size);
    }

}
