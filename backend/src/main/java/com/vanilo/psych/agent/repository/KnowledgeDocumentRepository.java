package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    Optional<KnowledgeDocument> findFirstByContent(String content);

    List<KnowledgeDocument> findBySource(String source);
}
