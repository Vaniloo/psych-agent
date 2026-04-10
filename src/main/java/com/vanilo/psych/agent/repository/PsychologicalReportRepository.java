package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PsychologicalReportRepository extends JpaRepository<PsychologicalReport, Long> {
    List<PsychologicalReport> findByUser(User user);

}
