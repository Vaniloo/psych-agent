package com.vanilo.psych.agent.repository;

import com.vanilo.psych.agent.entity.PsychologicalReport;
import com.vanilo.psych.agent.entity.User;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


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

    @Query(value = """
    SELECT risk, COUNT(*)
    FROM psychological_reports
    WHERE user_id = :userId
    GROUP BY risk
    """, nativeQuery = true)
    List<Object[]> findRiskDistributionByUserId(@Param("userId") Long userId);

    @Query(value = """
    SELECT risk, COUNT(*)
    FROM psychological_reports
    GROUP BY risk
    """, nativeQuery = true)
    List<Object[]> findRiskDistribution();

    @Query(value = """
    SELECT DATE(created_at), emotion, COUNT(*), AVG(confidence)
    FROM psychological_reports
    WHERE user_id = :userId
      AND created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
    GROUP BY DATE(created_at), emotion
    ORDER BY DATE(created_at) ASC
    """, nativeQuery = true)
    List<Object[]> findEmotionTrendByUserId(@Param("userId") Long userId);

    @Query(value = """
    SELECT DATE(created_at), emotion, COUNT(*), AVG(confidence)
    FROM psychological_reports
    WHERE created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
    GROUP BY DATE(created_at), emotion
    ORDER BY DATE(created_at) ASC
    """, nativeQuery = true)
    List<Object[]> findEmotionTrend();
}
