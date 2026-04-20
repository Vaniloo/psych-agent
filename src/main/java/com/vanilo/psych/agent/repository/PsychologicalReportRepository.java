package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Objects;



public interface PsychologicalReportRepository extends JpaRepository<PsychologicalReport, Long> {
    List<PsychologicalReport> findByUser(User user);
    @Query(value = """
    SELECT id, message, risk, emotion, confidence, created_at
    FROM psychological_reports
    WHERE user_id = :userId
    ORDER BY created_at DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findRecentReportsByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    @Query(value = """
    SELECT user_id,COUNT(*) as cnt
    FROM psychological_reports
    WHERE risk='high'
    GROUP BY user_id
    ORDER BY cnt DESC 
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]>findTopRiskUsers(@Param("limit") int limit);
    @Query(
            value= """
    SELECT id
    FROM users
    WHERE username= :username
    """,nativeQuery = true
    )
    Long findIdByUserId(@Param("username") String username);
}
