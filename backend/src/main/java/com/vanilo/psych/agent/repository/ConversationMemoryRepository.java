package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.ConversationMemory;
import com.vanilo.psych.agent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationMemoryRepository extends JpaRepository<ConversationMemory, Long> {
    Optional<ConversationMemory> findByUser(User user);
}
