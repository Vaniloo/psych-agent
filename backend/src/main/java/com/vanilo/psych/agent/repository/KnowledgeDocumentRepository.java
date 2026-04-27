package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
}
