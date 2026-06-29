package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.RiskAlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAlertEventRepository extends JpaRepository<RiskAlertEvent, Long> {
    List<RiskAlertEvent> findTop20ByStatusAndAttemptsLessThanOrderByCreatedAtAsc(String status, int attempts);
}
