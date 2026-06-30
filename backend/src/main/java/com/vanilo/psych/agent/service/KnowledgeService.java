package com.vanilo.psych.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilo.psych.agent.dto.KnowledgeAddRequest;
import com.vanilo.psych.agent.dto.KnowledgeImportRequest;
import com.vanilo.psych.agent.dto.KnowledgeSearchResponse;
import com.vanilo.psych.agent.entity.KnowledgeDocument;
import com.vanilo.psych.agent.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


@Service
public class KnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final String RAG_CACHE_VERSION_KEY = "rag:cache:version";
    private static final int DEFAULT_RECALL_LIMIT = 18;
    private static final int DEFAULT_RESULT_LIMIT = 6;
    private static final int MAX_RECALL_LIMIT = 30;
    private static final int MAX_RESULT_LIMIT = 10;

    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository repository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeLockService knowledgeLockService;
    private final TextChunkService textChunkService;
    private final RerankService rerankService;


    public KnowledgeService(VectorStore vectorStore,
                            KnowledgeDocumentRepository repository,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper,
                            KnowledgeLockService knowledgeLockService,
                            TextChunkService textChunkService,
                            RerankService rerankService) {
        this.vectorStore = vectorStore;
        this.repository = repository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeLockService = knowledgeLockService;
        this.textChunkService = textChunkService;
        this.rerankService = rerankService;
    }
    public void addKnowledge(KnowledgeAddRequest knowledgeAddRequest) {
        if (knowledgeAddRequest == null
                || knowledgeAddRequest.getContent() == null
                || knowledgeAddRequest.getContent().isBlank()) {
            throw new RuntimeException("content不能为空");
        }
        String content = knowledgeAddRequest.getContent();
        Optional<KnowledgeDocument> existing = repository.findFirstByContent(content);
        if (existing.isPresent()) {
            vectorStore.add(List.of(toVectorDocument(existing.get())));
            clearRagCache();
            return;
        }

        if (!knowledgeLockService.tryAddLock(content)) {
            return;
        }
        String id = UUID.randomUUID().toString();
        String category = knowledgeAddRequest.getCategory() == null ? "default" : knowledgeAddRequest.getCategory();
        String source = knowledgeAddRequest.getSource() == null ? "unknown" : knowledgeAddRequest.getSource();
        KnowledgeDocument document = new KnowledgeDocument(id, content, category, source, LocalDateTime.now());
        Document vectorDocument = toVectorDocument(document);
        vectorStore.add(List.of(vectorDocument));
        try {
            repository.save(document);
        } catch (RuntimeException exception) {
            vectorStore.delete(List.of(id));
            throw exception;
        }
        clearRagCache();
    }
    public List<KnowledgeSearchResponse> searchKnowledge(String query,String category){
        return searchKnowledgeInternal(query, category, DEFAULT_RESULT_LIMIT);
    }
    public void deleteKnowledge(String id){
        if(id==null||id.isBlank()){
            throw new RuntimeException("id不能为空");
        }
        if(!repository.existsById(id)){
            throw new RuntimeException("id="+id+"不存在");
        }
        vectorStore.delete(List.of(id));
        repository.deleteById(id);
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

    public List<KnowledgeSearchResponse> searchKnowledge(String query, String category, Integer limit) {
        int safeLimit = normalizeLimit(limit, DEFAULT_RESULT_LIMIT, MAX_RESULT_LIMIT);
        return searchKnowledgeInternal(query, category, safeLimit);
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
        try {
            repository.save(doc);
        } catch (RuntimeException exception) {
            vectorStore.delete(docs.stream().map(Document::getId).toList());
            throw exception;
        }
        clearRagCache();

    }

    public int reindexBySource(String source) {
        if (source == null || source.isBlank()) {
            throw new RuntimeException("source不能为空");
        }
        List<KnowledgeDocument> documents = repository.findBySource(source);
        if (documents.isEmpty()) {
            return 0;
        }
        vectorStore.add(documents.stream().map(this::toVectorDocument).toList());
        clearRagCache();
        return documents.size();
    }

    private Document toVectorDocument(KnowledgeDocument document) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", document.getId());
        metadata.put("category", document.getCategory());
        metadata.put("source", document.getSource());
        return Document.builder()
                .id(document.getId())
                .text(document.getContent())
                .metadata(metadata)
                .build();
    }
    private void clearRagCache() {
        try {
            stringRedisTemplate.opsForValue().increment(RAG_CACHE_VERSION_KEY);
        } catch (RuntimeException exception) {
            log.debug("Unable to invalidate RAG cache; retrieval will continue without Redis", exception);
        }
    }

    private List<KnowledgeSearchResponse> searchKnowledgeInternal(String query, String category, int resultLimit) {
        if(query==null||query.isBlank()){
            throw new RuntimeException("query不得为空");
        }
        String normalizedQuery = normalizeQuery(query);
        String normalizedCategory = normalizeCategory(category);
        String cacheKey = "rag:v3:%s:%s:%d:%s".formatted(
                readCacheVersion(),
                normalizedCategory == null ? "all" : normalizedCategory,
                resultLimit,
                normalizedQuery
        );
        List<KnowledgeSearchResponse> cached = readCachedResults(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<KnowledgeSearchResponse> candidates = recall(normalizedQuery, normalizedCategory);
        if (candidates.isEmpty()) {
            candidates = keywordFallback(normalizedQuery, normalizedCategory);
        }
        List<KnowledgeSearchResponse> results = prepareBusinessResults(
                normalizedQuery,
                normalizedCategory,
                candidates,
                resultLimit
        );
        writeCachedResults(cacheKey, results);
        return results;
    }

    private String readCacheVersion() {
        try {
            String version = stringRedisTemplate.opsForValue().get(RAG_CACHE_VERSION_KEY);
            return version == null ? "0" : version;
        } catch (RuntimeException exception) {
            log.debug("Unable to read RAG cache version; retrieval will continue without Redis", exception);
            return "offline";
        }
    }

    private List<KnowledgeSearchResponse> readCachedResults(String cacheKey) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            return cached == null
                    ? null
                    : objectMapper.readValue(cached, new TypeReference<List<KnowledgeSearchResponse>>() {});
        } catch (RuntimeException | JsonProcessingException exception) {
            log.debug("Unable to read RAG cache key {}", cacheKey, exception);
            return null;
        }
    }

    private void writeCachedResults(String cacheKey, List<KnowledgeSearchResponse> results) {
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(results),
                    Duration.ofMinutes(10)
            );
        } catch (RuntimeException | JsonProcessingException exception) {
            log.debug("Unable to write RAG cache key {}", cacheKey, exception);
        }
    }

    private List<KnowledgeSearchResponse> recall(String query, String category) {
        try {
            SearchRequest.Builder builder=SearchRequest.builder();
            builder.query(query);
            builder.topK(normalizeLimit(DEFAULT_RECALL_LIMIT, DEFAULT_RECALL_LIMIT, MAX_RECALL_LIMIT));
            if (category!=null&&!category.isBlank()){
                builder.filterExpression("category == '" + escapeFilterValue(category) + "'");
            }
            List<Document> documentList=vectorStore.similaritySearch(builder.build());
            return toSearchResponses(documentList);
        } catch (Exception exception) {
            log.warn("Vector retrieval failed; falling back to MySQL keyword search: {}", exception.getMessage());
            return Collections.emptyList();
        }
    }

    private List<KnowledgeSearchResponse> toSearchResponses(List<Document> documentList) {
        if (documentList == null || documentList.isEmpty()) {
            return Collections.emptyList();
        }
        return documentList.stream().map(
                doc->new KnowledgeSearchResponse(
                        doc.getText(),
                        doc.getMetadata().get("category")==null?"default":doc.getMetadata().get("category").toString(),
                        doc.getMetadata().get("source")==null?"unknown":doc.getMetadata().get("source").toString(),
                        doc.getId()
                )
        ).toList();
    }

    private List<KnowledgeSearchResponse> keywordFallback(String query, String category) {
        List<String> terms = buildQueryTerms(query);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findAll()
                .stream()
                .filter(doc -> category == null || category.equals(doc.getCategory()))
                .filter(doc -> containsAnyTerm(doc.getContent(), terms))
                .map(doc -> {
                    KnowledgeSearchResponse response = new KnowledgeSearchResponse(
                            doc.getContent(),
                            doc.getCategory(),
                            doc.getSource(),
                            doc.getId()
                    );
                    response.setMatchReason("数据库关键词兜底");
                    return response;
                })
                .limit(DEFAULT_RECALL_LIMIT)
                .toList();
    }

    private List<KnowledgeSearchResponse> prepareBusinessResults(String query,
                                                                 String category,
                                                                 List<KnowledgeSearchResponse> candidates,
                                                                 int resultLimit) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeSearchResponse> deduped = deduplicate(candidates);
        List<KnowledgeSearchResponse> rerankedResults=rerankService.rerank(query,category,deduped);
        return rerankedResults.stream()
                .peek(item -> enrichRelevance(query, category, item))
                .limit(resultLimit)
                .toList();
    }

    private List<KnowledgeSearchResponse> deduplicate(List<KnowledgeSearchResponse> candidates) {
        Map<String, KnowledgeSearchResponse> result = new LinkedHashMap<>();
        for (KnowledgeSearchResponse item : candidates) {
            String key = item.getId() == null || item.getId().isBlank()
                    ? normalizeQuery(item.getContent())
                    : item.getId();
            result.putIfAbsent(key, item);
        }
        return new ArrayList<>(result.values());
    }

    private void enrichRelevance(String query, String category, KnowledgeSearchResponse item) {
        String content = item.getContent() == null ? "" : item.getContent();
        String safeQuery = query == null ? "" : query;
        double score = 0.35;
        if (!safeQuery.isBlank() && content.contains(safeQuery)) {
            score += 0.35;
        }
        if (category != null && category.equals(item.getCategory())) {
            score += 0.15;
        }
        int phraseHits = 0;
        for (int i = 0; i < safeQuery.length() - 1; i++) {
            if (content.contains(safeQuery.substring(i, i + 2))) {
                phraseHits++;
            }
        }
        score += Math.min(0.15, phraseHits * 0.02);
        item.setRelevanceScore(Math.min(1.0, score));
        item.setMatchReason(buildMatchReason(category, item, phraseHits));
    }

    private String buildMatchReason(String category, KnowledgeSearchResponse item, int phraseHits) {
        if (category != null && category.equals(item.getCategory())) {
            return "分类匹配，内容语义相关";
        }
        if (phraseHits > 0) {
            return "关键词片段匹配";
        }
        return "向量语义召回";
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.strip().replaceAll("\\s+", " ");
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.strip();
    }

    private List<String> buildQueryTerms(String query) {
        String safeQuery = normalizeQuery(query);
        if (safeQuery.isBlank()) {
            return Collections.emptyList();
        }
        List<String> terms = new ArrayList<>();
        terms.add(safeQuery);
        for (String part : safeQuery.split("[,，。！？\\s]+")) {
            if (part.length() >= 2) {
                terms.add(part);
            }
        }
        for (int i = 0; i < safeQuery.length() - 1; i++) {
            terms.add(safeQuery.substring(i, i + 2));
        }
        return terms.stream().distinct().toList();
    }

    private boolean containsAnyTerm(String content, List<String> terms) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return terms.stream().anyMatch(content::contains);
    }

    private int normalizeLimit(Integer limit, int defaultValue, int maxValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }

    private String escapeFilterValue(String value) {
        return value.replace("'", "\\'");
    }

}
