package com.vanilo.psych.agent.service;

import com.vanilo.psych.agent.dto.KnowledgeAddRequest;
import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.entity.KnowledgeDocument;
import com.vanilo.psych.agent.repository.KnowledgeDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class KnowledgeService {
    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository repository;
    public KnowledgeService(VectorStore vectorStore, KnowledgeDocumentRepository repository) {
        this.vectorStore = vectorStore;
        this.repository = repository;
    }
    public void addKnowledge(KnowledgeAddRequest knowledgeAddRequest){
        if(knowledgeAddRequest.getContent() == null||knowledgeAddRequest.getContent().isBlank()){
            throw new RuntimeException("content不能为空");
        }
        String content = knowledgeAddRequest.getContent();
        List<KnowledgeSearchResponse> searchResults = searchKnowledge(content);
        String checkContent = searchResults.isEmpty() ? null : searchResults.get(0).getContent();
        if(checkContent!=null&&checkContent.equals(content)){
            return;
        }
        String id= UUID.randomUUID().toString();
        String category = knowledgeAddRequest.getCategory()==null?"default":knowledgeAddRequest.getCategory();
        String source = knowledgeAddRequest.getSource()==null?"unknown":knowledgeAddRequest.getSource();
        KnowledgeDocument doc=new KnowledgeDocument(
          id,content,category, source, LocalDateTime.now()
        );
        repository.save(doc);
        Map<String,Object> metadata = new HashMap<>();
        metadata.put("id",id);
        metadata.put("category",category);
        metadata.put("source",source);
        Document document = Document.builder()
                .id(id)
                .text(content)
                .metadata(metadata)
                .build();
        vectorStore.add(List.of(document));
    }
    public List<KnowledgeSearchResponse> searchKnowledge(String query){
        if(query==null||query.isBlank()){
            throw new RuntimeException("query不得为空");
        }
        List<Document> documentList=vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(3)
                        .build()
        );
        return documentList.stream().map(
                doc-> new KnowledgeSearchResponse(
                        doc.getText(),
                        doc.getMetadata().get("category")==null?"default":doc.getMetadata().get("category").toString(),
                        doc.getMetadata().get("source")==null?"unknown":doc.getMetadata().get("source").toString(),
                        doc.getMetadata().get("id")==null?null:doc.getMetadata().get("id").toString()
                )
        ).toList();
    }
    public void deleteKnowledge(String id){
        if(id==null||id.isBlank()){
            throw new RuntimeException("id不能为空");
        }
        if(!repository.existsById(id)){
            throw new RuntimeException("id="+id+"不存在");
        }
        repository.deleteById(id);
        vectorStore.delete(List.of(id));
    }
    public List<KnowledgeDocument> listAllDocuments(){
        return repository.findAll();
    }
    public Page<KnowledgeDocument> listDocumentsByPage(int page, int size){
        if(page<0){
            page=0;
        }
        if(size<=0||size>50){
            size=10;
        }
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdAt").descending());
        return repository.findAll(pageable);
    }

}
