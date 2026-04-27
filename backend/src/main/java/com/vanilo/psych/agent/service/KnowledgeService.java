package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.KnowledgeAddRequest;
import com.vanilo.psych.agent.dto.KnowledgeImportRequest;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class KnowledgeService {
    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository repository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeLockService knowledgeLockService;
    private final TextChunkService textChunkService;
    private final RerankService rerankService;


    public KnowledgeService(VectorStore vectorStore, KnowledgeDocumentRepository repository, StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, KnowledgeLockService knowledgeLockService, TextChunkService textChunkService,RerankService rerankService) {
        this.vectorStore = vectorStore;
        this.repository = repository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeLockService = knowledgeLockService;
        this.textChunkService = textChunkService;
        this.rerankService = rerankService;
    }
    public void addKnowledge(KnowledgeAddRequest knowledgeAddRequest){
        if(knowledgeAddRequest.getContent() == null||knowledgeAddRequest.getContent().isBlank()){
            throw new RuntimeException("content不能为空");
        }
        String content = knowledgeAddRequest.getContent();
        boolean locked= knowledgeLockService.tryAddLock(content);
        if (locked) {
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
            clearRagCache();
        }
        else{
            System.out.println("duplicate knowledge add skipped");
        }
    }
    public List<KnowledgeSearchResponse> searchKnowledge(String query,String category){
        if(query==null||query.isBlank()){
            throw new RuntimeException("query不得为空");
        }
        String cacheKey;
        if (category!=null&&!category.isBlank()){
            cacheKey="rag:"+category+":"+query;
        }
        else{
            cacheKey="rag:all:"+query;
        }
        String cached=stringRedisTemplate.opsForValue().get(cacheKey);
        if(cached!=null){
            try {
                System.out.println("cache hit:"+query);
                return objectMapper.readValue(cached,new TypeReference<List<KnowledgeSearchResponse>>(){});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("cache miss");
        SearchRequest.Builder builder=SearchRequest.builder();
        builder.query(query);
        builder.topK(10);
        if (category!=null&&!category.isBlank()){
            builder.filterExpression("category == '"+category+"'");
        }
        List<Document> documentList=vectorStore.similaritySearch(
                builder.build()
        );
        List<KnowledgeSearchResponse> searchResults = documentList.stream().map(
                doc->new KnowledgeSearchResponse(
                doc.getText(),
                doc.getMetadata().get("category")==null?"default":doc.getMetadata().get("category").toString(),
                doc.getMetadata().get("source")==null?"unknown":doc.getMetadata().get("source").toString(),
                doc.getId()
            )
        ).toList();
        List<KnowledgeSearchResponse> rerankedResults=rerankService.rerank(query,category,searchResults);
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(rerankedResults),
                    Duration.ofMinutes(10)
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return rerankedResults;
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
        clearRagCache();
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
    public List<KnowledgeSearchResponse> searchKnowledge(String query){
        return searchKnowledge(query,null);
    }
    public void importDocument(KnowledgeImportRequest knowledgeImportRequest){
        if(knowledgeImportRequest==null||knowledgeImportRequest.getContent()==null||knowledgeImportRequest.getContent().isBlank()){
            throw new RuntimeException("内容不能为空");
        }
        String id= UUID.randomUUID().toString();
        String content = knowledgeImportRequest.getContent();
        String category = knowledgeImportRequest.getCategory()==null?"default":knowledgeImportRequest.getCategory();
        String source = knowledgeImportRequest.getSource()==null?"unknown":knowledgeImportRequest.getSource();
        KnowledgeDocument doc=new KnowledgeDocument(
                id,content,category, source, LocalDateTime.now()
        );
        repository.save(doc);
        List<String> chunks =textChunkService.splitText(knowledgeImportRequest.getContent(),
                knowledgeImportRequest.getChunkSize()==null?400:knowledgeImportRequest.getChunkSize(),
                knowledgeImportRequest.getOverlap()==null?80:knowledgeImportRequest.getOverlap());
        List<Document> docs=new ArrayList<>();
        for(int i=0;i<chunks.size();i++){
            Map<String,Object> metaMap=new HashMap<>();
            metaMap.put("documentId",id);
            metaMap.put("chunkIndex",i);
            metaMap.put("category",category);
            metaMap.put("source",source);
            String chunk=chunks.get(i);
            String chunkId=id+"_chunk_"+i;
            Document docu=new Document(chunkId,chunk,metaMap);
            docs.add(docu);

        }
        vectorStore.add(docs);
        clearRagCache();

    }
    private void clearRagCache(){
        Set<String> keys = stringRedisTemplate.keys("rag:*");
        if(keys!=null&&!keys.isEmpty()){
            stringRedisTemplate.delete(keys);
        }
    }

}
